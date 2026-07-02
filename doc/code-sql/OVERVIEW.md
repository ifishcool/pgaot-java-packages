# code-sql 架构总览

## 概述

一行代码操作数据库，内置 Druid SQL 防火墙，支持分页、批量、多数据源、JPA。

## 架构图

```
┌─────────────────────────────────────────────┐
│                SqlTemplate                    │  ← 核心 API
│   sql() | page() | batch() | unsafe() | raw()│
└────────┬──────────────────┬─────────────────┘
         │                  │
         ▼                  ▼
┌─────────────────┐  ┌──────────────┐
│ TemplateExecutor │  │  RawExecutor  │
│ (JdbcTemplate)   │  │  (原生 JDBC)  │
└────────┬────────┘  └──────┬───────┘
         │                  │
         ▼                  ▼
┌─────────────────────────────────────────────┐
│              DruidDataSource                  │
│  ┌──────────────────────────────────────┐   │
│  │      WallFilter (代理层)              │   │
│  │  · SQL 语法解析                       │   │
│  │  · 规则匹配                           │   │
│  │  · 通过 / 拦截                        │   │
│  └──────────────────────────────────────┘   │
│  ┌──────────────────────────────────────┐   │
│  │       连接池 (DruidPool)              │   │
│  └──────────────────────────────────────┘   │
└────────────────────┬────────────────────────┘
                     │
                     ▼
                   MySQL
```

## 目录结构

```
code-sql/src/main/java/com/pgaot/sql/
├── api/                          ← 对外公开
│   ├── SqlTemplate.java          # 核心 API：sql/page/batch/unsafe/raw
│   ├── SqlTemplateConfig.java    # 防火墙配置：selectOnly/readWrite
│   └── JpaTemplate.java          # JPA 模板
├── core/executor/                ← 执行引擎
│   ├── TemplateExecutor.java     # JdbcTemplate 封装 + 分页
│   └── RawExecutor.java          # 原生 JDBC
├── support/                      ← 辅助类
│   ├── PageResponse.java         # rows/total/page/size/pages
│   └── PageQuery.java            # 页码校验 + offset 计算
├── common/
│   ├── config/EnvConfig.java     # 环境变量 → DataSource + 连接池
│   ├── code/IResultCode.java     # 错误码接口
│   ├── code/ErrorCode.java       # 错误码（20_xxx_xxx）
│   └── constants/
│       ├── SqlConstants.java     # Pool/Page/Batch 常量
│       └── Messages.java         # 提示信息常量
├── exception/SqlException.java   # SQL 异常（6 个工厂方法）
└── jpa/entity/UserEntity.java    # pgaot_user 实体
```

## 核心类速查

| 类 | 层级 | 职责 |
|---|---|---|
| `SqlTemplate` | api | 核心入口，5 个方法 |
| `SqlTemplateConfig` | api | 防火墙配置 |
| `JpaTemplate` | api | Hibernate CRUD |
| `TemplateExecutor` | core | JdbcTemplate + 分页 |
| `RawExecutor` | core | 原生 JDBC + 手动事务 |
| `PageResponse` | support | 分页响应 + convert |
| `PageQuery` | support | 分页参数校验 |
| `EnvConfig` | common | 环境变量 → DataSource |
| `SqlException` | exception | SQL 异常（6 个工厂方法） |

## 依赖

| 依赖 | 版本 | 用途 |
|---|---|---|
| druid | 1.2.23 | 连接池 + 防火墙 |
| spring-jdbc | 6.2.7 | JdbcTemplate |
| hibernate-core | 6.6.4 | JPA |
| mysql-connector-j | 9.7.0 | MySQL 驱动 |
| lombok | 1.18.36 | 编译期代码生成（provided） |
