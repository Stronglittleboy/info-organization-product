<template>
  <div class="topic-detail-page">
    <div class="page-header">
      <router-link to="/topics" class="back-link">← 专题</router-link>
    </div>

    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="28"><Loading /></el-icon>
    </div>

    <template v-else-if="topic">
      <div class="topic-header">
        <h2 class="topic-name">{{ topic.name }}</h2>
        <p class="topic-hint">这里汇集你围绕【{{ topic.name }}】持续积累的内容</p>
        <div class="topic-stat">{{ topic.entryCount }} 条素材</div>
      </div>

      <el-empty v-if="entries.length === 0" description="这个专题还没有素材" />

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
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import { getTopicDetail, getTopicEntries } from '@/api/entry'
import type { TopicResponse, EntryResponse } from '@/types/entry'

const route = useRoute()
const topicId = route.params.topicId as string

const topic = ref<TopicResponse | null>(null)
const entries = ref<EntryResponse[]>([])
const loading = ref(true)
const loadingMore = ref(false)
const hasMore = ref(false)
const currentOffset = ref(0)
const PAGE_SIZE = 20

async function loadData() {
  loading.value = true
  try {
    const [t, e] = await Promise.all([
      getTopicDetail(topicId),
      getTopicEntries(topicId, 0, PAGE_SIZE)
    ])
    topic.value = t
    entries.value = e.items
    hasMore.value = e.hasMore
    currentOffset.value = e.items.length
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  loadingMore.value = true
  try {
    const res = await getTopicEntries(topicId, currentOffset.value, PAGE_SIZE)
    entries.value.push(...res.items)
    hasMore.value = res.hasMore
    currentOffset.value += res.items.length
  } finally {
    loadingMore.value = false
  }
}

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
.loading-state {
  text-align: center;
  padding: 48px 0;
  color: #909399;
}
.topic-header {
  margin-bottom: 24px;
}
.topic-name {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 6px;
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
