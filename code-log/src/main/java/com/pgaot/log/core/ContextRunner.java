package com.pgaot.log.core;

import com.pgaot.log.api.LogContext;

import java.util.concurrent.Callable;

/**
 * 上下文生命周期包装器 — 自动 init/clear，适配任意框架.
 *
 * <pre>{@code
 * // 包裹 Runnable
 * ContextRunner.run("alice", "Alice", "t1", () -> doWork());
 *
 * // 包裹 Callable（有返回值）
 * String result = ContextRunner.call("alice", "Alice", "t1", () -> fetchData());
 * }</pre>
 */
public final class ContextRunner {

    private ContextRunner() {}

    public static void run(String userId, String userName, String tenantId, Runnable task) {
        LogContext.init(userId, userName, tenantId);
        try { task.run(); } finally { LogContext.clear(); }
    }

    public static <T> T call(String userId, String userName, String tenantId, Callable<T> task) {
        LogContext.init(userId, userName, tenantId);
        try { return task.call(); }
        catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw new RuntimeException(e); }
        finally { LogContext.clear(); }
    }
}
