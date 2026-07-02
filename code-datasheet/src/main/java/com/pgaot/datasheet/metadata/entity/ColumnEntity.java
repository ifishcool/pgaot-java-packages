package com.pgaot.datasheet.metadata.entity;

import lombok.Data;

@Data
public class ColumnEntity {
    private Long id;
    private Long tableId;
    private String name;
    private String type;
    private boolean required;
    private int sortOrder;
}
