# API 设计

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
  "errors": [
    {
      "field": "insight_text",
      "message": "思考不能为空"
    }
  ]
}
```

---

## 1. 图片收集接口

### 1.1 上传图片

**接口:** `POST /api/entries/upload-image`

**Content-Type:** `multipart/form-data`

**请求参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 图片文件 |
| insight | String | 是 | 用户的思考（这张图说明了什么） |
| sourceType | String | 否 | 来源类型 |
| sourceTitle | String | 否 | 来源标题 |
| topicId | UUID | 否 | 专题ID |

**请求示例:**

```bash
curl -X POST http://192.168.31.173:8080/api/entries/upload-image \
  -F "file=@screenshot.png" \
  -F "insight=这是竞品的登录页面，采用了简洁的设计风格" \
  -F "sourceType=网页" \
  -F "sourceTitle=竞品分析"
```

**响应示例:**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
    "contentType": "image",
    "imagePath": "minio://info-org-files/user123/images/2026/05/xxx.jpg",
    "imageOcrText": "用户登录\n密码输入\n提交按钮",
    "insightText": "这是竞品的登录页面，采用了简洁的设计风格",
    "thumbnailUrl": "http://192.168.31.173:9000/info-org-files/user123/thumbnails/2026/05/xxx_thumb.jpg",
    "capturedAt": "2026-05-15T10:30:00Z"
  }
}
```

**错误码:**

- `400` - 参数错误（文件为空、insight 为空）
- `413` - 文件过大
- `415` - 不支持的文件类型
- `500` - 服务器错误（OCR 失败、存储失败）

---

### 1.2 获取 OCR 结果（实时）

**接口:** `POST /api/entries/ocr`

**Content-Type:** `multipart/form-data`

**说明:** 仅返回 OCR 结果，不保存到数据库。用于前端实时反馈。

**请求参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 图片文件 |

**响应示例:**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "text": "用户登录\n密码输入\n提交按钮",
    "confidence": 0.95
  }
}
```

---

## 2. URL 收集接口

### 2.1 收集 URL

**接口:** `POST /api/entries/collect-url`

**Content-Type:** `application/json`

**请求参数:**

```json
{
  "url": "https://example.com/article",
  "insight": "文章提到聚合根的设计原则很有启发",
  "sourceType": "网页",
  "topicId": "uuid"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| url | String | 是 | 原始链接 |
| insight | String | 是 | 用户的思考 |
| sourceType | String | 否 | 来源类型 |
| topicId | UUID | 否 | 专题ID |

**响应示例:**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
    "contentType": "url",
    "url": "https://example.com/article",
    "urlTitle": "DDD 领域驱动设计实践",
    "urlDescription": "本文介绍了 DDD 的核心概念...",
    "insightText": "文章提到聚合根的设计原则很有启发",
    "capturedAt": "2026-05-15T10:30:00Z"
  }
}
```

**错误码:**

- `400` - 参数错误（URL 格式错误、insight 为空）
- `403` - SSRF 防护（内网 IP）
- `500` - 服务器错误（网页抓取失败）

---

### 2.2 提取 URL 元数据（实时）

**接口:** `POST /api/entries/extract-url`

**Content-Type:** `application/json`

**说明:** 仅返回元数据，不保存到数据库。用于前端实时反馈。

**请求参数:**

```json
{
  "url": "https://example.com/article"
}
```

**响应示例:**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "title": "DDD 领域驱动设计实践",
    "description": "本文介绍了 DDD 的核心概念...",
    "favicon": "https://example.com/favicon.ico",
    "extractedText": "领域驱动设计是一种..."
  }
}
```

---

## 3. 文本收集接口

### 3.1 收集文本

**接口:** `POST /api/entries/collect-text`

**Content-Type:** `application/json`

**请求参数:**

```json
{
  "rawContent": "聚合根负责维护业务不变性...",
  "insight": "这是 DDD 的核心概念",
  "sourceType": "书籍",
  "sourceTitle": "领域驱动设计",
  "topicId": "uuid"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| rawContent | String | 是 | 原始文本内容 |
| insight | String | 否 | 用户的思考 |
| sourceType | String | 否 | 来源类型 |
| sourceTitle | String | 否 | 来源标题 |
| topicId | UUID | 否 | 专题ID |

**响应示例:**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
    "contentType": "text",
    "rawContent": "聚合根负责维护业务不变性...",
    "insightText": "这是 DDD 的核心概念",
    "capturedAt": "2026-05-15T10:30:00Z"
  }
}
```
