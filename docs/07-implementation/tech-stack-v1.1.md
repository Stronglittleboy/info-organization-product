# 技术选型 V1.1（定稿版）

项目：info-organization-product  
基于：V1 评审结果

---

# 1. 最终确定技术栈

## 1.1 后端技术栈

### 核心框架
- **Spring Boot 3.2+**

### Web 层
- **Spring Web MVC**

### 数据访问层
- **MyBatis-Plus 3.5+**
  - 理由：比 JPA 更轻量
  - SQL 控制更精细
  - 适合读写分离场景
  - 代码生成器提升开发效率

### 认证授权
- **Spring Security + JWT**
  - Access Token 过期时间：2 小时
  - Refresh Token 过期时间：7 天

### 日志
- **Logback**（Spring Boot 默认）
  - 日志级别：INFO
  - 滚动策略：每天一个文件，保留 30 天
  - 日志路径：`/app/logs`（容器内）

### 全文搜索
- **PostgreSQL 内置全文搜索 + zhparser**

---

## 1.2 数据库

### 主数据库
- **PostgreSQL 15+**

### 连接池
- **HikariCP**（Spring Boot 默认）
  - 最大连接数：10（NAS 环境保守配置）
  - 最小空闲连接数：2
  - 连接超时：30 秒

### 数据持久化
- **Docker Volume 挂载**
  - PostgreSQL 数据目录：`./data/postgres:/var/lib/postgresql/data`
  - 应用日志目录：`./data/logs:/app/logs`

### 备份策略
- **pg_dump 定时任务**
  - 每天凌晨 2 点备份
  - 保留最近 7 天备份
  - 备份路径：`./data/backups`

---

## 1.3 前端技术栈

### 核心框架
- **Vue 3.4+**
- **TypeScript**

### UI 组件库
- **Element Plus**
  - 理由：生态成熟
  - 中文文档完善
  - 组件丰富

### 状态管理
- **Pinia**

### 路由
- **Vue Router 4**

### HTTP 客户端
- **Axios**

### 构建工具
- **Vite**

### CSS 方案
- **Element Plus 内置样式 + 少量自定义 CSS**

---

## 1.4 部署方案

### 容器化
- **Docker**
  - 后端：Spring Boot 打包成 Docker 镜像（基于 openjdk:17-slim）
  - 前端：Nginx 静态托管（基于 nginx:alpine）
  - 数据库：PostgreSQL 官方镜像（postgres:15-alpine）

### 编排
- **Docker Compose**

### 反向代理
- **Nginx**
  - 前端静态资源托管
  - API 反向代理到后端容器
  - 端口：80（HTTP）

---

# 2. 技术栈总结

## 2.1 最终技术栈

### 后端
- Spring Boot 3.2+
- MyBatis-Plus 3.5+
- Spring Security + JWT
- Logback
- PostgreSQL 15+ (zhparser)

### 前端
- Vue 3.4+
- TypeScript
- Element Plus
- Pinia
- Vite

### 部署
- Docker + Docker Compose
- Nginx
- PostgreSQL 官方镜像
- Docker Volume 数据持久化

---

## 2.2 开发环境

### 后端
- JDK 17+
- Maven 3.9+
- IntelliJ IDEA
- PostgreSQL 15+ (Docker)

### 前端
- Node.js 18+
- pnpm
- VS Code + Volar

### 工具链
- Git
- Docker Desktop
- Apifox (API 测试)

---

# 3. 项目结构建议

## 3.1 后端项目结构
```
info-organization-backend/
├── src/main/java/com/example/info/
│   ├── controller/      # API 控制器
│   ├── service/         # 业务逻辑
│   ├── mapper/          # MyBatis Mapper
│   ├── entity/          # 实体类
│   ├── dto/             # 数据传输对象
│   ├── vo/              # 视图对象（读模型）
│   ├── config/          # 配置类
│   └── security/        # 安全相关
├── src/main/resources/
│   ├── mapper/          # MyBatis XML
│   ├── application.yml
│   └── logback-spring.xml
└── pom.xml
```

## 3.2 前端项目结构
```
info-organization-frontend/
├── src/
│   ├── views/           # 页面组件
│   ├── components/      # 通用组件
│   ├── api/             # API 调用
│   ├── stores/          # Pinia 状态
│   ├── router/          # 路由配置
│   ├── types/           # TypeScript 类型
│   └── main.ts
├── package.json
└── vite.config.ts
```

## 3.3 部署目录结构
```
info-organization-product/
├── docker-compose.yml
├── backend/
│   └── Dockerfile
├── frontend/
│   ├── Dockerfile
│   └── nginx.conf
└── data/
    ├── postgres/        # PostgreSQL 数据
    ├── logs/            # 应用日志
    └── backups/         # 数据库备份
```

---

# 4. 下一步

技术选型 V1.1 已定稿，下一步应：

1. **数据库表结构设计**
   - 基于领域模型
   - 定义索引
   - 编写迁移脚本

2. **项目初始化**
   - Spring Boot 项目初始化
   - Vue 3 项目初始化
   - docker-compose.yml 编写

3. **API 实现**
   - 按优先级实现接口
   - 收集页 → 待处理页 → 查找页

---

# 5. 当前结论

技术选型 V1.1 已定稿，所有细节已明确。  
可进入数据库表结构设计与项目初始化阶段。
