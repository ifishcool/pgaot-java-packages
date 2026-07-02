# 表管理

## 功能

创建、删除、修改数据表结构。元数据只存 ds_table，列信息从 INFORMATION_SCHEMA 实时读取。

## API

```java
TableInfo t = engine.tables().create("tenant_a", "sales", "销售表", "描述", columns);
engine.tables().drop("tenant_a", tableId);
engine.tables().rename("tenant_a", tableId, "new_name");
engine.tables().truncate("tenant_a", tableId);
engine.tables().addColumn("tenant_a", tableId, column);
engine.tables().dropColumn("tenant_a", tableId, "col_name");
engine.tables().renameColumn("tenant_a", tableId, "old", "new");
engine.tables().list("tenant_a");
engine.tables().get(tableId);
engine.tables().setMode("tenant_a", tableId, TableMode.READ_ONLY);
```

## 建表流程

```
TableApi.create("tenant_a", "sales", title, desc, columns)
    │
    ├─ 校验: 表名非空、列非空、无同名表
    ├─ 拼 DDL: CREATE TABLE tenant_a_sales (id..., product VARCHAR(512), amount DECIMAL(20,4))
    ├─ code-sql 执行
    ├─ INSERT INTO ds_table (name, title, owner_id, description)
    └─ 返回 TableInfo
```

## 删表流程

```
TableApi.drop("tenant_a", tableId)
    ├─ checkOwner → 非 owner 拒绝
    ├─ DROP TABLE IF EXISTS tenant_a_sales
    └─ DELETE FROM ds_table
```

## 列管理

| 操作 | DDL |
|---|---|
| addColumn | ALTER TABLE ADD COLUMN |
| dropColumn | ALTER TABLE DROP COLUMN |
| renameColumn | ALTER TABLE CHANGE COLUMN (类型从 INFORMATION_SCHEMA 查) |

## 列类型映射

| ColumnType | MySQL |
|---|---|
| STRING | VARCHAR(512) |
| TEXT | TEXT |
| INT | INT |
| BIGINT | BIGINT |
| TINYINT | TINYINT |
| DOUBLE | DOUBLE |
| DECIMAL | DECIMAL(20,4) |
| DATE | DATE |
| TIME | TIME |
| DATETIME | DATETIME |
| TIMESTAMP | TIMESTAMP |
| BOOLEAN | TINYINT(1) |
| JSON | JSON |

## 权限控制

表结构操作通过 `checkOwner` 校验，只有创建者可操作。

## 关键源码

| 文件 | 内容 |
|---|---|
| `TableApi.java` | 10 个方法 + checkOwner |
| `TableManager.java:13` | physicalName() |
| `TableManager.java:19-32` | createTable() |
| `TableManager.java:34-38` | dropTable() |
