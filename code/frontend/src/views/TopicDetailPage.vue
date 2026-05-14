<template>
  <div class="topic-detail-page">
    <div class="page-header">
      <router-link to="/topics" class="back-link">← 专题</router-link>
    </div>

    <div v-if="loading" class="skeleton-area">
      <el-skeleton :rows="1" animated style="margin-bottom: 16px" />
      <el-skeleton :rows="0" animated style="width: 60%; margin-bottom: 20px" />
      <div v-for="i in 3" :key="i" class="entry-card">
        <el-skeleton :rows="3" animated />
      </div>
    </div>

    <template v-else-if="topic">
      <div class="topic-header">
        <h2 class="topic-name">{{ topic.name }}</h2>
        <p v-if="topic.description" class="topic-desc">{{ topic.description }}</p>
        <p class="topic-hint">这里汇集你围绕【{{ topic.name }}】持续积累的内容</p>
        <div class="topic-stat">{{ topic.entryCount }} 条素材</div>
      </div>

      <!-- 专题内搜索 -->
      <div class="topic-search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索本专题内容..."
          clearable
          @keyup.enter="handleTopicSearch"
          @clear="handleClearSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleTopicSearch" />
          </template>
        </el-input>
      </div>

      <el-empty v-if="entries.length === 0 && !searchKeyword" description="这个专题还没有素材" />
      <el-empty v-else-if="entries.length === 0 && searchKeyword" description="未找到匹配的内容" />

      <div v-else class="entry-list">
        <div v-for="entry in entries" :key="entry.entryId" class="entry-card">
          <div class="entry-content">{{ contentPreview(entry.rawContent) }}</div>
          <div v-if="entry.insightText" class="entry-insight">💡 {{ entry.insightText }}</div>
          <div class="entry-meta">
            <span>{{ entry.sourceTitle || '' }}</span>
            <span>{{ formatTime(entry.capturedAt) }}</span>
          </div>
          <div class="entry-actions">
            <el-button size="small" text type="primary" @click="$router.push(`/entry/${entry.entryId}`)">
              详情
            </el-button>
          </div>
        </div>

        <div v-if="hasMore" class="load-more">
          <el-button :loading="loadingMore" @click="loadMore">加载更多</el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { getTopicDetail, getTopicEntries, searchEntries } from '@/api/entry'
import { debounce } from '@/utils/debounce'
import type { TopicResponse, EntryResponse } from '@/types/entry'

const route = useRoute()
const topicId = route.params.topicId as string

const topic = ref<TopicResponse | null>(null)
const entries = ref<EntryResponse[]>([])
const loading = ref(true)
const loadingMore = ref(false)
const hasMore = ref(false)
const nextCursor = ref<string | undefined>(undefined)
const PAGE_SIZE = 20
const searchKeyword = ref('')
const isSearching = ref(false)

async function loadData() {
  loading.value = true
  try {
    const [t, e] = await Promise.all([
      getTopicDetail(topicId),
      getTopicEntries(topicId, undefined, PAGE_SIZE)
    ])
    topic.value = t
    entries.value = e.items
    hasMore.value = e.hasMore
    nextCursor.value = e.nextCursor
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  loadingMore.value = true
  try {
    let res
    if (isSearching.value && searchKeyword.value.trim()) {
      res = await searchEntries({ keyword: searchKeyword.value.trim(), topicId, cursor: nextCursor.value, limit: PAGE_SIZE })
    } else {
      res = await getTopicEntries(topicId, nextCursor.value, PAGE_SIZE)
    }
    entries.value.push(...res.items)
    hasMore.value = res.hasMore
    nextCursor.value = res.nextCursor
  } finally {
    loadingMore.value = false
  }
}

async function handleTopicSearch() {
  const kw = searchKeyword.value.trim()
  if (!kw) {
    handleClearSearch()
    return
  }
  isSearching.value = true
  loading.value = true
  try {
    const res = await searchEntries({ keyword: kw, topicId, limit: PAGE_SIZE })
    entries.value = res.items
    hasMore.value = res.hasMore
    nextCursor.value = res.nextCursor
  } finally {
    loading.value = false
  }
}

async function handleClearSearch() {
  searchKeyword.value = ''
  isSearching.value = false
  loading.value = true
  try {
    const res = await getTopicEntries(topicId, undefined, PAGE_SIZE)
    entries.value = res.items
    hasMore.value = res.hasMore
    nextCursor.value = res.nextCursor
  } finally {
    loading.value = false
  }
}

const debouncedSearch = debounce(() => {
  handleTopicSearch()
}, 300)

watch(searchKeyword, (val) => {
  if (!val.trim()) {
    handleClearSearch()
  } else {
    debouncedSearch()
  }
})

function contentPreview(raw?: string) {
  if (!raw) return ''
  return raw.length > 200 ? raw.substring(0, 200) + '...' : raw
}

function formatTime(value?: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 10)
}

onMounted(() => loadData())
</script>

<style scoped>
.topic-detail-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}
.page-header {
  margin-bottom: 16px;
}
.back-link {
  font-size: 14px;
  color: #409eff;
  text-decoration: none;
}
.back-link:hover { text-decoration: underline; }
.skeleton-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.topic-search {
  margin-bottom: 20px;
}
.topic-header {
  margin-bottom: 16px;
}
.topic-name {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 6px;
}
.topic-desc {
  font-size: 15px;
  color: #606266;
  margin: 0 0 6px;
  line-height: 1.6;
}
.topic-hint {
  font-size: 14px;
  color: #909399;
  margin: 0 0 8px;
}
.topic-stat {
  font-size: 13px;
  color: #606266;
}
.entry-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.entry-card {
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fff;
}
.entry-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  margin-bottom: 6px;
}
.entry-insight {
  font-size: 13px;
  color: #409eff;
  padding: 6px 10px;
  background: #ecf5ff;
  border-radius: 6px;
  margin-bottom: 6px;
}
.entry-meta {
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}
.entry-actions {
  display: flex;
  gap: 8px;
}
.load-more {
  text-align: center;
  padding: 16px 0;
}
</style>
