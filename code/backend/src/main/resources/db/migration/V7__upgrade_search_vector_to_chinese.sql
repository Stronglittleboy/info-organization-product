-- V7: 将 search_vector 触发器从 'simple' 升级为 'chinese' (zhparser)
-- 'simple' 对中文只做逐字拆分，无法实现语义分词
-- 'chinese' 使用 zhparser 进行真正的中文分词，大幅提升搜索准确率和性能

CREATE OR REPLACE FUNCTION entries_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('chinese', COALESCE(NEW.raw_content, '')), 'A') ||
        setweight(to_tsvector('chinese', COALESCE(NEW.insight_text, '')), 'B') ||
        setweight(to_tsvector('chinese', COALESCE(NEW.source_title, '')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
