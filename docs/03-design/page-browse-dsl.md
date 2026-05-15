# 时间浏览页 Page DSL（审查稿）

> 结构化描述「按时间回溯」浏览页，便于评审与实现对照。  
> 关联：`page-list.md`（视觉分组规则）、`api-design-complete.md`。  
> **任务跟踪：** `docs/07-implementation/task-browse-timeline.md`

---

## 1. page

```yaml
page:
  id: browse_timeline
  route: /browse
  title: 浏览
  nav: primary_left
  layout: single_column
  max_width: 800px
  primary_goal: 在不依赖关键词的前提下，按记录时间（captured_at）回看素材
```

---

## 2. states

```yaml
states:
  - id: initial
    description: 进入页或筛选变更后，重新拉取第一页
  - id: loaded
    description: 已展示列表，可「加载更多」追加
  - id: empty
    description: 当前筛选下无条目
  - id: error
    description: 请求失败，可重试
```

---

## 3. regions

### 3.1 filter_bar（时间优先）

```yaml
region: filter_bar
  order: [time_presets, date_range, secondary_filters]
  children:
    - id: time_presets
      component: button_group
      options:
        - { id: all, label: 全部 }
        - { id: d7, label: 近7天 }
        - { id: d30, label: 近30天 }
      action: set_date_range_and_reload
    - id: date_range
      component: date_range_picker
      value_format: YYYY-MM-DD
      end_of_day_suffix: T23:59:59
      optional: true
      action: reload_from_first_page
    - id: topic_filter
      component: select
      placeholder: 按专题
      clearable: true
    - id: content_type_filter
      component: select
      options: [全部, text, url, image]
      clearable: true
    - id: insight_filter
      component: select
      placeholder: 有无思考
      options: [有思考, 无思考]
      clearable: true
```

### 3.2 hint

```yaml
region: search_cross_link
  copy: 想按关键词找回？去「查找」
  action: navigate /search
```

### 3.3 list（时间分组）

```yaml
region: grouped_list
  grouping_rule:
    - { label: 今天, rule: captured_at.date == today }
    - { label: 昨天, rule: captured_at.date == yesterday }
    - { label: 本周, rule: captured_at in (week_start..yesterday) }
    - { label: 更早, rule: captured_at < week_start OR outside preset window }
  note: 与 page-list.md 一致；分组标题由前端根据 captured_at 计算
  item:
    fields: [type_icon, title_line, snippet, captured_time, topic_tag]
    action_primary: navigate /entry/:entryId
  pagination:
    style: load_more
    api: entries.browse
    cursor_field: nextCursor
    order_by: captured_at DESC
```

---

## 4. API DSL（契约）

```yaml
api:
  id: entries.browse
  method: GET
  path: /entries/browse
  description: 按时间与可选筛选分页列出素材，不依赖搜索关键词
  query:
    startDate: { type: string, optional: true, example: "2026-05-01" }
    endDate: { type: string, optional: true, example: "2026-05-15T23:59:59" }
    topicId: { type: uuid_string, optional: true }
    contentType: { type: string, optional: true, enum: [text, url, image] }
    hasInsight: { type: boolean, optional: true }
    cursor: { type: string, optional: true, description: 上一页最后一条的 captured_at ISO }
    limit: { type: int, default: 30, max: 100 }
  response:
    shape: PageResponse
    fields:
      items: EntryResponse[]
      total: long
      hasMore: boolean
      nextCursor: string?
```

---

## 5. 与「查找」的分工

| 能力 | 查找 `/search` | 浏览 `/browse` |
|------|----------------|----------------|
| 关键词 | 必填 | 不使用（可选后续：范围内关键词 v2） |
| 时间 | 辅助筛选 | 主能力 + 快捷预设 |
| 典型任务 | 「记得写过某词」 | 「上周我记了什么」 |

---

## 6. 版本

| 版本 | 日期 | 说明 |
|------|------|------|
| 0.1 | 2026-05-15 | 初稿：页结构、API DSL、与查找分工 |
| 0.2 | 2026-05-15 | MVP 已实现：`GET /entries/browse`、前端 `/browse`、查找页入口 |
