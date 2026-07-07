package com.pgaot.datasheet.exception;
import lombok.Getter;

import com.pgaot.datasheet.common.code.ErrorCode;
import com.pgaot.datasheet.common.code.IResultCode;

/**
 * 数据表异常 — 通过静态工厂方法创建.
 *
 * <pre>{@code
 * throw DatasheetException.tableNotFound(id);
 * throw DatasheetException.notOwner();
 * }</pre>
 */
@Getter
public class DatasheetException extends RuntimeException {

    private final int code;

    public DatasheetException(IResultCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public DatasheetException(IResultCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.code = errorCode.getCode();
    }

    /** 表不存在 */
    public static DatasheetException tableNotFound(String tableId) {
        return new DatasheetException(ErrorCode.TABLE_NOT_FOUND, tableId);
    }

    /** 表名重复 */
    public static DatasheetException tableNameDuplicate(String name) {
        return new DatasheetException(ErrorCode.TABLE_NAME_DUPLICATE, name);
    }

    /** 非 owner 操作表结构 */
    public static DatasheetException notOwner() {
        return new DatasheetException(ErrorCode.TABLE_NOT_OWNER);
    }

    /** 必填列不可删除 */
    public static DatasheetException columnRequired(String columnName) {
        return new DatasheetException(ErrorCode.COLUMN_REQUIRED, columnName);
    }

    /** 数据校验失败 */
    public static DatasheetException rowValidationFailed(String detail) {
        return new DatasheetException(ErrorCode.ROW_VALIDATION_FAILED, detail);
    }

    /** SQL 操作不允许 */
    public static DatasheetException sqlOperationDenied(String op) {
        return new DatasheetException(ErrorCode.SQL_OPERATION_DENIED, op);
    }

    /** 无导出权限 */
    public static DatasheetException exportPermissionDenied() {
        return new DatasheetException(ErrorCode.EXPORT_PERMISSION_DENIED);
    }

    /** 错误码 */
}
