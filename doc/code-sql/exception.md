# code-sql 异常体系

## 异常类

```
RuntimeException
  └── SqlException
       ├── code: int (ErrorCode.getCode())
       ├── message: String
       └── 6 个静态工厂方法
```

## 静态工厂方法

| 工厂方法 | 参数 | 错误码 | 使用场景 |
|---|---|---|---|
| `connectionFailed(detail)` | String | `20_001_001` | Druid 连接失败 |
| `envMissing(key)` | String | `20_001_002` | 环境变量缺失 |
| `executionFailed(detail)` | String | `20_002_001` | JdbcTemplate 执行异常 |
| `wallBlocked(detail)` | String | `20_003_001` | WallFilter 拦截 |
| `pageParamInvalid(detail)` | String | `20_004_001` | 页码/每页条数越界 |
| `jpaFailed(detail)` | String | `20_005_001` | Hibernate 操作异常 |

## 使用规范

```java
// 用工厂方法
throw SqlException.wallBlocked(e.getMessage());
throw SqlException.executionFailed("timeout");

// 不要直接 new + ErrorCode
throw new SqlException(ErrorCode.SQL_BLOCKED_BY_WALL.getCode(), msg);
```

## wall 关键字识别

`SqlTemplate` 中 `sql()` 和 `page()` 都检查异常 message 是否含 `"wall"`：

```java
if (e.getMessage() != null && e.getMessage().contains(Messages.WALL_KEYWORD))
```

Druid 抛出的 `WallCheckException` 和 `SQLNotAllowException` 的 message 都含 "wall"，这是唯一可靠的区分方式。

## 错误码

| 编号 | 枚举名 | 说明 |
|---|---|---|
| `20_001_001` | `CONNECTION_FAILED` | 数据库连接失败 |
| `20_001_002` | `ENV_MISSING` | 缺少环境变量 |
| `20_002_001` | `SQL_EXECUTION_FAILED` | SQL 执行失败 |
| `20_003_001` | `SQL_BLOCKED_BY_WALL` | SQL 被防火墙拦截 |
| `20_004_001` | `PAGE_PARAM_INVALID` | 分页参数无效 |
| `20_005_001` | `JPA_EXECUTION_FAILED` | JPA 操作失败 |

编号段 `20_xxx_xxx`，子段：
- `20_001` 连接/环境
- `20_002` SQL 执行
- `20_003` 防火墙
- `20_004` 分页
- `20_005` JPA

## 关键源码

| 文件 | 内容 |
|---|---|
| `SqlException.java` | 异常类 + 6 个工厂方法 |
| `ErrorCode.java` | 错误码枚举 + main() 去重校验 |
| `Messages.java` | 提示信息常量（含 WALL_KEYWORD） |
