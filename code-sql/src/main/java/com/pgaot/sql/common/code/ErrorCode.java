package com.pgaot.sql.common.code;

import com.pgaot.sql.common.constants.Messages;

import java.util.HashSet;
import java.util.Set;

/**
 * 错误码枚举.
 *
 * <pre>
 * 编号段分配:
 *   10_001_xxx  连接/环境错误
 *   10_002_xxx  SQL 执行错误
 *   10_003_xxx  SQL 防火墙拦截
 *   10_004_xxx  分页参数错误
 *   10_005_xxx  JPA 操作错误
 * </pre>
 */
public enum ErrorCode implements IResultCode {

    // ===== 连接/环境 (10_001_xxx) =====
    CONNECTION_FAILED(10_001_001, "数据库连接失败"),
    ENV_MISSING(10_001_002, "缺少环境变量"),

    // ===== SQL 执行 (10_002_xxx) =====
    SQL_EXECUTION_FAILED(10_002_001, "SQL 执行失败"),

    // ===== 防火墙 (10_003_xxx) =====
    SQL_BLOCKED_BY_WALL(10_003_001, "SQL 被防火墙拦截"),

    // ===== 分页参数 (10_004_xxx) =====
    PAGE_PARAM_INVALID(10_004_001, "分页参数无效"),

    // ===== JPA (10_005_xxx) =====
    JPA_EXECUTION_FAILED(10_005_001, "JPA 操作失败"),
    ;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /** 检查错误码是否重复 */
    public static void main(String[] args) {
        Set<Integer> codes = new HashSet<>();
        for (ErrorCode ec : values()) {
            if (!codes.add(ec.code)) {
                throw new IllegalStateException(Messages.ERROR_CODE_DUPLICATE + ec.code + " → " + ec.name());
            }
        }
        System.out.println("错误码校验通过，共 " + codes.size() + " 个");
    }

    @Override
    public int getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}
