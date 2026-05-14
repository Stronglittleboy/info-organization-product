-- V5: 启用 pg_trgm 扩展，为中文 ILIKE 搜索提供 GIN 索引加速
-- pg_trgm 将文本拆分为三元组(trigram)，配合 GIN 索引可加速 LIKE/ILIKE 模糊查询
-- 200万级数据量下可实现亚秒级搜索响应

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_entries_raw_content_trgm
    ON entries USING gin(raw_content gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_entries_insight_text_trgm
    ON entries USING gin(insight_text gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_entries_source_title_trgm
    ON entries USING gin(source_title gin_trgm_ops);
