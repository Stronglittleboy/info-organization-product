<template>
  <div class="browse-page">
    <div class="page-intro">
      <h1 class="title">按时间浏览</h1>
      <p class="subtitle">按记录时间回看素材，无需关键词</p>
    </div>

    <el-card class="filter-card" shadow="never">
      <div class="preset-row">
        <span class="preset-label">时间</span>
        <el-radio-group v-model="preset" size="small" @change="onPresetChange">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="d7">近7天</el-radio-button>
          <el-radio-button label="d30">近30天</el-radio-button>
          <el-radio-button label="custom">自选</el-radio-button>
        </el-radio-group>
      </div>
      <div v-if="preset === 'custom'" class="range-row">
        <el-date-picker
          v-model="filterDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始"
          end-placeholder="结束"
          value-format="YYYY-MM-DD"
          size="small"
          clearable
          style="width: 100%; max-width: 320px"
          @change="onManualRangeChange"
        />
      </div>
      <div class="filter-row">
        <el-select
          v-model="filterTopicId"
          clearable
          placeholder="专题"
          size="small"
          style="width: 160px"
          @focus="loadTopicOptions"
          :loading="topicLoading"
        >
          <el-option v-for="t in topicOptions" :key="t.topicId" :label="t.name" :value="t.topicId" />
        </el-select>
        <el-select v-model="filterContentType" clearable placeholder="类型" size="small" style="width: 120px">
          <el-option label="文本" value="text" />
          <el-option label="链接" value="url" />
          <el-option label="图片" value="image" />
        </el-select>
        <el-select v-model="filterInsight" clearable placeholder="思考" size="small" style="width: 120px">
          <el-option label="有思考" :value="true" />
          <el-option label="无思考" :value="false" />
        </el-select>
      </div>
      <p class="cross-link">
        <router-link to="/search">想按关键词找回？去「查找」</router-link>
      </p>
    </el-card>

    <div v-if="loading" class="skeleton-area">
      <div v-for="i in 3" :key="i" class="entry-card">
        <el-skeleton :rows="3" animated />
      </div>
    </div>

    <el-empty v-else-if="!items.length" description="当前条件下暂无素材" />

    <div v-else class="grouped-list">
      <div v-for="block in groupedBlocks" :key="block.key" class="time-block">
        <div class="time-block-title">{{ block.label }}</div>
        <div v-for="entry in block.items" :key="entry.entryId" class="entry-card" @click="$router.push(`/entry/${entry.entryId}`)">
          <div class="entry-type">{{ typeIcon(entry.contentType) }}</div>
          <div class="entry-main">
            <div class="entry-title">{{ cardTitle(entry) }}</div>
            <div class="entry-snippet">{{ contentPreview(entry) }}</div>
            <div class="entry-meta">
              <el-tag v-if="entry.topicName" size="small" type="info">{{ entry.topicName }}</el-tag>
              <span>{{ formatTime(entry.capturedAt) }}</span>
            </div>
          </div>
          <el-button size="small" text type="primary" class="entry-go" @click.stop="$router.push(`/entry/${entry.entryId}`)">
            详情
          </el-button>
        </div>
      </div>

      <div v-if="hasMore" class="load-more">
        <el-button :loading="loadingMore" @click="loadMore">加载更多</el-button>
      </div>
      <p v-else-if="items.length" class="end-hint">已到底（本条件下共 {{ total }} 条）</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { browseEntries, getTopicList } from '@/api/entry'
import { debounce } from '@/utils/debounce'
import type { EntryResponse, TopicResponse } from '@/types/entry'

type GroupKey = 'today' | 'yesterday' | 'this_week' | 'older'

const GROUP_LABEL: Record<GroupKey, string> = {
  today: '今天',
  yesterday: '昨天',
  this_week: '本周',
  older: '更早'
}

const preset = ref<'all' | 'd7' | 'd30' | 'custom'>('d30')
const filterDateRange = ref<[string, string] | null>(null)
const filterTopicId = ref('')
const filterContentType = ref('')
const filterInsight = ref<boolean | ''>('')

const topicOptions = ref<TopicResponse[]>([])
const topicLoading = ref(false)

const items = ref<EntryResponse[]>([])
const total = ref(0)
const hasMore = ref(false)
const nextCursor = ref<string | undefined>(undefined)
const loading = ref(true)
const loadingMore = ref(false)

function pad2(n: number) {
  return n < 10 ? `0${n}` : `${n}`
}

function formatYmd(d: Date) {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
}

function startOfLocalDay(d: Date) {
  const x = new Date(d)
  x.setHours(0, 0, 0, 0)
  return x
}

function applyPresetRange() {
  const now = new Date()
  if (preset.value === 'all') {
    filterDateRange.value = null
    return
  }
  if (preset.value === 'custom') {
    return
  }
  const days = preset.value === 'd7' ? 6 : 29
  const start = new Date(startOfLocalDay(now))
  start.setDate(start.getDate() - days)
  filterDateRange.value = [formatYmd(start), formatYmd(now)]
}

function onPresetChange() {
  if (preset.value !== 'custom') {
    applyPresetRange()
  }
  void reloadFirstPage()
}

function onManualRangeChange() {
  preset.value = 'custom'
  void reloadFirstPage()
}

function localDateKey(d: Date) {
  return formatYmd(d)
}

function groupKeyForCaptured(iso?: string): GroupKey {
  if (!iso) return 'older'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return 'older'

  const now = new Date()
  const startToday = startOfLocalDay(now)
  const startYesterday = new Date(startToday)
  startYesterday.setDate(startYesterday.getDate() - 1)
  const windowStart = new Date(startToday)
  windowStart.setDate(windowStart.getDate() - 7)

  const key = localDateKey(d)
  if (key === localDateKey(now)) return 'today'
  if (key === localDateKey(startYesterday)) return 'yesterday'
  if (d >= windowStart && d < startYesterday) return 'this_week'
  return 'older'
}

const groupedBlocks = computed(() => {
  const order: GroupKey[] = ['today', 'yesterday', 'this_week', 'older']
  const buckets: Record<GroupKey, EntryResponse[]> = {
    today: [],
    yesterday: [],
    this_week: [],
    older: []
  }
  for (const e of items.value) {
    buckets[groupKeyForCaptured(e.capturedAt)].push(e)
  }
  return order
    .filter((k) => buckets[k].length > 0)
    .map((k) => ({ key: k, label: GROUP_LABEL[k], items: buckets[k] }))
})

function buildParams(cursor?: string) {
  return {
    topicId: filterTopicId.value || undefined,
    contentType: filterContentType.value || undefined,
    hasInsight: filterInsight.value === '' ? undefined : filterInsight.value,
    startDate: filterDateRange.value?.[0] || undefined,
    endDate: filterDateRange.value?.[1] ? `${filterDateRange.value[1]}T23:59:59` : undefined,
    cursor,
    limit: 30
  }
}

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

async function reloadFirstPage() {
  loading.value = true
  try {
    const res = await browseEntries(buildParams())
    items.value = res.items
    total.value = res.total
    hasMore.value = res.hasMore
    nextCursor.value = res.nextCursor
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (!nextCursor.value) return
  loadingMore.value = true
  try {
    const res = await browseEntries(buildParams(nextCursor.value))
    items.value.push(...res.items)
    hasMore.value = res.hasMore
    nextCursor.value = res.nextCursor
  } finally {
    loadingMore.value = false
  }
}

const debouncedReload = debounce(() => {
  void reloadFirstPage()
}, 300)

watch([filterTopicId, filterContentType, filterInsight], () => {
  debouncedReload()
})

watch(filterDateRange, () => {
  if (preset.value === 'custom') {
    debouncedReload()
  }
})

function formatTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

function typeIcon(ct?: string) {
  if (ct === 'url') return '🔗'
  if (ct === 'image') return '📷'
  return '📝'
}

function cardTitle(e: EntryResponse) {
  if (e.contentType === 'url' && e.urlTitle?.trim()) return e.urlTitle.trim()
  if (e.sourceTitle?.trim()) return e.sourceTitle.trim()
  const s = contentPreview(e)
  if (s === '（无正文）') return '（无标题）'
  return s.length > 80 ? s.slice(0, 80) + '…' : s
}

function contentPreview(e: EntryResponse) {
  const raw = e.rawContent || ''
  if (raw.length > 160) return raw.slice(0, 160) + '…'
  return raw || '（无正文）'
}

onMounted(() => {
  applyPresetRange()
  void reloadFirstPage()
})
</script>

<style scoped>
.browse-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px 16px 48px;
}
.page-intro {
  margin-bottom: 16px;
}
.title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}
.subtitle {
  margin: 6px 0 0;
  font-size: 14px;
  color: #909399;
}
.filter-card {
  border-radius: 12px;
  margin-bottom: 20px;
}
.preset-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.preset-label {
  font-size: 13px;
  color: #606266;
}
.range-row {
  margin-bottom: 12px;
}
.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.cross-link {
  margin: 14px 0 0;
  font-size: 13px;
}
.cross-link a {
  color: #409eff;
  text-decoration: none;
}
.cross-link a:hover {
  text-decoration: underline;
}
.skeleton-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.time-block-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 16px 0 10px;
}
.time-block:first-child .time-block-title {
  margin-top: 0;
}
.entry-card {
  display: flex;
  gap: 10px;
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fafafa;
  margin-bottom: 10px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.entry-card:hover {
  border-color: #c6e2ff;
  background: #ecf5ff;
}
.entry-type {
  font-size: 18px;
  line-height: 1.4;
}
.entry-main {
  flex: 1;
  min-width: 0;
}
.entry-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
  word-break: break-word;
}
.entry-snippet {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  word-break: break-word;
  margin-bottom: 8px;
}
.entry-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #909399;
}
.entry-go {
  align-self: flex-start;
}
.load-more {
  text-align: center;
  margin-top: 16px;
}
.end-hint {
  text-align: center;
  font-size: 13px;
  color: #c0c4cc;
  margin-top: 12px;
}
</style>
