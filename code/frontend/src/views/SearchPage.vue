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
          @change="handleFilterChange"
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
          @change="handleFilterChange"
        >
          <el-option label="有思考" :value="true" />
          <el-option label="无思考" :value="false" />
        </el-select>
        <el-date-picker
          v-model="filterDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          size="small"
          value-format="YYYY-MM-DD"
          @change="handleFilterChange"
          clearable
          style="width: 260px"
        />
      </div>
      <p class="browse-cross">
        <router-link to="/browse">按时间浏览全部素材 →</router-link>
      </p>
    </div>

    <!-- 空搜索引导 -->
    <div v-if="!searched" class="guide-state">
      <p class="guide-text">输入关键词，找回过去收集的内容</p>
    </div>

    <!-- 加载骨架屏 -->
    <div v-else-if="loading" class="skeleton-list">
      <div v-for="i in 3" :key="i" class="result-card">
        <el-skeleton :rows="3" animated />
      </div>
    </div>

    <!-- 无结果 -->
    <el-empty v-else-if="results.length === 0" description="没有找到匹配的内容">
      <div class="no-result-tips">
        <p>试试：</p>
        <ul>
          <li>使用更短或更常见的关键词</li>
          <li>检查是否有筛选条件限制了结果</li>
        </ul>
      </div>
    </el-empty>

    <!-- 结果区 -->
    <div v-else class="result-list">
      <div class="result-count">找到 {{ total }} 条结果</div>

      <div v-for="entry in results" :key="entry.entryId" class="result-card">
        <!-- 第一层：内容片段（关键词高亮 + 命中上下文） -->
        <div class="result-content" v-html="highlightedSnippet(entry.rawContent)"></div>

        <!-- 第二层：一句话思考 -->
        <div v-if="entry.insightText" class="result-insight">
          <span v-html="'💡 ' + highlightText(entry.insightText)"></span>
        </div>

        <!-- 第三层：次信息（弱化呈现） -->
        <div class="result-meta">
          <el-tag
            v-if="entry.topicName"
            size="small"
            type="info"
            class="topic-link"
            @click="goToTopic(entry.topicId)"
          >{{ entry.topicName }}</el-tag>
          <span v-if="entry.sourceTitle">{{ entry.sourceTitle }}</span>
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

      <!-- 页底推荐：相关专题 -->
      <div v-if="relatedTopics.length > 0" class="related-topics">
        <div class="related-topics-title">相关专题</div>
        <div class="related-topics-list">
          <el-tag
            v-for="topic in relatedTopics"
            :key="topic.topicId"
            class="topic-link"
            @click="goToTopic(topic.topicId)"
          >{{ topic.name }}</el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { searchEntries, recordReuse, getTopicList } from '@/api/entry'
import { debounce } from '@/utils/debounce'
import type { EntryResponse, TopicResponse } from '@/types/entry'

const router = useRouter()

const keyword = ref('')
const filterTopicId = ref<string>('')
const filterInsight = ref<boolean | ''>('')
const filterDateRange = ref<[string, string] | null>(null)
const searched = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const results = ref<EntryResponse[]>([])
const total = ref(0)
const hasMore = ref(false)
const nextCursor = ref<string | undefined>(undefined)
const PAGE_SIZE = 20

const topicOptions = ref<TopicResponse[]>([])
const topicLoading = ref(false)

const relatedTopics = computed(() => {
  const seen = new Map<string, { topicId: string; name: string }>()
  for (const entry of results.value) {
    if (entry.topicId && entry.topicName && !seen.has(entry.topicId)) {
      seen.set(entry.topicId, { topicId: entry.topicId, name: entry.topicName })
    }
  }
  return Array.from(seen.values())
})

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

function buildSearchParams(cursor?: string) {
  return {
    keyword: keyword.value.trim(),
    topicId: filterTopicId.value || undefined,
    hasInsight: filterInsight.value === '' ? undefined : filterInsight.value,
    startDate: filterDateRange.value?.[0] || undefined,
    endDate: filterDateRange.value?.[1] ? filterDateRange.value[1] + 'T23:59:59' : undefined,
    cursor,
    limit: PAGE_SIZE
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
    const res = await searchEntries(buildSearchParams())
    results.value = res.items
    total.value = res.total
    hasMore.value = res.hasMore
    nextCursor.value = res.nextCursor
  } finally {
    loading.value = false
  }
}

const handleFilterChange = debounce(() => {
  if (keyword.value.trim()) handleSearch()
}, 300)

async function loadMore() {
  loadingMore.value = true
  try {
    const res = await searchEntries(buildSearchParams(nextCursor.value))
    results.value.push(...res.items)
    hasMore.value = res.hasMore
    nextCursor.value = res.nextCursor
  } finally {
    loadingMore.value = false
  }
}

function extractSnippet(raw?: string): string {
  if (!raw) return ''
  const kw = keyword.value.trim()
  if (!kw) return raw.length > 200 ? raw.substring(0, 200) + '...' : raw

  const lowerRaw = raw.toLowerCase()
  const lowerKw = kw.toLowerCase()
  const idx = lowerRaw.indexOf(lowerKw)

  if (idx === -1) {
    return raw.length > 200 ? raw.substring(0, 200) + '...' : raw
  }

  const contextLen = 60
  const start = Math.max(0, idx - contextLen)
  const end = Math.min(raw.length, idx + kw.length + contextLen)
  let snippet = raw.substring(start, end)
  if (start > 0) snippet = '...' + snippet
  if (end < raw.length) snippet = snippet + '...'
  return snippet
}

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function highlightText(text?: string): string {
  if (!text) return ''
  const kw = keyword.value.trim()
  if (!kw) return escapeHtml(text)

  const escaped = escapeHtml(text)
  const escapedKw = escapeHtml(kw)
  const regex = new RegExp(`(${escapedKw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi')
  return escaped.replace(regex, '<mark class="highlight">$1</mark>')
}

function highlightedSnippet(raw?: string): string {
  const snippet = extractSnippet(raw)
  return highlightText(snippet)
}

function goToTopic(topicId?: string) {
  if (topicId) {
    router.push(`/topic/${topicId}`)
  }
}

async function handleCopy(entry: EntryResponse, type: 'raw' | 'with_insight') {
  const text = type === 'with_insight' && entry.insightText
    ? `${entry.rawContent}\n\n💡 ${entry.insightText}`
    : entry.rawContent
  try {
    await navigator.clipboard.writeText(text)
    try {
      const res = await recordReuse(entry.entryId, type === 'raw' ? 'COPY_RAW' : 'COPY_WITH_INSIGHT')
      if (res.totalCount > 1) {
        ElMessage.success(`已复制，这是你第 ${res.totalCount} 次取用这条内容`)
      } else {
        ElMessage.success('已复制')
      }
    } catch {
      ElMessage.success('已复制')
    }
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
.browse-cross {
  margin: 12px 0 0;
  font-size: 13px;
}
.browse-cross a {
  color: #409eff;
  text-decoration: none;
}
.browse-cross a:hover {
  text-decoration: underline;
}
.guide-state {
  text-align: center;
  padding: 60px 0;
}
.guide-text {
  color: #909399;
  font-size: 15px;
}
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.no-result-tips {
  text-align: left;
  font-size: 13px;
  color: #909399;
  line-height: 1.8;
}
.no-result-tips ul {
  padding-left: 18px;
  margin: 4px 0 0;
}
.no-result-tips p {
  margin: 0;
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

.topic-link {
  cursor: pointer;
  transition: all 0.2s;
}
.topic-link:hover {
  color: #409eff;
  border-color: #409eff;
}

.related-topics {
  margin-top: 24px;
  padding: 16px 18px;
  background: #fafbfc;
  border-radius: 12px;
  border: 1px solid #ebeef5;
}
.related-topics-title {
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
}
.related-topics-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

:deep(.highlight) {
  background: #fef0c7;
  color: #303133;
  padding: 0 1px;
  border-radius: 2px;
}
</style>
