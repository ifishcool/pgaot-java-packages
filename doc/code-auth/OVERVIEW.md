# code-auth 架构总览

## 概述

用户登录 → 策略模式认证 → JWT 生成 → Redis 写入 → 单设备管理。

## 架构图

```
┌─────────────────────────────────────────────────┐
│                    LoginEntry                     │  ← 唯一对外入口
│      (static: login/validate/logout + providers)  │
└────────────────────┬────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────┐
│                  LoginService                     │  ← 核心引擎
│   持有: StrategyRegistry + JwtUtil + TokenStore   │
│                                                   │
│   login(type, params)                              │
│     ├─ registry.get(type) → LoginStrategy         │
│     ├─ strategy.authenticate(params) → UserInfo   │
│     ├─ jwt.generate() → TokenPair                │
│     ├─ tokenStore.save()                          │
│     └─ return LoginResult                         │
│                                                   │
│   validate(token)                                  │
│     ├─ jwt.validate() → TokenClaims               │
│     ├─ tokenStore.getJti() → 比对 jti             │
│     └─ return LoginUser                           │
│                                                   │
│   logout(token)                                    │
│     ├─ jwt.validate() → TokenClaims               │
│     └─ tokenStore.remove()                        │
└─────────────────────────────────────────────────┘
         │              │              │
         ▼              ▼              ▼
┌────────────┐ ┌────────────┐ ┌──────────────┐
│ JwtUtil    │ │TokenStore  │ │StrategyReg.  │
│ (jjwt库)   │ │(Lettuce)   │ │(ConcurrentMap)│
└────────────┘ └────────────┘ └──────┬───────┘
                                     │
                                     ▼
                              ┌──────────────┐
                              │LoginStrategy │← 接口
                              ├──────────────┤
                              │YuntowerImpl  │← 当前唯一实现
                              └──────────────┘
```

## 目录结构

```
code-auth/src/main/java/com/pgaot/account/auth/
├── api/                          ← 对外公开
│   ├── LoginEntry.java           # 唯一入口，3 个 static 方法
│   ├── LoginType.java            # "yuntower" 常量
│   ├── model/
│   │   ├── LoginResult.java      # 成功/失败统一返回体
│   │   └── LoginUser.java        # 校验后当前用户
│   └── store/
│       ├── TokenStore.java       # Token 存储接口
│       └── RedisTokenStore.java  # Redis 实现
├── core/                         ← 内部引擎
│   ├── LoginService.java         # 核心引擎（50 行）
│   ├── jwt/
│   │   ├── JwtUtil.java          # 生成 + 校验
│   │   ├── TokenClaims.java      # JWT payload
│   │   └── TokenPair.java        # access + refresh
│   ├── strategy/
│   │   ├── LoginStrategy.java    # 策略接口
│   │   ├── StrategyRegistry.java # 注册中心
│   │   └── UserInfo.java         # 认证结果
│   ├── redis/Redis.java          # Lettuce 封装
│   └── yuntower/
│       ├── YuntowerAuthFactory.java  # 工厂
│       └── YuntowerStrategy.java     # 云塔实现
├── common/
│   ├── code/IResultCode.java     # 错误码接口
│   ├── code/ErrorCode.java       # 错误码（10_xxx_xxx）
│   ├── config/LoginConfig.java   # JWT 配置
│   ├── constants/AuthConstants.java  # 常量（环境变量 Key）
│   ├── constants/Messages.java   # 提示信息常量
│   └── util/Assert.java          # 参数校验
└── exception/LoginException.java # 异常（5 个工厂方法）
```

## 核心类速查

| 类                    | 层级      | 职责                                |
| --------------------- | --------- | ----------------------------------- |
| `LoginEntry`          | api       | 唯一入口，login/validate/logout     |
| `LoginService`        | core      | 认证引擎，协调策略/JWT/Redis        |
| `StrategyRegistry`    | core      | 策略注册与查找（ConcurrentHashMap） |
| `LoginStrategy`       | core      | 策略接口（1 个方法）                |
| `YuntowerStrategy`    | core      | 云塔 code → token → userInfo        |
| `YuntowerAuthFactory` | core      | 从环境变量创建 LoginService         |
| `JwtUtil`             | core      | JWT 生成/校验（jjwt 库）            |
| `TokenStore`          | api       | Token 存储接口                      |
| `RedisTokenStore`     | api       | Redis 实现（Lettuce）               |
| `Redis`               | core      | 通用 Redis 客户端                   |
| `LoginConfig`         | common    | JWT 密钥 + 过期时间                 |
| `LoginException`      | exception | 认证异常（5 个工厂方法）            |

## LoginEntry 注入能力

`LoginEntry` 支持两种运行时装配方式：

- `configure(service, tokenManager)`：注入固定实例（适合单测、隔离环境）
- `configureProviders(serviceProvider, tokenProvider)`：注入延迟工厂（适合多租户、多环境动态切换）

`resetDefaults()` 可恢复默认工厂（`YuntowerAuthFactory.fromEnv()`）。

## 依赖

| 依赖                      | 版本   | 用途     |
| ------------------------- | ------ | -------- |
| yuntower-account-java-sdk | 1.0.0  | 云塔 API |
| jjwt-api/impl/jackson     | 0.12.6 | JWT      |
| lettuce-core              | 6.4.1  | Redis    |
