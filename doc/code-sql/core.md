# SQL 执行、分页查询、批量操作

## SQL 执行

### 功能

一行代码执行任意 SQL，自动识别语句类型返回对应结果。

### 流程

```
db.sql("SELECT * FROM t_user WHERE id = ?", 1)
    │
    ▼
SqlTemplate.sql()
    ├─ requireNonBlank(sql)              ← null/isBlank → exception
    │
    ├─ template.execute(sql, params)     ← 委托执行
    │    │
    │    ├─ sql.trim().toUpperCase()
    │    │
    │    ├─ "SELECT" → queryForList → List<Map<String, Object>>
    │    ├─ "INSERT"|"UPDATE"|"DELETE" → update → int
    │    └─ 其他 → execute → void
    │
    └─ catch Exception
         ├─ message 含 "wall" → SqlException.wallBlocked()
         └─ 其他             → SqlException.executionFailed()
```

### 返回类型

| SQL 前缀 | 返回类型 | 值 |
|---|---|---|
| `SELECT` | `List<Map<String, Object>>` | key=列名, value=列值 |
| `INSERT` | `int` | 插入行数 |
| `UPDATE` | `int` | 更新行数 |
| `DELETE` | `int` | 删除行数 |
| 其他 | `null` | DDL 等 |

### 设计要点

- 泛型 `<T>` 返回，运行时强转，调用方需 `@SuppressWarnings("unchecked")`
- 防火墙在 Druid DataSource 代理层生效，对 execute 透明
- 异常分两类：Druid "wall" 关键字 → `wallBlocked()`，其余 → `executionFailed()`

### 参数校验

```java
// SqlTemplate.java
private static void requireNonBlank(String sql) {
    if (sql == null || sql.isBlank())
        throw SqlException.executionFailed(Messages.SQL_BLANK);
}
```

### 关键源码

| 文件 | 行数 | 内容 |
|---|---|---|
| `SqlTemplate.java:68-78` | 11 行 | sql() 入口 |
| `SqlTemplate.java:133-136` | 4 行 | requireNonBlank |
| `TemplateExecutor.java:20-26` | 7 行 | SQL 前缀路由 |

---

## 分页查询

### 功能

传入不含 LIMIT 的 SQL，自动查出总数和当前页数据。

### 流程

```
db.page("SELECT * FROM t_user WHERE age > ?", new PageQuery(1, 10), 18)
    │
    ▼
TemplateExecutor.page(sql, pq, params)
    │
    ├─ Step 1: 拼 COUNT
    │    "SELECT COUNT(*) FROM (" + sql + ") _t"
    │    → jdbc.queryForObject(countSql, Long.class, params) → total
    │
    ├─ Step 2: 拼分页
    │    sql + " LIMIT " + pq.getSize() + " OFFSET " + pq.getOffset()
    │    → jdbc.queryForList(pageSql, params) → rows
    │
    └─ Step 3: PageResponse.of(rows, total, page, size)
         → pages = ceil(total / size)
```

### PageQuery

```java
public class PageQuery {
    public PageQuery(int page, int size) {
        // page < 1 → exception
        // size < 1 || > 1000 → exception
    }
    public int getPage();    // 1-based
    public int getSize();    // 1~1000
    public int getOffset();  // (page - 1) * size
}
```

### PageResponse

```java
public class PageResponse<T> {
    List<T> rows;    // 当前页数据
    long total;      // 总记录数
    int page;        // 当前页码
    int size;        // 每页大小
    int pages;       // 总页数

    static <T> PageResponse<T> of(List<T> rows, long total, int page, int size);
    static <T> PageResponse<T> empty(int page, int size);
    <U> PageResponse<U> convert(Function<T, U> mapper);  // 类型转换
}
```

### 使用示例

```java
PageQuery pq = new PageQuery(1, 10);
PageResponse<Map<String, Object>> r = db.page("SELECT * FROM t_user", pq);
r.getRows();   // 当前页
r.getTotal();  // 总数
r.getPages();  // 总页数
```

### 设计要点

- COUNT 用子查询包裹：`SELECT COUNT(*) FROM (原SQL) _t`——兼容大多数 MySQL 场景
- LIMIT/OFFSET 是 MySQL 方言
- 分页约束写死在 `SqlConstants.Page`，PageQuery 引用常量——改一处全局生效

### 关键源码

| 文件 | 行数 | 内容 |
|---|---|---|
| `TemplateExecutor.java:38-55` | 18 行 | page() 完整实现 |
| `PageQuery.java:16-28` | 13 行 | 构造校验 + offset |
| `PageResponse.java` | 全 60 行 | of/empty/convert |

---

## 批量操作

### 功能

一条 SQL + 多组参数，一次网络往返批量写入。

### 流程

```
db.batch("INSERT INTO t_user (name, age) VALUES (?, ?)",
    List.of(new Object[]{"A", 1}, new Object[]{"B", 2}))
    │
    ▼
SqlTemplate.batch()
    ├─ requireNonBlank(sql)
    ├─ batch == null || isEmpty() → exception
    └─ jdbc.batchUpdate(sql, batch)
         └─ PreparedStatement.addBatch() + executeBatch()
```

### 设计要点

- 底层 `JdbcTemplate.batchUpdate()`，Spring 内部做 addBatch + executeBatch
- 校验 batch 不为空——提前给出中文提示
- 防火墙同样生效（经过 Druid DataSource）

### 关键源码

| 文件 | 行数 | 内容 |
|---|---|---|
| `SqlTemplate.java:93-103` | 11 行 | batch() |
