# 原生 JDBC、多数据源、连接池

## 原生 JDBC

### 功能

绕过 JdbcTemplate，直接操作 `Connection` / `PreparedStatement`，用于存储过程和手动事务。

### RawExecutor

```java
// 执行任意 SQL（存储过程等）
Object result = db.raw().execute("CALL sp_update_stats(?)", "2024-01");

// 手动事务
db.raw().transaction(() -> {
    db.sql("UPDATE t_account SET balance = balance - 100 WHERE id = 1");
    db.sql("UPDATE t_account SET balance = balance + 100 WHERE id = 2");
});
```

### execute() 实现

```java
public Object execute(String sql, Object... params) {
    try (Connection conn = dataSource.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        for (int i = 0; i < params.length; i++)
            ps.setObject(i + 1, params[i]);

        if (ps.execute())
            return toResultList(ps.getResultSet());  // 有结果集 → List<Map>
        return ps.getUpdateCount();                   // 无结果集 → 影响行数
    }
}
```

### transaction() 实现

```java
public void transaction(Runnable task) {
    try (Connection conn = dataSource.getConnection()) {
        conn.setAutoCommit(false);
        task.run();
        conn.commit();
    }
    // 注意：Exception 不自动回滚，调用方应在 Runnable 内处理
}
```

### 设计要点

- 和 TemplateExecutor 共享同一个 DruidDataSource——复用连接池和防火墙
- `toResultList()` 通过 `ResultSetMetaData` 动态读列名映射为 `List<Map>`
- Connection/PreparedStatement/ResultSet 用 try-with-resources 自动关闭

### 关键源码

| 文件 | 行数 | 内容 |
|---|---|---|
| `RawExecutor.java:24-32` | 9 行 | execute() |
| `RawExecutor.java:35-43` | 9 行 | transaction() |
| `RawExecutor.java:45-55` | 11 行 | toResultList() |

---

## 多数据源

### 功能

一套代码连接多个数据库，通过环境变量后缀区分。

### 命名规则

```
无后缀 = 默认:   CODE_SQL_URL / CODE_SQL_USER / CODE_SQL_PASS
有后缀 = 命名:   CODE_SQL_URL_MAIN / CODE_SQL_USER_MAIN / CODE_SQL_PASS_MAIN
```

### 使用

```java
SqlTemplate mainDb = new SqlTemplate(SqlTemplateConfig.fromEnv("MAIN"));
SqlTemplate logDb  = new SqlTemplate(SqlTemplateConfig.fromEnv("LOG"));
```

JPA 同样支持：

```java
JpaTemplate jpa = JpaTemplate.fromEnv("MAIN", UserEntity.class);
```

### 实现

```java
// EnvConfig.java
public static DruidDataSource createDataSource(String name) {
    String suffix = name != null && !name.isBlank() ? "_" + name : "";
    DruidDataSource ds = new DruidDataSource();
    ds.setUrl(env(URL + suffix));     // CODE_SQL_URL → CODE_SQL_URL_MAIN
    ds.setUsername(env(USER + suffix));
    ds.setPassword(env(PASS + suffix));
    // 连接池同样带后缀: CODE_SQL_POOL_MAX_ACTIVE_MAIN
    ds.setInitialSize(intEnv("CODE_SQL_POOL_INITIAL" + suffix, ...));
    // ...
}
```

### 设计要点

- 每个数据源是独立 `DruidDataSource` 实例——各自连接池
- 环境变量 Key 常量（`URL`/`USER`/`PASS`/`POOL_*`）在 `EnvConfig` 中 public，`JpaTemplate` 直接引用——避免重复定义

---

## 连接池配置

### 配置方式

所有参数通过环境变量设定，不设则用默认值。

### 必填参数

| 环境变量 | 说明 |
|---|---|
| `CODE_SQL_URL` | MySQL JDBC URL |
| `CODE_SQL_USER` | 数据库用户名 |
| `CODE_SQL_PASS` | 数据库密码 |

### 可选参数

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `CODE_SQL_POOL_INITIAL` | `5` | 初始连接数 |
| `CODE_SQL_POOL_MIN_IDLE` | `5` | 最小空闲连接 |
| `CODE_SQL_POOL_MAX_ACTIVE` | `20` | 最大活跃连接 |
| `CODE_SQL_POOL_MAX_WAIT` | `60000` | 获取连接最大等待 ms |

### 固定配置（不放开环境变量）

```java
// SqlConstants.Pool
EVICTION_RUN_MS       = 60000    // 60s 检测空闲连接
MIN_EVICTABLE_IDLE_MS = 300000   // 空闲 5 分钟可回收
VALIDATION_QUERY      = "SELECT 1"
testWhileIdle         = true     // 空闲时检测有效性
testOnBorrow          = false    // 借出不检测（避免性能损耗）
testOnReturn          = false    // 归还不检测
```

### 关键源码

| 文件 | 行数 | 内容 |
|---|---|---|
| `EnvConfig.java:31-51` | 21 行 | createDataSource() |
| `EnvConfig.java:53-57` | 5 行 | env() 读取 |
| `EnvConfig.java:59-62` | 4 行 | intEnv() 读取 |
| `SqlConstants.java:10-19` | 10 行 | Pool 常量 |
