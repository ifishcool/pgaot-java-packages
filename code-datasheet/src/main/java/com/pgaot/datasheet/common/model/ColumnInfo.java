package com.pgaot.datasheet.common.model;

import lombok.Data;

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
