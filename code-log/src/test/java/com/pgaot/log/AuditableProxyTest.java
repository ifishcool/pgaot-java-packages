package com.pgaot.log;

import com.pgaot.log.annotation.Auditable;
import com.pgaot.log.api.LogContext;
import com.pgaot.log.core.AuditableProxy;

class AuditableProxyTest {

    @Auditable(action = "UPDATE", tableName = "scores")
    void updateScore(Long id) {}

    @org.junit.jupiter.api.Test
    void shouldInvokeAndLogAudit() {
        LogContext.init("alice", "Alice", "t1");
        AuditableProxy.invoke(this, "updateScore",
                () -> null, "{\"score\":95}", "{\"score\":100}");
        LogContext.clear();
    }
}
