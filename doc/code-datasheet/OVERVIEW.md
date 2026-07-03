# code-datasheet 架构总览

## 概述

多租户数据表平台 — 表前缀隔离 + Druid AST 表名提取/权限校验 + 细粒度共享 + Jackson 导入导出。

## 架构图

```
┌───────────────────────────────────────┐
│         DatasheetEngine                │  ← AutoCloseable
│       tables() / data() / shares()     │
│       fromEnv() / close()              │
└────┬───────────────┬──────────────────┘
     │               │
     ▼               ▼
┌──────────┐  ┌──────────────┐  ┌──────────┐
│ TableApi │  │   DataApi    │  │ ShareApi │
│ create   │  │ insert/update │  │ share    │
│ drop     │  │ delete        │  │ unshare  │
│ rename   │  │ sql(raw)      │  │ list     │
│ truncate │  │ updateCell    │  │ listReceived│
│ addCol   │  │ deleteRow     │  │ listSent │
│ dropCol  │  │ import/export │  └────┬─────┘
│ list/get │  └─────┬────────┘       │
│ setMode  │        │                │
└────┬─────┘        ▼                ▼
     │       ┌──────────┐  ┌──────────────┐  ┌──────────┐
     │       │RowManager│  │ SqlExecutor  │  │ Export   │
     │       │增删改+    │  │ AST提取+     │  │ Manager  │
     │       │模式+共享  │  │ 权限+模式    │  │ Jackson  │
     │       │权限校验   │  │ +表名替换    │  │ CSV/JSON │
     ▼       └────┬─────┘  └─────┬────────┘  └────┬─────┘
┌──────────┐     │              │                 │
│  Table   │     ▼              ▼                 ▼
│  Manager │  ┌────────────────────────────────────┐
│  DDL     │  │       MetadataStore                 │
└────┬─────┘  │    ds_table + ds_share              │
     │        └──────────────┬─────────────────────┘
     ▼                       │
┌──────────────────────────────────────────────────┐
│              code-sql                             │
│   adminSql(无WallFilter, maxActive=2)             │
│   + readWriteSql(readWriteDelete)                 │
│   + metaJpa(Hibernate, ds_table/ds_share)         │
└────────────────────┬─────────────────────────────┘
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
    → readWriteDelete 防火墙执行（禁止 DDL）

权限链路:
  owner → 直接操作（仅受模式限制）
  shared → 检查 ds_share 中具体权限（SELECT/INSERT/UPDATE/DELETE）
  导出 → 检查 owner/share 的 SELECT 权限 + 模式（WRITE_ONLY 拒绝）
```

## 模式控制

| 模式 | SELECT | INSERT/UPDATE | DELETE |
|---|---|---|---|
| READ_ONLY | 允许 | 禁止 | 禁止 |
| WRITE_ONLY | 禁止 | 允许 | 禁止 |
| READ_WRITE | 允许 | 允许 | 禁止 |
| ALL | 允许 | 允许 | 允许 |

## 连接模型

| 连接 | 用途 | 防火墙 | 连接池 |
|---|---|---|---|
| adminSql | DDL / insert/update/delete(内部生成) | 无 | maxActive=2 |
| readWriteSql | engine.data().sql() 用户原始SQL | readWriteDelete (禁DDL) | 默认 20 |
| metaJpa | ds_table/ds_share 元数据 | 无(Hibernate) | Hibernate 内置池 |

- WallFilter 在 DataSource 创建时通过 `EnvConfig.createDataSource` 附加，SqlTemplate 不修改 DataSource
- `DatasheetEngine` 实现 `AutoCloseable`，`close()` 关闭所有三个连接池
- admin 池仅 2 个连接（DDL 低频操作）

## 目录结构

```
code-datasheet/src/main/java/com/pgaot/datasheet/
├── api/
│   ├── DatasheetEngine.java          # 入口 (AutoCloseable)
│   ├── TableApi.java                 # 表管理
│   ├── DataApi.java                  # 数据操作
│   └── ShareApi.java                 # 共享管理
├── core/
│   ├── TableManager.java             # DDL 生成
│   ├── RowManager.java               # 增删改 + 模式/共享权限校验
│   ├── SqlExecutor.java              # AST提取 + 权限 + 替换
│   └── ExportManager.java            # 导入导出 (Jackson)
├── metadata/
│   ├── MetadataStore.java            # 元数据 CRUD（委托 JPA Repository）
│   └── entity/                       # TableEntity, ShareEntity
├── common/
│   ├── code/IResultCode.java         # 结果码接口
│   ├── code/ErrorCode.java           # 30_xxx_xxx (12 个)
│   ├── config/DatasheetConfig.java   # 三连接配置 + close()
│   ├── constants/                    # Messages, DatasheetConstants
│   └── model/                        # TableInfo, ColumnInfo, TableMode, SharePermission
└── exception/DatasheetException.java # 8 个静态工厂
```

## 依赖

| 依赖 | 版本 | 用途 |
|---|---|---|
| code-sql | 1.0.0 | SQL 执行引擎（含 Druid + JPA） |
| jackson-databind | 2.18.3 | JSON 导入导出 |
| jackson-dataformat-csv | 2.18.3 | CSV 导入导出 |
| lombok | 1.18.36 | 编译期代码生成（provided） |
