# API 草案 V1

项目：info-organization-product  
基于：领域模型 V1 + 状态流 V1 + 查询模型 V1

---

# 1. 设计原则

1. 按页面任务拆分接口，不做万能 CRUD
2. 读写分离，读接口返回优化的读模型
3. 写接口只接受必要字段
4. 首版不做复杂批量操作

---

# 2. 收集页接口

## 2.1 创建素材
```
POST /api/entries

Request:
{
  "rawContent": "string (必填)",
  "sourceType": "BOOK | WEB | CONVERSATION | MANUAL | OTHER (可空)",
  "sourceTitle": "string (可空)",
  "sourceRef": "string (可空)"
}

Response:
{
  "entryId": "uuid",
  "capturedAt": "timestamp"
}
```

## 2.2 对刚保存条目补一句思考
```
PATCH /api/entries/:entryId/insight

Request:
{
  "insightText": "string (必填, 建议 500 字内)"
}

Response:
{
  "success": true
}
```

## 2.3 对刚保存条目加入专题
```
PATCH /api/entries/:entryId/topic

Request:
{
  "topicId": "uuid (必填)"
}

Response:
{
  "success": true
}
```

## 2.4 获取最近记录
```
GET /api/entries/recent?limit=5

Response:
{
  "items": [
    {
      "entryId": "uuid",
      "contentPreview": "string",
      "sourceType": "enum",
      "capturedAt": "timestamp",
      "hasInsight": boolean,
      "topicName": "string (可空)"
    }
  ]
}
```

---

# 3. 待处理页接口

## 3.1 获取待处理流
```
GET /api/entries/pending?offset=0&limit=20

Response:
{
  "items": [
    {
      "entryId": "uuid",
      "contentPreview": "string",
      "sourceType": "enum",
      "capturedAt": "timestamp",
      "currentSuggestedAction": "ADD_INSIGHT | ADD_TOPIC",
      "insightText": "string (可空)",
      "topicId": "uuid (可空)",
      "topicName": "string (可空)"
    }
  ],
  "total": integer,
  "hasMore": boolean
}
```

## 3.2 提交一句思考
```
PATCH /api/entries/:entryId/insight

Request:
{
  "insightText": "string (必填)"
}

Response:
{
  "success": true
}
```

## 3.3 设置专题
```
PATCH /api/entries/:entryId/topic

Request:
{
  "topicId": "uuid (必填)"
}

Response:
{
  "success": true
}
```

## 3.4 跳过条目
```
POST /api/entries/:entryId/skip

Request: (空)

Response:
{
  "success": true
}
```

---

# 4. 查找页接口

## 4.1 搜索素材
```
GET /api/entries/search

Query Parameters:
- keyword: string (必填)
- topicId: uuid (可空)
- hasInsight: boolean (可空)
- startDate: timestamp (可空)
- endDate: timestamp (可空)
- offset: integer (默认 0)
- limit: integer (默认 20)

Response:
{
  "items": [
    {
      "entryId": "uuid",
      "contentSnippet": "string",
      "insightText": "string (可空)",
      "topicId": "uuid (可空)",
      "topicName": "string (可空)",
      "sourceType": "enum",
      "capturedAt": "timestamp"
    }
  ],
  "total": integer,
  "hasMore": boolean
}
```

## 4.2 记录复制行为
```
POST /api/entries/:entryId/reuse

Request:
{
  "reuseType": "COPY_RAW | COPY_WITH_INSIGHT"
}

Response:
{
  "success": true
}
```

---

# 5. 专题页接口

## 5.1 获取专题详情
```
GET /api/topics/:topicId

Response:
{
  "topicId": "uuid",
  "name": "string",
  "description": "string",
  "entryCount": integer
}
```

## 5.2 获取专题内素材列表
```
GET /api/topics/:topicId/entries?offset=0&limit=20

Response:
{
  "items": [
    {
      "entryId": "uuid",
      "contentPreview": "string",
      "insightText": "string (可空)",
      "sourceType": "enum",
      "capturedAt": "timestamp"
    }
  ],
  "total": integer,
  "hasMore": boolean
}
```

## 5.3 创建专题
```
POST /api/topics

Request:
{
  "name": "string (必填, 建议 100 字内)",
  "description": "string (可空, 建议 500 字内)"
}

Response:
{
  "topicId": "uuid",
  "name": "string",
  "description": "string"
}
```

## 5.4 获取专题列表
```
GET /api/topics?offset=0&limit=50

Response:
{
  "items": [
    {
      "topicId": "uuid",
      "name": "string",
      "entryCount": integer
    }
  ],
  "total": integer
}
```

---

# 6. 详情页接口

## 6.1 获取素材详情
```
GET /api/entries/:entryId

Response:
{
  "entryId": "uuid",
  "rawContent": "text",
  "sourceType": "enum",
  "sourceTitle": "string (可空)",
  "sourceRef": "string (可空)",
  "capturedAt": "timestamp",
  "insightText": "text (可空)",
  "topicId": "uuid (可空)",
  "topicName": "string (可空)",
  "reusedCount": integer,
  "lastReusedAt": "timestamp (可空)"
}
```

## 6.2 更新一句思考
```
PATCH /api/entries/:entryId/insight

Request:
{
  "insightText": "string (必填)"
}

Response:
{
  "success": true
}
```

## 6.3 更换专题
```
PATCH /api/entries/:entryId/topic

Request:
{
  "topicId": "uuid (必填)"
}

Response:
{
  "success": true
}
```

## 6.4 记录复制行为
```
POST /api/entries/:entryId/reuse

Request:
{
  "reuseType": "COPY_RAW | COPY_WITH_INSIGHT"
}

Response:
{
  "success": true
}
```

---

# 7. 错误响应格式

统一错误响应：
```
{
  "error": {
    "code": "string",
    "message": "string"
  }
}
```

常见错误码：
- `INVALID_INPUT`: 输入参数不合法
- `NOT_FOUND`: 资源不存在
- `UNAUTHORIZED`: 未授权
- `FORBIDDEN`: 无权限
- `INTERNAL_ERROR`: 服务器内部错误

---

# 8. 认证

所有接口需要认证，建议使用：
- Bearer Token
- 或 Session Cookie

首版不在 API 草案中详细定义认证机制。

---

# 9. 当前结论

API 草案 V1 已定义完成，可作为：
- 前后端联调契约
- 后续 API 文档生成输入
- 研发任务拆解输入
