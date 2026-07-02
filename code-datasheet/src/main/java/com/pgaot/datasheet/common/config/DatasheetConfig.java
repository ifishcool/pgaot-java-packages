package com.pgaot.datasheet.common.config;

import com.pgaot.sql.api.SqlTemplate;
import com.pgaot.sql.api.SqlTemplateConfig;

public class DatasheetConfig {

    private final SqlTemplate adminSql;
    private final SqlTemplate readWriteSql;

    public DatasheetConfig() {
        this.adminSql     = new SqlTemplate(SqlTemplateConfig.fromEnv());
        this.readWriteSql = new SqlTemplate(SqlTemplateConfig.fromEnv().readWriteDelete());
    }

    public SqlTemplate adminSql() { return adminSql; }

    /** 用户 SQL 连接（允许增删改查，禁止 DDL） */
    public SqlTemplate readWriteSql() { return readWriteSql; }
}
