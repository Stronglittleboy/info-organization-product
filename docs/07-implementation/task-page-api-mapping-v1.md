# 任务-页面-API DSL 映射表 V1

项目：info-organization-product

---

# 1. 收集页任务映射

## 页面
`views/CollectPage.vue`

## 涉及任务
- TASK-101~108

## API 映射

### API-001: 创建素材
POST /api/entries

请求 DTO：
- CreateEntryRequest

响应 DTO：
- EntryResponse

对应任务：
- TASK-102: DTO 定义
- TASK-103: Service 实现
- TASK-104: Controller 实现
- TASK-105: 前端 API 封装
- TASK-106: 前端组件调用

DSL：
- rawContent: string, 必填
- contentType?: string
- sourceType?: string
- sourceTitle?: string
- sourceLink?: string

返回：
- entryId: string
- rawContent: string
- contentType: string
- sourceType?: string
- sourceTitle?: string
- sourceLink?: string
- capturedAt: string
- insightText?: string
- topicId?: string
- topicName?: string

### API-002: 补一句思考
POST /api/entries/{entryId}/insight

请求 DTO：
- AddInsightRequest

响应 DTO：
- EntryResponse

对应任务：
- TASK-102
- TASK-103
- TASK-104
- TASK-105
- TASK-106

DSL：
- insightText: string, 必填, 最大 500 字符

### API-003: 加入专题
POST /api/entries/{entryId}/topic

请求 DTO：
- SetTopicRequest

响应 DTO：
- EntryResponse

对应任务：
- TASK-102
- TASK-103
- TASK-104
- TASK-105
- TASK-106

DSL：
- topicId: string, 必填

### API-004: 获取最近素材
GET /api/entries/recent?limit=5

响应 DTO：
- RecentEntryItemResponse[]

对应任务：
- TASK-102
- TASK-103
- TASK-104
- TASK-105
- TASK-106

返回：
- entryId: string
- rawContent: string
- contentType: string
- sourceType?: string
- sourceTitle?: string
- capturedAt: string
- insightText?: string
- topicId?: string
- topicName?: string

---

# 2. 待处理页任务映射

## 页面
`views/PendingPage.vue`

## 涉及任务
- TASK-201~204

### API-005: 获取待处理流
GET /api/pending?page=1&size=20

响应 DTO：
- PendingEntryItemResponse[]

对应任务：
- TASK-201
- TASK-202
- TASK-203

返回：
- entryId: string
- rawContent: string
- contentType: string
- sourceType?: string
- sourceTitle?: string
- capturedAt: string
- insightText?: string
- topicId?: string
- topicName?: string
- currentSuggestedAction: add_insight | set_topic
- skipCount: number
- lastSkippedAt?: string

### API-006: 跳过条目
POST /api/entries/{entryId}/skip

响应 DTO：
- SkipResponse

对应任务：
- TASK-201
- TASK-202
- TASK-203

返回：
- success: boolean
- message?: string

---

# 3. 查找页任务映射

## 页面
`views/SearchPage.vue`

## 涉及任务
- TASK-301~305

### API-007: 搜索素材
GET /api/search

查询参数：
- q: string
- hasInsight?: boolean
- topicId?: string
- startDate?: string
- endDate?: string
- page?: number
- size?: number

响应 DTO：
- SearchResultItemResponse[]

对应任务：
- TASK-301
- TASK-303
- TASK-304

返回：
- entryId: string
- rawContent: string
- contentType: string
- sourceType?: string
- sourceTitle?: string
- capturedAt: string
- insightText?: string
- topicId?: string
- topicName?: string
- reuseCount: number
- lastReusedAt?: string

### API-008: 记录复用
POST /api/entries/{entryId}/reuse

请求 DTO：
- ReuseRequest

响应 DTO：
- ReuseResponse

对应任务：
- TASK-302
- TASK-303
- TASK-304

DSL：
- reuseType: copy | copy_with_insight

返回：
- success: boolean
- message?: string

---

# 4. 专题页任务映射

## 页面
- `views/TopicPage.vue`
- `views/TopicDetailPage.vue`

## 涉及任务
- TASK-401~404

### API-009: 创建专题
POST /api/topics

请求 DTO：
- CreateTopicRequest

响应 DTO：
- TopicResponse

DSL：
- name: string, 必填, 最大 100 字符
- description?: string, 最大 500 字符

### API-010: 获取专题列表
GET /api/topics?page=1&size=20

响应 DTO：
- TopicResponse[]

### API-011: 获取专题详情
GET /api/topics/{topicId}

响应 DTO：
- TopicDetailResponse

### API-012: 获取专题内素材
GET /api/topics/{topicId}/entries?page=1&size=20

响应 DTO：
- TopicEntryItemResponse[]

---

# 5. 详情页任务映射

## 页面
`views/EntryDetailPage.vue`

## 涉及任务
- TASK-501~504

### API-013: 获取素材详情
GET /api/entries/{entryId}

响应 DTO：
- EntryDetailResponse

返回：
- entryId: string
- rawContent: string
- contentType: string
- sourceType?: string
- sourceTitle?: string
- sourceLink?: string
- capturedAt: string
- insightText?: string
- topicId?: string
- topicName?: string
- viewCount: number
- reuseCount: number
- lastViewedAt?: string
- lastReusedAt?: string

### API-014: 更新思考
PUT /api/entries/{entryId}/insight

请求 DTO：
- UpdateInsightRequest

DSL：
- insightText: string, 必填, 最大 500 字符

### API-015: 更换专题
PUT /api/entries/{entryId}/topic

请求 DTO：
- UpdateTopicRequest

DSL：
- topicId: string | null

---

# 6. 任务-API-页面完整映射表

| 任务ID | API编号 | API路径 | 页面组件 | 优先级 |
|---|---|---|---|---|
| TASK-104 | API-001 | POST /api/entries | CollectPage.vue | P0 |
| TASK-104 | API-002 | POST /api/entries/{id}/insight | CollectPage.vue | P0 |
| TASK-104 | API-003 | POST /api/entries/{id}/topic | CollectPage.vue | P0 |
| TASK-104 | API-004 | GET /api/entries/recent | CollectPage.vue | P0 |
| TASK-202 | API-005 | GET /api/pending | PendingPage.vue | P1 |
| TASK-202 | API-006 | POST /api/entries/{id}/skip | PendingPage.vue | P1 |
| TASK-303 | API-007 | GET /api/search | SearchPage.vue | P1 |
| TASK-303 | API-008 | POST /api/entries/{id}/reuse | SearchPage.vue | P1 |
| TASK-402 | API-009 | POST /api/topics | TopicPage.vue | P2 |
| TASK-402 | API-010 | GET /api/topics | TopicPage.vue | P2 |
| TASK-402 | API-011 | GET /api/topics/{id} | TopicDetailPage.vue | P2 |
| TASK-402 | API-012 | GET /api/topics/{id}/entries | TopicDetailPage.vue | P2 |
| TASK-502 | API-013 | GET /api/entries/{id} | EntryDetailPage.vue | P2 |
| TASK-502 | API-014 | PUT /api/entries/{id}/insight | EntryDetailPage.vue | P2 |
| TASK-502 | API-015 | PUT /api/entries/{id}/topic | EntryDetailPage.vue | P2 |

---

# 7. 统一错误响应

ErrorResponse:
- success: false
- errorCode: string
- message: string
- timestamp: string

常见错误码：
- ENTRY_NOT_FOUND
- TOPIC_NOT_FOUND
- INVALID_PARAMETER
- UNAUTHORIZED
- INTERNAL_ERROR

---

**任务-页面-API DSL 映射已完成。**
