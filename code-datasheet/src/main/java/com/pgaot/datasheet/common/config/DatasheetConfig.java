package com.pgaot.datasheet.common.config;

import com.pgaot.sql.api.SqlTemplate;
import com.pgaot.sql.api.SqlTemplateConfig;

public class DatasheetConfig {

    private final SqlTemplate adminSql;
    private final SqlTemplate readWriteSql;

    public DatasheetConfig() {
        this.adminSql     = new SqlTemplate(SqlTemplateConfig.fromEnv());
        this.readWriteSql = new SqlTemplate(SqlTemplateConfig.fromEnv().readWrite());
    }

    /** 管理员连接 — DDL/内部 SQL */
    public SqlTemplate adminSql() { return adminSql; }

    /** 读写连接 — 用户 SQL（禁止 DDL/DELETE） */
    public SqlTemplate readWriteSql() { return readWriteSql; }
}
