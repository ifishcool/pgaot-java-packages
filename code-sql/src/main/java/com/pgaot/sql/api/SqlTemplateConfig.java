package com.pgaot.sql.api;

import com.alibaba.druid.wall.WallConfig;
import com.pgaot.sql.common.config.EnvConfig;
import com.pgaot.sql.common.constants.SqlConstants;

import javax.sql.DataSource;
import java.util.List;

/**
 * SQL 模板配置.
 *
 * <p>数据源延迟创建，确保 WallConfig 预设（selectOnly/readWrite/readWriteDelete）
 * 在 DataSource 创建时以 WallFilter 形式附加，SqlTemplate 不修改 DataSource。
 *
 * <pre>{@code
 * SqlTemplateConfig c = SqlTemplateConfig.fromEnv("MAIN").readWriteDelete();
 * SqlTemplate db = new SqlTemplate(c);
 * }</pre>
 */
public class SqlTemplateConfig {

    private final String name;
    private final WallConfig wallConfig;
    private DataSource dataSource;
    private int defaultMaxActive = SqlConstants.Pool.DEFAULT_MAX_ACTIVE;

    /** 默认数据源（无 WallFilter） */
    public static SqlTemplateConfig fromEnv() { return fromEnv(""); }

    /** 命名数据源（CODE_SQL_URL_{name}），无 WallFilter */
    public static SqlTemplateConfig fromEnv(String name) {
        return new SqlTemplateConfig(name, new WallConfig());
    }

    /** 传入已有 DataSource（不再从环境变量创建） */
    public SqlTemplateConfig(DataSource dataSource) {
        this(dataSource, new WallConfig());
    }

    public SqlTemplateConfig(DataSource dataSource, WallConfig wallConfig) {
        this.name = null;
        this.dataSource = dataSource;
        this.wallConfig = wallConfig != null ? wallConfig : new WallConfig();
    }

    private SqlTemplateConfig(String name, WallConfig wallConfig) {
        this.name = name;
        this.wallConfig = wallConfig;
    }

    /** 限制连接池大小（仅环境变量模式生效） */
    public SqlTemplateConfig maxPool(int maxActive) {
        this.defaultMaxActive = maxActive;
        return this;
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

    /** 读写+删除模式 — 允许增删改查，禁止 DDL 和危险函数 */
    public SqlTemplateConfig readWriteDelete() {
        wallConfig.setDropTableAllow(false);
        wallConfig.setAlterTableAllow(false); wallConfig.setTruncateAllow(false);
        wallConfig.setRenameTableAllow(false); wallConfig.setCreateTableAllow(false);
        wallConfig.setFunctionCheck(true);
        wallConfig.getDenyFunctions().addAll(List.of("sleep", "benchmark", "load_file"));
        return this;
    }

    /** 延迟创建 DataSource，将 WallConfig 作为 WallFilter 附加 */
    public DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = EnvConfig.createDataSource(
                    name, wallConfig, defaultMaxActive);
        }
        return dataSource;
    }

    public WallConfig getWallConfig() { return wallConfig; }
}
