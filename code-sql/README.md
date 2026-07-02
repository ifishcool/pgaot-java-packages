# PGAOT Code SQL

[![JDK](https://img.shields.io/badge/JDK-21%2B-blue)](https://adoptium.net/)
[![License](https://img.shields.io/badge/license-GPL--3.0-green)](LICENSE)

PGAOT平台通用 SQL 引擎 — Druid 防火墙 + SQL自定义 + JPA 支持。

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
    <artifactId>code-sql</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 环境变量

```
CODE_SQL_URL              # MySQL 连接地址（必填）
CODE_SQL_USER             # 数据库用户名（必填）
CODE_SQL_PASS             # 数据库密码（必填）
```

多数据源加 `_NAME` 后缀即可，如 `CODE_SQL_URL_MAIN`。

> 连接池参数可通过 `CODE_SQL_POOL_INITIAL` / `CODE_SQL_POOL_MIN_IDLE` / `CODE_SQL_POOL_MAX_ACTIVE` / `CODE_SQL_POOL_MAX_WAIT` 覆盖，均含默认值。

---

## 快速开始

```java
import com.pgaot.sql.api.SqlTemplate;
import com.pgaot.sql.api.SqlTemplateConfig;
import com.pgaot.sql.support.PageQuery;
import com.pgaot.sql.support.PageResponse;

import java.util.List;
import java.util.Map;

// 只读模式 — 仅允许 SELECT，拦截注入与危险操作
SqlTemplate safe = new SqlTemplate(SqlTemplateConfig.fromEnv().selectOnly());

// 读写模式 — 允许增删改，禁止 DDL/DELETE/危险函数
SqlTemplate rw = new SqlTemplate(SqlTemplateConfig.fromEnv().readWrite());

// 读写删模式 — 允许增删改查，禁止 DDL 和危险函数
SqlTemplate rwd = new SqlTemplate(SqlTemplateConfig.fromEnv().readWriteDelete());

// 自定义防火墙 — 精细控制
SqlTemplateConfig config = new SqlTemplateConfig(
    SqlTemplateConfig.fromEnv().getDataSource());
config.selectOnly();
config.getWallConfig().setSelectHavingAlwayTrueCheck(false); // 关闭 HAVING 检查
SqlTemplate custom = new SqlTemplate(config);

// 查询 → List<Map>
List<Map<String, Object>> rows = db.sql(
    "SELECT name, age FROM t_user WHERE age > ? LIMIT ?", 18, 10);

// 分页 → PageResponse
PageResponse<Map<String, Object>> page = db.page(
    "SELECT * FROM t_user WHERE age > ?", new PageQuery(1, 10), 18);
System.out.println("第 " + page.getPage() + " 页，共 " + page.getTotal() + " 条");

// 增删改 → 影响行数
int n = db.sql("UPDATE t_user SET name = ? WHERE id = ?", "张三", 1);

// 批量
db.batch("INSERT INTO t_user (name, age) VALUES (?, ?)", List.of(
    new Object[]{"张三", 25},
    new Object[]{"李四", 30}
));

// 绕过防火墙
db.unsafe("TRUNCATE TABLE t_log");

// 手动事务
db.raw().transaction(() -> {
    db.sql("UPDATE t_account SET balance = balance - 100 WHERE id = 1");
    db.sql("UPDATE t_account SET balance = balance + 100 WHERE id = 2");
});
```

### JPA 模式

```java
import com.pgaot.sql.api.JpaTemplate;
import com.pgaot.sql.jpa.entity.UserEntity;

JpaTemplate jpa = JpaTemplate.fromEnv(UserEntity.class);

// 新增 — 对接 code-auth 登录返回
UserEntity u = new UserEntity();
u.setUserId("user_10001");
u.setNickname("测试用户");
u.setAvatar("https://cdn.example.com/avatar/10001.png");
jpa.save(u);

// 查询
jpa.findAll(UserEntity.class)
    .forEach(row -> System.out.println(row.getNickname()));

// 更新
u.setNickname("新昵称");
jpa.update(u);

// 删除
jpa.delete(UserEntity.class, u.getId());
jpa.close();
```

---

## API 参考

### SqlTemplate

| 方法 | 返回 | 说明 |
|---|---|---|
| `sql(sql, params...)` | `List<Map>` 或 `int` | 自动识别类型，防火墙检查 |
| `page(sql, pq, params...)` | `PageResponse` | 分页查询，自动 COUNT + LIMIT/OFFSET |
| `unsafe(sql, params...)` | `List<Map>` 或 `int` | 绕过防火墙，管理员专用 |
| `batch(sql, batch)` | void | 批量执行 |
| `raw()` | RawExecutor | 原生 JDBC 执行器 |

### SqlTemplateConfig

| 方法 | 说明 |
|---|---|
| `fromEnv()` | 从 `CODE_SQL_URL/USER/PASS` 创建 |
| `fromEnv(String name)` | 从 `CODE_SQL_URL_{NAME}` 创建命名数据源 |
| `selectOnly()` | 仅允许 SELECT，禁止写操作、DDL、危险函数 |
| `readWrite()` | 允许 SELECT/INSERT/UPDATE，禁止 DELETE/DDL/危险函数 |
| `readWriteDelete()` | 允许增删改查，禁止 DDL 和危险函数 |

### PageQuery

```java
PageQuery pq = new PageQuery(1, 10); // 第1页，每页10条
pq.getPage();   // 1
pq.getSize();   // 10
pq.getOffset(); // 0（LIMIT 的 OFFSET 起始位置）
```

页码范围 1~N，每页大小范围 1~1000，越界抛 `SqlException`。

### PageResponse

```java
PageResponse<Map<String, Object>> r = db.page(sql, pq, params);

r.getRows();   // List<Map<String, Object>>
r.getTotal();  // 总记录数
r.getPage();   // 当前页码
r.getSize();   // 每页大小
r.getPages();  // 总页数
```

### JpaTemplate

| 方法 | 说明 |
|---|---|
| `findAll(Class<T>)` | 查询全部 |
| `findById(Class<T>, Object id)` | 按主键查询 |
| `save(Object)` | 新增 |
| `update(Object)` | 更新 (merge) |
| `delete(Class<T>, Object id)` | 删除 |
| `query(String hql, Class<T>, Object... params)` | HQL 查询 |

### RawExecutor

| 方法 | 说明 |
|---|---|
| `execute(sql, params...)` | 执行任意 SQL |
| `transaction(Runnable)` | 手动事务，自动提交/回滚 |

---

## 异常处理

`SqlException` 通过静态工厂方法创建，语义清晰：

```java
import com.pgaot.sql.exception.SqlException;

SqlException.wallBlocked("DROP TABLE 被拦截");      // 20_003_001
SqlException.executionFailed("timeout");            // 20_002_001
SqlException.connectionFailed("Access denied");     // 20_001_001
SqlException.envMissing("CODE_SQL_URL");            // 20_001_002
SqlException.pageParamInvalid("页码必须 >= 1");      // 20_004_001
SqlException.jpaFailed("query error");              // 20_005_001
```

---

## 安全模型

```
SQL 请求 → Druid WallFilter → 通过 → JdbcTemplate → MySQL
                    ↓
                  拦截 → SqlException.wallBlocked()
```

### selectOnly() 拦截

- **写操作**: INSERT, UPDATE, DELETE, SET, MERGE
- **DDL**: DROP TABLE, ALTER TABLE, TRUNCATE, CREATE TABLE, RENAME TABLE
- **其他**: GRANT, REVOKE, FLUSH, KILL, CALL
- **绕过手法**: 注释、大小写、多空格、反引号、制表符、多语句注入等 50+ 种

### readWrite() 拦截

放行 INSERT/UPDATE/DELETE/SET，拦截全部 DDL 和危险函数。

---

## 项目结构

```
code-sql/
├── sql/init.sql                        # 建表 SQL
└── src/main/java/com/pgaot/sql/
    ├── api/
    │   ├── SqlTemplate.java            # 核心 API（SQL/分页/批量/unsafe）
    │   ├── SqlTemplateConfig.java      # 防火墙安全配置
    │   └── JpaTemplate.java            # JPA 模板
    │
    ├── core/executor/
    │   ├── TemplateExecutor.java       # JdbcTemplate 封装 + 分页
    │   └── RawExecutor.java            # 原生 JDBC（存储过程/事务）
    │
    ├── support/
    │   ├── PageResponse.java           # 分页响应
    │   └── PageQuery.java              # 分页参数校验
    │
    ├── common/
    │   ├── config/EnvConfig.java       # 环境变量 + 连接池
    │   ├── code/IResultCode.java       # 结果码接口
    │   ├── code/ErrorCode.java         # 错误码（分段 + 去重）
    │   └── constants/
    │       ├── SqlConstants.java       # 数值常量
    │       └── Messages.java           # 提示信息常量
    │
    ├── exception/SqlException.java     # 异常（6 个静态工厂）
    └── jpa/entity/UserEntity.java      # PGAOT 用户实体
```

## License

GPL-3.0
