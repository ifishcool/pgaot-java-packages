# PGAOT Code Auth

[![JDK](https://img.shields.io/badge/JDK-21%2B-blue)](https://adoptium.net/)
[![License](https://img.shields.io/badge/license-GPL--3.0-green)](LICENSE)

PGAOT平台通用认证框架 — 策略模式 + JWT + 单设备登录 + API Token + Redis 持久化。

---

## 环境要求

- JDK 21+
- Maven 3.6+

## 安装

**1. 创建 GitHub Token**

Settings → Developer settings → Personal access tokens → Tokens (classic) → 勾选 `read:packages`

**2. 配置 `~/.m2/settings.xml`**

```xml
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>你的GitHub用户名</username>
            <password>你的Token</password>
        </server>
    </servers>
</settings>
```

**3. pom.xml**

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/ifishcool/pgaot-java-packages</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.pgaot</groupId>
    <artifactId>code-auth</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 环境变量

```
YUNTOWER_APP_ID          # 云塔应用 ID（必填）
YUNTOWER_APP_SECRET      # 云塔应用密钥（必填）
CODE_AUTH_JWT_SECRET     # JWT 签名密钥，至少 32 字符（必填）
CODE_AUTH_REDIS_URI      # Redis 连接地址（必填）
CODE_AUTH_TOKEN_TTL      # Token 有效秒数（可选，默认 604800 = 7 天）
CODE_AUTH_KEY_PREFIX     # Redis Key 前缀（可选，默认 login:token）
CODE_SQL_URL             # MySQL 连接地址（必填，API Token 存储）
CODE_SQL_USER            # 数据库用户名（必填）
CODE_SQL_PASS            # 数据库密码（必填）
```

---

## 快速开始

```java
import com.pgaot.account.auth.api.LoginEntry;
import com.pgaot.account.auth.api.LoginType;
import com.pgaot.account.auth.api.model.LoginResult;
import com.pgaot.account.auth.api.model.LoginUser;
import java.util.Map;

// 1. 登录 — 调一次，JWT 自动生成、Redis 自动写入
LoginResult result = LoginEntry.login(LoginType.YUNTOWER, Map.of("code", "xxx"));

if (result.isSuccess()) {
    System.out.println("登录成功: " + result.getNickname());

    // 2. 校验 — 验 JWT + 单设备登录检查
    LoginUser user = LoginEntry.validate(result.getAccessToken());

    // 3. 退出
    LoginEntry.logout(result.getAccessToken());
} else {
    System.out.println("登录失败: [" + result.getCode() + "] " + result.getMessage());
}
```

---

## 认证流程

```
1. 前端调 Web SDK → 跳转云塔授权页
2. 用户点 "同意授权"
3. 云塔回调 redirect_url?status=success&code=xxx
4. 后端调 LoginEntry.login(LoginType.YUNTOWER, Map.of("code", "xxx"))
   → 内部自动: 云塔 API → JWT 生成 → Redis 写入 → 返回 LoginResult
5. 后续请求: Authorization: Bearer {jwt} → LoginEntry.validate(token)
   → 解析 JWT → 对比 Redis 里的 jti（单设备登录检查）
```

---

## API 参考

### LoginEntry

| 方法                                                 | 说明                                            |
| ---------------------------------------------------- | ----------------------------------------------- |
| `login(type, params)`                                | 登录 — 不抛异常，通过 `isSuccess()` 判断        |
| `validate(token)`                                    | 校验 JWT + 单设备登录检查                       |
| `logout(token)`                                      | 退出登录                                        |
| `configure(service, tokenManager)`                   | 注入固定实例（测试/多租户）                     |
| `configureProviders(serviceProvider, tokenProvider)` | 注入延迟工厂（多环境切换）                      |
| `resetDefaults()`                                    | 恢复默认工厂（`YuntowerAuthFactory.fromEnv()`） |

#### 多环境/单测注入示例

```java
LoginEntry.configureProviders(
    () -> YuntowerAuthFactory.create(appIdA, secretA, jwtA, redisA),
    () -> new ApiTokenManager(new TokenRepository(JpaTemplate.fromEnv("A", true, ApiTokenEntity.class)))
);

// 用例结束后恢复默认
LoginEntry.resetDefaults();
```

### LoginType

| 常量                 | params                  |
| -------------------- | ----------------------- |
| `LoginType.YUNTOWER` | `Map.of("code", "xxx")` |

### LoginResult

| 方法                | 说明             |
| ------------------- | ---------------- |
| `isSuccess()`       | 是否成功         |
| `getCode()`         | 错误码（0=成功） |
| `getMessage()`      | 错误消息         |
| `getAccessToken()`  | 访问凭证         |
| `getRefreshToken()` | 刷新凭证         |
| `getUserId()`       | 用户唯一标识     |
| `getNickname()`     | 昵称             |
| `getAvatar()`       | 头像 URL         |

### LoginUser

| 方法             | 说明               |
| ---------------- | ------------------ |
| `getUserId()`    | 用户唯一标识       |
| `getJti()`       | JWT 唯一 ID        |
| `get(key)`       | 额外字段           |
| `getString(key)` | 额外字段（String） |

### TokenStore

| 方法                     | 说明                            |
| ------------------------ | ------------------------------- |
| `save(userId, jti, ttl)` | 存 jti（覆盖旧值 = 单设备登录） |
| `getJti(userId)`         | 查当前有效 jti                  |
| `remove(userId)`         | 退出登录                        |

### Redis

```java
import com.pgaot.account.auth.core.redis.Redis;
Redis redis = new Redis();  // 自动读 CODE_AUTH_REDIS_URI
redis.set("key", "value", 3600);
```

---

### API Token（第三方令牌）

```java
import com.pgaot.account.auth.core.token.scope.Scope;

// 创建 — 仅展示一次完整 token
TokenInfo t = LoginEntry.tokens().create("alice", "数据表",
    List.of(Scope.Datasheet.DATA));
String token = t.getToken(); // pat_xxx... 存好

// 校验
String userId = LoginEntry.tokens().validate(token, "datasheet:data");

// 管理
LoginEntry.tokens().list("alice");        // 查看所有
LoginEntry.tokens().revoke("alice", id);  // 吊销
```

| Scope            | 值               | 说明         |
| ---------------- | ---------------- | ------------ |
| `Datasheet.DATA` | `datasheet:data` | 数据表读写删 |
| `SUPER`          | `*:*:*`          | 超级管理员   |

---

## 项目结构

```
code-auth/
└── src/main/java/com/pgaot/account/auth/
    ├── api/
    │   ├── LoginEntry.java              # 入口
    │   ├── LoginType.java               # 登录方式常量
    │   ├── ApiTokenManager.java         # API Token 管理
    │   ├── model/
    │   │   ├── LoginResult.java         # 登录返回
    │   │   └── LoginUser.java           # 校验返回
    │   └── store/
    │       ├── TokenStore.java          # 存储接口
    │       └── RedisTokenStore.java     # Redis 实现
    │
    ├── core/
    │   ├── LoginService.java            # 核心引擎
    │   ├── jwt/
    │   │   ├── JwtUtil.java             # JWT 生成/校验
    │   │   ├── TokenClaims.java         # JWT payload
    │   │   └── TokenPair.java           # access + refresh
    │   ├── strategy/
    │   │   ├── LoginStrategy.java       # 策略接口
    │   │   ├── StrategyRegistry.java    # 注册中心
    │   │   └── UserInfo.java            # 认证结果
    │   ├── redis/Redis.java             # 通用缓存
    │   ├── token/                        # API Token
    │   │   ├── TokenGenerator.java       # pat_ 生成 + SHA-256
    │   │   ├── TokenStore.java           # MySQL CRUD
    │   │   └── scope/Scope.java          # 权限范围
    │   └── yuntower/
    │       ├── YuntowerAuthFactory.java # 工厂
    │       └── YuntowerStrategy.java    # 云塔实现
    │
    ├── common/
    │   ├── code/IResultCode.java        # 结果码接口
    │   ├── code/ErrorCode.java          # 错误码
    │   ├── config/LoginConfig.java      # JWT 配置
    │   ├── constants/AuthConstants.java  # 常量（环境变量 Key）
    │   ├── constants/Messages.java      # 提示信息常量
    │   └── util/Assert.java             # 参数校验
    │
    └── exception/LoginException.java    # 异常
```

## License

GPL-3.0
