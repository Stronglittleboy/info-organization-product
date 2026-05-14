-- V2: 创建触发器

-- updated_at 自动更新
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

CREATE TRIGGER topics_updated_at_trigger
    BEFORE UPDATE ON topics
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 全文搜索向量自动更新（simple 配置，兼容标准 PostgreSQL）
CREATE OR REPLACE FUNCTION entries_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('simple', COALESCE(NEW.raw_content, '')), 'A') ||
        setweight(to_tsvector('simple', COALESCE(NEW.insight_text, '')), 'B') ||
        setweight(to_tsvector('simple', COALESCE(NEW.source_title, '')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER entries_search_vector_trigger
    BEFORE INSERT OR UPDATE OF raw_content, insight_text, source_title
    ON entries
    FOR EACH ROW
    EXECUTE FUNCTION entries_search_vector_update();
