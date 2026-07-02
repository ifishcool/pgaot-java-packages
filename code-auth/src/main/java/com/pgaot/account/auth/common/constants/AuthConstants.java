package com.pgaot.account.auth.common.constants;

/** 常量 — 按模块内部分组. */
public final class AuthConstants {
    private AuthConstants() {}

    /** 环境变量 Key */
    public static class Env {
        private Env() {}
        public static final String YUNTOWER_APP_ID     = "YUNTOWER_APP_ID";
        public static final String YUNTOWER_APP_SECRET = "YUNTOWER_APP_SECRET";
        public static final String JWT_SECRET          = "CODE_AUTH_JWT_SECRET";
        public static final String REDIS_URI           = "CODE_AUTH_REDIS_URI";
        public static final String TOKEN_TTL           = "CODE_AUTH_TOKEN_TTL";
        public static final String KEY_PREFIX          = "CODE_AUTH_KEY_PREFIX";
    }
}
