package com.pgaot.log.common.code;

import java.util.HashSet;
import java.util.Set;

/**
 * 错误码 — 编号段 40_xxx_xxx.
 *
 * <pre>
 *   40_001_xxx  日志上下文
 *   40_002_xxx  审计日志
 * </pre>
 */
public enum ErrorCode implements IResultCode {

    LOG_CONTEXT_EMPTY(40_001_001, "日志上下文未初始化"),
    AUDIT_WRITE_FAILED(40_002_001, "审计日志写入失败"),
    ;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) { this.code = code; this.message = message; }

    public static void main(String[] args) {
        Set<Integer> codes = new HashSet<>();
        for (ErrorCode ec : values()) {
            if (!codes.add(ec.code))
                throw new IllegalStateException("重复错误码: " + ec.code + " → " + ec.name());
        }
        System.out.println("错误码校验通过，共 " + codes.size() + " 个");
    }

    @Override public int getCode() { return code; }
    @Override public String getMessage() { return message; }
}
