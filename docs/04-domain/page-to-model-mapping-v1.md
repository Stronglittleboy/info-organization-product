# 页面到模型映射推导 V1

项目：info-organization-product  
路径：/vol3/1000/private/workProject/info-organization-product

---

# 1. 本文档目标

在进入纯技术领域模型前，先建立**前台页面与后端模型的映射关系**。

目标：
1. 让产品/UI 能理解：页面动作背后对应什么数据操作
2. 让架构师能理解：哪些是写模型，哪些是读模型
3. 让奥野监督人能验证：模型是否守住方法论主链

本文档不是最终技术实现，而是**设计推导过程**。

---

# 2. 核心原则

## 原则 1：页面动作优先，模型服务页面
不是先设计完美模型，再看页面能不能用。  
而是：页面要什么，模型就提供什么。

## 原则 2：读写分离
页面展示用的数据（读模型），和页面操作用的数据（写模型），不一定是同一个对象。

## 原则 3：方法论主链必须在模型中显性存在
- 先收进来 → Entry 创建
- 附着思考 → Insight
- 归入持续问题 → Topic
- 直接取用 → Reuse

这 4 个环节必须在模型中有明确对应。

---

# 3. 收集页映射

## 3.1 页面核心动作
1. 用户输入内容并保存
2. 保存后可选：补一句思考
3. 保存后可选：加入专题

## 3.2 对应的写操作
### 动作 1：保存
**写模型：**
- 创建 `Entry`
- 字段：rawContent, sourceType, sourceRef, capturedAt

**为什么这样切：**
- Entry 是系统最核心的事实对象
- 它代表"一条信息被收进系统"
- 符合奥野"先收进来"原则

### 动作 2：补一句思考
**写模型：**
- 更新 Entry.insightText

**为什么这样切：**
- 首版只允许一句话思考
- 因此 Insight 不需要独立成表，作为 Entry 的字段即可
- 符合奥野"信息必须附着自己的理解"原则

### 动作 3：加入专题
**写模型：**
- 更新 Entry.topicId

**为什么这样切：**
- 首版只允许一个主专题
- 因此 topicId 作为 Entry 的外键即可
- 符合奥野"持续问题需要稳定上下文"原则

## 3.3 对应的读模型
收集页需要展示"刚刚记录的 3~5 条"。

**读模型：**
```
RecentEntryItem {
  entryId
  contentPreview (前 100 字)
  sourceType
  capturedAt
  hasInsight (boolean)
  topicName (可空)
}
```

**为什么这样切：**
- 收集页不需要完整原文
- 不需要完整思考
- 只需要知道"有没有思考、有没有专题"
- 这是典型的读模型投影

---

# 4. 待处理页映射

## 4.1 页面核心动作
1. 拉取待处理流
2. 看到"当前建议动作"
3. 补一句思考 或 加入专题
4. 跳过

## 4.2 对应的读模型
待处理页最关键的是：**系统要告诉用户"当前这条该做什么"**。

**读模型：**
```
PendingEntryItem {
  entryId
  contentPreview
  sourceType
  capturedAt
  currentSuggestedAction (enum: ADD_INSIGHT | ADD_TOPIC)
  insightText (可空)
  topicId (可空)
  topicName (可空)
}
```

**为什么有 currentSuggestedAction：**
- 前台不应该自己判断"该补思考还是该入专题"
- 后端根据 Entry 当前状态推导出建议动作
- 这样前台只需要渲染，不需要理解状态机

**推导规则：**
- 若 insightText 为空 → ADD_INSIGHT
- 若 insightText 有值但 topicId 为空 → ADD_TOPIC
- 若都有值 → 不进入待处理流

## 4.3 对应的写操作
### 动作：补一句思考
**写模型：**
- 更新 Entry.insightText

### 动作：加入专题
**写模型：**
- 更新 Entry.topicId

### 动作：跳过
**写模型：**
- 更新 Entry.skippedAt
- 不删除，不移出系统
- 只是标记"用户暂时跳过"

**为什么这样切：**
- 跳过不是永久忽略
- 后续可以根据时间窗口让它重新回到前部
- 这符合"轻整理"原则，不逼用户立刻决策

---

# 5. 查找页映射

## 5.1 页面核心动作
1. 搜索
2. 查看结果
3. 复制原文 或 复制原文+思考

## 5.2 对应的读模型
查找页需要的是：**能直接展示并复制的结果**。

**读模型：**
```
SearchResultItem {
  entryId
  contentSnippet (命中片段)
  insightText
  topicId
  topicName
  sourceType
  capturedAt
}
```

**为什么这样切：**
- 查找页需要完整思考，不是只有"有没有"
- 需要专题名，不只是 ID
- 需要命中片段，不是完整原文
- 这是专门为查找页优化的读模型

## 5.3 对应的写操作
### 动作：复制
**写模型：**
- 创建 `ReuseRecord`
- 字段：entryId, reuseType (COPY_RAW | COPY_WITH_INSIGHT), reusedAt

**为什么这样切：**
- 首版"用"的定义是"直接取用"
- 因此只记录复制行为
- 不做输出清单，不做复杂组合
- 符合首版克制原则

---

# 6. 专题页映射

## 6.1 页面核心动作
1. 查看专题说明
2. 查看专题内素材列表
3. 进入详情

## 6.2 对应的读模型
专题页需要的是：**围绕一个问题的上下文视图**。

**读模型：**
```
TopicDetailView {
  topicId
  name
  description
  entryCount
  entries: [
    {
      entryId
      contentPreview
      insightText
      sourceType
      capturedAt
    }
  ]
}
```

**为什么这样切：**
- 专题页不是简单的"按 topicId 过滤 Entry"
- 它需要专题本身的说明
- 需要条目数
- 需要每条 Entry 的思考
- 这是专门为"持续问题上下文"设计的读模型

## 6.3 对应的写操作
首版专题页不承载写操作，只读。

---

# 7. 详情页映射

## 7.1 页面核心动作
1. 查看完整原文
2. 查看/编辑一句思考
3. 查看/更换专题
4. 复制

## 7.2 对应的读模型
详情页需要的是：**单条完整视图**。

**读模型：**
```
EntryDetailView {
  entryId
  rawContent (完整)
  sourceType
  sourceTitle
  sourceRef
  capturedAt
  insightText
  topicId
  topicName
  reusedCount
  lastReusedAt
}
```

**为什么这样切：**
- 详情页需要完整原文，不是片段
- 需要完整来源信息
- 需要复用次数（虽然首版不外显，但模型要有）
- 这是最完整的读模型

## 7.3 对应的写操作
### 动作：更新思考
**写模型：**
- 更新 Entry.insightText

### 动作：更换专题
**写模型：**
- 更新 Entry.topicId

### 动作：复制
**写模型：**
- 创建 ReuseRecord
- 更新 Entry.reusedCount
- 更新 Entry.lastReusedAt

---

# 8. 核心对象总结

通过上面的映射推导，我们得出首版需要的核心对象：

## 8.1 写模型（持久化对象）
### Entry
- entryId
- rawContent
- sourceType
- sourceTitle
- sourceRef
- capturedAt
- insightText (可空)
- topicId (可空)
- skippedAt (可空)
- reusedCount
- lastReusedAt

### Topic
- topicId
- name
- description
- createdAt

### ReuseRecord
- reuseId
- entryId
- reuseType
- reusedAt

## 8.2 读模型（查询投影）
- RecentEntryItem
- PendingEntryItem
- SearchResultItem
- TopicDetailView
- EntryDetailView

---

# 9. 方法论主链验证

## 9.1 "先收进来"
✅ Entry 创建时只需要 rawContent，其他都可空

## 9.2 "附着思考"
✅ insightText 作为 Entry 的核心字段存在

## 9.3 "归入持续问题"
✅ Topic 作为独立对象存在，Entry 通过 topicId 关联

## 9.4 "直接取用"
✅ ReuseRecord 记录复制行为，Entry 维护复用计数

## 9.5 "不逼用户经营"
✅ 跳过只是标记，不是删除  
✅ 待处理流由系统推导建议动作，不暴露状态机

---

# 10. 当前结论

这份映射推导完成后，可以进入下一步：

1. 把写模型正式定义成领域模型文档
2. 把读模型正式定义成查询模型文档
3. 把状态推导规则正式定义成状态流文档

但在进入之前，应该先让 4 个角色评审这份映射推导。
