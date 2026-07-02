# code-datasheet 异常体系

## 异常类

```
RuntimeException
  └── DatasheetException
       ├── code: int
       ├── message: String
       └── 7 个静态工厂方法
```

## 静态工厂

| 工厂方法 | 参数 | 错误码 | 场景 |
|---|---|---|---|
| `tableNotFound(id)` | String | `30_001_001` | 表不存在 |
| `tableNameDuplicate(name)` | String | `30_001_002` | 表名重复 |
| `notOwner()` | 无 | `30_001_003` | 非 owner 操作表结构 |
| `columnRequired(name)` | String | `30_001_006` | 删除必填列 |
| `rowValidationFailed(detail)` | String | `30_003_001` | 数据校验失败 |
| `sqlOperationDenied(op)` | String | `30_004_002` | SQL 操作不允许 |

## 错误码

| 编号 | 枚举名 | 说明 |
|---|---|---|
| `30_001_001` | `TABLE_NOT_FOUND` | 表不存在 |
| `30_001_002` | `TABLE_NAME_DUPLICATE` | 表名重复 |
| `30_001_003` | `TABLE_NOT_OWNER` | 只有创建者可操作 |
| `30_001_004` | `COLUMN_NOT_FOUND` | 列不存在 |
| `30_001_005` | `COLUMN_NAME_DUPLICATE` | 列名重复 |
| `30_001_006` | `COLUMN_REQUIRED` | 必填列不能删除 |
| `30_003_001` | `ROW_VALIDATION_FAILED` | 数据校验失败 |
| `30_003_002` | `ROW_COUNT_EXCEEDED` | 行数超限 |
| `30_004_001` | `SQL_TABLE_NOT_REGISTERED` | 表未注册 |
| `30_004_002` | `SQL_OPERATION_DENIED` | 操作类型不允许 |
| `30_004_003` | `SQL_PARSE_FAILED` | SQL 解析失败 |

编号段 `30_xxx_xxx`：`30_001` 表管理、`30_003` 数据操作、`30_004` SQL 执行。
