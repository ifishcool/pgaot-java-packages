# 隔离模型

## 设计

两层隔离：表前缀 + 权限校验。

## 物理表命名

| 租户 | 逻辑表名 | 物理表名 |
|---|---|---|
| tenant_a | sales | tenant_a_sales |
| tenant_b | sales | tenant_b_sales |

## SQL 执行链路

```
engine.data().sql("tenant_a", "SELECT * FROM sales WHERE ...")
    │
    ├─ 1. Druid AST 解析 → 提取目标表 + 源表
    │    目标表: INSERT/UPDATE/DELETE 的主表（按写操作校验）
    │    源表:   FROM/JOIN/子查询中的表（按 SELECT 校验）
    │
    ├─ 2. 逐表校验
    │    ├─ 表归属: ds_table WHERE owner_id=?
    │    └─ 模式: READ_ONLY→禁写, WRITE_ONLY→禁读/禁DELETE
    │
    ├─ 3. 表名替换: sales → tenant_a_sales（正则 \\b 词边界）
    │
    └─ 4. readWrite 防火墙执行（禁止 DDL/DELETE）
```

## 目标表 vs 源表

INSERT/UPDATE/DELETE 语句区分目标表和源表，分别用不同操作类型校验：

```
INSERT INTO rw_table SELECT ... FROM ro_table
    → 目标表 rw_table: INSERT 校验
    → 源表 ro_table:   SELECT 校验（READ_ONLY 可读）

UPDATE rw_table JOIN wo_table SET ...
    → 目标表 rw_table: UPDATE 校验
    → 源表 wo_table:   SELECT 校验（WRITE_ONLY 禁读 → 拦截）
```

## DDL 保护

表结构操作通过 `checkOwner()` 校验。数据操作不做 owner 校验，由表前缀和表名归属自然隔离。

## 注入防护

- `engine.data().sql()` 走 readWrite 防火墙：Druid 拦截 DROP/ALTER/TRUNCATE/CREATE TABLE、多语句注入
- 表名替换用 `\\b` 词边界正则，不会误替换列名或字符串中的同名文本
- 子查询中的表名被 AST 完整提取，无法绕过归属校验
