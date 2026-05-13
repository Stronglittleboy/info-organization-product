# 收集页真实落库与联调记录 V1

## 本次完成
- Entry 改为真实 PostgreSQL 落库
- 新增 `GET /entries/recent`
- 前端接入 recent 列表
- 完成前后端接口联调

## 后端实现
- `Entry` 实体已按数据库真实结构对齐
- `EntryMapper` 改为显式 SQL：
  - UUID 字段通过 `CAST(? AS uuid)` 写入
  - `deleted` 按 `smallint` 处理，0=未删除
- `EntryServiceImpl` 已从内存实现切换为真实 DB 实现
- `EntryController` 已支持：
  - `POST /entries`
  - `GET /entries/recent`

## 前端实现
- `CollectPage.vue` 新增“最近收集”区域
- 保存收集后自动刷新 recent 列表
- `src/api/entry.ts` 新增 `getRecentEntries()`

## 实测验证
### 自动化测试
- `EntryControllerTest`：通过

### 运行态联调
后端启动后验证：
- `GET http://127.0.0.1:8080/api/entries/recent`：成功
- `POST http://127.0.0.1:8080/api/entries`：成功写入
- 再次 `GET recent`：能看到新增记录

## 关键排障记录
1. MyBatis Mapper 扫描缺失
   - 已通过 `@MapperScan` 修复
2. PostgreSQL UUID 字段不接受 varchar 直写
   - 改为 SQL 显式 `CAST(... AS uuid)`
3. `deleted` 字段实际是 `smallint`
   - 改为整数 0/1 方案

## 当前状态
收集页已经形成真实可运行闭环：
- 前端页面可提交
- 后端接口可写库
- recent 列表可读取真实数据

## 下一步建议
- 增加 topic 归类能力
- 增加 insight_text 生成/编辑能力
- 增加分页与筛选
- 补前端 E2E 联调测试
