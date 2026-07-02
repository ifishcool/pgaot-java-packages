# JWT 与会话管理

涵盖 JWT 生成/校验、单设备登录、退出登录、Token 存储。

---

## JWT 生成

### Payload 结构

```json
{
  "sub": "user_10001",       // userId
  "jti": "uuid-xxxx",        // 每次登录生成的唯一 ID
  "iat": 1719900000,         // 签发时间
  "exp": 1720504800,         // 过期时间
  "nickname": "张三",        // extra 字段
  "avatar": "https://..."    // extra 字段
}
```

### 生成流程

```
JwtUtil.generate(userId, extra)
   │
   ├─ jti = UUID.randomUUID().toString()
   ├─ now = new Date()
   ├─ accessToken = buildToken(userId, jti, extra, now, accessExpires)
   │     └─ Jwts.builder()
   │          .subject(userId)           ← sub
   │          .id(jti)                   ← jti
   │          .issuedAt(now)             ← iat
   │          .expiration(now + ttl)     ← exp
   │          .claim(key, value)         ← extra 逐个写入
   │          .signWith(key)             ← HMAC-SHA256
   │          .compact()
   │
   ├─ refreshToken = buildToken(..., refreshExpires)
   │     ← 结构和 accessToken 一样，过期时间更长
   │
   └─ return TokenPair(accessToken, refreshToken, jti, accessExpires)
```

### TokenPair

```java
record TokenPair(String accessToken, String refreshToken, String jti, long expiresIn) {}
```

## JWT 校验

### 流程

```
JwtUtil.validate(token)
   │
   ├─ Jwts.parser()
   │    .verifyWith(key)                    ← 验签名
   │    .build()
   │    .parseSignedClaims(token)            ← 解析 + 验过期
   │    .getPayload()
   │    └─ 签名/过期/格式错误 → LoginException.tokenInvalid(msg)
   │
   ├─ claims.getSubject()   → userId
   ├─ claims.getId()        → jti
   ├─ claims.getIssuedAt()  → iat
   ├─ claims.getExpiration() → exp
   │
   ├─ 提取 extra: 遍历 claims，排除 sub/jti/iat/exp → extra Map
   │
   └─ return TokenClaims(userId, jti, iat, exp, extra)
```

### TokenClaims

```java
record TokenClaims(
    String userId,
    String jti,
    Date issuedAt,
    Date expiration,
    Map<String, Object> extra
) {}
```

### 设计要点

- `TokenClaims` 和 `TokenPair` 用 Java record——不可变对象
- extra 通过排除法提取：标准 JWT claims 过滤掉，其余全算业务字段。新增字段不需改校验逻辑
- 签名算法 HMAC-SHA256，密钥从 `CODE_AUTH_JWT_SECRET` 读取
- `refreshToken` 当前预留未启用

---

## 单设备登录

### 原理

每次登录生成唯一 jti，存入 Redis。校验时对比 JWT 中的 jti 和 Redis 中的 jti——不一致即被踢下线。

### 时间线演示

```
T1: 设备A登录 → jti="aaa" → Redis: "aaa"
T2: 设备A请求 → JWT.jti="aaa" == Redis.jti="aaa" → 是
T3: 设备B登录 → jti="bbb" → Redis: "bbb" (覆盖)
T4: 设备A请求 → JWT.jti="aaa" != Redis.jti="bbb" → 否 TokenKicked
```

### 校验代码

```java
// LoginService.java:50-58
public LoginUser validate(String jwtToken) {
    TokenClaims claims = jwt.validate(jwtToken);              // 解 JWT
    String currentJti = tokenStore.getJti(claims.getUserId()); // 查 Redis

    if (currentJti != null && !claims.getJti().equals(currentJti)) {
        throw LoginException.tokenKicked();  // ← 被踢下线
    }
    return new LoginUser(claims);
}
```

### 容错设计

`if (currentJti != null && ...)`——Redis 挂了 `currentJti` 为 null，直接放行。**Redis 故障时系统仍可用**，只失去单设备保护。

---

## 退出登录

### 流程

```
LoginEntry.logout(token)
   → jwt.validate(token) → TokenClaims
   → tokenStore.remove(userId) → redis.del("login:token:{userId}")
```

### 关于"退出即失效"

退出后 Redis jti 被删除，`currentJti` 为 null。由于 `if (currentJti != null && ...)` 的判断，实际上退出后 token 仍能通过校验。这是有意为之的容错设计——退出移除单设备保护，而非立即使 token 失效。需立即失效可扩展黑名单机制。

---

## Token 存储

### 接口定义

```java
public interface TokenStore {
    String KEY_PREFIX =
        System.getenv("CODE_AUTH_KEY_PREFIX") != null
            ? System.getenv("CODE_AUTH_KEY_PREFIX")
            : "login:token";

    static String key(String userId) {
        return KEY_PREFIX + ":" + userId;  // "login:token:10001"
    }

    void save(String userId, String jti, long ttl);
    String getJti(String userId);   // null = 未存储
    void remove(String userId);
}
```

### Redis 实现

```java
public class RedisTokenStore implements TokenStore {
    private final Redis redis;

    public RedisTokenStore(String redisUri) { this.redis = new Redis(redisUri); }

    public void save(String userId, String jti, long ttl) {
        redis.set(TokenStore.key(userId), jti, ttl);   // SETEX
    }
    public String getJti(String userId) {
        return redis.get(TokenStore.key(userId));       // GET
    }
    public void remove(String userId) {
        redis.del(TokenStore.key(userId));              // DEL
    }
}
```

### 设计要点

- `KEY_PREFIX` 可被环境变量 `CODE_AUTH_KEY_PREFIX` 覆盖
- `key()` 是接口 static 方法（Java 8+），实现类不用重复
- `getJti()` 返回 null 的语义是"无存储记录"，这是单设备容错的关键
- 接口允许替换实现（数据库存储、JWT 自包含等）

## 关键源码

| 文件 | 内容 |
|---|---|
| `JwtUtil.java:36-42` | generate() |
| `JwtUtil.java:49-64` | validate() |
| `JwtUtil.java:67-73` | buildToken() |
| `LoginService.java:50-58` | validate + jti 比对 |
| `LoginService.java:62-65` | logout() |
| `TokenStore.java` | 接口 + key() |
| `RedisTokenStore.java` | Redis 实现 |
