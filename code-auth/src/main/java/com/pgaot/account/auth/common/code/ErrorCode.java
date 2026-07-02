package com.pgaot.account.auth.common.code;

import java.util.HashSet;
import java.util.Set;

/**
 * 错误码枚举 — 实现 IResultCode 接口.
 *
 * <pre>
 * 编号段分配:
 *   10_001_xxx  认证模块
 *   10_002_xxx  Token 模块
 * </pre>
 */
public enum ErrorCode implements IResultCode {

    // ===== 认证模块 (10_001_xxx) =====
    AUTH_FAILED(10_001_001, "认证失败"),
    AUTH_UNSUPPORTED_TYPE(10_001_002, "不支持的登录方式"),
    AUTH_CONFIG_MISSING(10_001_004, "缺少配置"),

    // ===== Token 模块 (10_002_xxx) =====
    TOKEN_INVALID(10_002_001, "Token 无效或已过期"),
    TOKEN_KICKED(10_002_003, "账号在其他设备登录"),
    ;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) { this.code = code; this.message = message; }

    /** 检查错误码是否重复 */
    public static void main(String[] args) {
        Set<Integer> codes = new HashSet<>();
        for (ErrorCode ec : values()) {
            if (!codes.add(ec.code)) {
                throw new IllegalStateException("重复错误码: " + ec.code + " → " + ec.name());
            }
        }
        System.out.println("错误码校验通过，共 " + codes.size() + " 个");
    }

    @Override
    public int getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}
