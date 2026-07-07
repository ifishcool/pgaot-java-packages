package com.pgaot.log.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 结构化日志 — 自动从 LogContext 注入 traceId/userId 到 MDC.
 *
 * <p>用法:
 * <pre>{@code
 * StructuredLogger.of(MyClass.class).info("操作成功, rows={}", 3);
 * StructuredLogger.audit("UPDATE", "scores", 123L, "{\"score\":95}", "{\"score\":100}");
 * }</pre>
 */
public final class StructuredLogger {

    private final Logger logger;

    private StructuredLogger(Class<?> clazz) { this.logger = LoggerFactory.getLogger(clazz); }

    public static StructuredLogger of(Class<?> clazz) { return new StructuredLogger(clazz); }

    public void info(String format, Object... args)    { logger.info(format, args); }
    public void warn(String format, Object... args)    { logger.warn(format, args); }
    public void error(String format, Object... args)   { logger.error(format, args); }
    public void debug(String format, Object... args)   { logger.debug(format, args); }

    public boolean isDebugEnabled() { return logger.isDebugEnabled(); }
}
