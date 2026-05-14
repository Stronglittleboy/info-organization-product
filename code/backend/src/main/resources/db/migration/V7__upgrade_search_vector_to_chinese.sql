-- V7: 将 search_vector 触发器从 'simple' 升级为 'jiebacfg' (结巴分词)
-- simple: 逐字拆分，"人工智能" → "人","工","智","能"
-- jiebacfg: 语义分词，"人工智能" → "人工智能","人工","智能"

CREATE OR REPLACE FUNCTION entries_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('jiebacfg', COALESCE(NEW.raw_content, '')), 'A') ||
        setweight(to_tsvector('jiebacfg', COALESCE(NEW.insight_text, '')), 'B') ||
        setweight(to_tsvector('jiebacfg', COALESCE(NEW.source_title, '')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
