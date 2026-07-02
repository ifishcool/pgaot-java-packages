package com.pgaot.datasheet.common.constants;
/** 提示信息常量 */

public final class Messages {
    private Messages() {}

    public static final String TABLE_EMPTY    = "表名不能为空";
    public static final String COLUMNS_EMPTY  = "至少需要一列";
    public static final String MODE_READ_ONLY  = "表 %s 为只读模式，禁止写操作";
    public static final String MODE_WRITE_ONLY = "表 %s 为只写模式，禁止查询";
    public static final String MODE_DELETE_BLOCKED = "表 %s 为只写模式，禁止 DELETE";
    public static final String TABLE_NO_ACCESS = "（无权访问）";
}
