package com.pgaot.sql.common.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.pgaot.sql.common.constants.SqlConstants;
import com.pgaot.sql.exception.SqlException;

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

    public static final String URL  = "CODE_SQL_URL";
    public static final String USER = "CODE_SQL_USER";
    public static final String PASS = "CODE_SQL_PASS";

    private EnvConfig() {}

    public static DruidDataSource createDataSource() {
        return createDataSource("");
    }

    /** 命名数据源 — 环境变量: CODE_SQL_URL_{name}, CODE_SQL_USER_{name}, CODE_SQL_PASS_{name} */
    public static DruidDataSource createDataSource(String name) {
        String suffix = name != null && !name.isBlank() ? "_" + name : "";
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl(env(URL + suffix));
        ds.setUsername(env(USER + suffix));
        ds.setPassword(env(PASS + suffix));

        // 连接池参数 — 可通过环境变量覆盖默认值
        ds.setInitialSize(intEnv("CODE_SQL_POOL_INITIAL" + suffix, SqlConstants.Pool.DEFAULT_INITIAL_SIZE));
        ds.setMinIdle(intEnv("CODE_SQL_POOL_MIN_IDLE" + suffix, SqlConstants.Pool.DEFAULT_MIN_IDLE));
        ds.setMaxActive(intEnv("CODE_SQL_POOL_MAX_ACTIVE" + suffix, SqlConstants.Pool.DEFAULT_MAX_ACTIVE));
        ds.setMaxWait(intEnv("CODE_SQL_POOL_MAX_WAIT" + suffix, SqlConstants.Pool.DEFAULT_MAX_WAIT_MS));
        ds.setTimeBetweenEvictionRunsMillis(SqlConstants.Pool.EVICTION_RUN_MS);
        ds.setMinEvictableIdleTimeMillis(SqlConstants.Pool.MIN_EVICTABLE_IDLE_MS);
        ds.setValidationQuery(SqlConstants.Pool.VALIDATION_QUERY);
        ds.setTestWhileIdle(true);
        ds.setTestOnBorrow(false);
        ds.setTestOnReturn(false);

        return ds;
    }

    public static String env(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) throw SqlException.envMissing(key);
        return v;
    }

    private static int intEnv(String key, int defaultValue) {
        String v = System.getenv(key);
        return v != null && !v.isBlank() ? Integer.parseInt(v) : defaultValue;
    }
}
