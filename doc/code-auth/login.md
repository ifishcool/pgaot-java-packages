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
    │    │    └─ return new UserInfo(localUserId, nickname, avatar, email)
    │    │
    │    ├─ Step 3: 构建 extra Map {nickname, avatar, email, ...extraFields}
    │    │
    │    ├─ Step 4: jwt.generate(userId, extra) → TokenPair
    │    │
    │    ├─ Step 5: tokenStore.save(userId, jti, accessExpires)
    │    │    └─ redis.setex("login:token:{userId}", jti, ttl)
    │    │
    │    ├─ Step 6: userRepo.upsert(userId, nickname, avatar, email)
    │    │    └─ 持久化到 pgaot_user 表
    │    │
    │    └─ Step 7: return LoginResult(accessToken, refreshToken, userId, nickname, avatar, email)
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

### LoginEntry 可注入 Provider

`LoginEntry` 默认通过 `YuntowerAuthFactory.fromEnv()` 延迟创建 `LoginService`，并支持在运行时注入固定实例或 Provider，便于多环境切换和单测隔离。

```java
// 固定实例注入
LoginEntry.configure(loginService, tokenManager);

// 延迟工厂注入（按需创建）
LoginEntry.configureProviders(serviceProvider, tokenProvider);

// 恢复默认工厂
LoginEntry.resetDefaults();
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
    private final String email;         // 邮箱
}
```

### UserInfo 结构

策略认证成功后的内部传递对象，含 userId/nickname/avatar/email/extra。extra 写入 JWT payload，email 同时写入 JWT + 数据库。

## 关键源码

| 文件                             | 行数  | 内容                                       |
| -------------------------------- | ----- | ------------------------------------------ |
| `LoginEntry.java:39-59`          | 21 行 | Provider 注入与默认恢复                    |
| `LoginEntry.java:68-75`          | 8 行  | login 入口 + 异常转换                      |
| `LoginService.java:25-31`        | 7 行  | 构造，组装依赖                             |
| `LoginService.java:34-48`        | 15 行 | 核心登录流程（含邮箱持久化）               |
| `YuntowerStrategy.java:25-34`    | 10 行 | 云塔 code → UserInfo（含 email）           |
| `YuntowerAuthFactory.java:27-51` | 25 行 | 从环境变量构建 LoginService                |
| `UserRepository.java:25-38`      | 14 行 | upsert 持久化 userId/nickname/avatar/email |
