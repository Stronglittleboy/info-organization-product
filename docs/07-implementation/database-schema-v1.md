# 数据库表结构设计 V1

项目：info-organization-product  
基于：领域模型 V1 + 技术选型 V1.1（PostgreSQL 15 + MyBatis-Plus）

---

# 1. 设计原则

1. 基于领域模型设计表结构
2. 优先考虑查询性能
3. 合理使用索引
4. 支持中文全文搜索
5. 预留扩展字段

---

# 2. 表结构设计

## 2.1 用户表（t_user）

```sql
CREATE TABLE t_user (
    user_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(50) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    email           VARCHAR(100),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_user_username ON t_user(username);
CREATE INDEX idx_user_email ON t_user(email);

COMMENT ON TABLE t_user IS '用户表';
COMMENT ON COLUMN t_user.user_id IS '用户ID';
COMMENT ON COLUMN t_user.username IS '用户名';
COMMENT ON COLUMN t_user.password_hash IS '密码哈希';
```

---

## 2.2 素材表（t_entry）

```sql
CREATE TABLE t_entry (
    entry_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    raw_content     TEXT NOT NULL,
    source_type     VARCHAR(20),
    source_title    VARCHAR(500),
    source_ref      VARCHAR(1000),
    captured_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    insight_text    TEXT,
    topic_id        UUID,
    skipped_at      TIMESTAMP,
    reused_count    INTEGER NOT NULL DEFAULT 0,
    last_reused_at  TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_entry_user FOREIGN KEY (user_id) REFERENCES t_user(user_id),
    CONSTRAINT fk_entry_topic FOREIGN KEY (topic_id) REFERENCES t_topic(topic_id),
    CONSTRAINT chk_source_type CHECK (source_type IN ('BOOK', 'WEB', 'CONVERSATION', 'MANUAL', 'OTHER'))
);

-- 核心索引
CREATE INDEX idx_entry_user_captured ON t_entry(user_id, captured_at DESC);
CREATE INDEX idx_entry_user_topic_captured ON t_entry(user_id, topic_id, captured_at DESC);
CREATE INDEX idx_entry_skipped ON t_entry(skipped_at) WHERE skipped_at IS NOT NULL;

-- 待处理流索引
CREATE INDEX idx_entry_pending ON t_entry(user_id, captured_at DESC) 
    WHERE insight_text IS NULL OR topic_id IS NULL;

-- 全文搜索索引
CREATE INDEX idx_entry_fulltext ON t_entry USING GIN(
    to_tsvector('chinese', COALESCE(raw_content, '') || ' ' || COALESCE(insight_text, ''))
);

COMMENT ON TABLE t_entry IS '素材表';
COMMENT ON COLUMN t_entry.entry_id IS '素材ID';
COMMENT ON COLUMN t_entry.raw_content IS '原始内容';
COMMENT ON COLUMN t_entry.insight_text IS '一句话思考';
COMMENT ON COLUMN t_entry.skipped_at IS '跳过时间（待处理流）';
```

---

## 2.3 专题表（t_topic）

```sql
CREATE TABLE t_topic (
    topic_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_topic_user FOREIGN KEY (user_id) REFERENCES t_user(user_id),
    CONSTRAINT uk_topic_user_name UNIQUE (user_id, name)
);

CREATE INDEX idx_topic_user ON t_topic(user_id);

COMMENT ON TABLE t_topic IS '专题表';
COMMENT ON COLUMN t_topic.topic_id IS '专题ID';
COMMENT ON COLUMN t_topic.name IS '专题名称';
COMMENT ON COLUMN t_topic.description IS '专题说明';
```

---

## 2.4 复用记录表（t_reuse_record）

```sql
CREATE TABLE t_reuse_record (
    reuse_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_id        UUID NOT NULL,
    user_id         UUID NOT NULL,
    reuse_type      VARCHAR(20) NOT NULL,
    reused_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_reuse_entry FOREIGN KEY (entry_id) REFERENCES t_entry(entry_id),
    CONSTRAINT fk_reuse_user FOREIGN KEY (user_id) REFERENCES t_user(user_id),
    CONSTRAINT chk_reuse_type CHECK (reuse_type IN ('COPY_RAW', 'COPY_WITH_INSIGHT'))
);

CREATE INDEX idx_reuse_entry ON t_reuse_record(entry_id, reused_at DESC);
CREATE INDEX idx_reuse_user ON t_reuse_record(user_id, reused_at DESC);

COMMENT ON TABLE t_reuse_record IS '复用记录表';
COMMENT ON COLUMN t_reuse_record.reuse_type IS '复用类型：COPY_RAW-复制原文, COPY_WITH_INSIGHT-复制原文+思考';
```

---

# 3. 触发器设计

## 3.1 更新 updated_at 触发器

```sql
-- 创建通用更新时间函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 应用到各表
CREATE TRIGGER trg_user_updated_at
    BEFORE UPDATE ON t_user
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_entry_updated_at
    BEFORE UPDATE ON t_entry
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_topic_updated_at
    BEFORE UPDATE ON t_topic
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

## 3.2 复用计数同步触发器

```sql
-- 创建复用记录后自动更新 Entry 的复用计数
CREATE OR REPLACE FUNCTION sync_entry_reuse_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE t_entry
    SET reused_count = reused_count + 1,
        last_reused_at = NEW.reused_at
    WHERE entry_id = NEW.entry_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reuse_sync
    AFTER INSERT ON t_reuse_record
    FOR EACH ROW
    EXECUTE FUNCTION sync_entry_reuse_count();
```

---

# 4. 中文全文搜索配置

## 4.1 安装 zhparser 插件

```sql
-- 需要在 PostgreSQL 中安装 zhparser 扩展
CREATE EXTENSION IF NOT EXISTS zhparser;

-- 创建中文全文搜索配置
CREATE TEXT SEARCH CONFIGURATION chinese (PARSER = zhparser);
ALTER TEXT SEARCH CONFIGURATION chinese ADD MAPPING FOR n,v,a,i,e,l WITH simple;
```

## 4.2 全文搜索查询示例

```sql
-- 搜索示例
SELECT 
    entry_id,
    ts_headline('chinese', raw_content, query) as snippet,
    insight_text
FROM t_entry,
    plainto_tsquery('chinese', '信息整理') query
WHERE user_id = :userId
    AND to_tsvector('chinese', COALESCE(raw_content, '') || ' ' || COALESCE(insight_text, '')) @@ query
ORDER BY captured_at DESC
LIMIT 20;
```

---

# 5. 初始化数据

## 5.1 测试用户

```sql
-- 密码：password123（实际应用中应使用 BCrypt 加密）
INSERT INTO t_user (username, password_hash, email) VALUES
('demo', '$2a$10$...', 'demo@example.com');
```

---

# 6. 数据库迁移脚本

## 6.1 V1__init_schema.sql

```sql
-- 完整初始化脚本
-- 包含上述所有表、索引、触发器、函数
```

## 6.2 迁移工具建议

- **Flyway**（推荐）
  - Spring Boot 集成简单
  - 版本管理清晰
  - 支持回滚

- 或 **Liquibase**
  - 功能更强大
  - 学习曲线稍陡

---

# 7. 性能优化建议

## 7.1 索引策略
- 待处理流查询：已建立复合索引
- 全文搜索：已建立 GIN 索引
- 专题查询：已建立外键索引

## 7.2 查询优化
- 使用 LIMIT 分页
- 避免 SELECT *
- 合理使用 LEFT JOIN

## 7.3 连接池配置
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
```

---

# 8. 备份与恢复

## 8.1 备份脚本

```bash
#!/bin/bash
# backup.sh
BACKUP_DIR=/data/backups
DATE=$(date +%Y%m%d_%H%M%S)
pg_dump -U postgres -d info_organization > $BACKUP_DIR/backup_$DATE.sql
# 保留最近 7 天
find $BACKUP_DIR -name "backup_*.sql" -mtime +7 -delete
```

## 8.2 恢复脚本

```bash
#!/bin/bash
# restore.sh
psql -U postgres -d info_organization < $1
```

---

# 9. MyBatis-Plus 实体类映射

## 9.1 Entry 实体示例

```java
@TableName("t_entry")
public class Entry {
    @TableId(type = IdType.ASSIGN_UUID)
    private UUID entryId;
    
    private UUID userId;
    private String rawContent;
    private String sourceType;
    private String sourceTitle;
    private String sourceRef;
    private LocalDateTime capturedAt;
    private String insightText;
    private UUID topicId;
    private LocalDateTime skippedAt;
    private Integer reusedCount;
    private LocalDateTime lastReusedAt;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

---

# 10. 当前结论

数据库表结构设计 V1 已完成，包括：
- 4 张核心表
- 完整索引策略
- 中文全文搜索配置
- 触发器与函数
- 迁移脚本建议
- MyBatis-Plus 映射示例

可作为后续项目初始化与代码实施的直接输入。
