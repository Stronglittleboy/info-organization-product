# 字段语义定义

## 核心字段关系

### 设计原则

1. **raw_content 是统一的"客观内容"字段**
   - 所有素材类型都使用它做全文搜索
   - 存储的是"原始信息"，不包含用户的主观思考

2. **insight_text 是用户的"主观思考"字段**
   - 存储用户对素材的理解、分析、洞察
   - 这是产品的核心价值所在

3. **专用字段是"源数据"**
   - image_ocr_text、url_extracted_text 等
   - 保留原始提取结果，便于追溯和重新处理

---

## 字段定义表

| 字段名 | 类型 | 职责 | 谁填写 | 是否必填 |
|--------|------|------|--------|---------|
| **content_type** | VARCHAR(20) | 素材类型标识 | 系统 | 是 |
| **raw_content** | TEXT | 原始内容/客观事实 | 用户/系统 | 是 |
| **insight_text** | TEXT | 用户的思考/主观洞察 | 用户 | 图片/URL必填，文本可选 |
| **image_path** | TEXT | 图片存储路径 | 系统 | 图片类型必填 |
| **image_ocr_text** | TEXT | OCR 提取的文字 | 系统 | 图片类型自动填充 |
| **url** | TEXT | 原始链接 | 用户 | URL类型必填 |
| **url_title** | VARCHAR(500) | 网页标题 | 系统 | URL类型自动填充 |
| **url_description** | TEXT | 网页描述 | 系统 | URL类型自动填充 |
| **url_extracted_text** | TEXT | 网页正文提取 | 系统 | URL类型自动填充 |

---

## 不同素材类型的字段使用策略

### 文本类型 (content_type = 'text')

```
用户输入：
  - raw_content: "聚合根负责维护业务不变性..."（用户输入）
  - insight_text: "这是 DDD 的核心概念"（可选）

系统处理：
  - content_type = 'text'
  - 其他字段为 NULL
```

**字段关系：**
- raw_content：用户输入的原文
- insight_text：用户的思考（可选）

---

### 图片类型 (content_type = 'image')

```
用户操作：
  1. 上传图片
  2. 系统自动 OCR 提取文字
  3. 用户填写"这张图说明了什么"

系统处理：
  - content_type = 'image'
  - image_path = 'minio://bucket/xxx.jpg'
  - image_ocr_text = '用户登录\n密码输入\n提交按钮'（OCR结果）
  - raw_content = '用户登录\n密码输入\n提交按钮'（复制OCR结果）
  - insight_text = '这是竞品的登录页面，采用了简洁的设计风格'（用户填写）
```

**字段关系：**
- image_path：图片存储位置（源文件）
- image_ocr_text：OCR 原始结果（源数据）
- raw_content：复制 OCR 结果（用于统一搜索）
- insight_text：用户的思考（必填）

**为什么要复制？**
- raw_content 是统一的搜索字段，所有类型都用它
- image_ocr_text 保留原始 OCR 结果，如果 OCR 算法升级，可以重新处理
- 如果只存 image_ocr_text，搜索逻辑就要特殊处理图片类型

---

### URL 类型 (content_type = 'url')

```
用户操作：
  1. 粘贴 URL
  2. 系统自动提取网页元数据和正文
  3. 用户填写"这篇文章的哪个观点值得记录"

系统处理：
  - content_type = 'url'
  - url = 'https://example.com/article'
  - url_title = 'DDD 领域驱动设计实践'（自动提取）
  - url_description = '本文介绍了 DDD 的核心概念...'（自动提取）
  - url_extracted_text = '领域驱动设计是一种...'（正文提取，完整）
  - raw_content = '领域驱动设计是一种...'（复制正文前 1000 字）
  - insight_text = '文章提到聚合根的设计原则很有启发'（用户填写）
```

**字段关系：**
- url：原始链接（源数据）
- url_title：网页标题（源数据）
- url_description：网页描述（源数据）
- url_extracted_text：正文提取（源数据，完整）
- raw_content：复制正文前 1000 字（用于统一搜索）
- insight_text：用户的思考（必填）

**为什么只复制前 1000 字？**
- 网页正文可能很长（几万字）
- 全文搜索索引会很大
- 前 1000 字通常包含核心内容
- 如果需要全文搜索，可以搜索 url_extracted_text

---

## 搜索逻辑

### 搜索向量构建

```sql
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
```

### 权重说明

| 权重 | 字段 | 原因 |
|------|------|------|
| A（最高） | insight_text | 用户的思考最重要，因为这是用户自己的语言 |
| B（次高） | url_title | URL 标题通常是核心主题 |
| C（中等） | raw_content | 原始内容（文本/OCR/正文） |
| D（最低） | source_title | 来源标题，辅助信息 |

---

## 数据流转示例

### 图片收集流程

```
1. 用户上传图片
   ↓
2. 保存到 MinIO/本地
   image_path = 'minio://bucket/xxx.jpg'
   ↓
3. 调用 OCR 服务
   image_ocr_text = '用户登录\n密码输入\n提交按钮'
   ↓
4. 复制到 raw_content
   raw_content = '用户登录\n密码输入\n提交按钮'
   ↓
5. 用户填写 insight
   insight_text = '这是竞品的登录页面'
   ↓
6. 触发器更新搜索向量
   search_vector = to_tsvector(insight_text + raw_content + ...)
   ↓
7. 保存到数据库
```

### URL 收集流程

```
1. 用户粘贴 URL
   url = 'https://example.com/article'
   ↓
2. 调用元数据提取服务
   url_title = 'DDD 领域驱动设计实践'
   url_description = '本文介绍了...'
   url_extracted_text = '领域驱动设计是一种...'（完整正文）
   ↓
3. 复制正文前 1000 字到 raw_content
   raw_content = '领域驱动设计是一种...'（前 1000 字）
   ↓
4. 用户填写 insight
   insight_text = '文章提到聚合根的设计原则'
   ↓
5. 触发器更新搜索向量
   search_vector = to_tsvector(insight_text + url_title + raw_content + ...)
   ↓
6. 保存到数据库
```

---

## 常见问题

### Q1: 为什么不直接搜索 image_ocr_text 和 url_extracted_text？

**A:** 为了统一搜索逻辑。

- 如果每种类型都有自己的搜索字段，搜索查询会很复杂
- raw_content 是统一的"内容"字段，所有类型都用它
- 专用字段（image_ocr_text、url_extracted_text）保留原始数据，便于追溯

### Q2: 如果 OCR 算法升级，如何重新处理？

**A:** 批量重新 OCR。

```sql
-- 1. 重新 OCR 所有图片
UPDATE entries 
SET image_ocr_text = new_ocr_result(image_path)
WHERE content_type = 'image';

-- 2. 同步到 raw_content
UPDATE entries 
SET raw_content = image_ocr_text
WHERE content_type = 'image';

-- 3. 触发器会自动更新 search_vector
```

### Q3: 如果用户修改了 insight，搜索向量会更新吗？

**A:** 会自动更新。

```sql
CREATE TRIGGER update_search_vector_trigger
BEFORE INSERT OR UPDATE ON entries
FOR EACH ROW
EXECUTE FUNCTION update_search_vector();
```

任何字段更新都会触发搜索向量重新计算。

### Q4: raw_content 的长度限制是多少？

**A:** TEXT 类型，理论上无限制。

- 但建议 URL 正文只复制前 1000 字
- 如果需要全文搜索，可以搜索 url_extracted_text

---

## 字段演进计划

### v0.2.0（当前）
- content_type: text, image, url
- 基础字段完整

### v0.3.0（规划）
- 新增 PDF 支持
  - pdf_path: PDF 存储路径
  - pdf_extracted_text: PDF 文本提取
  - raw_content: 复制 PDF 文本

### 未来可能
- 音频/视频（如果需求明确）
  - audio_path / video_path
  - audio_transcript: 语音转文字
  - raw_content: 复制转录文本
