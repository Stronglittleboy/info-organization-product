# 查询模型 V1

项目：info-organization-product  
基于：`page-to-model-mapping-v1.md` 评审通过版

---

# 1. 设计目标

定义首版 5 个读模型的实现方式、字段、分页规则。

原则：
1. 读模型服务页面展示，不是写模型的直接映射
2. 每个页面有专门优化的读模型
3. 首版优先用查询投影，不做复杂缓存

---

# 2. RecentEntryItem（收集页）

## 用途
收集页"刚刚记录"列表

## 字段
```
{
  entryId: UUID
  contentPreview: String (前 100 字)
  sourceType: Enum
  capturedAt: Timestamp
  hasInsight: Boolean
  topicName: String (可空)
}
```

## 查询逻辑
```sql
SELECT 
  e.entryId,
  LEFT(e.rawContent, 100) as contentPreview,
  e.sourceType,
  e.capturedAt,
  (e.insightText IS NOT NULL) as hasInsight,
  t.name as topicName
FROM Entry e
LEFT JOIN Topic t ON e.topicId = t.topicId
WHERE e.capturedBy = :userId
ORDER BY e.capturedAt DESC
LIMIT 5
```

## 分页
固定 5 条，不分页

---

# 3. PendingEntryItem（待处理页）

## 用途
待处理页统一处理流

## 字段
```
{
  entryId: UUID
  contentPreview: String (前 200 字)
  sourceType: Enum
  capturedAt: Timestamp
  currentSuggestedAction: Enum (ADD_INSIGHT | ADD_TOPIC)
  insightText: String (可空)
  topicId: UUID (可空)
  topicName: String (可空)
}
```

## 查询逻辑
```sql
SELECT 
  e.entryId,
  LEFT(e.rawContent, 200) as contentPreview,
  e.sourceType,
  e.capturedAt,
  CASE 
    WHEN e.insightText IS NULL THEN 'ADD_INSIGHT'
    WHEN e.topicId IS NULL THEN 'ADD_TOPIC'
  END as currentSuggestedAction,
  e.insightText,
  e.topicId,
  t.name as topicName
FROM Entry e
LEFT JOIN Topic t ON e.topicId = t.topicId
WHERE e.capturedBy = :userId
  AND NOT (e.insightText IS NOT NULL AND e.topicId IS NOT NULL)
ORDER BY 
  (e.insightText IS NULL) DESC,
  e.capturedAt DESC,
  (e.skippedAt IS NULL) DESC
LIMIT 20 OFFSET :offset
```

## 分页
- 每页 20 条
- 支持 offset 分页

---

# 4. SearchResultItem（查找页）

## 用途
查找页搜索结果

## 字段
```
{
  entryId: UUID
  contentSnippet: String (命中片段前后各 50 字)
  insightText: String (可空)
  topicId: UUID (可空)
  topicName: String (可空)
  sourceType: Enum
  capturedAt: Timestamp
}
```

## 查询逻辑
```sql
-- 全文搜索实现依赖具体数据库
-- PostgreSQL 示例：
SELECT 
  e.entryId,
  ts_headline('chinese', e.rawContent, query) as contentSnippet,
  e.insightText,
  e.topicId,
  t.name as topicName,
  e.sourceType,
  e.capturedAt
FROM Entry e
LEFT JOIN Topic t ON e.topicId = t.topicId,
  plainto_tsquery('chinese', :keyword) query
WHERE e.capturedBy = :userId
  AND (
    to_tsvector('chinese', e.rawContent) @@ query
    OR to_tsvector('chinese', e.insightText) @@ query
  )
  AND (:topicId IS NULL OR e.topicId = :topicId)
  AND (:hasInsight IS NULL OR (e.insightText IS NOT NULL) = :hasInsight)
  AND (:startDate IS NULL OR e.capturedAt >= :startDate)
  AND (:endDate IS NULL OR e.capturedAt <= :endDate)
ORDER BY e.capturedAt DESC
LIMIT 20 OFFSET :offset
```

## 分页
- 每页 20 条
- 支持 offset 分页

## 筛选参数
- keyword: String (必填)
- topicId: UUID (可空)
- hasInsight: Boolean (可空)
- startDate: Timestamp (可空)
- endDate: Timestamp (可空)

---

# 5. TopicDetailView（专题页）

## 用途
专题页上下文视图

## 字段
```
{
  topicId: UUID
  name: String
  description: String
  entryCount: Integer
  entries: [
    {
      entryId: UUID
      contentPreview: String (前 200 字)
      insightText: String (可空)
      sourceType: Enum
      capturedAt: Timestamp
    }
  ]
}
```

## 查询逻辑
```sql
-- 主查询
SELECT 
  t.topicId,
  t.name,
  t.description,
  COUNT(e.entryId) as entryCount
FROM Topic t
LEFT JOIN Entry e ON e.topicId = t.topicId AND e.capturedBy = :userId
WHERE t.topicId = :topicId
GROUP BY t.topicId

-- 条目列表查询
SELECT 
  e.entryId,
  LEFT(e.rawContent, 200) as contentPreview,
  e.insightText,
  e.sourceType,
  e.capturedAt
FROM Entry e
WHERE e.topicId = :topicId
  AND e.capturedBy = :userId
ORDER BY e.capturedAt DESC
LIMIT 20 OFFSET :offset
```

## 分页
- entries 每页 20 条
- 支持 offset 分页

---

# 6. EntryDetailView（详情页）

## 用途
详情页完整视图

## 字段
```
{
  entryId: UUID
  rawContent: Text (完整)
  sourceType: Enum
  sourceTitle: String (可空)
  sourceRef: String (可空)
  capturedAt: Timestamp
  insightText: Text (可空)
  topicId: UUID (可空)
  topicName: String (可空)
  reusedCount: Integer
  lastReusedAt: Timestamp (可空)
}
```

## 查询逻辑
```sql
SELECT 
  e.entryId,
  e.rawContent,
  e.sourceType,
  e.sourceTitle,
  e.sourceRef,
  e.capturedAt,
  e.insightText,
  e.topicId,
  t.name as topicName,
  e.reusedCount,
  e.lastReusedAt
FROM Entry e
LEFT JOIN Topic t ON e.topicId = t.topicId
WHERE e.entryId = :entryId
  AND e.capturedBy = :userId
```

## 分页
不分页，单条完整返回

---

# 7. 实现方式建议

## 首版实现
- 优先用查询投影（直接 SQL 查询）
- 不做视图（避免维护成本）
- 不做缓存（首版数据量可控）

## 后续优化方向
- 待处理流可考虑缓存（如果查询频繁）
- 查找页可考虑 Elasticsearch（如果全文搜索性能不足）
- 专题页 entryCount 可考虑冗余字段

---

# 8. 索引建议

基于查询模型，建议以下索引：

## Entry 表
- (capturedBy, capturedAt DESC)
- (capturedBy, topicId, capturedAt DESC)
- (capturedBy, insightText IS NOT NULL, topicId IS NOT NULL, capturedAt DESC)
- 全文索引 (rawContent, insightText)

## Topic 表
- (topicId)

## ReuseRecord 表
- (entryId, reusedAt DESC)

---

# 9. 当前结论

查询模型 V1 已定义完成，可作为：
- API 草案设计输入
- 后续数据库查询实现参考
