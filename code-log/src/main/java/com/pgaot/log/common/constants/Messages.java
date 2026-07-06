package com.pgaot.log.common.constants;

public final class Messages {
    private Messages() {}

    public static final String CONTEXT_NOT_INITIALIZED = "LogContext 未初始化，请先调用 LogContext.init()";
    public static final String USER_ID_REQUIRED       = "审计日志需要 userId";
    public static final String ACTION_REQUIRED        = "审计日志需要 action";
}
