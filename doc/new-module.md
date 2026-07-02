# 新增模块指南

## Checklist

1. [ ] 在 [GLOBAL.md](GLOBAL.md) 错误码编号段表中登记新编号段（与已有模块不重叠）
2. [ ] 根目录创建 `<module>/`，包含独立 `pom.xml`（`groupId=com.pgaot`）
3. [ ] 包名 `com.pgaot.<module>.<layer>`
4. [ ] 创建 `IResultCode` 接口 + `ErrorCode` 枚举（含 `main()` 去重校验）
5. [ ] 创建异常类 + 静态工厂方法（每个 ErrorCode 对应一个工厂方法）
6. [ ] 创建 `Messages` 常量类管理所有提示信息硬编码字符串
7. [ ] 创建数值常量类（`XxxConstants`），按内部类分组
8. [ ] 根 `README.md` 模块表添加一行
9. [ ] `.github/workflows/maven-publish.yml` 的 `tags` 列表加 `'<module>/v*'`
10. [ ] 写模块 `README.md` + `CHANGELOG.md`
11. [ ] 写测试
12. [ ] `doc/` 下创建对应模块文件夹及文档

## CI 工作原理

### Tag 命名

```yaml
# .github/workflows/maven-publish.yml
on:
  push:
    tags: ['code-sql/v*', 'code-auth/v*', 'new-module/v*']
```

### 执行流程

```
git tag code-sql/v1.0.1 && git push --tags
    │
    ▼
GitHub Actions 收到 tag push 事件
    │
    ▼
匹配到 'code-sql/v*'
    │
    ▼
Step: Parse module name from tag
    TAG=refs/tags/code-sql/v1.0.1
    MODULE=${TAG%%/*}           → MODULE=code-sql
    │
    ▼
mvn -f code-sql/pom.xml package
mvn -f code-sql/pom.xml deploy
```

### CI 完整内容

```yaml
name: Maven Package
on:
  push:
    tags: ['code-sql/v*', 'code-auth/v*']
jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
    - uses: actions/checkout@v4
    - name: Parse module name from tag
      run: |
        TAG=${GITHUB_REF#refs/tags/}
        MODULE=${TAG%%/*}
        echo "MODULE=$MODULE" >> $GITHUB_ENV
    - uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        server-id: github
        settings-path: ${{ github.workspace }}
    - run: mvn -B package -f $MODULE/pom.xml -s $GITHUB_WORKSPACE/settings.xml
      env:
        GITHUB_TOKEN: ${{ secrets.GH_PACKAGES_TOKEN }}
    - run: mvn deploy -f $MODULE/pom.xml -s $GITHUB_WORKSPACE/settings.xml
      env:
        GITHUB_TOKEN: ${{ secrets.GH_PACKAGES_TOKEN }}
```
