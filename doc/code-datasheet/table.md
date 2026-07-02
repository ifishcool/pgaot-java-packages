# 表管理

## 功能

创建、删除、修改数据表结构，建表时自动 GRANT 权限给租户。

## API

```java
TableInfo t = engine.tables().create("tenant_a", "sales", "销售表", "描述", columns);
engine.tables().drop("tenant_a", tableId);
engine.tables().rename("tenant_a", tableId, "new_name");
engine.tables().truncate("tenant_a", tableId);
engine.tables().addColumn("tenant_a", tableId, column);
engine.tables().dropColumn("tenant_a", tableId, "col_name");
engine.tables().renameColumn("tenant_a", tableId, "old", "new");
engine.tables().list("tenant_a");          // 该租户所有表
engine.tables().get(tableId);              // 表结构详情
```

## 建表流程

```
TableApi.create("tenant_a", "sales", title, desc, columns)
    │
    ▼
TableManager.createTable(...)
    │
    ├─ 1. 校验：表名非空、列非空、该用户无同名表
    │
    ├─ 2. 拼 DDL
    │    CREATE TABLE tenant_a_sales (id BIGINT AUTO_INCREMENT PRIMARY KEY, ...)
    │    → code-sql 执行
    │
    ├─ 3. GRANT 授权
    │    GRANT SELECT, INSERT, UPDATE, DELETE ON tenant_a_sales TO 'tenant_a'@'%'
    │
    ├─ 4. 写元数据
    │    INSERT INTO ds_table (name, title, owner_id, description)
    │    INSERT INTO ds_column × N
    │
    └─ 5. 返回 TableInfo (id + name + columns...)
```

## 删表流程

```
TableApi.drop("tenant_a", tableId)
    │
    ├─ checkOwner → 非 owner 拒绝
    ├─ DROP TABLE IF EXISTS tenant_a_sales
    └─ DELETE FROM ds_column + ds_table
```

## 列管理

| 操作 | 元数据 | DDL |
|---|---|---|
| addColumn | INSERT ds_column | ALTER TABLE ADD COLUMN |
| dropColumn | DELETE ds_column | ALTER TABLE DROP COLUMN |
| renameColumn | UPDATE ds_column SET name | ALTER TABLE CHANGE COLUMN |

`dropColumn` 不允许删除必填列（`COLUMN_REQUIRED`）。

## 列类型映射

| ColumnType | MySQL | 说明 |
|---|---|---|
| STRING | VARCHAR(512) | 字符串 |
| NUMBER | DECIMAL(20,4) | 数值 |
| DATE | DATETIME | 日期时间 |
| BOOLEAN | TINYINT(1) | 布尔 |

## 权限控制

表结构操作（drop/rename/truncate/addColumn/dropColumn/renameColumn）通过 `checkOwner` 校验：只有表的创建者可以修改结构，其他用户调用直接抛 `TABLE_NOT_OWNER`。

## 关键源码

| 文件 | 内容 |
|---|---|
| `TableApi.java` | 9 个方法 + checkOwner |
| `TableManager.java:20-22` | physicalName() |
| `TableManager.java:26-41` | createTable() + GRANT |
| `TableManager.java:43-47` | dropTable() |
| `TableManager.java:85-94` | renameTable() |
