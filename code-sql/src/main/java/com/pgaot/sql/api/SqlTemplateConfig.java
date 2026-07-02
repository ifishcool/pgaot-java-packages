package com.pgaot.sql.api;

import com.alibaba.druid.wall.WallConfig;
import com.pgaot.sql.common.config.EnvConfig;

import javax.sql.DataSource;
import java.util.List;

/**
 * SQL 模板配置 — 数据源 + Druid WallFilter.
 *
 * <pre>{@code
 * SqlTemplateConfig c = SqlTemplateConfig.fromEnv("MAIN");
 * c.selectOnly();
 * SqlTemplate db = new SqlTemplate(c);
 * }</pre>
 */
public class SqlTemplateConfig {

    private final DataSource dataSource;
    private final WallConfig wallConfig;

    /** 默认数据源 */
    public static SqlTemplateConfig fromEnv() { return fromEnv(""); }

    /** 命名数据源（CODE_SQL_URL_{name}） */
    public static SqlTemplateConfig fromEnv(String name) {
        return new SqlTemplateConfig(EnvConfig.createDataSource(name));
    }

    public SqlTemplateConfig(DataSource dataSource) { this(dataSource, new WallConfig()); }

    public SqlTemplateConfig(DataSource dataSource, WallConfig wallConfig) {
        this.dataSource = dataSource;
        this.wallConfig = wallConfig != null ? wallConfig : new WallConfig();
    }

    public SqlTemplateConfig selectOnly() {
        wallConfig.setSelectAllow(true);
        wallConfig.setInsertAllow(false); wallConfig.setUpdateAllow(false);
        wallConfig.setDeleteAllow(false); wallConfig.setDropTableAllow(false);
        wallConfig.setAlterTableAllow(false); wallConfig.setTruncateAllow(false);
        wallConfig.setRenameTableAllow(false); wallConfig.setCreateTableAllow(false);
        wallConfig.setSelectIntoAllow(false); wallConfig.setSelectAllColumnAllow(true);
        wallConfig.setSetAllow(false); wallConfig.setCallAllow(false);
        wallConfig.setDescribeAllow(false); wallConfig.setShowAllow(false);
        wallConfig.setUseAllow(false); wallConfig.setMergeAllow(false);
        wallConfig.setFunctionCheck(true);
        wallConfig.getDenyFunctions().addAll(List.of(
                "sleep", "benchmark", "load_file", "into outfile", "into dumpfile"));
        return this;
    }

    public SqlTemplateConfig readWrite() {
        wallConfig.setDeleteAllow(false); wallConfig.setDropTableAllow(false);
        wallConfig.setAlterTableAllow(false); wallConfig.setTruncateAllow(false);
        wallConfig.setRenameTableAllow(false); wallConfig.setCreateTableAllow(false);
        wallConfig.setFunctionCheck(true);
        wallConfig.getDenyFunctions().addAll(List.of("sleep", "benchmark", "load_file"));
        return this;
    }

    public DataSource getDataSource() { return dataSource; }
    public WallConfig getWallConfig() { return wallConfig; }
}
