package com.pgaot.sql.common.constants;

/**
 * SQL 常量 — 参考 code-auth {@code Constants} 模式按模块内部分组.
 */
public final class SqlConstants {

    private SqlConstants() {}

    /** 连接池默认值 */
    public static class Pool {
        public static final int DEFAULT_INITIAL_SIZE = 5;
        public static final int DEFAULT_MIN_IDLE = 5;
        public static final int DEFAULT_MAX_ACTIVE = 20;
        public static final int DEFAULT_MAX_WAIT_MS = 60000;
        public static final int EVICTION_RUN_MS = 60000;
        public static final int MIN_EVICTABLE_IDLE_MS = 300000;
        public static final String VALIDATION_QUERY = "SELECT 1";
    }

    /** 分页限制 */
    public static class Page {
        public static final int MIN_PAGE = 1;
        public static final int MIN_SIZE = 1;
        public static final int MAX_SIZE = 1000;
    }

    /** 批次限制 */
    public static class Batch {
        public static final int MAX_BATCH_SIZE = 5000;
    }

}
