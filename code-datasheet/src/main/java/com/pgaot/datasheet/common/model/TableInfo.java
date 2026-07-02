package com.pgaot.datasheet.common.model;

import lombok.Data;
import java.util.List;
/** 表信息 */
import java.util.Map;

@Data
public class TableInfo {
    private String id;
    private String name;
    private String title;
    private String ownerId;
    private String description;
    private String mode;
    /** 列信息: [{name, type, nullable}] */
    private List<Map<String, Object>> columns;
}
