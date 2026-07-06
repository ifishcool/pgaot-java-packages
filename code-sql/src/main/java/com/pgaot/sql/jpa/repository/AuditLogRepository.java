package com.pgaot.sql.jpa.repository;

import com.pgaot.sql.api.JpaTemplate;
import com.pgaot.sql.jpa.entity.AuditLogEntity;

import java.util.List;

public class AuditLogRepository {

    private final JpaTemplate jpa;

    public AuditLogRepository(JpaTemplate jpa) { this.jpa = jpa; }

    public AuditLogEntity save(AuditLogEntity e) { jpa.save(e); return e; }

    public List<AuditLogEntity> listByUser(String userId, int limit) {
        return jpa.query("FROM AuditLogEntity WHERE userId = ?1 ORDER BY createdAt DESC",
                AuditLogEntity.class, userId).stream().limit(limit).toList();
    }

    public List<AuditLogEntity> listByTable(String tableName, int limit) {
        return jpa.query("FROM AuditLogEntity WHERE tableName = ?1 ORDER BY createdAt DESC",
                AuditLogEntity.class, tableName).stream().limit(limit).toList();
    }

    public List<AuditLogEntity> listByTraceId(String traceId) {
        return jpa.query("FROM AuditLogEntity WHERE traceId = ?1 ORDER BY createdAt DESC",
                AuditLogEntity.class, traceId);
    }
}
