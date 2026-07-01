package com.pgaot.account.auth.exception;

import com.pgaot.account.auth.common.code.ErrorCode;

/**
 * 认证异常 — 所有登录/校验/退出相关的错误统一抛此异常.
 *
 * <pre>{@code
 * throw new LoginException(ErrorCode.TOKEN_KICKED);
 * }</pre>
 */
public class LoginException extends RuntimeException {

    private final int code;

    /** @param errorCode 错误码枚举 */
    public LoginException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * @param errorCode 错误码枚举
     * @param detail    详细描述（覆盖枚举默认消息）
     */
    public LoginException(ErrorCode errorCode, String detail) {
        super(detail != null ? detail : errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /** @param message 错误消息（code 默认为 -1） */
    public LoginException(String message) {
        super(message);
        this.code = -1;
    }

    /**
     * @param message 错误消息
     * @param cause   原始异常
     */
    public LoginException(String message, Throwable cause) {
        super(message, cause);
        this.code = -1;
    }

    /** 业务错误码 */
    public int getCode() { return code; }
}
