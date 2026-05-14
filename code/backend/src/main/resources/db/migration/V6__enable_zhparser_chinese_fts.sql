-- V6: 启用 zhparser 中文分词扩展，创建中文全文搜索配置
-- 需要自定义 PostgreSQL 镜像（已编译安装 zhparser）

CREATE EXTENSION IF NOT EXISTS zhparser;

CREATE TEXT SEARCH CONFIGURATION chinese (PARSER = zhparser);

ALTER TEXT SEARCH CONFIGURATION chinese
    ADD MAPPING FOR n,v,a,i,e,l,t WITH simple;

-- zhparser 参数调优：适合千万级数据的短文本搜索场景
-- 启用多短语复合切分，提升召回率
ALTER ROLE postgres SET zhparser.multi_short = on;
ALTER ROLE postgres SET zhparser.multi_duality = on;
ALTER ROLE postgres SET zhparser.multi_zmain = on;
