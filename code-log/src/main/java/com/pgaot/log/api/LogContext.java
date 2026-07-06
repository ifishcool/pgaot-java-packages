package com.pgaot.log.api;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * 日志上下文 — traceId/userId/tenantId 的 ThreadLocal + MDC 双写.
 *
 * <p>用法:
 * <pre>{@code
 * LogContext.init("alice", "Alice", "tenant-1");
 * LogContext.info("用户 {} 执行了操作", "alice");  // 自动带 traceId
 * LogContext.clear();
 * }</pre>
 */
public final class LogContext {

    public static final String KEY_TRACE_ID  = "traceId";
    public static final String KEY_USER_ID   = "userId";
    public static final String KEY_TENANT_ID = "tenantId";

    private static final ThreadLocal<String> TRACE_ID  = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ID   = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_NAME = new ThreadLocal<>();
    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();

    private LogContext() {}

    /** 初始化上下文（通常由 Filter 调用） */
    public static void init(String userId, String userName, String tenantId) {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        TRACE_ID.set(traceId);
        USER_ID.set(userId);
        USER_NAME.set(userName);
        TENANT_ID.set(tenantId);
        MDC.put(KEY_TRACE_ID, traceId);
        MDC.put(KEY_USER_ID, userId);
        MDC.put(KEY_TENANT_ID, tenantId);
    }

    /** 仅设 traceId（非 HTTP 场景如定时任务） */
    public static void initTrace() {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        TRACE_ID.set(traceId);
        MDC.put(KEY_TRACE_ID, traceId);
    }

    /** 清理（请求结束时调用） */
    public static void clear() {
        TRACE_ID.remove();
        USER_ID.remove();
        USER_NAME.remove();
        TENANT_ID.remove();
        MDC.clear();
    }

    public static String getTraceId()  { return TRACE_ID.get(); }
    public static String getUserId()   { return USER_ID.get(); }
    public static String getUserName() { return USER_NAME.get(); }
    public static String getTenantId() { return TENANT_ID.get(); }

    public static boolean isInitialized() { return getTraceId() != null; }
}
