package com.pgaot.datasheet.metadata.entity;

import lombok.Data;

@Data
public class TableEntity {
    private Long id;
    private String name;
    private String title;
    private String ownerId;
    private String description;
    private String mode; // READ_ONLY / READ_WRITE
}
