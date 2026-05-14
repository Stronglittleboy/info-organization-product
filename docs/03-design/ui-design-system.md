# UI 设计系统 - 卡片式设计语言

## 设计原则

### 1. 简洁优雅
- 减少视觉噪音
- 突出核心内容
- 留白充足

### 2. 卡片式布局
- 每个信息单元独立成卡片
- 卡片之间有明确边界
- 卡片内部层次清晰

### 3. 渐进式展示
- 先显示核心信息
- 次要信息折叠或隐藏
- 按需展开

### 4. 即时反馈
- 操作有明确反馈
- 加载状态可见
- 错误提示友好

---

## 颜色系统

### 主色调
```
--primary: #3B82F6      // 蓝色 - 主操作按钮
--primary-hover: #2563EB
--primary-light: #DBEAFE
```

### 中性色
```
--gray-50: #F9FAFB      // 背景色
--gray-100: #F3F4F6     // 卡片背景
--gray-200: #E5E7EB     // 边框
--gray-400: #9CA3AF     // 次要文字
--gray-600: #4B5563     // 正文
--gray-900: #111827     // 标题
```

### 语义色
```
--success: #10B981      // 成功
--warning: #F59E0B      // 警告
--error: #EF4444        // 错误
--info: #3B82F6         // 信息
```

---

## 字体系统

### 字体家族
```css
font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 
             'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 
             sans-serif;
```

### 字号
```
--text-xs: 12px         // 辅助信息
--text-sm: 14px         // 次要文字
--text-base: 16px       // 正文
--text-lg: 18px         // 小标题
--text-xl: 20px         // 标题
--text-2xl: 24px        // 大标题
```

### 字重
```
--font-normal: 400      // 正文
--font-medium: 500      // 强调
--font-semibold: 600    // 标题
```

---

## 间距系统

```
--space-1: 4px
--space-2: 8px
--space-3: 12px
--space-4: 16px
--space-5: 20px
--space-6: 24px
--space-8: 32px
--space-10: 40px
--space-12: 48px
```

---

## 圆角系统

```
--radius-sm: 4px        // 小元素（标签）
--radius-md: 8px        // 中等元素（按钮、输入框）
--radius-lg: 12px       // 大元素（卡片）
--radius-xl: 16px       // 超大元素（模态框）
```

---

## 阴影系统

```css
/* 轻微阴影 - 悬浮状态 */
--shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);

/* 标准阴影 - 卡片 */
--shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1),
             0 2px 4px -1px rgba(0, 0, 0, 0.06);

/* 强阴影 - 模态框 */
--shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1),
             0 4px 6px -2px rgba(0, 0, 0, 0.05);
```

---

## 组件规范

### 卡片 (Card)

**基础卡片**
```css
.card {
  background: var(--gray-100);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  box-shadow: var(--shadow-sm);
  transition: all 0.2s;
}

.card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}
```

**卡片内容层次**
```
┌─────────────────────────┐
│ [图标] 标题              │  ← 头部（font-semibold, text-lg）
├─────────────────────────┤
│ 主要内容                 │  ← 正文（font-normal, text-base）
│                         │
├─────────────────────────┤
│ 次要信息 · 时间          │  ← 底部（text-sm, gray-400）
└─────────────────────────┘
```

### 按钮 (Button)

**主按钮**
```css
.btn-primary {
  background: var(--primary);
  color: white;
  padding: var(--space-3) var(--space-6);
  border-radius: var(--radius-md);
  font-weight: var(--font-medium);
  font-size: var(--text-base);
  transition: all 0.2s;
}

.btn-primary:hover {
  background: var(--primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}
```

**次要按钮**
```css
.btn-secondary {
  background: var(--gray-100);
  color: var(--gray-600);
  border: 1px solid var(--gray-200);
  /* 其他同主按钮 */
}
```

### 输入框 (Input)

```css
.input {
  width: 100%;
  padding: var(--space-3) var(--space-4);
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-md);
  font-size: var(--text-base);
  transition: all 0.2s;
}

.input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.input::placeholder {
  color: var(--gray-400);
}
```

### 文本域 (Textarea)

```css
.textarea {
  /* 继承 input 样式 */
  min-height: 120px;
  resize: vertical;
}
```

---

## 布局规范

### 页面容器

```css
.page-container {
  max-width: 800px;
  margin: 0 auto;
  padding: var(--space-6);
}
```

### 卡片列表

```css
.card-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
```

### 时间分组

```css
.time-group {
  margin-bottom: var(--space-6);
}

.time-group-title {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--gray-400);
  margin-bottom: var(--space-3);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
```

---

## 交互规范

### 悬浮效果
- 卡片：上移 2px + 阴影加深
- 按钮：上移 1px + 阴影加深
- 链接：颜色加深

### 点击反馈
- 按钮：按下时缩小 98%
- 卡片：按下时缩小 99%

### 加载状态
- 骨架屏（Skeleton）
- 加载动画（Spinner）
- 进度条（Progress）

### 过渡动画
```css
transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
```

---

## 响应式设计

### 断点
```
--mobile: 640px
--tablet: 768px
--desktop: 1024px
```

### 移动端适配
- 卡片内边距减小：16px → 12px
- 字号适当缩小
- 按钮高度增加（便于点击）
- 间距适当缩小

---

## 图标系统

### 推荐图标库
- **Heroicons** (推荐) - 简洁现代
- **Lucide** - 轻量级
- **Material Icons** - 丰富

### 图标尺寸
```
--icon-sm: 16px
--icon-md: 20px
--icon-lg: 24px
--icon-xl: 32px
```

### 图标颜色
- 默认：继承文字颜色
- 强调：var(--primary)
- 次要：var(--gray-400)

---

## 示例：收集页面卡片

```html
<div class="page-container">
  <h1 class="page-title">收集素材</h1>
  
  <div class="input-card">
    <textarea 
      class="textarea" 
      placeholder="粘贴文本、图片或链接..."
    ></textarea>
    
    <div class="upload-hint">
      或点击上传图片 📷
    </div>
  </div>
</div>
```

```css
.page-title {
  font-size: var(--text-2xl);
  font-weight: var(--font-semibold);
  color: var(--gray-900);
  margin-bottom: var(--space-6);
}

.input-card {
  background: white;
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  box-shadow: var(--shadow-md);
}

.upload-hint {
  margin-top: var(--space-4);
  text-align: center;
  color: var(--gray-400);
  font-size: var(--text-sm);
}
```

---

## 示例：列表页面卡片

```html
<div class="time-group">
  <div class="time-group-title">今天</div>
  
  <div class="card-list">
    <div class="entry-card">
      <div class="entry-icon">📷</div>
      <div class="entry-content">
        <div class="entry-title">这个架构图说明了 DDD 的分层思想</div>
        <div class="entry-meta">竞品分析 · 10:30</div>
      </div>
      <div class="entry-thumbnail">
        <img src="..." alt="">
      </div>
    </div>
  </div>
</div>
```

```css
.entry-card {
  display: flex;
  gap: var(--space-4);
  background: white;
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  box-shadow: var(--shadow-sm);
  transition: all 0.2s;
}

.entry-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.entry-icon {
  font-size: var(--text-2xl);
  flex-shrink: 0;
}

.entry-content {
  flex: 1;
  min-width: 0;
}

.entry-title {
  font-size: var(--text-base);
  font-weight: var(--font-medium);
  color: var(--gray-900);
  margin-bottom: var(--space-2);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.entry-meta {
  font-size: var(--text-sm);
  color: var(--gray-400);
}

.entry-thumbnail {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-md);
  overflow: hidden;
  flex-shrink: 0;
}

.entry-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
```
