package com.pgaot.account.auth.exception;

import com.pgaot.account.auth.common.code.ErrorCode;
import com.pgaot.account.auth.common.code.IResultCode;

/**
 * 认证异常 — 通过静态工厂方法创建，语义清晰.
 *
 * <pre>{@code
 * throw LoginException.tokenKicked();
 * throw LoginException.tokenInvalid("JWT 签名错误");
 * }</pre>
 */
public class LoginException extends RuntimeException {

    private final int code;

    public LoginException(IResultCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public LoginException(IResultCode errorCode, String detail) {
        super(detail != null ? detail : errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public LoginException(String message) {
        super(message);
        this.code = -1;
    }

    public LoginException(String message, Throwable cause) {
        super(message, cause);
        this.code = -1;
    }

    // ===== 静态工厂方法 =====

    public static LoginException authFailed(String detail) {
        return new LoginException(ErrorCode.AUTH_FAILED, detail);
    }

    public static LoginException unsupportedType(String type) {
        return new LoginException(ErrorCode.AUTH_UNSUPPORTED_TYPE, type);
    }

    public static LoginException configMissing(String key) {
        return new LoginException(ErrorCode.AUTH_CONFIG_MISSING, key);
    }

    public static LoginException tokenInvalid(String detail) {
        return new LoginException(ErrorCode.TOKEN_INVALID, detail);
    }

    public static LoginException tokenKicked() {
        return new LoginException(ErrorCode.TOKEN_KICKED);
    }

    /** 业务错误码 */
    public int getCode() { return code; }
}
