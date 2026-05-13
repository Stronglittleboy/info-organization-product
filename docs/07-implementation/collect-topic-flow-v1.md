# 收集页 Topic 归类链路实现记录 V1

## 本次完成
- 收集接口支持 `topicName`
- 后端支持 topic 自动查询 / 自动创建
- entry 写入时自动关联 `topic_id`
- recent 列表返回 `topicId` 与 `topicName`
- 前端收集页新增“专题名称”输入框
- 前后端联调通过

## 后端实现
### 请求模型
- `CreateEntryRequest` 新增：
  - `topicName`

### 新增实体/Mapper
- `Topic`
- `TopicMapper`
  - `findByUserIdAndName`
  - `insertTopic`

### Entry 侧改造
- `Entry` 新增运行态字段：
  - `topicName`（非表字段）
- `EntryMapper`
  - 插入时支持 `topic_id`
  - recent 查询 `LEFT JOIN topics`
  - 返回 `topic_name`

### Service 改造
- `EntryServiceImpl`
  - 新增 `resolveTopic()`
  - 有 `topicName` 时先查 topic
  - 不存在则自动创建 topic
  - 创建 entry 时自动绑定 `topicId`

## 前端实现
- `src/types/entry.ts` 新增 `topicName`
- `src/views/CollectPage.vue` 新增专题输入框
- 保存时携带 `topicName`
- recent 列表展示专题名

## 测试与联调
### 自动化测试
- `EntryControllerTest` 新增 topic 归类测试
- 全量通过

### 运行态联调
- `POST /api/entries` with `topicName`：成功
- 返回：`topicId` / `topicName`
- `GET /api/entries/recent`：可看到 topic 信息

## 本次排障
1. 测试容器 DNS 无法解析 `wangliang.dns.army`
   - 测试执行改为显式 `DB_URL=jdbc:postgresql://101.69.20.54:55432/info_organization`
2. 长跑后端进程仍是旧代码
   - 重启进程后联调恢复正常

## 当前状态
收集页现在已具备：
- 手工输入内容
- 指定专题名
- 自动创建专题
- 自动建立 entry 与 topic 关联
- recent 列表可见专题归类结果

## 下一步建议
- 支持 topic 下拉选择 + 模糊搜索
- 支持 topic 列表接口
- 增加 insight_text 编辑与自动生成链路
- 增加 recent 过滤：按 topic 查看
