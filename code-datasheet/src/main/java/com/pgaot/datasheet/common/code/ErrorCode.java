package com.pgaot.datasheet.common.code;

import java.util.HashSet;
import java.util.Set;

/**
 * 错误码 — 编号段 30_xxx_xxx.
 *
 * <pre>
 *   30_001_xxx  表管理
 *   30_003_xxx  数据操作
 *   30_004_xxx  SQL 执行
 * </pre>
 */
public enum ErrorCode implements IResultCode {

    // ===== 表管理 (30_001_xxx) =====
    TABLE_NOT_FOUND(30_001_001, "表不存在"),
    TABLE_NAME_DUPLICATE(30_001_002, "表名重复"),
    TABLE_NOT_OWNER(30_001_003, "只有创建者可以操作表结构"),
    COLUMN_NOT_FOUND(30_001_004, "列不存在"),
    COLUMN_NAME_DUPLICATE(30_001_005, "列名重复"),
    COLUMN_REQUIRED(30_001_006, "必填列不能删除"),

    // ===== 数据操作 (30_003_xxx) =====
    ROW_VALIDATION_FAILED(30_003_001, "数据校验失败"),
    ROW_COUNT_EXCEEDED(30_003_002, "单次操作行数超限"),

    // ===== SQL 执行 (30_004_xxx) =====
    SQL_TABLE_NOT_REGISTERED(30_004_001, "SQL 中引用的表未注册"),
    SQL_OPERATION_DENIED(30_004_002, "SQL 操作类型不允许"),
    SQL_PARSE_FAILED(30_004_003, "SQL 解析失败"),
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
