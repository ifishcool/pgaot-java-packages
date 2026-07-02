# 发布流程

## 发布前检查

```bash
# 1. 编译
cd <module>
mvn compile test-compile

# 2. 跑测试
mvn exec:java -Dexec.mainClass="com.pgaot.<module>.XxxTest" -Dexec.classpathScope="test"

# 3. ErrorCode 去重校验（每次必做）
java -cp target/classes com.pgaot.<module>.common.code.ErrorCode
# 输出: 错误码校验通过，共 X 个

# 4. 更新 CHANGELOG.md
# 5. git add + git commit
```

## 发布

```bash
git tag <模块>/v<版本>
git push --tags
```

示例：

```bash
git tag code-sql/v1.0.1
git tag code-auth/v1.0.2
```

## CI 自动完成

- `mvn package` — 编译 + 打包
- `mvn deploy` — 发布到 GitHub Packages (`https://maven.pkg.github.com/ifishcool/pgaot-java-packages`)

## 前置条件

GitHub 仓库 Settings → Secrets and variables → Actions → 配置 `GH_PACKAGES_TOKEN`。

Token 创建路径：

```
GitHub → Settings → Developer settings → Personal access tokens
→ Tokens (classic) → Generate new token
→ 勾选 read:packages + write:packages
→ 生成后填入 Actions Secret
```

## 本地使用发布后的包

`~/.m2/settings.xml`：

```xml
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>你的GitHub用户名</username>
            <password>你的Token</password>
        </server>
    </servers>
</settings>
```

`pom.xml`：

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/ifishcool/pgaot-java-packages</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.pgaot</groupId>
    <artifactId>code-sql</artifactId>
    <version>1.0.0</version>
</dependency>
```
