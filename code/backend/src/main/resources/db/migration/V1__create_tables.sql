-- V1: 创建核心表结构

-- 用户表
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_users_username ON users(username) WHERE deleted = 0;

-- 专题表
CREATE TABLE topics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_topics_user_id ON topics(user_id) WHERE deleted = 0;

-- 素材表
CREATE TABLE entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    raw_content TEXT NOT NULL,
    content_type VARCHAR(20) NOT NULL DEFAULT 'text',
    source_type VARCHAR(20),
    source_title VARCHAR(255),
    source_link TEXT,
    captured_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    insight_text TEXT,
    topic_id UUID REFERENCES topics(id) ON DELETE SET NULL,
    skipped_at TIMESTAMP,
    skip_count INT NOT NULL DEFAULT 0,
    last_viewed_at TIMESTAMP,
    view_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    search_vector tsvector
);

CREATE INDEX idx_entries_user_id ON entries(user_id) WHERE deleted = 0;
CREATE INDEX idx_entries_captured_at ON entries(user_id, captured_at DESC) WHERE deleted = 0;
CREATE INDEX idx_entries_topic_id ON entries(topic_id) WHERE deleted = 0 AND topic_id IS NOT NULL;
CREATE INDEX idx_entries_no_insight ON entries(user_id) WHERE deleted = 0 AND insight_text IS NULL;
CREATE INDEX idx_entries_search_vector ON entries USING gin(search_vector);

-- 复用记录表
CREATE TABLE reuse_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_id UUID NOT NULL REFERENCES entries(id),
    user_id UUID NOT NULL REFERENCES users(id),
    reuse_type VARCHAR(20) NOT NULL DEFAULT 'copy',
    reused_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reuse_entry_id ON reuse_records(entry_id, reused_at DESC);
