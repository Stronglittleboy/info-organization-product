# 数据库设计

## 表结构

### entries 表（核心表）

```sql
CREATE TABLE entries (
    -- 主键
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- 用户关联
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- 素材类型
    content_type VARCHAR(20) NOT NULL DEFAULT 'text',
    -- 'text' | 'image' | 'url'
    
    -- 统一内容字段
    raw_content TEXT,           -- 原始内容（文本/OCR结果/URL正文）
    insight_text TEXT,          -- 用户的思考
    
    -- 图片专用字段
    image_path TEXT,            -- 图片存储路径
    image_ocr_text TEXT,        -- OCR 提取的文字
    
    -- URL 专用字段
    url TEXT,                   -- 原始链接
    url_title VARCHAR(500),     -- 网页标题
    url_description TEXT,       -- 网页描述
    url_extracted_text TEXT,    -- 网页正文提取
    
    -- 来源信息
    source_type VARCHAR(50),    -- 来源类型（网页/书籍/视频等）
    source_title VARCHAR(255),  -- 来源标题
    source_link TEXT,           -- 来源链接
    
    -- 专题关联
    topic_id UUID REFERENCES topics(id) ON DELETE SET NULL,
    
    -- 时间戳
    captured_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 全文搜索向量
    search_vector TSVECTOR
);
```

### 索引

```sql
-- 主要查询索引
CREATE INDEX idx_entries_user_id ON entries(user_id);
CREATE INDEX idx_entries_content_type ON entries(content_type);
CREATE INDEX idx_entries_topic_id ON entries(topic_id);
CREATE INDEX idx_entries_captured_at ON entries(captured_at DESC);

-- 全文搜索索引
CREATE INDEX idx_entries_search_vector ON entries USING GIN(search_vector);

-- 三元组相似度搜索索引（模糊搜索）
CREATE INDEX idx_entries_insight_trgm ON entries USING GIN(insight_text gin_trgm_ops);
CREATE INDEX idx_entries_raw_content_trgm ON entries USING GIN(raw_content gin_trgm_ops);

-- 组合索引（常用查询）
CREATE INDEX idx_entries_user_captured ON entries(user_id, captured_at DESC);
CREATE INDEX idx_entries_user_type ON entries(user_id, content_type);
```

---

## 迁移脚本

### V7__add_multi_type_support.sql

```sql
-- 添加素材类型字段
ALTER TABLE entries 
ADD COLUMN IF NOT EXISTS content_type VARCHAR(20) NOT NULL DEFAULT 'text';

-- 添加图片相关字段
ALTER TABLE entries 
ADD COLUMN IF NOT EXISTS image_path TEXT,
ADD COLUMN IF NOT EXISTS image_ocr_text TEXT;

-- 添加 URL 相关字段
ALTER TABLE entries 
ADD COLUMN IF NOT EXISTS url TEXT,
ADD COLUMN IF NOT EXISTS url_title VARCHAR(500),
ADD COLUMN IF NOT EXISTS url_description TEXT,
ADD COLUMN IF NOT EXISTS url_extracted_text TEXT;

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_entries_content_type ON entries(content_type);
CREATE INDEX IF NOT EXISTS idx_entries_user_type ON entries(user_id, content_type);

-- 更新搜索向量触发器函数
CREATE OR REPLACE FUNCTION update_search_vector()
RETURNS TRIGGER AS $$
BEGIN
  NEW.search_vector := 
    -- 用户思考（权重最高）
    setweight(to_tsvector('jiebacfg', coalesce(NEW.insight_text, '')), 'A') ||
    
    -- URL 标题（次高）
    setweight(to_tsvector('jiebacfg', coalesce(NEW.url_title, '')), 'B') ||
    
    -- 原始内容（中等）
    setweight(to_tsvector('jiebacfg', coalesce(NEW.raw_content, '')), 'C') ||
    
    -- 来源标题（最低）
    setweight(to_tsvector('jiebacfg', coalesce(NEW.source_title, '')), 'D');
  
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 重新创建触发器（如果已存在则先删除）
DROP TRIGGER IF EXISTS update_search_vector_trigger ON entries;

CREATE TRIGGER update_search_vector_trigger
BEFORE INSERT OR UPDATE ON entries
FOR EACH ROW
EXECUTE FUNCTION update_search_vector();

-- 更新现有数据的搜索向量
UPDATE entries SET updated_at = updated_at;

COMMENT ON COLUMN entries.content_type IS '素材类型: text, image, url';
COMMENT ON COLUMN entries.raw_content IS '原始内容（文本/OCR结果/URL正文）';
COMMENT ON COLUMN entries.insight_text IS '用户的思考';
COMMENT ON COLUMN entries.image_path IS '图片存储路径（minio://bucket/path 或 file:///path）';
COMMENT ON COLUMN entries.image_ocr_text IS 'OCR 提取的文字（源数据）';
COMMENT ON COLUMN entries.url IS '原始链接';
COMMENT ON COLUMN entries.url_title IS '网页标题';
COMMENT ON COLUMN entries.url_description IS '网页描述';
COMMENT ON COLUMN entries.url_extracted_text IS '网页正文提取（源数据）';
```

---

## 查询示例

### 1. 按类型查询

```sql
-- 查询所有图片
SELECT id, image_path, insight_text, captured_at
FROM entries
WHERE user_id = :userId
  AND content_type = 'image'
ORDER BY captured_at DESC
LIMIT 20;

-- 查询所有 URL
SELECT id, url, url_title, insight_text, captured_at
FROM entries
WHERE user_id = :userId
  AND content_type = 'url'
ORDER BY captured_at DESC
LIMIT 20;
```

### 2. 全文搜索

```sql
-- 搜索所有类型
SELECT 
    id, 
    content_type,
    CASE 
        WHEN content_type = 'text' THEN raw_content
        WHEN content_type = 'image' THEN insight_text
        WHEN content_type = 'url' THEN url_title
    END as display_content,
    ts_rank(search_vector, query) as rank
FROM entries, 
     to_tsquery('jiebacfg', :searchQuery) query
WHERE user_id = :userId
  AND search_vector @@ query
ORDER BY rank DESC, captured_at DESC
LIMIT 20;
```

### 3. 按时间分组

```sql
-- 今天/昨天/本周/更早
SELECT 
    CASE 
        WHEN captured_at::date = CURRENT_DATE THEN '今天'
        WHEN captured_at::date = CURRENT_DATE - 1 THEN '昨天'
        WHEN captured_at >= CURRENT_DATE - INTERVAL '7 days' THEN '本周'
        ELSE '更早'
    END as time_group,
    id,
    content_type,
    insight_text,
    captured_at
FROM entries
WHERE user_id = :userId
ORDER BY captured_at DESC;
```

### 4. 按专题查询

```sql
-- 查询某个专题下的所有素材
SELECT 
    e.id,
    e.content_type,
    e.insight_text,
    e.captured_at,
    t.name as topic_name
FROM entries e
LEFT JOIN topics t ON e.topic_id = t.id
WHERE e.user_id = :userId
  AND e.topic_id = :topicId
ORDER BY e.captured_at DESC;
```

---

## 数据完整性约束

### 业务规则

1. **content_type 必须是有效值**
```sql
ALTER TABLE entries 
ADD CONSTRAINT chk_content_type 
CHECK (content_type IN ('text', 'image', 'url'));
```

2. **图片类型必须有 image_path**
```sql
ALTER TABLE entries 
ADD CONSTRAINT chk_image_path 
CHECK (
    content_type != 'image' OR 
    (content_type = 'image' AND image_path IS NOT NULL)
);
```

3. **URL 类型必须有 url**
```sql
ALTER TABLE entries 
ADD CONSTRAINT chk_url 
CHECK (
    content_type != 'url' OR 
    (content_type = 'url' AND url IS NOT NULL)
);
```

4. **insight_text 对图片和 URL 必填**
```sql
ALTER TABLE entries 
ADD CONSTRAINT chk_insight_required 
CHECK (
    content_type = 'text' OR 
    (content_type IN ('image', 'url') AND insight_text IS NOT NULL AND insight_text != '')
);
```

---

## 性能优化

### 1. 分区表（未来优化）

如果数据量很大（百万级），可以按时间分区：

```sql
-- 按月分区
CREATE TABLE entries_2026_05 PARTITION OF entries
FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');

CREATE TABLE entries_2026_06 PARTITION OF entries
FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
```

### 2. 物化视图（未来优化）

如果统计查询频繁，可以创建物化视图：

```sql
-- 每日统计
CREATE MATERIALIZED VIEW daily_stats AS
SELECT 
    user_id,
    DATE(captured_at) as date,
    content_type,
    COUNT(*) as count
FROM entries
GROUP BY user_id, DATE(captured_at), content_type;

-- 定期刷新
REFRESH MATERIALIZED VIEW daily_stats;
```

---

## 数据迁移

### 现有数据迁移

如果已有数据，需要设置默认值：

```sql
-- 所有现有数据默认为文本类型
UPDATE entries 
SET content_type = 'text'
WHERE content_type IS NULL;
```

---

## 备份策略

### 1. 数据库备份

```bash
# 每日全量备份
pg_dump -h localhost -p 55432 -U postgres info_organization > backup_$(date +%Y%m%d).sql

# 增量备份（WAL 归档）
# 配置 postgresql.conf:
# wal_level = replica
# archive_mode = on
# archive_command = 'cp %p /backup/wal/%f'
```

### 2. 文件备份

```bash
# MinIO 数据备份
rsync -av /vol1/1000/docker_container/dev-env/minio/data/ /backup/minio/

# 本地文件备份
rsync -av /vol3/1000/private/workProject/info-organization-product/data/uploads/ /backup/uploads/
```

---

## 监控指标

### 1. 表大小监控

```sql
SELECT 
    pg_size_pretty(pg_total_relation_size('entries')) as total_size,
    pg_size_pretty(pg_relation_size('entries')) as table_size,
    pg_size_pretty(pg_indexes_size('entries')) as indexes_size;
```

### 2. 索引使用情况

```sql
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE tablename = 'entries'
ORDER BY idx_scan DESC;
```

### 3. 慢查询监控

```sql
-- 配置 postgresql.conf:
-- log_min_duration_statement = 1000  # 记录超过 1 秒的查询
```
