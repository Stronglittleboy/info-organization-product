# API 设计（完整版）

## 基础信息

**Base URL:** `http://192.168.31.173:8080/api`

**认证方式:** JWT Token (后续实现)

**响应格式:** JSON

---

## 通用响应格式

### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 错误响应
```json
{
  "code": 400,
  "message": "错误描述",
  "errors": [{"field": "insight_text", "message": "思考不能为空"}]
}
```

---

## 1. 图片收集

### POST /api/entries/upload-image
上传图片并进行 OCR 识别

**请求:** multipart/form-data
- file: 图片文件（必填）
- insight: 用户思考（必填）
- sourceType: 来源类型（可选）
- topicId: 专题ID（可选）

**响应:**
```json
{
  "code": 200,
  "data": {
    "id": "uuid",
    "contentType": "image",
    "imagePath": "minio://...",
    "imageOcrText": "识别到的文字",
    "insightText": "用户的思考",
    "thumbnailUrl": "http://...",
    "capturedAt": "2026-05-15T10:30:00Z"
  }
}
```

### POST /api/entries/ocr
仅返回 OCR 结果，不保存

---

## 2. URL 收集

### POST /api/entries/collect-url
收集 URL 并提取元数据

**请求:** application/json
```json
{
  "url": "https://example.com/article",
  "insight": "文章提到聚合根的设计原则",
  "sourceType": "网页",
  "topicId": "uuid"
}
```

**响应:**
```json
{
  "code": 200,
  "data": {
    "id": "uuid",
    "contentType": "url",
    "url": "https://example.com/article",
    "urlTitle": "DDD 领域驱动设计实践",
    "urlDescription": "本文介绍了...",
    "insightText": "文章提到聚合根的设计原则",
    "capturedAt": "2026-05-15T10:30:00Z"
  }
}
```

### POST /api/entries/extract-url
仅返回元数据，不保存

---

## 3. 文本收集

### POST /api/entries/collect-text

**请求:**
```json
{
  "rawContent": "聚合根负责维护业务不变性...",
  "insight": "这是 DDD 的核心概念",
  "sourceType": "书籍",
  "topicId": "uuid"
}
```

---

## 4. 查询接口

### GET /api/entries
列表查询

**参数:**
- page: 页码（默认 1）
- size: 每页数量（默认 20）
- contentType: 素材类型（text/image/url）
- topicId: 专题ID

### GET /api/entries/{id}
详情查询

### GET /api/entries/search?q=关键词
全文搜索

### GET /api/entries/grouped
按时间分组（今天/昨天/本周/更早）

---

## 5. 更新/删除

### PUT /api/entries/{id}
更新条目

### DELETE /api/entries/{id}
删除条目

---

## 6. 文件访问

### GET /api/files/{userId}/images/{year}/{month}/{filename}
获取图片（需要权限验证）

### GET /api/files/{userId}/thumbnails/{year}/{month}/{filename}
获取缩略图

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 413 | 文件过大 |
| 415 | 不支持的文件类型 |
| 500 | 服务器内部错误 |

---

## 性能要求

- 列表查询: < 500ms
- 搜索: < 500ms
- 图片上传: < 10s
- OCR 识别: < 5s
- URL 元数据提取: < 5s
