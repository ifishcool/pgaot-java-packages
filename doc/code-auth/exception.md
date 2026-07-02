# code-auth 异常体系

## 异常类

```
RuntimeException
  └── LoginException
       ├── code: int (ErrorCode.getCode())
       ├── message: String
       └── 5 个静态工厂方法
```

## 静态工厂方法

| 工厂方法 | 参数 | 错误码 | 使用场景 |
|---|---|---|---|
| `authFailed(detail)` | String | `10_001_001` | 第三方认证失败 |
| `unsupportedType(type)` | String | `10_001_002` | 未注册的登录方式 |
| `configMissing(key)` | String | `10_001_004` | 缺少环境变量 |
| `tokenInvalid(detail)` | String | `10_002_001` | JWT 签名/过期/格式错误 |
| `tokenKicked()` | 无 | `10_002_003` | 被其他设备挤下线 |

## 使用规范

```java
// 正确: 调用方用工厂方法
throw LoginException.tokenKicked();
throw LoginException.tokenInvalid(e.getMessage());

// 错误: 不要直接 new + ErrorCode
throw new LoginException(ErrorCode.TOKEN_KICKED, "xxx");
```

## 错误码

| 编号 | 枚举名 | 说明 |
|---|---|---|
| `10_001_001` | `AUTH_FAILED` | 认证失败 |
| `10_001_002` | `AUTH_UNSUPPORTED_TYPE` | 不支持的登录方式 |
| `10_001_004` | `AUTH_CONFIG_MISSING` | 缺少配置 |
| `10_002_001` | `TOKEN_INVALID` | Token 无效或已过期 |
| `10_002_003` | `TOKEN_KICKED` | 账号在其他设备登录 |

编号段 `10_xxx_xxx`，子段 `10_001`=认证、`10_002`=Token。段内留白为后续相近错误预留。

## 关键源码

| 文件 | 内容 |
|---|---|
| `LoginException.java` | 异常类 + 工厂方法 |
| `ErrorCode.java` | 错误码枚举 + main() 去重校验 |
| `IResultCode.java` | 错误码接口 |
