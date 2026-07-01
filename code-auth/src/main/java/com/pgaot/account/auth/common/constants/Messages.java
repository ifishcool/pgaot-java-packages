package com.pgaot.account.auth.common.constants;

/**
 * 错误消息常量 — 全部硬编码字符串统一管理.
 *
 * <p>分类: ENV=环境变量, AUTH=认证, TOKEN=Token, STRATEGY=策略, REDIS=缓存
 */
public final class Messages {
    private Messages() {}

    // ===== 环境变量 =====
    public static final String ENV_MISSING = "缺少环境变量: ";

    // ===== 认证 =====
    public static final String AUTH_UNSUPPORTED_TYPE = "不支持的登录方式: ";
    public static final String AUTH_CODE_EMPTY = "code 不能为空";

    // ===== Token =====
    public static final String TOKEN_KICKED = "账号在其他设备登录";
    
    // ===== Redis =====
    public static final String REDIS_URI_EMPTY = "Redis URI 为空";
}
