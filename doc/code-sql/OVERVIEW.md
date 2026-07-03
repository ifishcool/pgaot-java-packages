# code-sql 架构总览

## 概述

一行代码操作数据库，内置 Druid SQL 防火墙，支持分页、批量、多数据源、JPA、事务、连接池生命周期管理。

## 架构图

```
┌──────────────────────────────────────────────────┐
│                SqlTemplate                         │  ← 核心 API (AutoCloseable)
│   sql() | page() | batch() | unsafe() | raw()     │
│   close()                                          │
└────────┬──────────────────┬──────────────────────┘
         │                  │
         ▼                  ▼
┌──────────────────┐  ┌──────────────┐
│ TemplateExecutor  │  │  RawExecutor  │
│ (JdbcTemplate)    │  │  (原生 JDBC)   │
│ execute/page/batch│  │  transaction  │
└────────┬─────────┘  │  transactionCall│
         │            └──────┬────────┘
         ▼                   │
┌──────────────────────────────────────────────────┐
│              DruidDataSource                       │
│  ┌───────────────────────────────────────────┐   │
│  │      WallFilter (代理层，DS 创建时附加)     │   │
│  └───────────────────────────────────────────┘   │
│  ┌───────────────────────────────────────────┐   │
│  │       连接池 (DruidPool)                   │   │
│  └───────────────────────────────────────────┘   │
└────────────────────┬─────────────────────────────┘
                     │
                     ▼
                   MySQL
```

## 目录结构

```
code-sql/src/main/java/com/pgaot/sql/
├── api/                          ← 对外公开
│   ├── SqlTemplate.java          # 核心 API + AutoCloseable
│   ├── SqlTemplateConfig.java    # 防火墙配置 + 延迟创建 DS
│   └── JpaTemplate.java          # JPA 模板
├── core/executor/                ← 执行引擎
│   ├── TemplateExecutor.java     # JdbcTemplate 封装 + 分页
│   └── RawExecutor.java          # 原生 JDBC + 事务(rollback)
├── support/                      ← 辅助类
│   ├── PageResponse.java         # rows/total/page/size/pages + convert
│   └── PageQuery.java            # 页码校验 + offset 计算
├── common/
│   ├── config/EnvConfig.java     # 环境变量 → DataSource + WallFilter
│   │                             #   createDataSource(name, wallConfig, maxActive)
│   │                             #   autoDdl(name) → 读 CODE_SQL_AUTO_DDL
│   ├── code/IResultCode.java     # 错误码接口
│   ├── code/ErrorCode.java       # 错误码（20_xxx_xxx）
│   └── constants/
│       ├── SqlConstants.java     # Pool/Page/Batch 常量
│       └── Messages.java         # 提示信息常量
├── exception/SqlException.java   # SQL 异常（6 个工厂方法）
├── jpa/entity/                   # JPA 实体
│   ├── ApiTokenEntity.java       # api_token
│   ├── UserEntity.java           # pgaot_user
│   ├── DsTableEntity.java        # ds_table
│   └── DsShareEntity.java        # ds_share
└── jpa/repository/               # JPA 仓储
    ├── TokenRepository.java      # API Token CRUD
    ├── UserRepository.java       # 用户 upsert
    ├── TableRepository.java      # ds_table CRUD
    └── ShareRepository.java      # ds_share CRUD
```

## 核心类速查

| 类 | 层级 | 职责 |
|---|---|---|
| `SqlTemplate` | api | 核心入口，sql/page/batch/unsafe/raw，实现 AutoCloseable |
| `SqlTemplateConfig` | api | 防火墙配置，DS 延迟创建（WallFilter 在 DS 创建时附加） |
| `JpaTemplate` | api | Hibernate CRUD，autoDdl 由环境变量控制 |
| `TemplateExecutor` | core | JdbcTemplate + 分页 |
| `RawExecutor` | core | 原生 JDBC + transaction(Callable) 含显式 rollback |
| `PageResponse` | support | 分页响应 + convert + empty |
| `PageQuery` | support | 分页参数校验（page>=1, size 1-1000） |
| `EnvConfig` | common | 环境变量 → DataSource，支持 WallConfig + maxActive 重载 |
| `TokenRepository` | jpa | API Token 创建/查找/吊销(touchLastUsed) |
| `SqlException` | exception | SQL 异常（6 个工厂方法） |

## 安全设计

- **WallFilter 不修改 DataSource**：`SqlTemplate` 构造函数不再调用 `setProxyFilters`。WallFilter 在 `EnvConfig.createDataSource(name, wallConfig, maxActive)` 中附加。
- **SqlTemplateConfig 延迟创建 DS**：`fromEnv(name).readWriteDelete()` 先配置 WallConfig，`getDataSource()` 时一次性创建带 WallFilter 的 DS。
- **事务显式 rollback**：`RawExecutor.transaction(Callable)` 失败时调用 `conn.rollback()`，不依赖连接关闭时的隐式行为。

## 依赖

| 依赖 | 版本 | 用途 |
|---|---|---|
| druid | 1.2.23 | 连接池 + 防火墙 |
| spring-jdbc | 6.2.7 | JdbcTemplate |
| hibernate-core | 6.6.4 | JPA |
| mysql-connector-j | 9.7.0 | MySQL 驱动 |
| lombok | 1.18.36 | 编译期代码生成（provided） |
| JUnit Jupiter | 5.11.4 | 单元测试（test） |
