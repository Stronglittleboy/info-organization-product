# 项目实施计划 V1

项目：info-organization-product  
基于：所有设计文档 + 技术选型 V1.1

---

# 1. 实施总览

## 1.1 实施策略
按页面优先级逐个实现，每个页面完成后端 → 前端 → 联调的完整闭环。

## 1.2 实施顺序
1. 环境搭建与项目初始化
2. 收集页（最高优先级）
3. 待处理页
4. 查找页
5. 专题页
6. 详情页

## 1.3 预估工期
- 环境搭建：1 天
- 收集页：2-3 天
- 待处理页：2-3 天
- 查找页：3-4 天
- 专题页：2-3 天
- 详情页：1-2 天
- 联调与优化：2-3 天

**总计：13-19 天**

---

# 2. 阶段 0：环境搭建（1 天）

## 2.1 开发环境要求
- JDK 17+
- Node.js 18+
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL 15+（或通过 Docker）
- IDE：IntelliJ IDEA / VS Code

## 2.2 项目初始化步骤

### 后端初始化
```bash
cd /vol3/1000/private/workProject/info-organization-product/code/backend

# 创建标准 Spring Boot 目录结构
mkdir -p src/main/java/com/example/infoorg/{config,controller,service,mapper,entity,dto,security,exception}
mkdir -p src/main/resources
mkdir -p src/test/java/com/example/infoorg

# 复制 pom.xml 和 application.yml（已生成）
# 使用 Maven 下载依赖
mvn clean install
```

### 前端初始化
```bash
cd /vol3/1000/private/workProject/info-organization-product/code/frontend

# 安装依赖
npm install

# 创建标准 Vue 3 目录结构
mkdir -p src/{views,components,api,stores,router,types,utils}
```

### 数据库初始化
```bash
cd /vol3/1000/private/workProject/info-organization-product/code/docker

# 启动 PostgreSQL
docker-compose up -d postgres

# 等待数据库就绪
docker-compose logs -f postgres

# 执行初始化脚本
docker exec -i info-org-postgres psql -U postgres -d info_organization < init-db/V1__init_schema.sql
```

## 2.3 验收标准
- ✅ 后端项目能启动，访问 http://localhost:8080/api/actuator/health 返回 UP
- ✅ 前端项目能启动，访问 http://localhost:3000 能看到页面
- ✅ 数据库表结构创建成功，能查询到 users/entries/topics/reuse_records 表

---

# 3. 阶段 1：收集页实现（2-3 天）

## 3.1 后端任务

### 任务 1.1：实体类与 Mapper
**文件：**
- `entity/Entry.java`
- `entity/User.java`
- `mapper/EntryMapper.java`

**内容：**
- Entry 实体类（对应 entries 表）
- User 实体类（对应 users 表）
- EntryMapper 接口（MyBatis-Plus BaseMapper）

**验收：**
- 能通过 Mapper 执行基本 CRUD

### 任务 1.2：DTO 定义
**文件：**
- `dto/request/CreateEntryRequest.java`
- `dto/request/AddInsightRequest.java`
- `dto/request/SetTopicRequest.java`
- `dto/response/EntryResponse.java`
- `dto/response/RecentEntryItemResponse.java`

**内容：**
- 请求/响应 DTO
- 参数校验注解

**验收：**
- DTO 字段与 API 草案一致

### 任务 1.3：Service 层
**文件：**
- `service/EntryService.java`
- `service/impl/EntryServiceImpl.java`

**内容：**
- 创建素材
- 补一句思考
- 加入专题
- 获取最近素材列表

**验收：**
- 单元测试通过

### 任务 1.4：Controller 层
**文件：**
- `controller/EntryController.java`

**内容：**
- POST /api/entries
- POST /api/entries/{entryId}/insight
- POST /api/entries/{entryId}/topic
- GET /api/entries/recent

**验收：**
- Postman 测试通过

## 3.2 前端任务

### 任务 1.5：API 封装
**文件：**
- `api/entry.ts`

**内容：**
- createEntry()
- addInsight()
- setTopic()
- getRecentEntries()

**验收：**
- 能调用后端接口

### 任务 1.6：收集页组件
**文件：**
- `views/CollectPage.vue`
- `components/EntryCard.vue`

**内容：**
- 输入框
- 保存按钮
- 保存后反馈区
- 最近记录列表

**验收：**
- 页面布局符合中保真设计

### 任务 1.7：状态管理
**文件：**
- `stores/entry.ts`

**内容：**
- 收集页状态管理
- 最近素材缓存

**验收：**
- 状态更新正确

## 3.3 联调任务

### 任务 1.8：完整流程测试
**测试场景：**
1. 输入内容 → 保存 → 成功提示
2. 保存后 → 补一句思考 → 成功
3. 保存后 → 加入专题 → 成功
4. 刷新页面 → 最近记录显示正确

**验收：**
- 所有场景通过

---

# 4. 阶段 2：待处理页实现（2-3 天）

## 4.1 后端任务

### 任务 2.1：待处理流查询
**文件：**
- `service/PendingService.java`
- `mapper/EntryMapper.java`（扩展）

**内容：**
- 获取待处理流
- currentSuggestedAction 推导逻辑
- 跳过逻辑

**验收：**
- 待处理流排序正确
- 建议动作推导正确

### 任务 2.2：Controller 扩展
**文件：**
- `controller/PendingController.java`

**内容：**
- GET /api/pending
- POST /api/entries/{entryId}/skip

**验收：**
- Postman 测试通过

## 4.2 前端任务

### 任务 2.3：待处理页组件
**文件：**
- `views/PendingPage.vue`
- `components/PendingCard.vue`

**内容：**
- 待处理卡片流
- 当前建议动作高亮
- 快速补思考
- 快速入专题
- 跳过按钮

**验收：**
- 页面布局符合中保真设计

## 4.3 联调任务

### 任务 2.4：完整流程测试
**测试场景：**
1. 进入待处理页 → 看到待处理条目
2. 补一句思考 → 条目移出待处理流
3. 加入专题 → 条目移出待处理流
4. 跳过 → 条目暂时消失

**验收：**
- 所有场景通过

---

# 5. 阶段 3：查找页实现（3-4 天）

## 5.1 后端任务

### 任务 3.1：全文搜索
**文件：**
- `service/SearchService.java`
- `mapper/EntryMapper.java`（扩展）

**内容：**
- PostgreSQL 全文搜索
- 中文分词支持
- 过滤条件（时间/专题/有无思考）
- 分页

**验收：**
- 搜索结果正确
- 中文分词生效

### 任务 3.2：复用记录
**文件：**
- `service/ReuseService.java`
- `entity/ReuseRecord.java`
- `mapper/ReuseRecordMapper.java`

**内容：**
- 记录复制行为
- 复用统计

**验收：**
- 复用记录保存成功

### 任务 3.3：Controller 扩展
**文件：**
- `controller/SearchController.java`

**内容：**
- GET /api/search
- POST /api/entries/{entryId}/reuse

**验收：**
- Postman 测试通过

## 5.2 前端任务

### 任务 3.4：查找页组件
**文件：**
- `views/SearchPage.vue`
- `components/SearchResultCard.vue`

**内容：**
- 搜索框
- 过滤器
- 结果列表
- 复制按钮

**验收：**
- 页面布局符合中保真设计

## 5.3 联调任务

### 任务 3.5：完整流程测试
**测试场景：**
1. 输入关键词 → 搜索 → 看到结果
2. 过滤条件 → 结果更新
3. 复制原文 → 成功提示
4. 复制原文+思考 → 成功提示

**验收：**
- 所有场景通过

---

# 6. 阶段 4：专题页实现（2-3 天）

## 6.1 后端任务

### 任务 4.1：专题管理
**文件：**
- `service/TopicService.java`
- `entity/Topic.java`
- `mapper/TopicMapper.java`

**内容：**
- 创建专题
- 获取专题列表
- 获取专题详情
- 获取专题内素材

**验收：**
- 专题 CRUD 正确

### 任务 4.2：Controller 扩展
**文件：**
- `controller/TopicController.java`

**内容：**
- POST /api/topics
- GET /api/topics
- GET /api/topics/{topicId}
- GET /api/topics/{topicId}/entries

**验收：**
- Postman 测试通过

## 6.2 前端任务

### 任务 4.3：专题页组件
**文件：**
- `views/TopicPage.vue`
- `views/TopicDetailPage.vue`
- `components/TopicCard.vue`

**内容：**
- 专题列表
- 专题详情
- 专题内素材列表

**验收：**
- 页面布局符合中保真设计

## 6.3 联调任务

### 任务 4.4：完整流程测试
**测试场景：**
1. 创建专题 → 成功
2. 查看专题列表 → 显示正确
3. 进入专题详情 → 看到相关素材
4. 从专题进入详情 → 跳转正确

**验收：**
- 所有场景通过

---

# 7. 阶段 5：详情页实现（1-2 天）

## 7.1 后端任务

### 任务 5.1：详情查询
**文件：**
- `service/EntryService.java`（扩展）

**内容：**
- 获取单条详情
- 更新思考
- 更换专题

**验收：**
- 详情返回完整

### 任务 5.2：Controller 扩展
**文件：**
- `controller/EntryController.java`（扩展）

**内容：**
- GET /api/entries/{entryId}
- PUT /api/entries/{entryId}/insight
- PUT /api/entries/{entryId}/topic

**验收：**
- Postman 测试通过

## 7.2 前端任务

### 任务 5.3：详情页组件
**文件：**
- `views/EntryDetailPage.vue`

**内容：**
- 完整原文
- 来源信息
- 思考编辑
- 专题选择
- 复制按钮

**验收：**
- 页面布局符合中保真设计

## 7.3 联调任务

### 任务 5.4：完整流程测试
**测试场景：**
1. 从任意页面进入详情 → 显示正确
2. 编辑思考 → 保存成功
3. 更换专题 → 保存成功
4. 复制 → 成功提示

**验收：**
- 所有场景通过

---

# 8. 阶段 6：联调与优化（2-3 天）

## 8.1 全流程测试
**测试场景：**
1. 收集 → 待处理 → 补思考 → 查找 → 复制
2. 收集 → 待处理 → 入专题 → 专题页查看
3. 查找 → 详情 → 编辑 → 返回查找
4. 跳过 → 时间窗口后再出现

## 8.2 性能优化
- 查询优化
- 索引验证
- 分页性能
- 前端加载优化

## 8.3 用户体验优化
- 加载状态
- 错误提示
- 空状态文案
- 操作反馈

## 8.4 部署测试
```bash
cd /vol3/1000/private/workProject/info-organization-product/code/docker
docker-compose up -d
```

**验收：**
- Docker 环境完整运行
- 数据持久化正确
- 日志正常

---

# 9. 开发规范

## 9.1 代码规范
- 后端：阿里巴巴 Java 开发手册
- 前端：Vue 3 官方风格指南
- 命名：驼峰命名法
- 注释：关键逻辑必须注释

## 9.2 Git 规范
- 分支：feature/xxx, bugfix/xxx
- 提交：feat/fix/docs/style/refactor/test
- 示例：`feat: 实现收集页后端接口`

## 9.3 测试规范
- 单元测试覆盖率 > 60%
- 关键业务逻辑必须测试
- 接口测试用 Postman Collection

---

# 10. 风险与应对

## 10.1 技术风险
**风险：PostgreSQL 中文全文搜索效果不理想**
- 应对：先用 LIKE，后续升级 Elasticsearch

**风险：MyBatis-Plus 学习曲线**
- 应对：参考官方文档，先实现基本功能

## 10.2 进度风险
**风险：单人开发，时间压力大**
- 应对：严格按优先级，先做核心功能

**风险：求职期可能中断**
- 应对：每个阶段完成后提交代码，保持可恢复

---

# 11. 下一步行动

## 立即可做
1. 在本地安装开发环境
2. 克隆/初始化代码仓库
3. 执行阶段 0：环境搭建

## 第一周目标
- 完成环境搭建
- 完成收集页
- 完成待处理页

## 第二周目标
- 完成查找页
- 完成专题页
- 完成详情页

## 第三周目标
- 联调优化
- 部署测试
- 文档完善

---

**实施计划已就绪，可以开始编码。** 💻
