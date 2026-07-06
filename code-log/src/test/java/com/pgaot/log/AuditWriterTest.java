package com.pgaot.log;

import com.pgaot.log.api.AuditLogger;
import com.pgaot.log.api.LogContext;
import com.pgaot.log.common.model.AuditEvent;
import com.pgaot.log.core.AuditWriter;
import com.pgaot.sql.api.JpaTemplate;
import com.pgaot.sql.jpa.entity.AuditLogEntity;
import com.pgaot.sql.jpa.repository.AuditLogRepository;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
class AuditWriterTest {

    private static AuditLogRepository repo;

    @BeforeAll
    static void requireDb() {
        assumeTrue(EnvLoader.hasDb(), "跳过：需要数据库");
        JpaTemplate jpa = JpaTemplate.fromEnv("", true, AuditLogEntity.class);
        repo = new AuditLogRepository(jpa);
        AuditLogger.configure(new AuditWriter(repo));
    }

    @Test
    void shouldWriteAuditLog() {
        LogContext.init("alice", "Alice", "tenant-1");

        AuditLogger.log(AuditEvent.builder()
                .userId("alice")
                .userName("Alice")
                .tenantId("tenant-1")
                .action("UPDATE")
                .tableName("scores")
                .rowId(123L)
                .beforeData("{\"score\":95}")
                .afterData("{\"score\":100}")
                .remark("测试审计写入")
                .build());

        var logs = repo.listByUser("alice", 1);
        assertFalse(logs.isEmpty(), "审计日志应写入成功");
        assertEquals("UPDATE", logs.getFirst().getAction());
        assertNotNull(logs.getFirst().getTraceId());

        LogContext.clear();
    }
}
