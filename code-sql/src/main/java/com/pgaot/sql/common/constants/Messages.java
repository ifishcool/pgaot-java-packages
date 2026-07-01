package com.pgaot.sql.common.constants;

/** 提示信息常量 */
public final class Messages {

    private Messages() {}

    public static final String SQL_BLANK = "SQL 不能为空";
    public static final String BATCH_EMPTY = "批量参数不能为空";
    public static final String DATASOURCE_NOT_DRUID = "数据源必须是 DruidDataSource";
    public static final String PAGE_MIN = "页码必须 >= ";
    public static final String PAGE_RANGE = "每页大小必须在 ";
    public static final String PAGE_CURRENT = ", 当前: ";
    public static final String WALL_KEYWORD = "wall";
    public static final String ERROR_CODE_DUPLICATE = "重复错误码: ";
}
