package com.pgaot.account.auth.api.store;

/**
 * Token 存储接口 — 单设备登录的核心.
 *
 * <p>原理：每次登录时 save() 覆盖旧 jti，校验时对比 JWT 里的 jti 与存储的 jti.
 * 对不上 = 该设备被其他设备挤下线.
 *
 * <p>Key 格式: {prefix}:{userId} → jti
 * <p>prefix 由环境变量 CODE_AUTH_KEY_PREFIX 配置，默认 "login:token"
 *
 * <p>实现类: RedisTokenStore（生产，持久化 + 自动过期）
 */
public interface TokenStore {

    /** Key 前缀，默认 "login:token"，可通过 CODE_AUTH_KEY_PREFIX 环境变量覆盖 */
    String KEY_PREFIX = System.getenv("CODE_AUTH_KEY_PREFIX") != null
            ? System.getenv("CODE_AUTH_KEY_PREFIX")
            : "login:token";

    /** 拼接 Redis Key */
    static String key(String userId) { return KEY_PREFIX + ":" + userId; }

    /**
     * 存储 jti — 每次登录调用，覆盖旧值.
     *
     * @param userId     用户标识
     * @param jti        JWT 唯一 ID
     * @param ttlSeconds 过期时间（秒）
     */
    void save(String userId, String jti, long ttlSeconds);

    /**
     * 获取当前有效的 jti.
     *
     * @return 当前 jti，未存储返回 null
     */
    String getJti(String userId);

    /** 删除 — 退出登录时调用 */
    void remove(String userId);
}
