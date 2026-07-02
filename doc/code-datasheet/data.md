# 数据操作

## 功能

增删改行、SQL 执行、CSV/JSON 导出。

## API

```java
engine.data().insert("tenant_a", tableId, row);
engine.data().insert("tenant_a", tableId, rows);          // 批量（最多 1000）
engine.data().update("tenant_a", tableId, whereClause, values);
engine.data().delete("tenant_a", tableId, whereClause);
SqlResult r = engine.data().sql("tenant_a", "SELECT * FROM sales WHERE amount > 100");
String csv  = engine.data().exportCsv("tenant_a", tableId, columns, whereClause);
String json = engine.data().exportJson("tenant_a", tableId, columns, whereClause);
```

## insert 流程

```
DataApi.insert("tenant_a", "123", {product:"A", amount:100})
    │
    ▼
RowManager.insert("tenant_a", 123, rows)
    │
    ├─ 1. 查 MetadataStore → 表名 sales + 列定义 [product(STRING), amount(NUMBER)]
    ├─ 2. 校验每行数据
    │    ├─ 必填列是否提供
    │    ├─ NUMBER → Double.valueOf()
    │    ├─ DATE → LocalDateTime.parse()
    │    ├─ BOOLEAN → Boolean.valueOf()
    │    └─ 类型不匹配 → ROW_VALIDATION_FAILED
    ├─ 3. 拼 INSERT INTO tenant_a_sales (product, amount) VALUES (?, ?)
    └─ 4. code-sql batch() 执行
```

## update / delete 流程

```
DataApi.update("tenant_a", "123", "amount < 100", {amount: 0})
    │
    ▼
RowManager.update(...)
    ├─ 查表名 → tenant_a_sales
    ├─ 拼 UPDATE tenant_a_sales SET amount = ? WHERE amount < 100
    └─ code-sql 执行

DataApi.delete("tenant_a", "123", "amount < 100")
    │
    ▼
RowManager.delete(...)
    ├─ DELETE FROM tenant_a_sales WHERE amount < 100
    └─ code-sql 执行
```

## sql 执行流程

```
DataApi.sql("tenant_a", "SELECT * FROM sales WHERE amount > 100")
    │
    ▼
SqlExecutor.execute("tenant_a", rawSql)
    │
    ├─ 1. 校验操作类型（仅允许 SELECT/INSERT/UPDATE/DELETE）
    │
    ├─ 2. Druid AST 解析
    │    SQLUtils.parseStatements(sql, MYSQL)
    │    → SQLSelectStatement
    │      └─ SQLExprTableSource(expr="sales")
    │
    ├─ 3. 表名替换
    │    列出该用户所有表 → {sales, orders}
    │    遍历 AST 节点，匹配到逻辑表名 → 替换为 tenant_a_sales
    │    SQLUtils.toSQLString()
    │
    └─ 4. code-sql sql() 执行
```

## 导出流程

```
DataApi.exportCsv("tenant_a", "123", ["product","amount"], "amount > 50")
    │
    ▼
ExportManager.exportCsv(...)
    │
    ├─ 1. 查表名 → tenant_a_sales
    ├─ 2. SELECT product, amount FROM tenant_a_sales WHERE amount > 50
    ├─ 3. 最多导出 50000 行，超限抛异常
    └─ 4. 拼 CSV 字符串
         product,amount
         "A",100
         "B",200
```

## 关键源码

| 文件 | 内容 |
|---|---|
| `DataApi.java` | 7 个委托方法 |
| `RowManager.java:25-45` | insert() + 批量 |
| `RowManager.java:48-52` | delete() |
| `RowManager.java:55-69` | update() |
| `RowManager.java:77-87` | convertValue() |
| `SqlExecutor.java:26-56` | execute() + Druid AST 替换 |
| `SqlExecutor.java:58-97` | rewrite() + AST 遍历 |
| `ExportManager.java:23-38` | exportCsv() |
| `ExportManager.java:41-60` | exportJson() |
