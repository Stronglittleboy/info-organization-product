# syntax=docker/dockerfile:1.4
# 多阶段：① Maven 打包 ② JRE 运行镜像
# 需 BuildKit；宿主机仓库通过 compose additional_contexts「maven-host」绑定到 /root/.m2/repository（默认 ./maven-cache）。
# 若镜像源对 docker.io/docker/dockerfile 返回 401：请先能拉取 docker/dockerfile:1.4（直连或可用镜像加速），见 code/docker/.env.example 说明。
# ------------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

ARG HTTP_PROXY
ARG HTTPS_PROXY
ARG NO_PROXY
ENV HTTP_PROXY=${HTTP_PROXY} \
    HTTPS_PROXY=${HTTPS_PROXY} \
    NO_PROXY=${NO_PROXY} \
    http_proxy=${HTTP_PROXY} \
    https_proxy=${HTTPS_PROXY} \
    no_proxy=${NO_PROXY}

COPY docker/settings.xml /root/.m2/settings.xml
COPY pom.xml .
COPY src ./src

RUN --mount=type=bind,from=maven-host,source=.,target=/root/.m2/repository,rw \
    set -e; \
    for i in 1 2 3 4 5; do \
      if mvn -B -Dmaven.test.skip=true package; then exit 0; fi; \
      echo "mvn package failed, retry $$i/5 in 45s..."; \
      if [ "$$i" = "5" ]; then exit 1; fi; \
      sleep 45; \
    done

# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
