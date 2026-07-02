package com.pgaot.datasheet.common.model;

import lombok.Data;
import java.util.List;

@Data
public class TableInfo {
    private String id;
    private String name;
    private String title;
    private String ownerId;
    private String description;
    private List<ColumnInfo> columns;
}
