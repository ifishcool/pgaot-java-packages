# API Token 第三方令牌

## 功能

为第三方系统签发长期访问令牌，替代用户密码。支持创建/校验/吊销/列表管理。

## Token 格式

```
pat_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

- `pat_` 前缀，方便日志审计
- 32 字节随机 + Base64URL 编码
- SHA-256 哈希存入 MySQL，原始值不存储
- 创建时仅展示一次完整 token

## 权限控制

两个 Scope：

| 常量 | 值 | 说明 |
|---|---|---|
| `Scope.Datasheet.DATA` | `datasheet:data` | 数据表读写删 |
| `Scope.SUPER` | `*:*:*` | 超级管理员 |

Scope 匹配支持通配符 `*`，新增模块时在 `Scope` 类中添加 static inner class 即可。

## API

```java
// 创建
TokenInfo t = LoginEntry.tokens().create("alice", "数据表",
    List.of(Scope.Datasheet.DATA));

// 校验
String userId = LoginEntry.tokens().validate(token, "datasheet:data");

// 吊销
LoginEntry.tokens().revoke("alice", tokenId);

// 列表
LoginEntry.tokens().list("alice");
```

## 存储

- MySQL 表 `api_token`（首次使用自动 CREATE TABLE IF NOT EXISTS）
- 字段：id, user_id, name, token_hash, prefix, scopes, last_used, expires_at, created_at
- `token_hash` 唯一索引，用于快速校验

## 校验流程

```
ApiTokenManager.validate(token, requiredScope)
    │
    ├─ TokenGenerator.hash(token) → SHA-256
    ├─ TokenStore.findByHash(hash) → 查 MySQL
    ├─ row == null → LoginException.apiTokenInvalid()
    ├─ Scope.matchesAny(scopes, required) → 通配符匹配
    ├─ updateLastUsed() → 记录最后使用时间
    └─ return userId
```

## 关键源码

| 文件 | 内容 |
|---|---|
| `ApiTokenManager.java` | 对外 API |
| `TokenGenerator.java` | pat_ 生成 + SHA-256 |
| `TokenStore.java` | MySQL CRUD（code-sql） |
| `scope/Scope.java` | 权限范围解析 + 匹配 |
