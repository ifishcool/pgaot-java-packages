# Redis 客户端与配置体系

## Redis 客户端

基于 Lettuce 同步 API 的通用 Redis 操作。

### 连接

```java
Redis redis = new Redis("redis://:password@host:6379/0");
// 或从环境变量 CODE_AUTH_REDIS_URI 自动读取
Redis redis = new Redis();
```

### 支持的操作

| 数据类型 | 方法 |
|---|---|
| String | `get`, `set`, `setex`, `del`, `exists`, `expire`, `incr`, `incrby` |
| Hash | `hset`, `hget` |
| List | `lpush`, `rpush` |
| Set | `sadd` |
| 通用 | `keys`（生产慎用）, `close` |

### 实现

```java
public class Redis {
    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> redis;

    public Redis(String redisUri) {
        if (redisUri == null || redisUri.isBlank())
            throw new IllegalArgumentException("Redis URI 为空");
        this.client = RedisClient.create(redisUri);
        this.connection = client.connect();
        this.redis = connection.sync();   // 同步 API
    }
    // ...
}
```

### 关键源码

| 文件 | 内容 |
|---|---|
| `Redis.java:25-35` | 构造函数 |
| `Redis.java:38-63` | 全部操作 |

---

## 配置体系

### 环境变量

| 变量 | 必填 | 默认值 | 说明 |
|---|---|---|---|
| `YUNTOWER_APP_ID` | 必填 | — | 云塔应用 ID |
| `YUNTOWER_APP_SECRET` | 必填 | — | 云塔密钥 |
| `CODE_AUTH_JWT_SECRET` | 必填 | — | JWT 签名密钥（≥32 字符） |
| `CODE_AUTH_REDIS_URI` | 必填 | — | Redis 连接地址 |
| `CODE_AUTH_TOKEN_TTL` | 可选 | `604800`（7天） | Token 有效秒数 |
| `CODE_AUTH_KEY_PREFIX` | 可选 | `login:token` | Redis Key 前缀 |

以上 Key 在代码中统一收口于 `AuthConstants.Env`，修改时只需改一处。

### LoginConfig

```java
public class LoginConfig {
    private final String jwtSecret;        // JWT 签名密钥
    private final long accessExpires;      // access_token 有效秒数
    private final long refreshExpires;     // refresh_token 有效秒数

    // 默认: access=12h, refresh=24d
    public LoginConfig(String jwtSecret) {
        this(jwtSecret, 43200, 2073600);
    }

    // 自定义过期时间
    public LoginConfig(String jwtSecret, long accessExpires, long refreshExpires) {
        this.jwtSecret = jwtSecret;
        this.accessExpires = accessExpires;
        this.refreshExpires = refreshExpires;
    }
}
```

### 配置流向

```
环境变量
    │
    ▼
YuntowerAuthFactory.fromEnv()
    ├─ YUNTOWER_APP_ID / APP_SECRET → YuntowerAccountClient
    ├─ CODE_AUTH_JWT_SECRET → LoginConfig → JwtUtil
    ├─ CODE_AUTH_REDIS_URI   → RedisTokenStore → Redis
    └─ CODE_AUTH_TOKEN_TTL   → LoginConfig（覆盖默认 604800）
```

### 关键源码

| 文件 | 内容 |
|---|---|
| `LoginConfig.java` | JWT 配置 |
| `YuntowerAuthFactory.java:27-51` | 从环境变量构建 |
| `YuntowerAuthFactory.java:61-64` | 环境变量读取 |
