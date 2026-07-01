# Changelog

## v1.0.0 (2026-07-01)

首个正式版本。

### 功能

- 策略模式认证框架（LoginStrategy 接口，扩展新登录只需实现接口）
- 内置云塔 OAuth 登录（YuntowerStrategy，调用 yuntower-account-java-sdk）
- JWT 生成 + 解析 + 校验（JwtUtil，jjwt 0.12.6）
- 单设备登录（TokenStore + jti 机制，每次登录覆盖旧值）
- 双 Token 支持（access_token + refresh_token，TTL 可配置，默认 7 天）
- InMemoryTokenStore（开发/单机，ConcurrentHashMap）
- RedisTokenStore（生产，Lettuce 6.4.1，持久化 + 自动过期）
- 通用 Redis 缓存工具（Redis 类：字符串/自增/Hash/List/Set）
- TokenStore.key() 统一 Redis Key 管理
- 完整错误码枚举（ErrorCode，按模块分区：100xxx/200xxx）
- 统一错误消息常量（Messages，按模块分类：ENV/AUTH/TOKEN/STRATEGY）
- 静态门面 API（LoginEntry.login/validate/logout）
- LoginResult.isSuccess() 不抛异常判断成败
- 环境变量驱动配置（YUNTOWER_APP_ID / CODE_AUTH_REDIS_URI / CODE_AUTH_TOKEN_TTL）
- 接口完整 Javadoc 注释
- 交互式登录测试（LoginTest：自动打开浏览器 → 授权 → 全流程）
- TokenStore 功能测试（TokenStoreTest，Redis 读写验证）
- JWT 单元测试（JwtUtilTest）

### 依赖

| 依赖 | 版本 | 说明 |
|---|---|---|
| jjwt-api | 0.12.6 | JWT |
| lettuce-core | 6.4.1 | Redis |
| yuntower-account-java-sdk | 1.0.0 | 云塔 OAuth |
| jackson-databind | 2.18.3 | JSON（云塔 SDK 传递） |
