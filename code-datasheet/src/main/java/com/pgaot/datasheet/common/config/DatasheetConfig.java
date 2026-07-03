package com.pgaot.datasheet.common.config;
/** 数据源配置 — admin 全开放 + readWriteDelete 防火墙 */

import com.pgaot.sql.api.JpaTemplate;
import com.pgaot.sql.api.SqlTemplate;
import com.pgaot.sql.api.SqlTemplateConfig;
import com.pgaot.sql.jpa.entity.DsShareEntity;
import com.pgaot.sql.jpa.entity.DsTableEntity;

public class DatasheetConfig {

    private final SqlTemplate adminSql;
    private final SqlTemplate readWriteSql;
    private final JpaTemplate metaJpa;

    public DatasheetConfig() { this(""); }

    public DatasheetConfig(String name) {
        this.adminSql = new SqlTemplate(SqlTemplateConfig.fromEnv(name));
        this.readWriteSql = new SqlTemplate(
                SqlTemplateConfig.fromEnv(name).readWriteDelete());
        this.metaJpa = JpaTemplate.fromEnv(name, true, DsTableEntity.class, DsShareEntity.class);
    }

    public SqlTemplate adminSql() { return adminSql; }
    public SqlTemplate readWriteSql() { return readWriteSql; }

    /** 元数据 JPA 连接（ds_table / ds_share，自动建表） */
    public JpaTemplate metaJpa() { return metaJpa; }
}
