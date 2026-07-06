package com.pgaot.log.exception;

import com.pgaot.log.common.code.ErrorCode;
import com.pgaot.log.common.code.IResultCode;

public class LogException extends RuntimeException {

    private final int code;

    public LogException(IResultCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public LogException(IResultCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.code = errorCode.getCode();
    }

    public static LogException contextNotInitialized() {
        return new LogException(ErrorCode.LOG_CONTEXT_EMPTY);
    }

    public static LogException auditWriteFailed(String detail) {
        return new LogException(ErrorCode.AUDIT_WRITE_FAILED, detail);
    }

    public int getCode() { return code; }
}
