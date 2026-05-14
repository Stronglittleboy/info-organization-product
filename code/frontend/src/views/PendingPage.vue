<template>
  <div class="pending-page">
    <div class="page-header">
      <router-link to="/collect" class="back-link">← 收集</router-link>
      <h2 class="page-title">待处理</h2>
    </div>

    <!-- 提醒条 -->
    <div v-if="total > 0" class="reminder-bar">
      最近有 {{ total }} 条内容还可以再变得更好用
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <!-- 空状态 -->
    <el-empty v-else-if="items.length === 0" description="当前没有待处理的内容">
      <el-button type="primary" @click="$router.push('/collect')">去收集</el-button>
    </el-empty>

    <!-- 处理流 -->
    <div v-else class="pending-list">
      <div
        v-for="entry in items"
        :key="entry.entryId"
        class="pending-card"
      >
        <!-- 内容摘要 -->
        <div class="card-content">{{ entry.contentPreview }}</div>

        <!-- 来源与时间 -->
        <div class="card-meta">
          <span v-if="entry.sourceType">{{ entry.sourceType }}</span>
          <span>{{ formatTime(entry.capturedAt) }}</span>
        </div>

        <!-- 当前建议动作提示 -->
        <div class="suggested-action">
          <el-tag
            :type="entry.currentSuggestedAction === 'ADD_INSIGHT' ? 'warning' : 'info'"
            size="small"
          >
            {{ entry.currentSuggestedAction === 'ADD_INSIGHT' ? '建议：补一句思考' : '建议：加入专题' }}
          </el-tag>
        </div>

        <!-- 已有思考展示 -->
        <div v-if="entry.insightText" class="existing-insight">
          💡 {{ entry.insightText }}
        </div>

        <!-- 情况 A：无思考 - 显示思考输入 -->
        <div v-if="entry.currentSuggestedAction === 'ADD_INSIGHT'" class="action-section">
          <el-input
            v-model="insightInputs[entry.entryId]"
            type="textarea"
            :rows="2"
            placeholder="写下你的一句话思考..."
          />
          <div class="action-buttons">
            <el-button
              type="primary"
              size="small"
              :loading="saving[entry.entryId]"
              :disabled="!insightInputs[entry.entryId]?.trim()"
              @click="handleSaveInsight(entry.entryId)"
            >
              保存
            </el-button>
            <el-button size="small" @click="handleSkip(entry.entryId)">跳过</el-button>
          </div>
        </div>

        <!-- 情况 B：有思考但无专题 - 显示专题选择 -->
        <div v-if="entry.currentSuggestedAction === 'ADD_TOPIC'" class="action-section">
          <el-select
            v-model="topicSelections[entry.entryId]"
            filterable
            allow-create
            clearable
            default-first-option
            placeholder="选择或输入专题名"
            :loading="topicLoading"
            @focus="loadTopicOptions"
            style="width: 100%"
          >
            <el-option
              v-for="t in topicOptions"
              :key="t.topicId"
              :label="t.name"
              :value="t.topicId"
            />
          </el-select>
          <div class="action-buttons">
            <el-button
              type="primary"
              size="small"
              :loading="saving[entry.entryId]"
              :disabled="!topicSelections[entry.entryId]"
              @click="handleSetTopic(entry.entryId)"
            >
              保存
            </el-button>
            <el-button size="small" @click="handleSkip(entry.entryId)">跳过</el-button>
          </div>
        </div>
      </div>

      <!-- 加载更多 -->
      <div v-if="hasMore" class="load-more">
        <el-button :loading="loadingMore" @click="loadMore">加载更多</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getPendingEntries,
  updateInsight,
  updateEntryTopic,
  skipEntry,
  getTopicList
} from '@/api/entry'
import type { PendingEntryResponse, TopicResponse } from '@/types/entry'

const items = ref<PendingEntryResponse[]>([])
const total = ref(0)
const hasMore = ref(false)
const loading = ref(true)
const loadingMore = ref(false)
const currentOffset = ref(0)
const PAGE_SIZE = 20

const insightInputs = reactive<Record<string, string>>({})
const topicSelections = reactive<Record<string, string>>({})
const saving = reactive<Record<string, boolean>>({})

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

async function loadEntries() {
  loading.value = true
  try {
    const res = await getPendingEntries(0, PAGE_SIZE)
    items.value = res.items
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
    const res = await getPendingEntries(currentOffset.value, PAGE_SIZE)
    items.value.push(...res.items)
    hasMore.value = res.hasMore
    currentOffset.value += res.items.length
  } finally {
    loadingMore.value = false
  }
}

function removeFromList(entryId: string) {
  items.value = items.value.filter(e => e.entryId !== entryId)
  total.value = Math.max(0, total.value - 1)
}

async function handleSaveInsight(entryId: string) {
  const text = insightInputs[entryId]?.trim()
  if (!text) return

  saving[entryId] = true
  try {
    await updateInsight(entryId, text)
    ElMessage.success('思考已保存')
    const entry = items.value.find(e => e.entryId === entryId)
    if (entry) {
      entry.insightText = text
      if (entry.topicId) {
        removeFromList(entryId)
      } else {
        entry.currentSuggestedAction = 'ADD_TOPIC'
      }
    }
    delete insightInputs[entryId]
  } finally {
    saving[entryId] = false
  }
}

async function handleSetTopic(entryId: string) {
  const topicId = topicSelections[entryId]
  if (!topicId) return

  saving[entryId] = true
  try {
    await updateEntryTopic(entryId, topicId)
    ElMessage.success('已加入专题')
    removeFromList(entryId)
    delete topicSelections[entryId]
  } finally {
    saving[entryId] = false
  }
}

async function handleSkip(entryId: string) {
  try {
    await skipEntry(entryId)
    removeFromList(entryId)
    ElMessage.info('已跳过，24小时后再出现')
  } catch {
    ElMessage.error('跳过失败')
  }
}

function formatTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

onMounted(() => {
  loadEntries()
})
</script>

<style scoped>
.pending-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.back-link {
  font-size: 14px;
  color: #409eff;
  text-decoration: none;
}
.back-link:hover {
  text-decoration: underline;
}
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin: 0;
}
.reminder-bar {
  padding: 10px 16px;
  background: #fdf6ec;
  color: #e6a23c;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 20px;
}
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 48px 0;
  color: #909399;
}
.pending-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.pending-card {
  padding: 18px 20px;
  border: 1px solid #ebeef5;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,.04);
}
.card-content {
  font-size: 15px;
  color: #303133;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  margin-bottom: 8px;
}
.card-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}
.suggested-action {
  margin-bottom: 12px;
}
.existing-insight {
  font-size: 13px;
  color: #409eff;
  padding: 8px 12px;
  background: #ecf5ff;
  border-radius: 6px;
  margin-bottom: 12px;
}
.action-section {
  margin-top: 4px;
}
.action-buttons {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.load-more {
  text-align: center;
  padding: 16px 0;
}
</style>
