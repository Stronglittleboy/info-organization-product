<template>
  <div class="search-page">
    <!-- 搜索区 -->
    <div class="search-area">
      <el-input
        v-model="keyword"
        size="large"
        placeholder="搜索你收集过的内容..."
        clearable
        @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button :icon="Search" @click="handleSearch" />
        </template>
      </el-input>

      <!-- 筛选条 -->
      <div class="filter-bar">
        <el-select
          v-model="filterTopicId"
          clearable
          placeholder="按专题筛选"
          size="small"
          @change="handleSearch"
          @focus="loadTopicOptions"
          :loading="topicLoading"
        >
          <el-option v-for="t in topicOptions" :key="t.topicId" :label="t.name" :value="t.topicId" />
        </el-select>
        <el-select
          v-model="filterInsight"
          clearable
          placeholder="有无思考"
          size="small"
          @change="handleSearch"
        >
          <el-option label="有思考" :value="true" />
          <el-option label="无思考" :value="false" />
        </el-select>
      </div>
    </div>

    <!-- 空搜索引导 -->
    <div v-if="!searched" class="guide-state">
      <p class="guide-text">输入关键词，找回过去收集的内容</p>
    </div>

    <!-- 加载中 -->
    <div v-else-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="28"><Loading /></el-icon>
    </div>

    <!-- 无结果 -->
    <el-empty v-else-if="results.length === 0" description="没有找到匹配的内容" />

    <!-- 结果区 -->
    <div v-else class="result-list">
      <div class="result-count">找到 {{ total }} 条结果</div>

      <div v-for="entry in results" :key="entry.entryId" class="result-card">
        <!-- 第一层：内容片段 -->
        <div class="result-content">{{ contentPreview(entry.rawContent) }}</div>

        <!-- 第二层：一句话思考 -->
        <div v-if="entry.insightText" class="result-insight">
          💡 {{ entry.insightText }}
        </div>

        <!-- 第三层：次信息 -->
        <div class="result-meta">
          <el-tag v-if="entry.topicName" size="small" type="info">{{ entry.topicName }}</el-tag>
          <span>{{ entry.sourceTitle || '' }}</span>
          <span>{{ formatTime(entry.capturedAt) }}</span>
        </div>

        <!-- 动作 -->
        <div class="result-actions">
          <el-button size="small" @click="handleCopy(entry, 'raw')">复制原文</el-button>
          <el-button
            v-if="entry.insightText"
            size="small"
            @click="handleCopy(entry, 'with_insight')"
          >
            复制原文+思考
          </el-button>
          <el-button size="small" text type="primary" @click="$router.push(`/entry/${entry.entryId}`)">
            详情
          </el-button>
        </div>
      </div>

      <div v-if="hasMore" class="load-more">
        <el-button :loading="loadingMore" @click="loadMore">加载更多</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Search, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { searchEntries, recordReuse, getTopicList } from '@/api/entry'
import type { EntryResponse, TopicResponse } from '@/types/entry'

const keyword = ref('')
const filterTopicId = ref<string>('')
const filterInsight = ref<boolean | ''>('')
const searched = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const results = ref<EntryResponse[]>([])
const total = ref(0)
const hasMore = ref(false)
const currentOffset = ref(0)
const PAGE_SIZE = 20

const topicOptions = ref<TopicResponse[]>([])
const topicLoading = ref(false)

async function loadTopicOptions() {
  if (topicOptions.value.length > 0) return
  topicLoading.value = true
  try {
    const res = await getTopicList(0, 50)
    topicOptions.value = res.items
  } finally {
    topicLoading.value = false
  }
}

async function handleSearch() {
  if (!keyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  searched.value = true
  loading.value = true
  try {
    const res = await searchEntries({
      keyword: keyword.value.trim(),
      topicId: filterTopicId.value || undefined,
      hasInsight: filterInsight.value === '' ? undefined : filterInsight.value,
      offset: 0,
      limit: PAGE_SIZE
    })
    results.value = res.items
    total.value = res.total
    hasMore.value = res.hasMore
    currentOffset.value = res.items.length
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  loadingMore.value = true
  try {
    const res = await searchEntries({
      keyword: keyword.value.trim(),
      topicId: filterTopicId.value || undefined,
      hasInsight: filterInsight.value === '' ? undefined : filterInsight.value,
      offset: currentOffset.value,
      limit: PAGE_SIZE
    })
    results.value.push(...res.items)
    hasMore.value = res.hasMore
    currentOffset.value += res.items.length
  } finally {
    loadingMore.value = false
  }
}

function contentPreview(raw?: string) {
  if (!raw) return ''
  return raw.length > 300 ? raw.substring(0, 300) + '...' : raw
}

async function handleCopy(entry: EntryResponse, type: 'raw' | 'with_insight') {
  const text = type === 'with_insight' && entry.insightText
    ? `${entry.rawContent}\n\n💡 ${entry.insightText}`
    : entry.rawContent
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
    recordReuse(entry.entryId, type === 'raw' ? 'COPY_RAW' : 'COPY_WITH_INSIGHT').catch(() => {})
  } catch {
    ElMessage.error('复制失败，请手动选择复制')
  }
}

function formatTime(value?: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 10)
}
</script>

<style scoped>
.search-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}
.search-area {
  margin-bottom: 20px;
}
.filter-bar {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}
.guide-state {
  text-align: center;
  padding: 60px 0;
}
.guide-text {
  color: #909399;
  font-size: 15px;
}
.loading-state {
  text-align: center;
  padding: 48px 0;
  color: #909399;
}
.result-count {
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
}
.result-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.result-card {
  padding: 16px 18px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fff;
}
.result-content {
  font-size: 15px;
  color: #303133;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.result-insight {
  font-size: 13px;
  color: #409eff;
  padding: 6px 10px;
  background: #ecf5ff;
  border-radius: 6px;
  margin-bottom: 8px;
}
.result-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}
.result-actions {
  display: flex;
  gap: 8px;
}
.load-more {
  text-align: center;
  padding: 16px 0;
}
</style>
