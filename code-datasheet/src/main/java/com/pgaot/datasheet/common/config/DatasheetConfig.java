package com.pgaot.datasheet.common.config;

import com.pgaot.sql.api.JpaTemplate;
import com.pgaot.sql.api.SqlTemplate;
import com.pgaot.sql.api.SqlTemplateConfig;
import com.pgaot.sql.common.config.EnvConfig;
import com.pgaot.sql.jpa.entity.DsShareEntity;
import com.pgaot.sql.jpa.entity.DsTableEntity;

/**
 * 数据源配置.
 *
 * <p>连接池设计:
 * <ul>
 *   <li><b>adminSql</b> — 无防火墙，仅 DDL。maxPool=2。</li>
 *   <li><b>readWriteSql</b> — readWriteDelete 防火墙，用户 SQL。</li>
 *   <li><b>metaJpa</b> — Hibernate JPA，元数据操作。</li>
 * </ul>
 *
 * <p>WallFilter 在 DataSource 创建时附加（通过 EnvConfig），SqlTemplate 不修改 DataSource。
 *
 * <p>CODE_SQL_AUTO_DDL=true 开启自动建表（生产应设为 false）。
 */
public class DatasheetConfig {

    private final SqlTemplate adminSql;
    private final SqlTemplate readWriteSql;
    private final JpaTemplate metaJpa;

    static final int ADMIN_POOL_MAX_ACTIVE = 2;

    public DatasheetConfig() { this(""); }

    public DatasheetConfig(String name) {
        this.adminSql = new SqlTemplate(
                SqlTemplateConfig.fromEnv(name).maxPool(ADMIN_POOL_MAX_ACTIVE));
        this.readWriteSql = new SqlTemplate(
                SqlTemplateConfig.fromEnv(name).readWriteDelete());
        this.metaJpa = JpaTemplate.fromEnv(name,
                EnvConfig.autoDdl(name),
                DsTableEntity.class, DsShareEntity.class);
    }

    public SqlTemplate adminSql() { return adminSql; }
    public SqlTemplate readWriteSql() { return readWriteSql; }

    /** 元数据 JPA 连接（ds_table / ds_share，自动建表） */
    public JpaTemplate metaJpa() { return metaJpa; }

    /** 关闭所有连接池 */
    public void close() {
        adminSql.close();
        readWriteSql.close();
        metaJpa.close();
    }
}
