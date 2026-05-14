# ============================================================================
# 阶段 1: 扩展编译器 - 编译 pg_jieba 和 pgvector
# ============================================================================
FROM postgres:15-alpine AS extension-builder

ARG HTTP_PROXY
ARG HTTPS_PROXY

# 安装编译依赖
RUN apk add --no-cache \
    build-base \
    git \
    cmake \
    clang \
    llvm

# 编译 pg_jieba
RUN set -eux; \
    git clone --depth 1 --recurse-submodules \
        https://github.com/jaiminpan/pg_jieba.git /tmp/pg_jieba; \
    cd /tmp/pg_jieba && mkdir build && cd build; \
    cmake -DCMAKE_BUILD_TYPE=Release -DCMAKE_POLICY_VERSION_MINIMUM=3.5 ..; \
    make -j$(nproc) && make install

# 编译 pgvector（创建必要的软链接）
RUN set -eux; \
    git clone --depth 1 --branch v0.8.0 \
        https://github.com/pgvector/pgvector.git /tmp/pgvector; \
    cd /tmp/pgvector; \
    # 创建 clang-19 和 llvm-lto 软链接
    ln -sf /usr/bin/clang /usr/bin/clang-19; \
    mkdir -p /usr/lib/llvm19/bin; \
    ln -sf /usr/bin/llvm-lto /usr/lib/llvm19/bin/llvm-lto; \
    make -j$(nproc) && make install

# 验证编译产物
RUN set -eux; \
    test -f "$(pg_config --pkglibdir)/pg_jieba.so"; \
    test -f "$(pg_config --pkglibdir)/vector.so"; \
    test -f "$(pg_config --sharedir)/extension/pg_jieba.control"; \
    test -f "$(pg_config --sharedir)/extension/vector.control"

# ============================================================================
# 阶段 2: 最终镜像 - 只包含运行时依赖和编译好的扩展
# ============================================================================
FROM postgres:15-alpine

# 从构建阶段复制编译好的扩展
COPY --from=extension-builder \
    /usr/local/lib/postgresql/pg_jieba.so \
    /usr/local/lib/postgresql/pg_jieba.so

COPY --from=extension-builder \
    /usr/local/lib/postgresql/vector.so \
    /usr/local/lib/postgresql/vector.so

# 复制扩展控制文件和 SQL 脚本
COPY --from=extension-builder \
    /usr/local/share/postgresql/extension/pg_jieba.control \
    /usr/local/share/postgresql/extension/pg_jieba--1.1.1.sql \
    /usr/local/share/postgresql/extension/

COPY --from=extension-builder \
    /usr/local/share/postgresql/extension/vector* \
    /usr/local/share/postgresql/extension/

# 复制 jieba 词典文件
COPY --from=extension-builder \
    /usr/local/share/postgresql/tsearch_data/jieba* \
    /usr/local/share/postgresql/tsearch_data/

# 安装运行时依赖（C++ 标准库）
RUN apk add --no-cache libstdc++

# 验证扩展可用
RUN set -eux; \
    test -f /usr/local/lib/postgresql/pg_jieba.so; \
    test -f /usr/local/lib/postgresql/vector.so; \
    echo "Extensions installed successfully"
