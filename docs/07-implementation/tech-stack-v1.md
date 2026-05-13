# 技术选型 V1

项目：info-organization-product  
基于：所有现有设计文档分析

---

# 1. 技术需求提取

## 1.1 核心功能需求
- 文本内容存储与检索（支持中文全文搜索）
- 用户认证与多租户隔离
- RESTful API
- 读写分离优化
- 分页查询
- 时间排序与筛选
- 轻量级状态管理

## 1.2 性能需求
- 首版预期用户量：< 1000
- 单用户素材量：< 10000 条
- 查询响应时间：< 500ms
- 全文搜索响应时间：< 1s

## 1.3 部署需求
- 部署环境：用户 NAS（Debian 12）
- 资源限制：CPU 4 核，内存 15GB
- 网络：内网访问为主
- 维护成本：低

## 1.4 开发约束
- 单人开发（用户本人）
- Java 开发经验 6 年
- 求职期项目，需要展示技术深度
- 需要快速迭代验证

---

# 2. 技术栈选型

## 2.1 后端技术栈

### 推荐方案：Spring Boot 3.x + PostgreSQL

#### 核心框架
- **Spring Boot 3.2+**
  - 理由：用户 6 年 Java 经验，熟悉 Spring 生态
  - 成熟稳定，社区活跃
  - 适合求职展示

#### Web 层
- **Spring Web MVC**
  - RESTful API 标准实现
  - 符合 API 草案设计

#### 数据访问层
- **Spring Data JPA + Hibernate**
  - 理由：领域模型已明确，ORM 可快速落地
  - 支持读写分离
  - 可选：MyBatis（如果需要更精细的 SQL 控制）

#### 认证授权
- **Spring Security + JWT**
  - 标准方案
  - 支持多租户隔离

#### 全文搜索
- **PostgreSQL 内置全文搜索**
  - 理由：首版数据量可控，不需要独立搜索引擎
  - 支持中文分词（zhparser 插件）
  - 降低部署复杂度

---

## 2.2 数据库选型

### 推荐方案：PostgreSQL 15+

#### 选择理由
1. **支持中文全文搜索**
   - 内置 tsvector/tsquery
   - 可安装 zhparser 中文分词插件
   - 满足查询模型需求

2. **JSON 支持**
   - sourceRef 等可选字段可用 JSONB 存储
   - 灵活性高

3. **成熟稳定**
   - NAS 环境友好
   - 资源占用可控

4. **索引能力强**
   - 支持 GIN 索引（全文搜索）
   - 支持复合索引
   - 满足查询模型索引需求

#### 备选方案
- **MySQL 8.0+**
  - 如果用户更熟悉 MySQL
  - 全文搜索能力稍弱
  - 中文分词需要额外配置

---

## 2.3 前端技术栈

### 推荐方案：Vue 3 + TypeScript

#### 核心框架
- **Vue 3.4+**
  - 理由：轻量、易上手
  - 适合单人开发
  - 组合式 API 符合现代开发习惯

#### UI 组件库
- **Element Plus** 或 **Naive UI**
  - 理由：开箱即用
  - 中文文档友好
  - 组件丰富

#### 状态管理
- **Pinia**
  - Vue 3 官方推荐
  - 轻量简洁

#### 路由
- **Vue Router 4**
  - 标准方案

#### HTTP 客户端
- **Axios**
  - 成熟稳定

#### 构建工具
- **Vite**
  - 快速开发体验
  - 适合单人项目

#### 备选方案
- **React 18 + TypeScript**
  - 如果用户更熟悉 React
  - 生态更丰富
  - 学习曲线稍陡

---

## 2.4 部署方案

### 推荐方案：Docker Compose

#### 容器化
- **Docker**
  - 后端：Spring Boot 打包成 Docker 镜像
  - 前端：Nginx 静态托管
  - 数据库：PostgreSQL 官方镜像

#### 编排
- **Docker Compose**
  - 理由：单机部署，Compose 足够
  - 配置简单
  - 适合 NAS 环境

#### 反向代理
- **Nginx**
  - 前端静态资源托管
  - API 反向代理
  - 可选：配置 HTTPS

#### 备选方案
- **直接部署**
  - 如果不想用 Docker
  - 前端：Nginx 直接托管 dist
  - 后端：systemd 管理 Spring Boot jar
  - 数据库：apt 安装 PostgreSQL

---

# 3. 技术栈总结

## 3.1 推荐技术栈

### 后端
- Spring Boot 3.2+
- Spring Data JPA + Hibernate
- Spring Security + JWT
- PostgreSQL 15+ (内置全文搜索 + zhparser)

### 前端
- Vue 3.4+
- TypeScript
- Element Plus / Naive UI
- Pinia
- Vite

### 部署
- Docker + Docker Compose
- Nginx
- PostgreSQL 官方镜像

---

## 3.2 技术栈优势

### 对用户的优势
1. **展示技术深度**
   - Spring Boot 3 + JPA 展示 Java 后端能力
   - Vue 3 + TS 展示前端现代化能力
   - Docker 展示 DevOps 能力

2. **快速开发**
   - 熟悉的技术栈
   - 成熟的脚手架
   - 丰富的文档

3. **低维护成本**
   - 单机部署
   - 资源占用可控
   - 不依赖外部服务

4. **可扩展**
   - 后续可升级到 Elasticsearch（如果全文搜索不够）
   - 后续可引入 Redis（如果需要缓存）
   - 后续可拆分微服务（如果需要）

---

# 4. 技术风险与应对

## 4.1 PostgreSQL 中文全文搜索性能
### 风险
数据量增长后，内置全文搜索可能不够快

### 应对
- 首版先用 PostgreSQL 内置
- 监控查询性能
- 如果不够，再升级到 Elasticsearch

## 4.2 单机部署扩展性
### 风险
用户量增长后，单机可能不够

### 应对
- 首版目标 < 1000 用户，单机足够
- 架构上已做读写分离，后续可横向扩展

## 4.3 前端状态管理复杂度
### 风险
页面状态可能变复杂

### 应对
- 首版页面少，Pinia 足够
- 后续可引入更复杂状态管理

---

# 5. 开发环境建议

## 5.1 后端开发环境
- JDK 17+
- Maven 3.9+
- IntelliJ IDEA
- PostgreSQL 15+ (本地或 Docker)

## 5.2 前端开发环境
- Node.js 18+
- pnpm / npm
- VS Code + Volar

## 5.3 工具链
- Git
- Docker Desktop (本地测试)
- Postman / Apifox (API 测试)

---

# 6. 下一步建议

基于技术选型，下一步应：

1. **初始化项目骨架**
   - Spring Boot 项目初始化
   - Vue 3 项目初始化
   - Docker Compose 配置

2. **数据库表结构设计**
   - 基于领域模型设计表结构
   - 定义索引
   - 编写迁移脚本

3. **API 实现优先级**
   - 先实现收集页接口
   - 再实现待处理页接口
   - 最后实现查找页接口

---

# 7. 当前结论

技术选型 V1 已完成，推荐技术栈：
- 后端：Spring Boot 3 + PostgreSQL 15
- 前端：Vue 3 + TypeScript
- 部署：Docker Compose

可进入下一阶段：项目初始化与数据库设计。
