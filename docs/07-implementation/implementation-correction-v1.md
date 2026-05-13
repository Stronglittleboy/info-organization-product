# 项目实施路径修正说明

## 修正原因
原实施过程中存在两个错误前提：
1. 第三方服务未统一纳入 `/vol1/1000/docker_container/dev-env/docker-compose.yaml`
2. 项目代码仓未立即纳入 Git 管理

## 修正后规则
1. 所有第三方服务（如 PostgreSQL）统一由：
   `/vol1/1000/docker_container/dev-env/docker-compose.yaml`
   管理
2. 容器数据统一落在：
   `/vol1/1000/docker_container/dev-env/<service>/data`
3. 项目配置默认使用：
   公网IP + 端口 + 账号密码
4. 项目代码统一纳入 Git 仓库管理

## 当前已修正项
- PostgreSQL 已迁移到 dev-env compose 管理
- PostgreSQL 数据目录已迁移到 `/vol1/1000/docker_container/dev-env/info-org-postgres/data`
- 后端 `application.yml` 已改为公网IP连接模板
- 新增 `.env.example`
- 项目代码仓已初始化 Git
