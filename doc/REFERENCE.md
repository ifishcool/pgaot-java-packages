# 参考速查

## 环境变量

### code-auth

| 变量 | 必填 | 默认值 | 说明 |
|---|---|---|---|
| `YUNTOWER_APP_ID` | 必填 | — | 云塔应用 ID |
| `YUNTOWER_APP_SECRET` | 必填 | — | 云塔应用密钥 |
| `CODE_AUTH_JWT_SECRET` | 必填 | — | JWT 签名密钥（≥32字符） |
| `CODE_AUTH_REDIS_URI` | 必填 | — | Redis 连接地址 |
| `CODE_AUTH_TOKEN_TTL` | 可选 | `604800`（7天） | Token 有效秒数 |
| `CODE_AUTH_KEY_PREFIX` | 可选 | `login:token` | Redis Key 前缀 |

### code-sql

| 变量 | 必填 | 默认值 | 说明 |
|---|---|---|---|
| `CODE_SQL_URL` | 必填 | — | MySQL JDBC URL |
| `CODE_SQL_USER` | 必填 | — | 数据库用户名 |
| `CODE_SQL_PASS` | 必填 | — | 数据库密码 |
| `CODE_SQL_POOL_INITIAL` | 可选 | `5` | 初始连接数 |
| `CODE_SQL_POOL_MIN_IDLE` | 可选 | `5` | 最小空闲连接 |
| `CODE_SQL_POOL_MAX_ACTIVE` | 可选 | `20` | 最大活跃连接 |
| `CODE_SQL_POOL_MAX_WAIT` | 可选 | `60000` | 获取连接最大等待 ms |
| `CODE_SQL_AUTO_DDL` | 可选 | `false` | Hibernate 自动建表（生产应设 false） |

多数据源加 `_NAME` 后缀，如 `CODE_SQL_URL_MAIN`、`CODE_SQL_POOL_MAX_ACTIVE_MAIN`。

## 依赖版本矩阵

### code-auth

| 依赖 | 版本 | 用途 |
|---|---|---|
| yuntower-account-java-sdk | 1.0.0 | 云塔 API |
| jjwt-api | 0.13.0 | JWT 生成/解析 |
| jjwt-impl | 0.13.0 | JWT 实现（runtime） |
| jjwt-jackson | 0.13.0 | JWT JSON（runtime） |
| lettuce-core | 7.6.0 | Redis 客户端 |
| code-sql | 1.0.0 | SQL 引擎（JPA） |
| JUnit Jupiter | 5.11.4 | 单元测试（test） |

### code-sql

| 依赖 | 版本 | 用途 |
|---|---|---|
| druid | 1.2.28 | 连接池 + 防火墙 |
| spring-jdbc | 6.2.1 | JdbcTemplate |
| hibernate-core | 6.6.4 | JPA |
| mysql-connector-j | 9.7.0 | MySQL 驱动 |
| lombok | 1.18.46 | 编译期代码生成（provided） |
| JUnit Jupiter | 5.11.4 | 单元测试（test） |

### code-datasheet

| 依赖 | 版本 | 用途 |
|---|---|---|
| code-sql | 1.0.0 | SQL 执行引擎 |
| jackson-databind | 2.22.0 | JSON 导入导出 |
| jackson-dataformat-csv | 2.22.0 | CSV 导入导出 |
| lombok | 1.18.46 | 编译期代码生成（provided） |

### code-log

| 依赖 | 版本 | 用途 |
|---|---|---|
| code-sql | 1.0.0 | JPA 审计持久化 |
| slf4j-api | 2.0.17 | 日志门面（MDC） |
| lombok | 1.18.46 | 编译期代码生成（provided） |
| JUnit Jupiter | 5.11.4 | 单元测试（test） |

### code-web

| 依赖 | 版本 | 用途 |
|---|---|---|
| code-auth | 1.0.0 | 认证 |
| code-datasheet | 1.0.0 | 数据表 |
| code-log | 1.0.0 | 审计日志 |
| spring-boot-starter-web | 3.4.1 | REST API |
| spring-boot-starter-aop | 3.4.1 | AspectJ AOP |
| knife4j-openapi3-jakarta | 4.5.0 | API 文档 |
