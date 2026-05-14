-- V8: 使用 jiebacfg 分批回填所有 search_vector 并重建索引

DO $$
DECLARE
    batch_size INT := 5000;
    affected INT;
BEGIN
    LOOP
        WITH batch AS (
            SELECT id FROM entries
            WHERE search_vector IS NULL
               OR search_vector = ''::tsvector
            LIMIT batch_size
            FOR UPDATE SKIP LOCKED
        )
        UPDATE entries e SET search_vector =
            setweight(to_tsvector('jiebacfg', COALESCE(e.raw_content, '')), 'A') ||
            setweight(to_tsvector('jiebacfg', COALESCE(e.insight_text, '')), 'B') ||
            setweight(to_tsvector('jiebacfg', COALESCE(e.source_title, '')), 'C')
        FROM batch WHERE e.id = batch.id;
        GET DIAGNOSTICS affected = ROW_COUNT;
        EXIT WHEN affected = 0;
        RAISE NOTICE 'Backfilled % rows', affected;
    END LOOP;
END $$;

REINDEX INDEX idx_entries_search_vector;
