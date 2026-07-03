# PGAOT Code Datasheet

[![JDK](https://img.shields.io/badge/JDK-21%2B-blue)](https://adoptium.net/)
[![License](https://img.shields.io/badge/license-GPL--3.0-green)](LICENSE)

多租户数据表协作平台 — 表前缀隔离 + 模式控制 + 共享权限 + SQL 防火墙。

---

## 环境要求

- JDK 21+
- Maven 3.6+
- MySQL 8.0+

## 安装

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/ifishcool/pgaot-java-packages</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.pgaot</groupId>
    <artifactId>code-datasheet</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 环境变量

```
CODE_SQL_URL        # MySQL 连接地址（必填）
CODE_SQL_USER       # 数据库用户名（必填）
CODE_SQL_PASS       # 数据库密码（必填）

# 多数据源
CODE_SQL_URL_MAIN   # 命名数据源 MAIN
```

---

## 快速开始

```java
// 多数据源支持
DatasheetEngine engine = DatasheetEngine.fromEnv();          // 默认数据源
DatasheetEngine engine2 = DatasheetEngine.fromEnv("MAIN");   // 命名数据源

// 建表
TableInfo t = engine.tables().create("alice", "sales", "销售表", null, List.of(
    new ColumnInfo("product", ColumnType.STRING, true),
    new ColumnInfo("amount",  ColumnType.NUMBER, false)
));

// 共享给 bob（只读）
engine.shares().share("alice", t.getId(), "bob", SharePermission.SELECT_ONLY);

// bob 用 SQL 查询
List<Map<String, Object>> rows = engine.data().sql("bob",
    "SELECT * FROM sales WHERE amount > 50");

// 导入导出
engine.data().importCsv("alice", t.getId(), "product,amount\nA,100");
String csv = engine.data().exportCsv("alice", t.getId(), null, null);

// 删除表
engine.tables().drop("alice", t.getId());
```

---

## API 参考

### TableApi

| 方法 | 说明 |
|---|---|
| `create(ownerId, name, title, desc, columns)` | 建表 |
| `drop(ownerId, tableId)` | 删除表 |
| `rename/truncate/addColumn/dropColumn/renameColumn` | 表结构操作，需 owner |
| `list(userId)` | 该用户所有表 |
| `listWithSource(userId)` | 所有表 + 来源(OWNED/SHARED) + 权限 |
| `get(tableId)` | 表结构（列信息实时从 INFORMATION_SCHEMA 读取） |
| `setMode(ownerId, tableId, TableMode)` | 模式控制 |

### ColumnType

| 类型 | MySQL |
|---|---|
| `STRING` | VARCHAR(512) |
| `TEXT` | TEXT |
| `INT` | INT |
| `BIGINT` | BIGINT |
| `TINYINT` | TINYINT |
| `DOUBLE` | DOUBLE |
| `DECIMAL` | DECIMAL(20,4) |
| `DATE` | DATE |
| `TIME` | TIME |
| `DATETIME` | DATETIME |
| `TIMESTAMP` | TIMESTAMP |
| `BOOLEAN` | TINYINT(1) |
| `JSON` | JSON |

### DataApi

| 方法 | 说明 |
|---|---|
| `insert(userId, tableId, row/rows)` | 插入（最多 1000 行） |
| `update/delete` | 按条件更新/删除 |
| `updateCell(userId, tableId, rowId, col, val)` | 更新指定行列 |
| `deleteRow(userId, tableId, rowId)` | 删除指定行 |
| `sql(userId, sql)` | 执行用户 SQL（SELECT/INSERT/UPDATE/DELETE，禁止 DDL） |
| `exportCsv/exportJson` | 导出 |
| `importCsv/importJson` | 导入 |
| `importCsv/importJson` | 导入 |

### ShareApi

| 方法 | 说明 |
|---|---|
| `share(ownerId, tableId, toUser, perm)` | 共享表 |
| `unshare(ownerId, tableId, toUser)` | 取消共享 |
| `list/listSent/listReceived` | 查看共享关系 |

### SharePermission

- `SharePermission.ALL` — 全部权限
- `SharePermission.SELECT_ONLY` — 只读
- `new SharePermission(S,I,U,D)` — 自定义

### TableMode

| 模式 | SELECT | INSERT/UPDATE | DELETE |
|---|---|---|---|
| `READ_ONLY` | 允许 | 禁止 | 禁止 |
| `WRITE_ONLY` | 禁止 | 允许 | 禁止 |
| `READ_WRITE` | 允许 | 允许 | 禁止 |
| `ALL`（默认） | 允许 | 允许 | 允许 |

---

## 隔离模型

```
表前缀: 物理表名 = userId_tableName

SQL 执行:
  Druid AST 提取表名 → 目标表/源表分离校验 → 表名替换 → readWriteDelete 防火墙执行

模式: READ_ONLY(禁写) / WRITE_ONLY(禁读) / READ_WRITE(默认)

共享: 细粒度权限(SELECT/INSERT/UPDATE/DELETE) + 共享关系查询

DDL 保护: checkOwner() 非 owner 拒绝
```

## 项目结构

```
code-datasheet/src/main/java/com/pgaot/datasheet/
├── api/
│   ├── DatasheetEngine.java          # 入口
│   ├── TableApi.java                 # 表管理
│   ├── DataApi.java                  # 数据操作
│   └── ShareApi.java                 # 共享管理
├── core/
│   ├── TableManager.java             # DDL
│   ├── RowManager.java               # 增删改 + 模式/共享校验
│   ├── SqlExecutor.java              # AST 提取 + 权限 + 替换
│   └── ExportManager.java            # 导入导出
├── metadata/
│   ├── MetadataStore.java            # 委托 code-sql JPA + INFO_SCHEMA
│   └── entity/                       # TableEntity, ShareEntity
├── common/
│   ├── code/ErrorCode.java           # 30_xxx_xxx
│   ├── config/DatasheetConfig.java   # admin + readWriteDelete 双连接
│   ├── constants/
│   └── model/                        # TableInfo, ColumnInfo, TableMode, SharePermission
└── exception/DatasheetException.java # 7 个静态工厂
```

## License

GPL-3.0
