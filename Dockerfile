# ──── build stage ────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# 分层复制，利用 Docker 缓存
COPY pom.xml .
COPY code-sql/pom.xml code-sql/pom.xml
COPY code-sql/src     code-sql/src
COPY code-auth/pom.xml code-auth/pom.xml
COPY code-auth/src     code-auth/src
COPY code-datasheet/pom.xml code-datasheet/pom.xml
COPY code-datasheet/src     code-datasheet/src
COPY code-log/pom.xml code-log/pom.xml
COPY code-log/src     code-log/src
COPY code-web/pom.xml code-web/pom.xml
COPY code-web/src     code-web/src

# 按依赖顺序安装 + 打包
RUN mvn install -pl code-sql -DskipTests -q && \
    mvn install -pl code-auth,code-datasheet,code-log -DskipTests -q && \
    mvn package -pl code-web -DskipTests -q

# ──── runtime stage ────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/code-web/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
