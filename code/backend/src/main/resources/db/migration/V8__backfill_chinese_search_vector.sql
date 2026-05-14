-- V8: 使用 'chinese' 配置回填所有 search_vector，并重建 GIN 索引
-- 千万级数据量下此迁移可能耗时数分钟，分批执行避免长事务

DO $$
DECLARE
    batch_size INT := 5000;
    affected INT;
BEGIN
    LOOP
        UPDATE entries SET search_vector =
            setweight(to_tsvector('chinese', COALESCE(raw_content, '')), 'A') ||
            setweight(to_tsvector('chinese', COALESCE(insight_text, '')), 'B') ||
            setweight(to_tsvector('chinese', COALESCE(source_title, '')), 'C')
        WHERE id IN (
            SELECT id FROM entries
            WHERE search_vector IS NULL
               OR search_vector != (
                   setweight(to_tsvector('chinese', COALESCE(raw_content, '')), 'A') ||
                   setweight(to_tsvector('chinese', COALESCE(insight_text, '')), 'B') ||
                   setweight(to_tsvector('chinese', COALESCE(source_title, '')), 'C')
               )
            LIMIT batch_size
        );
        GET DIAGNOSTICS affected = ROW_COUNT;
        EXIT WHEN affected = 0;
        RAISE NOTICE 'Updated % rows', affected;
    END LOOP;
END $$;

-- 重建 GIN 索引以匹配新的分词结果
REINDEX INDEX idx_entries_search_vector;
