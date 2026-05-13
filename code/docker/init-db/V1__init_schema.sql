-- V1__init_schema.sql
-- 信息整理产品数据库初始化脚本
-- 基于：docs/07-implementation/database-schema-v1.md

-- 创建 zhparser 扩展（中文分词）
CREATE EXTENSION IF NOT EXISTS zhparser;

-- 创建中文全文搜索配置
CREATE TEXT SEARCH CONFIGURATION chinese_zh (PARSER = zhparser);
ALTER TEXT SEARCH CONFIGURATION chinese_zh ADD MAPPING FOR n,v,a,i,e,l WITH simple;

-- ============================================
-- 1. 用户表
-- ============================================
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_users_username ON users(username) WHERE deleted = 0;
CREATE INDEX idx_users_email ON users(email) WHERE deleted = 0;

COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.user_id IS '用户ID';
COMMENT ON COLUMN users.username IS '用户名';
COMMENT ON COLUMN users.password_hash IS '密码哈希';
COMMENT ON COLUMN users.email IS '邮箱';
COMMENT ON COLUMN users.deleted IS '逻辑删除标记：0=未删除，1=已删除';

-- ============================================
-- 2. 素材表（核心表）
-- ============================================
CREATE TABLE entries (
    entry_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id),
    raw_content TEXT NOT NULL,
    content_type VARCHAR(20) NOT NULL DEFAULT 'text',
    source_type VARCHAR(20),
    source_title VARCHAR(255),
    source_link TEXT,
    captured_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    insight_text TEXT,
    topic_id UUID,
    skipped_at TIMESTAMP,
    skip_count INT NOT NULL DEFAULT 0,
    last_viewed_at TIMESTAMP,
    view_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    search_vector tsvector
);

-- 索引
CREATE INDEX idx_entries_user_id ON entries(user_id) WHERE deleted = 0;
CREATE INDEX idx_entries_captured_at ON entries(user_id, captured_at DESC) WHERE deleted = 0;
CREATE INDEX idx_entries_topic_id ON entries(topic_id) WHERE deleted = 0 AND topic_id IS NOT NULL;
CREATE INDEX idx_entries_insight ON entries(user_id) WHERE deleted = 0 AND insight_text IS NOT NULL;
CREATE INDEX idx_entries_no_insight ON entries(user_id) WHERE deleted = 0 AND insight_text IS NULL;
CREATE INDEX idx_entries_skipped ON entries(user_id, skipped_at DESC) WHERE deleted = 0 AND skipped_at IS NOT NULL;
CREATE INDEX idx_entries_search_vector ON entries USING gin(search_vector);

COMMENT ON TABLE entries IS '素材表（核心聚合根）';
COMMENT ON COLUMN entries.entry_id IS '素材ID';
COMMENT ON COLUMN entries.user_id IS '用户ID';
COMMENT ON COLUMN entries.raw_content IS '原始内容';
COMMENT ON COLUMN entries.content_type IS '内容类型：text/link/image';
COMMENT ON COLUMN entries.source_type IS '来源类型：book/webpage/conversation/video/manual';
COMMENT ON COLUMN entries.source_title IS '来源标题';
COMMENT ON COLUMN entries.source_link IS '来源链接';
COMMENT ON COLUMN entries.captured_at IS '收集时间';
COMMENT ON COLUMN entries.insight_text IS '一句话思考';
COMMENT ON COLUMN entries.topic_id IS '所属专题ID';
COMMENT ON COLUMN entries.skipped_at IS '最近跳过时间';
COMMENT ON COLUMN entries.skip_count IS '跳过次数';
COMMENT ON COLUMN entries.last_viewed_at IS '最近查看时间';
COMMENT ON COLUMN entries.view_count IS '查看次数';
COMMENT ON COLUMN entries.search_vector IS '全文搜索向量';

-- 全文搜索触发器
CREATE OR REPLACE FUNCTION entries_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('chinese_zh', COALESCE(NEW.raw_content, '')), 'A') ||
        setweight(to_tsvector('chinese_zh', COALESCE(NEW.insight_text, '')), 'B') ||
        setweight(to_tsvector('chinese_zh', COALESCE(NEW.source_title, '')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER entries_search_vector_trigger
    BEFORE INSERT OR UPDATE OF raw_content, insight_text, source_title
    ON entries
    FOR EACH ROW
    EXECUTE FUNCTION entries_search_vector_update();

-- updated_at 触发器
CREATE OR REPLACE FUNCTION update_updated_at_column() RETURNS trigger AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER entries_updated_at_trigger
    BEFORE UPDATE ON entries
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 3. 专题表
-- ============================================
CREATE TABLE topics (
    topic_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_topics_user_id ON topics(user_id) WHERE deleted = 0;
CREATE INDEX idx_topics_last_used ON topics(user_id, last_used_at DESC NULLS LAST) WHERE deleted = 0;

COMMENT ON TABLE topics IS '专题表';
COMMENT ON COLUMN topics.topic_id IS '专题ID';
COMMENT ON COLUMN topics.user_id IS '用户ID';
COMMENT ON COLUMN topics.name IS '专题名称';
COMMENT ON COLUMN topics.description IS '专题说明';
COMMENT ON COLUMN topics.last_used_at IS '最近使用时间';

CREATE TRIGGER topics_updated_at_trigger
    BEFORE UPDATE ON topics
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 4. 复用记录表
-- ============================================
CREATE TABLE reuse_records (
    reuse_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_id UUID NOT NULL REFERENCES entries(entry_id),
    user_id UUID NOT NULL REFERENCES users(user_id),
    reuse_type VARCHAR(20) NOT NULL DEFAULT 'copy',
    reused_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reuse_entry_id ON reuse_records(entry_id, reused_at DESC);
CREATE INDEX idx_reuse_user_id ON reuse_records(user_id, reused_at DESC);

COMMENT ON TABLE reuse_records IS '复用记录表';
COMMENT ON COLUMN reuse_records.reuse_id IS '复用记录ID';
COMMENT ON COLUMN reuse_records.entry_id IS '素材ID';
COMMENT ON COLUMN reuse_records.user_id IS '用户ID';
COMMENT ON COLUMN reuse_records.reuse_type IS '复用类型：copy/copy_with_insight';
COMMENT ON COLUMN reuse_records.reused_at IS '复用时间';

-- ============================================
-- 5. 外键约束
-- ============================================
ALTER TABLE entries ADD CONSTRAINT fk_entries_topic
    FOREIGN KEY (topic_id) REFERENCES topics(topic_id) ON DELETE SET NULL;

-- ============================================
-- 6. 初始化数据（可选）
-- ============================================
-- 创建默认用户（密码：password，需要在应用层加密）
-- INSERT INTO users (username, password_hash, email)
-- VALUES ('demo', '$2a$10$...', 'demo@example.com');
