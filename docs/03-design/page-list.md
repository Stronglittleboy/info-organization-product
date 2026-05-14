# 列表页面设计 - 卡片式布局 + 时间分组

## 设计理念

**核心原则：** 卡片式布局，按时间分组，视觉清晰，快速浏览。

---

## 页面结构

```
┌─────────────────────────────────────┐
│  🔍 [搜索素材...]                    │
├─────────────────────────────────────┤
│                                     │
│  今天                                │
│                                     │
│  ┌─────────────────────────────┐    │
│  │ 📷                           │    │
│  │ [缩略图]                     │    │
│  │                             │    │
│  │ 这个架构图说明了             │    │
│  │ DDD 的分层思想               │    │
│  │                             │    │
│  │ 竞品分析 · 10:30            │    │
│  └─────────────────────────────┘    │
│                                     │
│  昨天                                │
│                                     │
│  ┌─────────────────────────────┐    │
│  │ 🔗 DDD 领域驱动设计实践       │    │
│  │                             │    │
│  │ 文章提到聚合根的设计原则      │    │
│  │                             │    │
│  │ DDD 学习 · 昨天 15:20       │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌─────────────────────────────┐    │
│  │ 📝 聚合根负责维护业务...      │    │
│  │                             │    │
│  │ 读书笔记 · 昨天 09:15       │    │
│  └─────────────────────────────┘    │
│                                     │
└─────────────────────────────────────┘
```

---

## 时间分组规则

| 分组 | 规则 |
|------|------|
| 今天 | captured_at 是今天 |
| 昨天 | captured_at 是昨天 |
| 本周 | captured_at 在最近 7 天内（不含今天和昨天） |
| 更早 | captured_at 超过 7 天 |

---

## 卡片设计

### 文本卡片

```
┌─────────────────────────────────┐
│ 📝 聚合根负责维护业务不变性...    │
│                                 │
│ 读书笔记 · 昨天 09:15           │
└─────────────────────────────────┘
```

### 图片卡片

```
┌─────────────────────────────────┐
│ 📷                               │
│ [缩略图 80x80]                   │
│                                 │
│ 这个架构图说明了 DDD 的分层思想  │
│                                 │
│ 竞品分析 · 10:30                │
└─────────────────────────────────┘
```

### URL 卡片

```
┌─────────────────────────────────┐
│ 🔗 DDD 领域驱动设计实践           │
│                                 │
│ 文章提到聚合根的设计原则很有启发  │
│                                 │
│ DDD 学习 · 昨天 15:20           │
└─────────────────────────────────┘
```

---

## 组件设计

### EntryList.vue

```vue
<template>
  <div class="list-container">
    <!-- 搜索框 -->
    <div class="search-bar">
      <input 
        type="text" 
        class="search-input" 
        placeholder="🔍 搜索素材..."
        v-model="searchQuery"
        @input="handleSearch"
      >
    </div>
    
    <!-- 时间分组列表 -->
    <div v-for="(group, groupName) in groupedEntries" :key="groupName" class="time-group">
      <div class="time-group-title">{{ groupName }}</div>
      
      <div class="card-list">
        <EntryCard 
          v-for="entry in group" 
          :key="entry.id" 
          :entry="entry"
          @click="goToDetail(entry.id)"
        />
      </div>
    </div>
    
    <!-- 空状态 -->
    <div v-if="isEmpty" class="empty-state">
      <div class="empty-icon">📭</div>
      <div class="empty-text">还没有收集任何素材</div>
      <button class="btn-primary" @click="goToCollect">开始收集</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getEntries } from '@/api/entries';

const router = useRouter();
const entries = ref([]);
const searchQuery = ref('');

const groupedEntries = computed(() => {
  const groups = {
    '今天': [],
    '昨天': [],
    '本周': [],
    '更早': []
  };
  
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000);
  const weekAgo = new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000);
  
  entries.value.forEach(entry => {
    const capturedAt = new Date(entry.capturedAt);
    
    if (capturedAt >= today) {
      groups['今天'].push(entry);
    } else if (capturedAt >= yesterday) {
      groups['昨天'].push(entry);
    } else if (capturedAt >= weekAgo) {
      groups['本周'].push(entry);
    } else {
      groups['更早'].push(entry);
    }
  });
  
  // 移除空分组
  return Object.fromEntries(
    Object.entries(groups).filter(([_, items]) => items.length > 0)
  );
});

const isEmpty = computed(() => entries.value.length === 0);

onMounted(async () => {
  const response = await getEntries();
  entries.value = response.data.items;
});
</script>
```

### EntryCard.vue

```vue
<template>
  <div class="entry-card" @click="$emit('click')">
    <!-- 图标 -->
    <div class="entry-icon">{{ iconMap[entry.contentType] }}</div>
    
    <!-- 内容 -->
    <div class="entry-content">
      <!-- 标题/思考 -->
      <div class="entry-title">
        <template v-if="entry.contentType === 'url'">
          {{ entry.urlTitle }}
        </template>
        <template v-else>
          {{ entry.insightText || entry.rawContent }}
        </template>
      </div>
      
      <!-- 次要信息 -->
      <div class="entry-meta">
        {{ entry.sourceTitle }} · {{ formatTime(entry.capturedAt) }}
      </div>
    </div>
    
    <!-- 缩略图（仅图片类型） -->
    <div v-if="entry.contentType === 'image'" class="entry-thumbnail">
      <img :src="entry.thumbnailUrl" :alt="entry.insightText">
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  entry: Object
});

const iconMap = {
  text: '📝',
  image: '📷',
  url: '🔗'
};

const formatTime = (timestamp) => {
  const date = new Date(timestamp);
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  
  if (date >= today) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  } else {
    return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' });
  }
};
</script>
```

---

## 样式设计

```css
.list-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

/* 搜索框 */
.search-bar {
  margin-bottom: 24px;
}

.search-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--gray-200);
  border-radius: 8px;
  font-size: 16px;
  transition: all 0.2s;
}

.search-input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

/* 时间分组 */
.time-group {
  margin-bottom: 32px;
}

.time-group-title {
  font-size: 12px;
  font-weight: 500;
  color: var(--gray-400);
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* 卡片列表 */
.card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 卡片 */
.entry-card {
  display: flex;
  gap: 16px;
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.2s;
}

.entry-card:hover {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.entry-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.entry-content {
  flex: 1;
  min-width: 0;
}

.entry-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--gray-900);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.entry-meta {
  font-size: 14px;
  color: var(--gray-400);
}

.entry-thumbnail {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.entry-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 24px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  color: var(--gray-400);
  margin-bottom: 24px;
}
```

---

## 交互细节

### 1. 悬浮效果
- 卡片上移 2px
- 阴影加深

### 2. 点击反馈
- 卡片缩小 99%
- 跳转到详情页

### 3. 加载状态
- 骨架屏（Skeleton）
- 显示 3 个占位卡片

### 4. 无限滚动
- 滚动到底部自动加载更多
- 显示加载动画

---

## 响应式设计

### 移动端适配

```css
@media (max-width: 640px) {
  .list-container {
    padding: 16px;
  }
  
  .entry-card {
    padding: 12px;
  }
  
  .entry-thumbnail {
    width: 60px;
    height: 60px;
  }
}
```
