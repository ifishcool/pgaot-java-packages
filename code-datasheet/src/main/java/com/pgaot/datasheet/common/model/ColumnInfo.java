package com.pgaot.datasheet.common.model;

import lombok.Data;
/** 列定义 */

@Data
public class ColumnInfo {
    private String name;
    private ColumnType type;
    private boolean required;

    public ColumnInfo() {}

    public ColumnInfo(String name, ColumnType type, boolean required) {
        this.name = name;
        this.type = type;
        this.required = required;
    }
}
