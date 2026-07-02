# PGAOT Code Datasheet

[![JDK](https://img.shields.io/badge/JDK-21%2B-blue)](https://adoptium.net/)
[![License](https://img.shields.io/badge/license-GPL--3.0-green)](LICENSE)

多租户数据表协作平台 — 表前缀隔离 + 模式控制 + SQL 防火墙。

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

~/.m2/settings.xml 需配置 GitHub Token（同 code-auth/code-sql）。

## 环境变量

```
CODE_SQL_URL    # MySQL 连接地址（必填）
CODE_SQL_USER   # 数据库用户名（必填）
CODE_SQL_PASS   # 数据库密码（必填）
```

---

## 快速开始

```java
DatasheetEngine engine = DatasheetEngine.fromEnv();

// 建表
TableInfo t = engine.tables().create("tenant_a", "sales", "销售表", null, List.of(
    new ColumnInfo("product", ColumnType.STRING, true),
    new ColumnInfo("amount",  ColumnType.NUMBER, false)
));

// 设只读模式
engine.tables().setMode("tenant_a", t.getId(), TableMode.READ_ONLY);

// 数据操作
engine.data().insert("tenant_a", t.getId(), Map.of("product", "A", "amount", 100));
List<Map<String, Object>> rows = engine.data().sql("tenant_a",
    "SELECT * FROM sales WHERE amount > 50");

// 导出
String csv = engine.data().exportCsv("tenant_a", t.getId(), null, null);
```

---

## API 参考

### TableApi

| 方法 | 说明 |
|---|---|
| `create(ownerId, name, title, desc, columns)` | 建表 |
| `drop(ownerId, tableId)` | 删表，需 owner |
| `rename(ownerId, tableId, newName)` | 重命名，需 owner |
| `truncate(ownerId, tableId)` | 清空，需 owner |
| `addColumn(ownerId, tableId, column)` | 加列，需 owner |
| `dropColumn(ownerId, tableId, columnName)` | 删列（必填列不可删），需 owner |
| `renameColumn(ownerId, tableId, oldName, newName)` | 重命名列，需 owner |
| `list(userId)` | 该用户所有表 |
| `get(tableId)` | 表结构 |
| `setMode(ownerId, tableId, TableMode)` | 模式控制 |

### DataApi

| 方法 | 说明 |
|---|---|
| `insert(userId, tableId, row)` | 插入单行 |
| `insert(userId, tableId, rows)` | 批量插入（最多 1000 行） |
| `update(userId, tableId, whereClause, values)` | 按条件更新 |
| `delete(userId, tableId, whereClause)` | 按条件删除 |
| `sql(userId, sql)` | 执行 SQL（SELECT/INSERT/UPDATE），禁止 DDL/DELETE |
| `exportCsv(userId, tableId, columns, whereClause)` | 导出 CSV |
| `exportJson(userId, tableId, columns, whereClause)` | 导出 JSON |

### TableMode

| 模式 | SELECT | INSERT/UPDATE | DELETE |
|---|---|---|---|
| `READ_ONLY` | 允许 | 禁止 | 禁止 |
| `WRITE_ONLY` | 禁止 | 允许 | 禁止 |
| `READ_WRITE`（默认） | 允许 | 允许 | 允许 |

---

## 隔离模型

```
建表: 逻辑名 sales → 物理表 tenant_a_sales

SQL:  SELECT * FROM sales
      → Druid AST 提取表名 [sales]
      → 逐表校验: owner归属 + TableMode
      → 表名替换: sales → tenant_a_sales
      → readWrite 防火墙执行

DDL:  checkOwner() → 非 owner 拒绝
```

---

## 项目结构

```
code-datasheet/src/main/java/com/pgaot/datasheet/
├── api/
│   ├── DatasheetEngine.java          # 入口
│   ├── TableApi.java                 # 表管理
│   └── DataApi.java                  # 数据操作
├── core/
│   ├── TableManager.java             # DDL
│   ├── RowManager.java               # 增删改行 + 模式校验
│   ├── SqlExecutor.java              # Druid AST 表名提取 + 权限 + 替换
│   └── ExportManager.java            # CSV/JSON
├── metadata/
│   ├── MetadataStore.java            # ds_table / ds_column CRUD
│   └── entity/                       # TableEntity, ColumnEntity
├── common/
│   ├── code/IResultCode.java         # 结果码接口
│   ├── code/ErrorCode.java           # 30_xxx_xxx
│   ├── config/DatasheetConfig.java   # 配置
│   ├── constants/
│   └── model/                        # TableInfo, ColumnInfo, TableMode
└── exception/DatasheetException.java # 7 个静态工厂
```

## License

GPL-3.0
