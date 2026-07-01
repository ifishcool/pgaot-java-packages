package com.pgaot.account.auth.core.yuntower;

import com.pgaot.account.auth.core.LoginService;
import com.pgaot.account.auth.common.config.LoginConfig;
import com.pgaot.account.auth.common.constants.Messages;
import com.pgaot.account.auth.api.LoginType;
import com.pgaot.account.auth.exception.LoginException;
import com.pgaot.account.auth.core.strategy.StrategyRegistry;
import com.pgaot.account.auth.api.store.RedisTokenStore;
import com.yuntower.account.sdk.YuntowerAccountClient;

import java.util.function.Function;

/**
 * 云塔 LoginService 工厂 — 需要环境变量 CODE_AUTH_REDIS_URI.
 */
public final class YuntowerAuthFactory {

    private YuntowerAuthFactory() {}

    /** 从环境变量创建（uid 直接当本地 userId） */
    public static LoginService fromEnv() {
        return fromEnv(uid -> uid);
    }

    /** 从环境变量创建，自定义 uid 映射 */
    public static LoginService fromEnv(Function<String, String> uidBinder) {
        String appId = env("YUNTOWER_APP_ID");
        String appSecret = env("YUNTOWER_APP_SECRET");
        String jwtSecret = env("CODE_AUTH_JWT_SECRET");
        String redisUri = env("CODE_AUTH_REDIS_URI");
        return create(appId, appSecret, jwtSecret, redisUri, uidBinder);
    }

    /** 手动传参创建（uid 直接当本地 userId） */
    public static LoginService create(String appId, String appSecret, String jwtSecret, String redisUri) {
        return create(appId, appSecret, jwtSecret, redisUri, uid -> uid);
    }

    /**
     * 手动传参 + 自定义 uid 映射.
     */
    public static LoginService create(String appId, String appSecret, String jwtSecret,
                                       String redisUri, Function<String, String> uidBinder) {
        YuntowerAccountClient yuntower = YuntowerAccountClient.create(appId, appSecret);
        StrategyRegistry registry = new StrategyRegistry()
                .register(LoginType.YUNTOWER, new YuntowerStrategy(yuntower, uidBinder));
        long ttl = ttlEnv("CODE_AUTH_TOKEN_TTL", 604800);
        return new LoginService(registry, new LoginConfig(jwtSecret, ttl, ttl * 2),
                new RedisTokenStore(redisUri));
    }

    private static long ttlEnv(String key, long defaultSeconds) {
        String v = System.getenv(key);
        if (v != null && !v.isBlank()) {
            try { return Long.parseLong(v); } catch (NumberFormatException ignored) {}
        }
        return defaultSeconds;
    }

    private static String env(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) throw new LoginException(Messages.ENV_MISSING + key);
        return v;
    }
}
