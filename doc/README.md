# PGAOT Java 开发文档

## 总览

| 文档 | 内容 |
|---|---|
| [GLOBAL.md](GLOBAL.md) | 全局约定：错误码编号段、包名规范、异常规范、常量管理 |
| [REFERENCE.md](REFERENCE.md) | 环境变量速查表、依赖版本矩阵 |

## code-auth — 通用认证框架

| 文档 | 内容 |
|---|---|
| [code-auth/OVERVIEW.md](code-auth/OVERVIEW.md) | 架构总览、目录结构、核心类速查 |
| [code-auth/login.md](code-auth/login.md) | 登录认证（流程 / 设计 / 实现） |
| [code-auth/jwt-and-session.md](code-auth/jwt-and-session.md) | JWT 生成/校验、单设备登录、退出登录、Token 存储 |
| [code-auth/strategy.md](code-auth/strategy.md) | 策略模式：扩展新登录方式 |
| [code-auth/redis-and-config.md](code-auth/redis-and-config.md) | Redis 客户端、配置体系 |
| [code-auth/exception.md](code-auth/exception.md) | 异常体系、错误码完整列表 |
| [code-auth/token.md](code-auth/token.md) | API Token：第三方令牌创建/校验/吊销 |

## code-sql — 通用 SQL 引擎

| 文档 | 内容 |
|---|---|
| [code-sql/OVERVIEW.md](code-sql/OVERVIEW.md) | 架构总览、目录结构、核心类速查 |
| [code-sql/core.md](code-sql/core.md) | SQL 执行、分页查询、批量操作 |
| [code-sql/firewall.md](code-sql/firewall.md) | Druid 防火墙：原理、预设、自定义 |
| [code-sql/advanced.md](code-sql/advanced.md) | 原生 JDBC、多数据源、连接池配置 |
| [code-sql/jpa.md](code-sql/jpa.md) | JPA 模式 |
| [code-sql/exception.md](code-sql/exception.md) | 异常体系、错误码完整列表 |

## code-datasheet — 多租户数据表平台

| 文档 | 内容 |
|---|---|
| [code-datasheet/OVERVIEW.md](code-datasheet/OVERVIEW.md) | 架构总览、目录结构、核心类速查 |
| [code-datasheet/table.md](code-datasheet/table.md) | 表管理：建表/删表/改列/模式控制 |
| [code-datasheet/data.md](code-datasheet/data.md) | 数据操作：增删改行、SQL 执行、导出 |
| [code-datasheet/isolation.md](code-datasheet/isolation.md) | 隔离模型：表前缀 + 共享权限校验 |
| [code-datasheet/exception.md](code-datasheet/exception.md) | 异常体系、错误码完整列表 |
| [code-datasheet/share.md](code-datasheet/share.md) | 共享功能：分享/取消/查看/权限控制 |

## code-log — 日志框架

| 文档 | 内容 |
|---|---|
| [code-log/OVERVIEW.md](code-log/OVERVIEW.md) | 架构总览、目录结构、核心类速查 |


## 工程实践

| 文档 | 内容 |
|---|---|
| [new-module.md](new-module.md) | 新增模块 Checklist + CI 工作原理 |
| [testing.md](testing.md) | 测试规范、运行方式 |
| [release.md](release.md) | 发布流程 |
