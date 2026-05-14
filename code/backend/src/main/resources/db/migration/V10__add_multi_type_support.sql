-- V7: 添加多类型素材支持（图片/URL）

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
UPDATE entries SET updated_at = updated_at WHERE updated_at IS NOT NULL;

-- 添加字段注释
COMMENT ON COLUMN entries.content_type IS '素材类型: text, image, url';
COMMENT ON COLUMN entries.raw_content IS '原始内容（文本/OCR结果/URL正文）';
COMMENT ON COLUMN entries.insight_text IS '用户的思考';
COMMENT ON COLUMN entries.image_path IS '图片存储路径（minio://bucket/path 或 file:///path）';
COMMENT ON COLUMN entries.image_ocr_text IS 'OCR 提取的文字（源数据）';
COMMENT ON COLUMN entries.url IS '原始链接';
COMMENT ON COLUMN entries.url_title IS '网页标题';
COMMENT ON COLUMN entries.url_description IS '网页描述';
COMMENT ON COLUMN entries.url_extracted_text IS '网页正文提取（源数据）';
