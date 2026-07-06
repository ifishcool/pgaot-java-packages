# 数据操作

## 功能

增删改行、SQL 执行、Jackson CSV/JSON 导入导出。

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
    ├─ 1. 查 MetadataStore → 表名 sales
    ├─ 2. 从 Map keys 提取列名
    ├─ 3. 拼 INSERT INTO tenant_a_sales (product, amount) VALUES (?, ?)
    └─ 4. code-sql batch() 执行（MySQL 原生类型校验）
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
    ├─ 1. SqlTableExtractor.parse(rawSql)
    │    └─ 操作类型校验 + Druid AST 提取 allNames/targetNames
    │
    ├─ 2. SqlPermissionChecker.validate(userId, parsed)
    │    └─ 归属校验 + 共享权限校验 + 模式校验
    │
    ├─ 3. SqlAstRewriter.rewrite(parsed, tableMap)
    │    └─ AST 节点级重写逻辑表名 → 物理表名
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
    ├─ 1. 权限校验：查表 → 非 owner 查 share(SELECT) → 模式检查(WRITE_ONLY拒绝)
    ├─ 2. 查表名 → tenant_a_sales
    ├─ 3. SELECT product, amount FROM tenant_a_sales WHERE amount > 50
    ├─ 4. 最多导出 100000 行，超限抛异常
    └─ 5. Jackson CsvMapper 格式化
```

## 导入流程

```
DataApi.importCsv("tenant_a", "123", csv)
    │
    ▼
ExportManager.parseCsv(csv)  → Jackson CsvMapper 解析为 List<Map>
    │
    ▼
RowManager.insert(rows)  → 批量插入
```

## 关键源码

| 文件                         | 内容                                                     |
| ---------------------------- | -------------------------------------------------------- |
| `DataApi.java`               | 7 个委托方法 + importCsv/importJson/updateCell/deleteRow |
| `RowManager.java:25-45`      | insert() + 批量，含共享 INSERT 权限校验                  |
| `RowManager.java:48-52`      | delete()，含共享 DELETE 权限校验                         |
| `RowManager.java:55-69`      | update()，含共享 UPDATE 权限校验                         |
| `SqlExecutor.java`           | 执行编排（解析 → 校验 → 重写 → 执行）                    |
| `SqlTableExtractor.java`     | Druid AST 解析 + allNames/targetNames 提取               |
| `SqlPermissionChecker.java`  | 归属/共享/模式校验                                       |
| `SqlAstRewriter.java`        | AST 节点级表名重写                                       |
| `SqlParsedQuery.java`        | SQL 解析结果模型                                         |
| `ExportManager.java:32-44`   | exportCsv() Jackson 格式化                               |
| `ExportManager.java:46-57`   | exportJson() Jackson 格式化                              |
| `ExportManager.java:70-92`   | query() 含权限/模式校验                                  |
| `ExportManager.java:100-115` | parseCsv() Jackson CsvMapper                             |
| `ExportManager.java:118-128` | parseJson() Jackson ObjectMapper                         |
