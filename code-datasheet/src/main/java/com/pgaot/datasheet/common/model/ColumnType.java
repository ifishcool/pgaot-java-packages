package com.pgaot.datasheet.common.model;
/** 列类型 — 映射到 MySQL 数据类型 */

public enum ColumnType {
    STRING,     // VARCHAR(512)
    TEXT,       // TEXT
    INT,        // INT
    BIGINT,     // BIGINT
    TINYINT,    // TINYINT
    DOUBLE,     // DOUBLE
    DECIMAL,    // DECIMAL(20,4)
    DATE,       // DATE
    TIME,       // TIME
    DATETIME,   // DATETIME
    TIMESTAMP,  // TIMESTAMP
    BOOLEAN,    // TINYINT(1)
    JSON        // JSON
}
