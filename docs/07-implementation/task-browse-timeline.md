# 任务清单：时间浏览（/browse + GET /entries/browse）

> 来源：`docs/03-design/page-browse-dsl.md`

## Phase 1 — 闭环 MVP（当前迭代）

- [x] **T1** 编写 Page + API DSL（`docs/03-design/page-browse-dsl.md`）
- [x] **T2** 后端：`EntryMapper` 增加 `browseEntries` / `countBrowseEntries`（无关键词，可选时间/专题/类型/思考）
- [x] **T3** 后端：`EntryService` + `EntryController` 暴露 `GET /entries/browse`（注册在 `/entries/{entryId}` 之前）
- [x] **T4** 前端：`browseEntries` API、`BrowsePage.vue`、路由 `/browse`、顶栏「浏览」入口
- [x] **T5** 前端：时间快捷（全部 / 7 天 / 30 天）+ 日期范围 + 筛选 + 分组列表 + 加载更多
- [ ] **T6** 联调：Docker / 本地起后端，手测筛选与分页
- [x] **T7**（可选）`tests/e2e` 增加 `/browse` 冒烟用例

## Phase 2 — 体验增强

- [x] **T8** 查找页增加文案入口：「按时间浏览 → /browse」
- [ ] **T9** 浏览页可选「范围内关键词」：DSL 中 v2；后端增加可选 `q` + SQL OR 条件
- [ ] **T10** 与 `GET /entries/grouped` 首屏合并：首屏用分组快照 + 列表拉更早（需产品确认）

## Phase 3 — 文档与对外

- [ ] **T11** 更新 `docs/05-api/api-design-complete.md` 中 browse 契约摘录
- [ ] **T12** 更新 `docs/02-product/product-definition.md` 高级搜索/时间能力状态

---

**完成定义（MVP）**：用户可在不输入关键词的情况下，在 `/browse` 按预设或自定义日期范围查看素材，按「今天/昨天/本周/更早」分组展示，并可通过「加载更多」沿时间轴向后翻页。
