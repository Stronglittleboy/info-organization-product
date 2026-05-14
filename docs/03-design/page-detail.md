# 详情页面设计 - 图片/URL/文本分类展示

## 设计理念

**核心原则：** 根据素材类型展示不同内容，突出用户思考，提供操作入口。

---

## 图片详情页

```
┌─────────────────────────────────────┐
│ ← 返回              [编辑] [删除]    │
├─────────────────────────────────────┤
│                                     │
│  [图片全屏显示]                      │
│  （点击放大）                        │
│                                     │
├─────────────────────────────────────┤
│                                     │
│  💭 我的思考                         │
│                                     │
│  这是竞品的登录页面，采用了           │
│  简洁的设计风格，值得借鉴...          │
│                                     │
├─────────────────────────────────────┤
│                                     │
│  📄 识别到的文字                     │
│                                     │
│  用户登录                            │
│  密码输入                            │
│  提交按钮                            │
│                                     │
├─────────────────────────────────────┤
│                                     │
│  📌 竞品分析                         │
│  🏷️ 产品设计                         │
│  🕐 2026-05-14 10:30                │
│                                     │
│  ┌─────────────────────────────┐    │
│  │    复制图片链接              │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌─────────────────────────────┐    │
│  │    下载原图                  │    │
│  └─────────────────────────────┘    │
│                                     │
└─────────────────────────────────────┘
```

---

## URL 详情页

```
┌─────────────────────────────────────┐
│ ← 返回              [编辑] [删除]    │
├─────────────────────────────────────┤
│                                     │
│  🔗 DDD 领域驱动设计实践             │
│                                     │
│  本文介绍了 DDD 的核心概念...        │
│                                     │
├─────────────────────────────────────┤
│                                     │
│  💭 我的思考                         │
│                                     │
│  文章提到聚合根的设计原则很有启发，    │
│  特别是关于边界划分的部分...          │
│                                     │
├─────────────────────────────────────┤
│                                     │
│  📄 网页正文（前 500 字）            │
│                                     │
│  领域驱动设计是一种软件开发方法...    │
│                                     │
├─────────────────────────────────────┤
│                                     │
│  📌 网页                             │
│  🏷️ DDD 学习                         │
│  🕐 2026-05-14 15:20                │
│                                     │
│  ┌─────────────────────────────┐    │
│  │    访问原网页                │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌─────────────────────────────┐    │
│  │    复制链接                  │    │
│  └─────────────────────────────┘    │
│                                     │
└─────────────────────────────────────┘
```

---

## 文本详情页

```
┌─────────────────────────────────────┐
│ ← 返回              [编辑] [删除]    │
├─────────────────────────────────────┤
│                                     │
│  📝 原始内容                         │
│                                     │
│  聚合根负责维护业务不变性，          │
│  它是领域模型的核心...                │
│                                     │
├─────────────────────────────────────┤
│                                     │
│  💭 我的思考                         │
│                                     │
│  这是 DDD 的核心概念，需要深入理解    │
│                                     │
├─────────────────────────────────────┤
│                                     │
│  📌 书籍 · 领域驱动设计              │
│  🏷️ DDD 学习                         │
│  🕐 2026-05-12 09:15                │
│                                     │
│  ┌─────────────────────────────┐    │
│  │    复制内容                  │    │
│  └─────────────────────────────┘    │
│                                     │
└─────────────────────────────────────┘
```

---

## 组件设计

### EntryDetail.vue

```vue
<template>
  <div class="detail-container">
    <!-- 顶部操作栏 -->
    <div class="detail-header">
      <button class="btn-back" @click="goBack">← 返回</button>
      <div class="header-actions">
        <button class="btn-secondary" @click="handleEdit">编辑</button>
        <button class="btn-danger" @click="handleDelete">删除</button>
      </div>
    </div>
    
    <!-- 图片类型 -->
    <ImageDetail v-if="entry.contentType === 'image'" :entry="entry" />
    
    <!-- URL 类型 -->
    <UrlDetail v-if="entry.contentType === 'url'" :entry="entry" />
    
    <!-- 文本类型 -->
    <TextDetail v-if="entry.contentType === 'text'" :entry="entry" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getEntryById, deleteEntry } from '@/api/entries';

const route = useRoute();
const router = useRouter();
const entry = ref(null);

onMounted(async () => {
  const response = await getEntryById(route.params.id);
  entry.value = response.data;
});

const goBack = () => {
  router.back();
};

const handleEdit = () => {
  router.push(`/entries/${entry.value.id}/edit`);
};

const handleDelete = async () => {
  if (confirm('确定要删除这条记录吗？')) {
    await deleteEntry(entry.value.id);
    router.push('/');
  }
};
</script>
```

### ImageDetail.vue

```vue
<template>
  <div class="detail-content">
    <!-- 图片展示 -->
    <div class="image-section">
      <img 
        :src="entry.imageUrl" 
        :alt="entry.insightText"
        class="detail-image"
        @click="showFullscreen"
      >
    </div>
    
    <!-- 我的思考 -->
    <div class="section">
      <div class="section-title">💭 我的思考</div>
      <div class="section-content">{{ entry.insightText }}</div>
    </div>
    
    <!-- OCR 文字 -->
    <div v-if="entry.imageOcrText" class="section">
      <div class="section-title">📄 识别到的文字</div>
      <div class="section-content ocr-text">{{ entry.imageOcrText }}</div>
    </div>
    
    <!-- 元信息 -->
    <div class="meta-section">
      <div class="meta-item">📌 {{ entry.sourceTitle || '未分类' }}</div>
      <div class="meta-item">🏷️ {{ entry.topicName || '无专题' }}</div>
      <div class="meta-item">🕐 {{ formatDateTime(entry.capturedAt) }}</div>
    </div>
    
    <!-- 操作按钮 -->
    <div class="action-buttons">
      <button class="btn-action" @click="copyImageUrl">复制图片链接</button>
      <button class="btn-action" @click="downloadImage">下载原图</button>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  entry: Object
});

const copyImageUrl = () => {
  navigator.clipboard.writeText(props.entry.imageUrl);
  // 显示提示
};

const downloadImage = () => {
  window.open(props.entry.imageUrl + '?download=true');
};

const showFullscreen = () => {
  // 显示全屏图片查看器
};
</script>
```

---

## 样式设计

```css
.detail-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

/* 顶部操作栏 */
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.btn-back {
  padding: 8px 16px;
  background: transparent;
  border: none;
  color: var(--gray-600);
  font-size: 16px;
  cursor: pointer;
  transition: color 0.2s;
}

.btn-back:hover {
  color: var(--gray-900);
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* 内容区域 */
.detail-content {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

/* 图片展示 */
.image-section {
  margin-bottom: 24px;
}

.detail-image {
  width: 100%;
  border-radius: 8px;
  cursor: zoom-in;
}

/* 内容区块 */
.section {
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--gray-200);
}

.section:last-of-type {
  border-bottom: none;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--gray-600);
  margin-bottom: 12px;
}

.section-content {
  font-size: 16px;
  color: var(--gray-900);
  line-height: 1.6;
  white-space: pre-wrap;
}

.ocr-text {
  background: var(--gray-50);
  padding: 12px;
  border-radius: 8px;
  font-family: monospace;
}

/* 元信息 */
.meta-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 24px;
}

.meta-item {
  font-size: 14px;
  color: var(--gray-600);
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.btn-action {
  width: 100%;
  padding: 12px;
  background: var(--gray-100);
  border: 1px solid var(--gray-200);
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-action:hover {
  background: var(--gray-200);
}
```

---

## 交互细节

### 1. 图片放大
- 点击图片显示全屏查看器
- 支持缩放、拖拽
- ESC 关闭

### 2. 复制功能
- 复制后显示提示："已复制到剪贴板"
- 3 秒后自动消失

### 3. 删除确认
- 显示确认对话框
- 确认后删除并返回列表页

### 4. 编辑跳转
- 跳转到编辑页面
- 预填充现有数据

---

## 响应式设计

### 移动端适配

```css
@media (max-width: 640px) {
  .detail-container {
    padding: 16px;
  }
  
  .detail-content {
    padding: 16px;
  }
  
  .header-actions {
    flex-direction: column;
    gap: 8px;
  }
}
```
