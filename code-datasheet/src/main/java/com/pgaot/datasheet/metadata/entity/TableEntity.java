package com.pgaot.datasheet.metadata.entity;

import lombok.Data;
/** 表实体 */

@Data
public class TableEntity {
    private Long id;
    private String name;
    private String title;
    private String ownerId;
    private String description;
    private String mode;
}
