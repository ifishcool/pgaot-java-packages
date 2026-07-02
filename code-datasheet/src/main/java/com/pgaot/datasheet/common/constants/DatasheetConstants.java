package com.pgaot.datasheet.common.constants;

public final class DatasheetConstants {
    private DatasheetConstants() {}

    /** 元数据表名 */
    public static final String META_TABLE  = "ds_table";
    public static final String META_COLUMN = "ds_column";
    /** 单次 insert 最大行数 */
    public static final int MAX_INSERT_ROWS = 1000;
    /** 导出最大行数 */
    public static final int MAX_EXPORT_ROWS = 50000;
}
