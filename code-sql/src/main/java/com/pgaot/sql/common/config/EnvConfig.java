package com.pgaot.sql.common.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.wall.WallConfig;
import com.alibaba.druid.wall.WallFilter;
import com.pgaot.sql.common.constants.SqlConstants;
import com.pgaot.sql.exception.SqlException;

import java.util.ArrayList;
import java.util.List;

/**
 * 环境变量配置 — 支持多数据源.
 *
 * <pre>{@code
 * // 默认（CODE_SQL_URL / CODE_SQL_USER / CODE_SQL_PASS）
 * SqlTemplateConfig.fromEnv();
 *
 * // 命名数据源（CODE_SQL_URL_MAIN / CODE_SQL_USER_MAIN / CODE_SQL_PASS_MAIN）
 * SqlTemplateConfig.fromEnv("MAIN");
 * }</pre>
 */
public final class EnvConfig {

    public static final String URL          = "CODE_SQL_URL";
    public static final String USER         = "CODE_SQL_USER";
    public static final String PASS         = "CODE_SQL_PASS";
    public static final String POOL_INITIAL = "CODE_SQL_POOL_INITIAL";
    public static final String POOL_MIN_IDLE = "CODE_SQL_POOL_MIN_IDLE";
    public static final String POOL_MAX_ACTIVE = "CODE_SQL_POOL_MAX_ACTIVE";
    public static final String POOL_MAX_WAIT = "CODE_SQL_POOL_MAX_WAIT";
    public static final String AUTO_DDL     = "CODE_SQL_AUTO_DDL";

    private EnvConfig() {}

    public static DruidDataSource createDataSource() {
        return createDataSource("");
    }

    /** 命名数据源 — 环境变量: CODE_SQL_URL_{name}, CODE_SQL_USER_{name}, CODE_SQL_PASS_{name} */
    public static DruidDataSource createDataSource(String name) {
        return createDataSource(name, SqlConstants.Pool.DEFAULT_MAX_ACTIVE);
    }

    public static DruidDataSource createDataSource(String name, int defaultMaxActive) {
        return createDataSource(name, null, defaultMaxActive);
    }

    /**
     * 创建数据源，可选附加 WallFilter.
     *
     * @param name            数据源名称后缀，"" 表示默认
     * @param wallConfig      防火墙规则，null 表示不加 WallFilter
     * @param defaultMaxActive 连接池默认最大连接数
     */
    public static DruidDataSource createDataSource(String name, WallConfig wallConfig, int defaultMaxActive) {
        String suffix = name != null && !name.isBlank() ? "_" + name : "";
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl(env(URL + suffix));
        ds.setUsername(env(USER + suffix));
        ds.setPassword(env(PASS + suffix));

        // 连接池参数 — 可通过环境变量覆盖默认值
        ds.setInitialSize(intEnv(POOL_INITIAL + suffix, SqlConstants.Pool.DEFAULT_INITIAL_SIZE));
        ds.setMinIdle(intEnv(POOL_MIN_IDLE + suffix, SqlConstants.Pool.DEFAULT_MIN_IDLE));
        ds.setMaxActive(intEnv(POOL_MAX_ACTIVE + suffix, defaultMaxActive));
        ds.setMaxWait(intEnv(POOL_MAX_WAIT + suffix, SqlConstants.Pool.DEFAULT_MAX_WAIT_MS));
        ds.setTimeBetweenEvictionRunsMillis(SqlConstants.Pool.EVICTION_RUN_MS);
        ds.setMinEvictableIdleTimeMillis(SqlConstants.Pool.MIN_EVICTABLE_IDLE_MS);
        ds.setValidationQuery(SqlConstants.Pool.VALIDATION_QUERY);
        ds.setTestWhileIdle(true);
        ds.setTestOnBorrow(false);
        ds.setTestOnReturn(false);

        // Druid 要求 initialSize/minIdle ≤ maxActive
        int maxActive = ds.getMaxActive();
        if (ds.getInitialSize() > maxActive) ds.setInitialSize(maxActive);
        if (ds.getMinIdle() > maxActive) ds.setMinIdle(maxActive);

        if (wallConfig != null) {
            WallFilter wall = new WallFilter();
            wall.setConfig(wallConfig);
            List<com.alibaba.druid.filter.Filter> filters = new ArrayList<>(ds.getProxyFilters());
            filters.add(wall);
            ds.setProxyFilters(filters);
        }

        return ds;
    }

    /** @return CODE_SQL_AUTO_DDL 是否为 "true"，默认 false */
    public static boolean autoDdl(String name) {
        String suffix = name != null && !name.isBlank() ? "_" + name : "";
        return "true".equalsIgnoreCase(System.getenv(AUTO_DDL + suffix));
    }

    public static String env(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) v = System.getProperty(key);
        if (v == null || v.isBlank()) throw SqlException.envMissing(key);
        return v;
    }

    private static int intEnv(String key, int defaultValue) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) v = System.getProperty(key);
        return v != null && !v.isBlank() ? Integer.parseInt(v) : defaultValue;
    }
}
