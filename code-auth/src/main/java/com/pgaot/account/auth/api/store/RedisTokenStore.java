package com.pgaot.account.auth.api.store;

import com.pgaot.account.auth.core.redis.Redis;

/**
 * Redis 实现的 TokenStore.
 *
 * <pre>{@code
 * TokenStore store = new RedisTokenStore("redis://:pwd@host:6379/1");
 * store.save("userId", "jti-xxx", 604800);
 * }</pre>
 */
public class RedisTokenStore implements TokenStore {

    private final Redis redis;

    /** @param redisUri Redis 连接地址 */
    public RedisTokenStore(String redisUri) {
        this.redis = new Redis(redisUri);
    }

    @Override
    public void save(String userId, String jti, long ttlSeconds) {
        redis.set(TokenStore.key(userId), jti, ttlSeconds);
    }

    @Override
    public String getJti(String userId) {
        return redis.get(TokenStore.key(userId));
    }

    @Override
    public void remove(String userId) {
        redis.del(TokenStore.key(userId));
    }

    /** 关闭连接 */
    public void close() { redis.close(); }
}
