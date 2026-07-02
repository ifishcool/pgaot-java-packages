package com.pgaot.account.auth.core.redis;

import com.pgaot.account.auth.common.constants.AuthConstants;
import com.pgaot.account.auth.common.constants.Messages;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * Redis 通用缓存操作.
 *
 * <p>自动从环境变量 CODE_AUTH_REDIS_URI 读取连接地址.
 *
 * <pre>{@code
 * Redis redis = new Redis();
 * redis.set("key", "value", 3600);
 * }</pre>
 */
public class Redis {

    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> redis;

    /** 从 CODE_AUTH_REDIS_URI 读取连接地址 */
    public Redis() { this(System.getenv(AuthConstants.Env.REDIS_URI)); }

    /** @param redisUri Redis 连接地址，如 "redis://:pwd@host:6379/1" */
    public Redis(String redisUri) {
        if (redisUri == null || redisUri.isBlank()) {
            throw new IllegalArgumentException(Messages.REDIS_URI_EMPTY);
        }
        this.client = RedisClient.create(redisUri);
        this.connection = client.connect();
        this.redis = connection.sync();
    }

    // === 字符串 ===
    public String get(String key) { return redis.get(key); }
    public void set(String key, String value) { redis.set(key, value); }
    public void set(String key, String value, long ttlSeconds) { redis.setex(key, ttlSeconds, value); }
    public void del(String key) { redis.del(key); }
    public boolean exists(String key) { return redis.exists(key) > 0; }
    public void expire(String key, long ttlSeconds) { redis.expire(key, ttlSeconds); }

    // === 自增 ===
    public long incr(String key) { return redis.incr(key); }
    public long incrBy(String key, long delta) { return redis.incrby(key, delta); }

    // === Hash ===
    public void hset(String key, String field, String value) { redis.hset(key, field, value); }
    public String hget(String key, String field) { return redis.hget(key, field); }

    // === List ===
    public void lpush(String key, String value) { redis.lpush(key, value); }
    public void rpush(String key, String value) { redis.rpush(key, value); }

    // === Set ===
    public void sadd(String key, String value) { redis.sadd(key, value); }

    /** keys 命令（生产慎用） */
    public java.util.List<String> keys(String pattern) { return redis.keys(pattern); }

    /** 关闭连接 */
    public void close() { connection.close(); client.shutdown(); }
}
