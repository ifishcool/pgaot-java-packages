# Changelog

## v1.0.0 (2026-07-02)

首个正式版本。

### 功能

**数据表管理**
- 建表/删表/重命名/清空（TableApi）
- 列管理（增/删/重命名列，类型映射 STRING/NUMBER/DATE/BOOLEAN）
- 列信息实时从 INFORMATION_SCHEMA 读取（无 ds_column 冗余）
- 模式控制（READ_ONLY / WRITE_ONLY / READ_WRITE）

**数据操作**
- 增删改行（DataApi：insert/update/delete）
- 用户原始 SQL 执行（DataApi.sql，Druid AST 提取表名 + 权限校验 + 表名替换）
- readWriteDelete 防火墙保护（允许增删改查，禁止 DDL）
- 列类型由 MySQL 原生校验

**共享权限**
- 细粒度共享权限（SharePermission：SELECT/INSERT/UPDATE/DELETE 任意组合）
- 共享/取消共享（ShareApi：share/unshare）
- 查看共享关系（listSent/listReceived/list）
- listWithSource 返回所有表 + 来源标记 + 权限

**导入导出**
- CSV/JSON 导出（exportCsv/exportJson）
- CSV/JSON 导入（importCsv/importJson）
- 导出再导入支持数据迁移

**隔离与安全**
- 表前缀隔离（userId_tableName）
- Druid AST 表名提取（目标表/源表区分校验）
- 跨租户访问拦截
- SQL 注入/DDL 防火墙拦截（由 code-sql readWriteDelete 模式提供）
- Owner 校验保护 DDL 操作

**元数据**
- ds_table + ds_share 自动建表
- 错误码分段枚举（30_xxx_xxx），含 main() 去重校验
- DatasheetException 统一异常（7 个静态工厂）

**测试**
- 27 步全 SQL 共享测试（ShareTest）
- 68 项 SQL 安全测试（SqlSecurityTest）
- 导入导出测试（ImportExportTest）
- 22 步集成隔离测试（IsolationTest）

### 依赖

| 依赖 | 版本 | 说明 |
|---|---|---|
| code-sql | 1.0.0 | SQL 执行引擎（含 Druid 解析器 + readWriteDelete） |
| lombok | 1.18.36 | 编译期代码生成（provided） |
