# 全局约定

## 错误码编号段

每个模块独占一个十万位段，**绝对禁止跨段**。

| 编号段 | 模块 | 子段分配 |
|---|---|---|
| `10_xxx_xxx` | code-auth | `10_001` 认证, `10_002` Token |
| `20_xxx_xxx` | code-sql | `20_001` 连接, `20_002` 执行, `20_003` 防火墙, `20_004` 分页, `20_005` JPA |
| `30_xxx_xxx` | 预留 | — |

规则：
- 子段用中间三位区分模块内部的子模块
- 每个 `ErrorCode` 枚举必须含 `main()` 方法做去重校验
- **新增模块前必须先登记编号段，再写代码**

## 包名约定

```
com.pgaot.<module>.<layer>
```

| 层 | 可见性 | 放什么 |
|---|---|---|
| `api` | public | 用户直接调用的入口类 |
| `api/model` | public | 返回值 DTO |
| `api/store` | public | 存储接口 |
| `core` | package-private 优先 | 内部引擎 |
| `common/code` | public | `IResultCode` + `ErrorCode` |
| `common/config` | public | 配置类 |
| `common/constants` | public | 常量（`Messages` / `SqlConstants`） |
| `common/util` | public | 工具类 |
| `exception` | public | 异常类（含静态工厂） |
| `support` | public | 辅助类 |

## 异常规范

每个模块只定义一个异常类。**调用方不应直接 `new` 异常并传 `ErrorCode`**——错误码内聚在静态工厂方法中。

```java
// 正确
throw LoginException.tokenKicked();
throw SqlException.wallBlocked(e.getMessage());

// 错误
throw new LoginException(ErrorCode.TOKEN_KICKED, "xxx");
throw new SqlException(ErrorCode.SQL_BLOCKED_BY_WALL.getCode(), msg);
```

## 常量管理

- **数值常量**（默认值、范围、超时）→ `XxxConstants` 类，用内部类分组
- **提示信息字符串**→ `Messages` 类
- **环境变量 Key** → 放在对应 Config 类中

## 编码风格

- JDK 21，可用 `var`、switch 表达式、records、text blocks
- 构造函数超过 3 个参数考虑用 Builder 或工厂
- public 方法必须有 Javadoc（`@param` + `@return`）
- 不写显而易见的注释（如 `// 获取用户名` 对应 getter）
- 设计决策的 WHY 写注释，WHAT 看方法签名
