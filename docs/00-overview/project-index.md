# Info Organization Product - 项目索引

## 项目概述

**一句话定义：** 帮助用户收集碎片信息（文本/图片/链接），附着自己的思考，并在未来需要时快速找回和取用。

**核心价值：** 不是文件管理器，不是网盘，而是"信息 + 思考"的整理工具。

---

## 当前状态

**阶段：** 多类型素材支持功能设计与实施中

**版本：** v0.2.0-dev

**最后更新：** 2026-05-15

---

## 技术栈

- **后端：** Spring Boot 3.2.5 + Java 17 + MyBatis Plus
- **前端：** Vue 3 + Vite
- **数据库：** PostgreSQL 15 + pg_jieba (中文分词) + pgvector (向量搜索)
- **存储：** MinIO (OSS) / 本地文件系统
- **部署：** Docker Compose

---

## 服务信息

| 服务 | 容器名 | 端口 | 访问地址 |
|------|--------|------|---------|
| PostgreSQL | info-org-postgres | 55432 | localhost:55432 |
| Backend | info-org-backend | 8080 | http://192.168.31.173:8080/api |
| Frontend | info-org-frontend | 8081 | http://192.168.31.173:8081 |
| MinIO API | dev-minio | 9000 | http://192.168.31.173:9000 |
| MinIO Console | dev-minio | 9001 | http://192.168.31.173:9001 |

---

## 文档导航

### 00-overview（项目概览）
- `project-index.md` - 本文件
- `project-status.md` - 当前进度和待办事项

### 01-research（研究材料）
- `methodology.md` - 信息整理方法论
- `competitive-analysis.md` - 竞品分析

### 02-product（产品定义）
- `product-definition.md` - 产品定义和范围
- `user-scenarios.md` - 用户场景
- `constraints.md` - 产品约束

### 03-design（设计文档）
- `ui-design-system.md` - UI 设计系统（卡片式设计语言）
- `page-collect.md` - 收集页面设计
- `page-list.md` - 列表页面设计
- `page-detail.md` - 详情页面设计

### 04-domain（领域模型）
- `domain-model.md` - 领域模型
- `database-schema.md` - 数据库设计
- `field-semantics.md` - 字段语义定义

### 05-api（接口设计）
- `api-design.md` - API 设计
- `storage-strategy.md` - 存储策略

### 06-reviews（评审记录）
- `2026-05-15-multi-type-material-review.md` - 多类型素材支持评审

---

## 快速开始

### 启动服务
```bash
cd /vol3/1000/private/workProject/info-organization-product/code/docker
docker compose up -d
```

### 访问前端
http://192.168.31.173:8081/collect

### 访问 MinIO Console
http://192.168.31.173:9001
账号：minioadmin / minioadmin123
