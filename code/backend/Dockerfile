# 不声明 # syntax=docker/dockerfile:*：部分私有镜像源对 docker/dockerfile 返回 401，会导致构建在解析阶段失败。
# 不使用 RUN --mount：避免依赖 Dockerfile 前端镜像；依赖缓存请多次构建或在本机 mvn 后 COPY jar（另做 CI 脚本）。
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# maven.test.skip=true：跳过 test 与 surefire，减少插件下载
RUN set -e; \
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
