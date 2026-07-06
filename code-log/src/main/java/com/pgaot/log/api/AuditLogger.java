package com.pgaot.log.api;

import com.pgaot.log.common.model.AuditEvent;
import com.pgaot.log.core.AuditWriter;

/**
 * 审计日志入口 — 委托 AuditWriter 持久化.
 *
 * <pre>{@code
 * AuditLogger.log(AuditEvent.builder()
 *     .userId("alice").action("UPDATE").tableName("scores")
 *     .rowId(123L).beforeData("{\"score\":95}").afterData("{\"score\":100}")
 *     .build());
 * }</pre>
 */
public final class AuditLogger {

    private static volatile AuditWriter writer;

    private AuditLogger() {}

    /** 注入 Writer（通常初始化时调用一次） */
    public static void configure(AuditWriter auditWriter) {
        writer = auditWriter;
    }

    /** 写入审计日志 */
    public static void log(AuditEvent event) {
        if (writer == null) return; // 静默降级
        if (event.getTraceId() == null && LogContext.isInitialized()) {
            event.setTraceId(LogContext.getTraceId());
        }
        writer.write(event);
    }
}
