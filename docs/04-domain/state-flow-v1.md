# 状态流设计 V1

项目：info-organization-product  
基于：`page-to-model-mapping-v1.md` 评审通过版

---

# 1. 核心状态维度

首版只用两个核心维度判断条目状态：

## 维度 A：是否有一句思考
- insightText IS NULL → 无思考
- insightText IS NOT NULL → 有思考

## 维度 B：是否已归入专题
- topicId IS NULL → 无专题
- topicId IS NOT NULL → 有专题

---

# 2. 待处理流规则

## 2.1 进入待处理流的条件
满足以下任一条件即进入：
1. insightText IS NULL AND topicId IS NULL
2. insightText IS NOT NULL AND topicId IS NULL
3. insightText IS NULL AND topicId IS NOT NULL

## 2.2 移出待处理流的条件
满足以下条件则移出：
- insightText IS NOT NULL AND topicId IS NOT NULL

## 2.3 跳过规则
当用户点击"跳过"时：
- 更新 Entry.skippedAt = NOW()
- 条目在当前流中后移
- 不立即永久移除

## 2.4 跳过后的再出现规则
跳过条目在以下情况可重新回到前部：
- 距离 skippedAt 超过 24 小时（首版参数）
- 或当前待处理流已明显变短（少于 5 条）

说明：24 小时是首版实现参数，后续可调整。

---

# 3. currentSuggestedAction 推导规则

待处理页读模型中的 currentSuggestedAction 字段推导规则：

## 情况 A：insightText IS NULL AND topicId IS NULL
→ currentSuggestedAction = ADD_INSIGHT

## 情况 B：insightText IS NOT NULL AND topicId IS NULL
→ currentSuggestedAction = ADD_TOPIC

## 情况 C：insightText IS NULL AND topicId IS NOT NULL
→ currentSuggestedAction = ADD_INSIGHT

## 情况 D：insightText IS NOT NULL AND topicId IS NOT NULL
→ 不进入待处理流

---

# 4. 待处理流排序规则

优先级从高到低：

## 一级优先
- insightText IS NULL 的条目优先于 insightText IS NOT NULL 的条目

## 二级优先
- capturedAt DESC（最近优先）

## 三级优先
- skippedAt IS NULL 的条目优先于 skippedAt IS NOT NULL 的条目

说明：这样符合方法论优先级——先让内容拥有自己的理解。

---

# 5. 收集页状态流

## 创建 Entry
- 初始状态：insightText = NULL, topicId = NULL
- 自动进入待处理流

## 保存后轻反馈动作
在收集页保存后，可对刚保存单条做：
- 补一句思考 → 更新 insightText
- 加入专题 → 更新 topicId

说明：这些动作只作用于刚保存单条，不作用于列表全量。

---

# 6. 查找页状态流

查找页不负责推进结构状态，只负责读取与复用。

## 支持的读维度
- 内容命中
- 思考命中
- 专题命中
- 时间过滤

## 支持的写行为
- 复制原文 → 创建 ReuseRecord(COPY_RAW)
- 复制原文+思考 → 创建 ReuseRecord(COPY_WITH_INSIGHT)
- 同步更新 Entry.reusedCount += 1
- 同步更新 Entry.lastReusedAt = NOW()

说明：查找页不改变 Entry 的 insightText / topicId 状态。

---

# 7. 专题页状态流

专题页本身不推进 Entry 状态，只读取某 Topic 关联的 Entry。

## 专题页需要的数据条件
- Topic 存在
- Entry.topicId = 当前 Topic

## 专题页的交互结果
- 进入详情
- 返回查找或来源页

说明：首版专题页不承载复杂管理状态。

---

# 8. 详情页状态流

详情页允许对单条 Entry 做完整轻完善。

## 允许的状态推进
1. insightText NULL → NOT NULL
2. topicId NULL → NOT NULL
3. 复制行为 → 产生 ReuseRecord + 更新 reusedCount

## 不允许的复杂状态
- 不支持多 Topic 并挂
- 不支持多 Insight 历史版本外显
- 不支持输出清单相关状态

---

# 9. 状态与页面关系总结

## 收集页
- 只负责创建 Entry 和刚保存单条的轻推进

## 待处理页
- 消费待处理流
- 根据系统推导给出 currentSuggestedAction

## 查找页
- 仅读取，不推进结构状态
- 复制时记录复用行为

## 专题页
- 只消费 Topic 上下文读取模型

## 详情页
- 单条轻推进的完整入口

---

# 10. 当前结论

状态流 V1 已定义完成，可作为：
- 查询模型设计输入
- API 草案设计输入
- 后续实施逻辑参考
