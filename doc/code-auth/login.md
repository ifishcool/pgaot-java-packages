# 登录认证

## 功能

用户通过云塔授权码（code）完成登录，得到 JWT 凭证（accessToken + refreshToken）和用户信息。

## 完整流程

```
1. 前端 → 云塔授权页 → 用户同意 → 回调带 code
2. 后端调用: LoginEntry.login(LoginType.YUNTOWER, Map.of("code", "xxx"))
    │
    ▼
3. LoginEntry.login() 内部:
    │
    ├─ 调 LoginService.login("yuntower", params)
    │    │
    │    ├─ Step 1: StrategyRegistry.get("yuntower") → YuntowerStrategy
    │    │    └─ null → LoginException.unsupportedType("yuntower")
    │    │
    │    ├─ Step 2: YuntowerStrategy.authenticate({code:"xxx"})
    │    │    ├─ code 非空校验
    │    │    ├─ yuntower.auth().getToken(code)         → TokenResponse
    │    │    ├─ yuntower.user().getUserInfo(accessToken) → UserProfileResponse
    │    │    ├─ uidBinder.apply(profile.getUid())        → localUserId
    │    │    └─ return new UserInfo(localUserId, nickname, avatar)
    │    │
    │    ├─ Step 3: 构建 extra Map {nickname, avatar, ...extraFields}
    │    │
    │    ├─ Step 4: jwt.generate(userId, extra) → TokenPair
    │    │
    │    ├─ Step 5: tokenStore.save(userId, jti, accessExpires)
    │    │    └─ redis.setex("login:token:{userId}", jti, ttl)
    │    │
    │    └─ Step 6: return LoginResult(accessToken, refreshToken, userId, nickname, avatar)
    │         └─ 内部: success=true, code=0, message="ok"
    │
    ├─ catch LoginException → return LoginResult(e.getCode(), e.getMessage())
    │    └─ 内部: success=false, tokens=null
    │
    └─ catch Exception → return LoginResult(-1, e.getMessage())
```

## 设计要点

### LoginEntry 不抛异常

`login()` 内部 catch 所有异常，转为 `LoginResult(code, message)`。调用方只需：

```java
LoginResult r = LoginEntry.login(LoginType.YUNTOWER, Map.of("code", "xxx"));
if (r.isSuccess()) {
    // 取 token、用户信息
} else {
    // 处理错误: r.getCode() + r.getMessage()
}
```

Controller 层不需要 try-catch。

### LoginService 单例

`LoginEntry` 内部持有 `LoginService` 静态单例——整个 JVM 只有一个认证引擎实例，JwtUtil 和 TokenStore 跟着一起单例。

```java
// LoginEntry.java
private static final LoginService SERVICE = YuntowerAuthFactory.fromEnv();
```

### uidBinder

云塔返回的 uid 是云塔体系的用户标识，本地业务可能有自己的 userId 体系。通过 `Function<String, String>` 让调用方自定义映射。

```java
// 默认: uid 直接当本地 userId
LoginService service = YuntowerAuthFactory.fromEnv();

// 自定义映射: uid → "local_" + uid
LoginService service = YuntowerAuthFactory.fromEnv(uid -> "local_" + uid);
```

### LoginResult 结构

```java
public class LoginResult {
    private final boolean success;      // true=成功, false=失败
    private final int code;             // 错误码（成功时为 0）
    private final String message;       // 错误消息（成功时为 "ok"）
    private final String accessToken;   // 访问凭证
    private final String refreshToken;  // 刷新凭证
    private final String userId;        // 用户 ID
    private final String nickname;      // 昵称
    private final String avatar;        // 头像 URL
}
```

## 关键源码

| 文件 | 行数 | 内容 |
|---|---|---|
| `LoginEntry.java:41-49` | 9 行 | login 入口 + 异常转换 |
| `LoginService.java:25-31` | 7 行 | 构造，组装依赖 |
| `LoginService.java:34-48` | 15 行 | 核心登录流程 |
| `YuntowerStrategy.java:25-34` | 10 行 | 云塔 code → UserInfo |
| `YuntowerAuthFactory.java:27-51` | 25 行 | 从环境变量构建 LoginService |
