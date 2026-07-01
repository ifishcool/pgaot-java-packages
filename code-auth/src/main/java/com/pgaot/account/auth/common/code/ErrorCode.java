package com.pgaot.account.auth.common.code;

/**
 * 错误码，按模块区间划分.
 * <pre>
 *   100xxx  认证模块
 *   200xxx  Token 模块
 * </pre>
 */
public enum ErrorCode {

    AUTH_FAILED(100001, "认证失败"),
    AUTH_UNSUPPORTED_TYPE(100002, "不支持的登录方式"),
    AUTH_CONFIG_MISSING(100004, "缺少配置"),

    TOKEN_INVALID(200001, "Token 无效或已过期"),
    TOKEN_KICKED(200003, "账号在其他设备登录");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) { this.code = code; this.message = message; }

    /** 错误码 */
    public int getCode() { return code; }

    /** 错误描述 */
    public String getMessage() { return message; }
}
