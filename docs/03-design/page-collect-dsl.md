# 收集页 Page DSL（审查稿）

> 本文档用结构化 DSL 描述「收集素材」页的产品与交互，便于评审与实现对照。  
> 关联：`page-collect.md`（方案 B）、`api-design-complete.md`。

---

## 1. page

```yaml
page:
  id: collect
  route: /collect
  title: 收集素材
  layout: single_column
  max_width: 800px
```

---

## 2. states（页面状态机）

```yaml
states:
  - id: idle
    description: 未识别素材类型，仅展示统一输入区
  - id: editing
    substates: [text, url, image]
    description: 已识别类型，展示对应表单 + 折叠「更多选项」
  - id: saved
    description: 提交成功后的反馈区（继续记录 / 补思考 / 更多处理）
```

**转移：**

| 从 | 事件 | 到 |
|----|------|-----|
| idle | 粘贴图片 / 拖拽图片到 idle 区 / 选择文件 | editing.image |
| idle | 粘贴单行 `http(s)://...` 文本 | editing.url（并自动拉取元数据） |
| idle | 粘贴其它文本 / 点「填写详情」/ Ctrl+⌘+Enter 有草稿 | editing.text |
| editing.* | 点「← 重新输入」/ Esc | idle |
| editing.* | 保存成功 | saved |
| saved | 继续记录 | idle |

---

## 3. regions（区域 DSL）

### 3.1 idle（统一输入）

```yaml
region: idle
  visible_when: state == idle
  children:
    - id: smart_textarea
      component: textarea
      placeholder: "粘贴文本、图片或链接..."
      min_height: 200px
      behaviors:
        - paste: smart_detect_clipboard
        - keydown: { combo: [Ctrl|Meta, Enter], action: draft_to_text_if_possible }
    - id: drop_zone
      description: 与 idle 容器同层，整卡可拖入图片
      behaviors:
        - dragover: prevent_default_highlight
        - drop: image_file_if_first_is_image
    - id: file_pick_hint
      text: "或点击上传图片"
      trigger: camera_emoji_button
      action: native_file_dialog_accept_images
    - id: idle_secondary_actions
      buttons:
        - id: to_text_detail
          label: 填写详情（文本）
          enabled_when: smart_textarea.trim_non_empty
        - id: to_url
          label: 识别为链接
          enabled_when: single_line_http_url(smart_textarea)
```

### 3.2 editing.text

```yaml
region: editing_text
  visible_when: state == editing && content_type == text
  header: "📝 文本"
  fields:
    - id: raw_content
      label: 正文
      required: true
      control: textarea
    - id: text_insight
      label: 你的思考（可选）
      required: false
      control: textarea
```

### 3.3 editing.url

```yaml
region: editing_url
  visible_when: state == editing && content_type == url
  header: "🔗 链接"
  fields:
    - id: collect_url
      label: 网页地址
      required: true
      control: input_url
    - id: url_meta_preview
      description: 自动拉取后展示标题/描述/摘要；失败时告警条
      loading_copy: "正在获取网页信息..."
    - id: url_insight
      label: 你的思考（必填）
      required: true
      control: textarea
```

### 3.4 editing.image

```yaml
region: editing_image
  visible_when: state == editing && content_type == image
  header: "📷 图片"
  children:
    - id: image_stage
      description: 预览或占位 + 可拖入替换
      behaviors:
        - drop: replace_image_file
    - id: ocr_block
      loading_copy: "正在识别文字..."
      on_fail:
        alert: "文字识别失败，请用思考字段描述要点"
      field_readonly_text: 识别到的文字
    - id: image_insight
      label: "这张图说明了什么？（必填）"
      required: true
      control: textarea
    - id: re_ocr_button
      label: 重新识别文字
      enabled_when: image_file_present
```

### 3.5 more_options（折叠）

```yaml
region: more_options
  visible_when: state == editing
  collapse_title: "⚙️ 更多选项"
  default_collapsed: true
  fields_by_content_type:
    text:
      - id: source_type
        label: 来源类型
        control: select
        options: [书籍, 网页, 对话, 手工录入, 其他]
      - id: source_title
        label: 来源标题
        control: input
      - id: source_link
        label: 来源链接
        control: input_url
        visible_when: source_type in [网页, WEB]
        note: 仅当来源类型为「网页」时展示；切换离开网页时清空该字段
    url:
      - id: url_source_type
        label: 来源类型
        options: [网页, 文章]
    image:
      - id: image_source_type
        label: 来源类型
        options: [截图, 照片]
    all:
      - id: topic
        label: 专题
        control: select_allow_create
```

---

## 4. visibility_rules（显隐规则汇总）

```yaml
rules:
  - id: R1
    field: more_options.text.source_link
    when: content_type == text AND source_type IN ["网页", "WEB"]
  - id: R2
    field: idle.file_hint
    when: state == idle
  - id: R3
    field: editing.type_chip
    when: state == editing
```

---

## 5. smart_detect_clipboard（粘贴识别）

```yaml
smart_detect_clipboard:
  order:
    - if_clipboard_has_image_file:
        action: prevent_default; enter_editing_image(file)
    - else_if_text_is_single_line_http_url:
        action: prevent_default; enter_editing_url(url); auto_fetch_metadata
    - else_if_text_non_empty:
        action: enter_editing_text(text)
```

---

## 6. api_mapping（前端 → 后端）

```yaml
api_mapping:
  text:
    when: NOT (source_title OR (web_source AND source_link))
    endpoint: POST /entries/collect-text
    body: { rawContent, insight?, sourceType?, topicId? }
  text_legacy:
    when: source_title OR (web_source AND source_link)
    endpoint: POST /entries
    body: CreateEntryRequest
  url:
    endpoint: POST /entries/collect-url
    body: { url, insight, sourceType?, topicId? }
  url_preview_only:
    endpoint: POST /entries/extract-url
    body: { url }
  image:
    endpoint: POST /entries/upload-image
    multipart: [file, insight, sourceType?, topicId?]
  image_ocr_preview:
    endpoint: POST /entries/ocr
    multipart: [file]
```

---

## 7. non_functional（非功能）

```yaml
nginx:
  client_max_body_size: 20m  # 经反向代理上传图片
  proxy_read_timeout: 120s
http_client:
  upload_ocr_timeout_ms: 120000
```

---

## 8. acceptance_criteria（验收要点）

1. 在 **idle** 状态：点击 📷 / 键盘 Enter 触发文件选择，可选中图片并进入 **editing.image**。  
2. 将图片 **拖到 idle 整卡区域**（不仅 textarea）可上传。  
3. 粘贴图片、粘贴单行 https 链接行为符合第 5 节顺序。  
4. **来源链接** 仅在「来源类型 = 网页（或兼容值 WEB）」时出现；切换为非网页时清空链接。  
5. 保存后 **saved** 区与「最近收集」列表正常刷新。  

---

## 9. revision_log

| 版本 | 日期 | 说明 |
|------|------|------|
| 0.2 | 2026-05-15 | 补充 R1 网页来源链接显隐、idle 整区拖拽、上传基础设施 DSL |
| 0.1 | 2026-05-15 | 初稿，对齐方案 B |
