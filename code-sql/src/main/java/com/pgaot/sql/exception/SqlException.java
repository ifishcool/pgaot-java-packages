package com.pgaot.sql.exception;
import lombok.Getter;

import com.pgaot.sql.common.code.ErrorCode;
import com.pgaot.sql.common.code.IResultCode;

/** SQL 异常 — 通过静态工厂方法创建，语义清晰 */
@Getter
public class SqlException extends RuntimeException {

    private final int code;

    public SqlException(int code, String message) {
        super(message);
        this.code = code;
    }

    public SqlException(IResultCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public SqlException(IResultCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.code = errorCode.getCode();
    }

    // ===== 静态工厂方法 =====

    public static SqlException wallBlocked(String detail) {
        return new SqlException(ErrorCode.SQL_BLOCKED_BY_WALL, detail);
    }

    public static SqlException executionFailed(String detail) {
        return new SqlException(ErrorCode.SQL_EXECUTION_FAILED, detail);
    }

    public static SqlException connectionFailed(String detail) {
        return new SqlException(ErrorCode.CONNECTION_FAILED, detail);
    }

    public static SqlException envMissing(String key) {
        return new SqlException(ErrorCode.ENV_MISSING, key);
    }

    public static SqlException pageParamInvalid(String detail) {
        return new SqlException(ErrorCode.PAGE_PARAM_INVALID, detail);
    }

    public static SqlException jpaFailed(String detail) {
        return new SqlException(ErrorCode.JPA_EXECUTION_FAILED, detail);
    }

}
