-- V6: 启用 pg_jieba 中文分词扩展 + pgvector 向量扩展
-- pg_jieba 基于结巴分词，分词质量远优于 SCWS (zhparser)

CREATE EXTENSION IF NOT EXISTS pg_jieba;
CREATE EXTENSION IF NOT EXISTS vector;

-- 如果配置不存在则创建
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_ts_config WHERE cfgname = 'jiebacfg'
    ) THEN
        CREATE TEXT SEARCH CONFIGURATION jiebacfg (PARSER = jieba);
        
        ALTER TEXT SEARCH CONFIGURATION jiebacfg
            ADD MAPPING FOR nz,n,m,f,x,s,t,nr,ns,nt,nw,nrt,eng,v,vd,vn,vf,vi,vx,vg,a,ad,an,d,df,dg WITH simple;
    END IF;
END $$;
