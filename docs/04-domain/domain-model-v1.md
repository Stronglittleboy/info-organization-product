# 领域模型 V1

项目：info-organization-product  
基于：`page-to-model-mapping-v1.md` 评审通过版

---

# 1. 核心聚合

## 1.1 Entry 聚合（核心聚合根）

### 定位
Entry 是系统最核心的聚合根，代表一条被收进系统的信息素材。

### 字段
- entryId: UUID (主键)
- rawContent: Text (必填)
- sourceType: Enum (可空: BOOK, WEB, CONVERSATION, MANUAL, OTHER)
- sourceTitle: String (可空)
- sourceRef: String (可空, URL 或引用)
- capturedAt: Timestamp (必填)
- capturedBy: UserId (必填)
- insightText: Text (可空, 一句话思考)
- topicId: UUID (可空, 外键)
- skippedAt: Timestamp (可空, 待处理跳过标记)
- reusedCount: Integer (默认 0)
- lastReusedAt: Timestamp (可空)
- createdAt: Timestamp
- updatedAt: Timestamp

### 约束
1. rawContent 必填且不可为空字符串
2. 首版 insightText 只允许一句话（建议前端限制 500 字）
3. 首版 topicId 只允许一个主专题
4. sourceType/sourceTitle/sourceRef 允许全部为空
5. reusedCount 只增不减

### 索引建议
- capturedAt DESC (收集页、待处理页)
- topicId + capturedAt DESC (专题页)
- skippedAt (待处理流过滤)
- 全文索引 rawContent + insightText (查找页)

---

## 1.2 Topic 聚合（上下文聚合根）

### 定位
Topic 代表一个持续问题域，不是分类目录。

### 字段
- topicId: UUID (主键)
- name: String (必填, 建议 100 字内)
- description: Text (可空, 建议 500 字内)
- createdAt: Timestamp
- updatedAt: Timestamp
- createdBy: UserId

### 约束
1. name 必填且应能表达一个问题域
2. description 建议有，但首版可允许为空
3. 首版不支持父子 Topic 层级
4. 首版不支持 Topic 归档/删除（预留字段可选）

---

## 1.3 ReuseRecord 实体

### 定位
记录一次"直接取用"行为，用于未来复用分析。

### 字段
- reuseId: UUID (主键)
- entryId: UUID (外键, 必填)
- reuseType: Enum (必填: COPY_RAW, COPY_WITH_INSIGHT)
- reusedAt: Timestamp (必填)
- reusedBy: UserId (必填)

### 约束
1. 只记录复制行为，不记录浏览
2. 首版不关联输出清单
3. 创建 ReuseRecord 时同步更新 Entry.reusedCount 和 Entry.lastReusedAt

---

# 2. 值对象

## 2.1 SourceRef
- sourceType
- sourceTitle
- sourceRef

说明：来源信息允许不完整，作为可缺省值对象。

## 2.2 InsightText
- text
- updatedAt

说明：首版严格控制为"一句话思考"语义。

---

# 3. 领域规则

## 规则 1：Entry 永远先于 Insight 和 Topic
必须先有 Entry，才可能补思考或加入专题。

## 规则 2：Insight 比 Topic 优先（推荐，不强制）
从方法论上，先形成自己的理解更自然。  
但系统不强制顺序，只在待处理流中优先推荐补 Insight。

## 规则 3：Topic 是上下文，不是分类桶
Topic 的存在目的是围绕问题积累，不是按类型归档。

## 规则 4：Reuse 是行为，不是结构
首版"用"的定义是直接取用，ReuseRecord 只记录行为。

## 规则 5：跳过不是删除
skippedAt 只是标记，Entry 仍然存在，可重新进入待处理流。

---

# 4. 生命周期

Entry 内部生命周期语义（不直接暴露给前台）：

- **Captured**：已收集
- **Interpreted**：已有一句思考
- **Contextualized**：已归入专题
- **Reused**：至少一次被复制取用

说明：这些是系统内部语义，前台只看到页面任务。

---

# 5. 当前结论

领域模型 V1 已定义完成，可作为：
- 状态流设计输入
- 查询模型设计输入
- API 草案设计输入
- 后续数据库表结构设计输入
