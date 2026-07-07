package com.pgaot.web.proxy;

import com.pgaot.web.annotation.Auditable;
import com.pgaot.log.api.AuditLogger;
import com.pgaot.log.api.LogContext;
import com.pgaot.log.common.model.AuditEvent;

import java.lang.reflect.Method;

/**
 * @Auditable 注解代理 — 拦截方法调用并记录审计日志.
 */
public final class AuditableProxy {

    private AuditableProxy() {}

    /**
     * 查找方法上的 @Auditable 注解，记录审计.
     *
     * @param target     目标对象
     * @param methodName 方法名
     * @param task       业务逻辑
     * @param beforeData 变更前数据（JSON）
     * @param afterData  变更后数据（JSON）
     */
    public static <T> T invoke(Object target, String methodName,
                                Callable<T> task, String beforeData, String afterData) {
        Auditable auditable = findAnnotation(target, methodName);
        T result;
        try {
            result = task.call();
        } catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw new RuntimeException(e); }

        if (auditable != null && LogContext.isInitialized()) {
            AuditLogger.log(AuditEvent.builder()
                .userId(LogContext.getUserId())
                .userName(LogContext.getUserName())
                .tenantId(LogContext.getTenantId())
                .action(auditable.action())
                .tableName(auditable.tableName())
                .beforeData(beforeData)
                .afterData(afterData)
                .build());
        }
        return result;
    }

    /** 仅记录审计，不执行业务（适合审计由外部触发的场景） */
    public static void audit(Method method, String beforeData, String afterData) {
        Auditable auditable = method.getAnnotation(Auditable.class);
        if (auditable == null || !LogContext.isInitialized()) return;
        AuditLogger.log(AuditEvent.builder()
            .userId(LogContext.getUserId())
            .userName(LogContext.getUserName())
            .tenantId(LogContext.getTenantId())
            .action(auditable.action())
            .tableName(auditable.tableName())
            .beforeData(beforeData)
            .afterData(afterData)
            .build());
    }

    private static Auditable findAnnotation(Object target, String methodName) {
        for (Method m : target.getClass().getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                Auditable a = m.getAnnotation(Auditable.class);
                if (a != null) return a;
            }
        }
        return null;
    }

    // java.util.concurrent.Callable 本地定义以兼容
    @FunctionalInterface
    public interface Callable<T> { T call() throws Exception; }
}
