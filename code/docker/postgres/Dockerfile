FROM postgres:15-alpine

RUN set -eux; \
    apk add --no-cache --virtual .build-deps \
        build-base \
        git \
        curl; \
    git clone https://github.com/amutu/zhparser.git /tmp/zhparser; \
    cd /tmp/zhparser; \
    make && make install; \
    apk del .build-deps; \
    rm -rf /tmp/zhparser /var/cache/apk/*
