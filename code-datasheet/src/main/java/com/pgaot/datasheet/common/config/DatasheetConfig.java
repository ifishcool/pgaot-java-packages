package com.pgaot.datasheet.common.config;
/** 数据源配置 — admin 全开放 + readWriteDelete 防火墙 */

import com.pgaot.sql.api.SqlTemplate;
import com.pgaot.sql.api.SqlTemplateConfig;

public class DatasheetConfig {

    private final SqlTemplate adminSql;
    private final SqlTemplate readWriteSql;

    /** 默认数据源 */
    public DatasheetConfig() { this(""); }

    /** 命名数据源 */
    public DatasheetConfig(String name) {
        this.adminSql = new SqlTemplate(SqlTemplateConfig.fromEnv(name));
        this.readWriteSql = new SqlTemplate(
                SqlTemplateConfig.fromEnv(name).readWriteDelete());
    }

    public SqlTemplate adminSql() { return adminSql; }

    /** 用户 SQL 连接（允许增删改查，禁止 DDL） */
    public SqlTemplate readWriteSql() { return readWriteSql; }
}
