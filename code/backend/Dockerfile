# syntax=docker/dockerfile:1.4
# 需 BuildKit：docker compose build 默认开启；maven-host 由 compose additional_contexts 注入
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# 不使用 dependency:go-offline：在不稳定网络下易触发 Content-Length 截断导致整层失败
# maven.test.skip=true：跳过 test 编译与 surefire，避免反复拉取 surefire-common 失败
RUN --mount=type=bind,from=maven-host,source=.,target=/root/.m2/repository,rw \
    set -e; \
    for i in 1 2 3 4 5; do \
      if mvn -B -Dmaven.test.skip=true package; then exit 0; fi; \
      echo "mvn package failed, retry $$i/5 in 45s..."; \
      if [ "$$i" = "5" ]; then exit 1; fi; \
      sleep 45; \
    done

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
