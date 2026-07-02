# code-datasheet 架构总览

## 概述

多租户数据表平台 — 表前缀隔离 + Druid AST 表名提取/权限校验 + owner 保护。

## 架构图

```
┌──────────────────────────────────┐
│         DatasheetEngine           │
│       tables() / data()           │
└────┬───────────────┬─────────────┘
     │               │
     ▼               ▼
┌──────────┐  ┌────────────┐
│ TableApi │  │   DataApi   │
│ create   │  │ insert/upd  │
│ drop     │  │ delete      │
│ rename   │  │ sql(raw)    │
│ truncate │  │ exportCsv   │
│ addCol   │  │ exportJson  │
│ dropCol  │  └─────┬──────┘
│ list/get │        │
│ setMode  │        ▼
└────┬─────┘  ┌──────────┐  ┌──────────┐  ┌──────────┐
     │        │RowManager│  │SqlExecutor│  │Export    │
     │        │增删改+    │  │AST提取+   │  │Manager   │
     │        │类型校验   │  │权限校验+  │  │CSV/JSON  │
     │        │+模式校验  │  │表名替换   │  │          │
     ▼        └────┬─────┘  └─────┬────┘  └────┬─────┘
┌──────────┐      │              │             │
│  Table   │      ▼              ▼             ▼
│  Manager │  ┌──────────────────────────────┐
│  DDL     │  │       MetadataStore           │
└────┬─────┘  │    ds_table + ds_column       │
     │        └──────────────┬───────────────┘
     ▼                       │
┌──────────────────────────────────────────┐
│              code-sql                     │
│         adminSql + readWriteSql           │
└────────────────────┬─────────────────────┘
                     │
                     ▼
                   MySQL
```

## 隔离模型

```
表前缀: tenant_a 建 sales → 物理表 tenant_a_sales
        tenant_b 建 sales → 物理表 tenant_b_sales

SQL 执行链路:
  用户 SQL → Druid AST 解析
    → 提取目标表(INSERT/UPDATE/DELETE主表) + 源表(SELECT FROM/JOIN/子查询)
    → 目标表按写操作校验模式，源表按 SELECT 校验模式
    → 表名替换: sales → tenant_a_sales
    → readWrite 防火墙执行（禁止 DDL/DELETE）

DDL 保护:
  删表/重命名/清空/改列 → checkOwner() → 非 owner 拒绝
```

## 模式控制

| 模式 | SELECT | INSERT/UPDATE | DELETE |
|---|---|---|---|
| READ_ONLY | 允许 | 禁止 | 禁止 |
| WRITE_ONLY | 禁止 | 允许 | 禁止 |
| READ_WRITE | 允许 | 允许 | 允许 |

## 连接模型

| 连接 | 用途 | 防火墙 |
|---|---|---|
| adminSql | DDL / insert/update/delete(内部生成) / export | 全开放 |
| readWriteSql | engine.data().sql() 用户原始SQL | readWrite (禁DDL/DELETE) |

## 目录结构

```
code-datasheet/src/main/java/com/pgaot/datasheet/
├── api/
│   ├── DatasheetEngine.java          # 入口
│   ├── TableApi.java                 # 表管理
│   └── DataApi.java                  # 数据操作
├── core/
│   ├── TableManager.java             # DDL 生成
│   ├── RowManager.java               # 增删改 + 模式校验
│   ├── SqlExecutor.java              # AST提取 + 权限 + 替换
│   └── ExportManager.java            # CSV/JSON
├── metadata/
│   ├── MetadataStore.java            # 元数据 CRUD（含自动建表）
│   └── entity/                       # TableEntity, ColumnEntity
├── common/
│   ├── code/IResultCode.java         # 结果码接口
│   ├── code/ErrorCode.java           # 30_xxx_xxx
│   ├── config/DatasheetConfig.java   # 双连接配置
│   ├── constants/
│   └── model/                        # TableInfo, ColumnInfo, TableMode
└── exception/DatasheetException.java # 7 个静态工厂

test/
├── IsolationTest.java                # 22 步集成测试
└── SqlSecurityTest.java              # 68 项安全测试
```

## 依赖

| 依赖 | 版本 | 用途 |
|---|---|---|
| code-sql | 1.0.0 | SQL 执行引擎（含 Druid） |
| lombok | 1.18.36 | 编译期代码生成（provided） |
