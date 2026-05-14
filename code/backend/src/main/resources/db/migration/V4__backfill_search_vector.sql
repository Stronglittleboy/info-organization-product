-- V4: 回填已有数据的 search_vector，并确保后续查询使用全文搜索
UPDATE entries SET search_vector =
    setweight(to_tsvector('simple', COALESCE(raw_content, '')), 'A') ||
    setweight(to_tsvector('simple', COALESCE(insight_text, '')), 'B') ||
    setweight(to_tsvector('simple', COALESCE(source_title, '')), 'C')
WHERE search_vector IS NULL;
