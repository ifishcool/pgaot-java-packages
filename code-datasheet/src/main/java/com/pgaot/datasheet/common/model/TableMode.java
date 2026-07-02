package com.pgaot.datasheet.common.model;

/** 表模式 — 控制表的数据操作权限 */
public enum TableMode {
    /** 只读: 仅允许 SELECT */
    READ_ONLY,
    /** 只写: 允许 INSERT/UPDATE，禁止 SELECT/DELETE */
    WRITE_ONLY,
    /** 读写: 允许 SELECT/INSERT/UPDATE，禁止 DELETE */
    READ_WRITE,
    /** 全部: 允许所有操作（建表默认） */
    ALL
}
