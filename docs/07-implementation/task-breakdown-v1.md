# 任务拆分详细清单 V1

项目：info-organization-product

---

# TASK-000: 环境搭建与项目初始化

## 优先级
P0

## 预估工时
8小时

## 任务描述
搭建开发环境，初始化前后端项目，创建数据库表结构。

## 子任务
- [ ] 安装 JDK 17, Maven, Node.js, Docker
- [ ] 创建后端项目目录结构
- [ ] 创建前端项目目录结构
- [ ] 启动 PostgreSQL 容器
- [ ] 执行数据库初始化脚本
- [ ] 验证后端能启动
- [ ] 验证前端能启动
- [ ] 验证数据库连接

## 验收标准
- ✅ 后端访问 /api/actuator/health 返回 UP
- ✅ 前端访问 http://localhost:3000 能看到页面
- ✅ 数据库表结构创建成功

## 依赖
无

---

# TASK-101: 收集页-后端实体与Mapper

## 优先级
P0

## 预估工时
4小时

## 任务描述
创建 Entry 和 User 实体类，创建 MyBatis-Plus Mapper。

## 子任务
- [ ] 创建 User.java 实体类
- [ ] 创建 Entry.java 实体类
- [ ] 创建 UserMapper.java
- [ ] 创建 EntryMapper.java
- [ ] 编写单元测试

## 交付物
- `entity/User.java`
- `entity/Entry.java`
- `mapper/UserMapper.java`
- `mapper/EntryMapper.java`
- `test/.../EntryMapperTest.java`

## 验收标准
- ✅ 实体类字段与数据库表一致
- ✅ Mapper 能执行基本 CRUD
- ✅ 单元测试通过

## 依赖
- TASK-000

---

# TASK-102: 收集页-后端DTO定义

## 优先级
P0

## 预估工时
3小时

## 任务描述
定义收集页相关的请求和响应 DTO。

## 子任务
- [ ] 创建 CreateEntryRequest.java
- [ ] 创建 AddInsightRequest.java
- [ ] 创建 SetTopicRequest.java
- [ ] 创建 EntryResponse.java
- [ ] 创建 RecentEntryItemResponse.java
- [ ] 添加参数校验注解

## 交付物
- `dto/request/CreateEntryRequest.java`
- `dto/request/AddInsightRequest.java`
- `dto/request/SetTopicRequest.java`
- `dto/response/EntryResponse.java`
- `dto/response/RecentEntryItemResponse.java`

## 验收标准
- ✅ DTO 字段与 API 草案一致
- ✅ 参数校验注解正确

## 依赖
- TASK-101

---

# TASK-103: 收集页-后端Service层

## 优先级
P0

## 预估工时
6小时

## 任务描述
实现收集页的业务逻辑。

## 子任务
- [ ] 创建 EntryService 接口
- [ ] 创建 EntryServiceImpl 实现类
- [ ] 实现 createEntry() 方法
- [ ] 实现 addInsight() 方法
- [ ] 实现 setTopic() 方法
- [ ] 实现 getRecentEntries() 方法
- [ ] 编写单元测试

## 交付物
- `service/EntryService.java`
- `service/impl/EntryServiceImpl.java`
- `test/.../EntryServiceTest.java`

## 验收标准
- ✅ 业务逻辑正确
- ✅ 单元测试覆盖率 > 80%
- ✅ 异常处理完善

## 依赖
- TASK-102

---

# TASK-104: 收集页-后端Controller层

## 优先级
P0

## 预估工时
4小时

## 任务描述
实现收集页的 REST API。

## 子任务
- [ ] 创建 EntryController.java
- [ ] 实现 POST /api/entries
- [ ] 实现 POST /api/entries/{entryId}/insight
- [ ] 实现 POST /api/entries/{entryId}/topic
- [ ] 实现 GET /api/entries/recent
- [ ] 添加参数校验
- [ ] 添加异常处理
- [ ] 编写 Postman Collection

## 交付物
- `controller/EntryController.java`
- `postman/collect-page.json`

## 验收标准
- ✅ API 路径与 API 草案一致
- ✅ Postman 测试全部通过
- ✅ 错误响应格式统一

## 依赖
- TASK-103

---

# TASK-105: 收集页-前端API封装

## 优先级
P0

## 预估工时
2小时

## 任务描述
封装收集页的前端 API 调用。

## 子任务
- [ ] 创建 api/entry.ts
- [ ] 实现 createEntry()
- [ ] 实现 addInsight()
- [ ] 实现 setTopic()
- [ ] 实现 getRecentEntries()
- [ ] 配置 axios 拦截器

## 交付物
- `api/entry.ts`
- `api/request.ts`（axios 配置）

## 验收标准
- ✅ API 函数能正确调用后端
- ✅ 错误处理完善
- ✅ TypeScript 类型定义完整

## 依赖
- TASK-104

---

# TASK-106: 收集页-前端页面组件

## 优先级
P0

## 预估工时
8小时

## 任务描述
实现收集页的前端页面。

## 子任务
- [ ] 创建 views/CollectPage.vue
- [ ] 创建 components/EntryCard.vue
- [ ] 实现输入框
- [ ] 实现保存按钮
- [ ] 实现保存后反馈区
- [ ] 实现最近记录列表
- [ ] 实现补一句思考
- [ ] 实现加入专题
- [ ] 添加加载状态
- [ ] 添加错误提示

## 交付物
- `views/CollectPage.vue`
- `components/EntryCard.vue`

## 验收标准
- ✅ 页面布局符合中保真设计
- ✅ 交互流程正确
- ✅ 响应式布局

## 依赖
- TASK-105

---

# TASK-107: 收集页-前端状态管理

## 优先级
P0

## 预估工时
3小时

## 任务描述
实现收集页的状态管理。

## 子任务
- [ ] 创建 stores/entry.ts
- [ ] 定义状态
- [ ] 实现 actions
- [ ] 实现 getters
- [ ] 集成到组件

## 交付物
- `stores/entry.ts`

## 验收标准
- ✅ 状态更新正确
- ✅ 组件能正确读取状态

## 依赖
- TASK-106

---

# TASK-108: 收集页-完整流程联调

## 优先级
P0

## 预估工时
4小时

## 任务描述
联调收集页的完整流程。

## 测试场景
1. 输入内容 → 保存 → 成功提示
2. 保存后 → 补一句思考 → 成功
3. 保存后 → 加入专题 → 成功
4. 刷新页面 → 最近记录显示正确
5. 错误场景 → 错误提示正确

## 验收标准
- ✅ 所有测试场景通过
- ✅ 无明显 bug
- ✅ 用户体验流畅

## 依赖
- TASK-107

---

# TASK-201: 待处理页-后端待处理流查询

## 优先级
P1

## 预估工时
6小时

## 任务描述
实现待处理流的查询逻辑。

## 子任务
- [ ] 创建 PendingService.java
- [ ] 实现待处理流查询
- [ ] 实现 currentSuggestedAction 推导
- [ ] 实现跳过逻辑
- [ ] 扩展 EntryMapper
- [ ] 编写单元测试

## 交付物
- `service/PendingService.java`
- `mapper/EntryMapper.java`（扩展）
- `test/.../PendingServiceTest.java`

## 验收标准
- ✅ 待处理流排序正确
- ✅ 建议动作推导正确
- ✅ 跳过逻辑正确

## 依赖
- TASK-108

---

# TASK-202: 待处理页-后端Controller扩展

## 优先级
P1

## 预估工时
3小时

## 任务描述
实现待处理页的 REST API。

## 子任务
- [ ] 创建 PendingController.java
- [ ] 实现 GET /api/pending
- [ ] 实现 POST /api/entries/{entryId}/skip
- [ ] 更新 Postman Collection

## 交付物
- `controller/PendingController.java`
- `postman/pending-page.json`

## 验收标准
- ✅ API 测试通过
- ✅ 分页正确

## 依赖
- TASK-201

---

# TASK-203: 待处理页-前端页面组件

## 优先级
P1

## 预估工时
8小时

## 任务描述
实现待处理页的前端页面。

## 子任务
- [ ] 创建 views/PendingPage.vue
- [ ] 创建 components/PendingCard.vue
- [ ] 实现待处理卡片流
- [ ] 实现当前建议动作高亮
- [ ] 实现快速补思考
- [ ] 实现快速入专题
- [ ] 实现跳过按钮
- [ ] 添加空状态

## 交付物
- `views/PendingPage.vue`
- `components/PendingCard.vue`

## 验收标准
- ✅ 页面布局符合中保真设计
- ✅ 建议动作高亮明显
- ✅ 交互流畅

## 依赖
- TASK-202

---

# TASK-204: 待处理页-完整流程联调

## 优先级
P1

## 预估工时
3小时

## 测试场景
1. 进入待处理页 → 看到待处理条目
2. 补一句思考 → 条目移出待处理流
3. 加入专题 → 条目移出待处理流
4. 跳过 → 条目暂时消失
5. 时间窗口后 → 跳过条目再出现

## 验收标准
- ✅ 所有测试场景通过

## 依赖
- TASK-203

---

# TASK-301: 查找页-后端全文搜索

## 优先级
P1

## 预估工时
8小时

## 任务描述
实现 PostgreSQL 全文搜索。

## 子任务
- [ ] 创建 SearchService.java
- [ ] 实现全文搜索逻辑
- [ ] 实现过滤条件（时间/专题/有无思考）
- [ ] 实现分页
- [ ] 验证中文分词效果
- [ ] 编写单元测试

## 交付物
- `service/SearchService.java`
- `test/.../SearchServiceTest.java`

## 验收标准
- ✅ 搜索结果正确
- ✅ 中文分词生效
- ✅ 过滤条件正确

## 依赖
- TASK-204

---

# TASK-302: 查找页-后端复用记录

## 优先级
P1

## 预估工时
4小时

## 任务描述
实现复用记录功能。

## 子任务
- [ ] 创建 ReuseRecord.java 实体
- [ ] 创建 ReuseRecordMapper.java
- [ ] 创建 ReuseService.java
- [ ] 实现记录复制行为
- [ ] 实现复用统计

## 交付物
- `entity/ReuseRecord.java`
- `mapper/ReuseRecordMapper.java`
- `service/ReuseService.java`

## 验收标准
- ✅ 复用记录保存成功
- ✅ 统计数据正确

## 依赖
- TASK-301

---

# TASK-303: 查找页-后端Controller扩展

## 优先级
P1

## 预估工时
3小时

## 任务描述
实现查找页的 REST API。

## 子任务
- [ ] 创建 SearchController.java
- [ ] 实现 GET /api/search
- [ ] 实现 POST /api/entries/{entryId}/reuse
- [ ] 更新 Postman Collection

## 交付物
- `controller/SearchController.java`
- `postman/search-page.json`

## 验收标准
- ✅ API 测试通过

## 依赖
- TASK-302

---

# TASK-304: 查找页-前端页面组件

## 优先级
P1

## 预估工时
10小时

## 任务描述
实现查找页的前端页面。

## 子任务
- [ ] 创建 views/SearchPage.vue
- [ ] 创建 components/SearchResultCard.vue
- [ ] 实现搜索框
- [ ] 实现过滤器
- [ ] 实现结果列表
- [ ] 实现复制按钮
- [ ] 实现分页
- [ ] 添加空状态

## 交付物
- `views/SearchPage.vue`
- `components/SearchResultCard.vue`

## 验收标准
- ✅ 页面布局符合中保真设计
- ✅ 搜索响应快速
- ✅ 复制功能正确

## 依赖
- TASK-303

---

# TASK-305: 查找页-完整流程联调

## 优先级
P1

## 预估工时
4小时

## 测试场景
1. 输入关键词 → 搜索 → 看到结果
2. 过滤条件 → 结果更新
3. 复制原文 → 成功提示
4. 复制原文+思考 → 成功提示
5. 分页 → 正确加载

## 验收标准
- ✅ 所有测试场景通过

## 依赖
- TASK-304

---

# TASK-401~404: 专题页任务
（详细拆分略，结构同上）

# TASK-501~504: 详情页任务
（详细拆分略，结构同上）

# TASK-601~604: 优化测试任务
（详细拆分略，结构同上）

---

**任务拆分已完成，共 30+ 个任务。** ✅
