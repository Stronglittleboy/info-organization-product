<template>
  <div class="collect-page">
    <!-- 主输入区 -->
    <el-card v-if="pageState !== 'saved'" class="collect-card">
      <template #header>
        <div class="card-header">收集</div>
      </template>

      <el-form @submit.prevent>
        <el-form-item label="内容">
          <el-input
            v-model="form.rawContent"
            type="textarea"
            :rows="8"
            placeholder="输入你想收集的内容"
            autofocus
          />
        </el-form-item>

        <el-form-item label="来源类型">
          <el-select v-model="form.sourceType" clearable placeholder="选择来源类型" style="width: 100%">
            <el-option label="书籍" value="BOOK" />
            <el-option label="网页" value="WEB" />
            <el-option label="对话" value="CONVERSATION" />
            <el-option label="手工录入" value="MANUAL" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>

        <el-form-item label="来源标题">
          <el-input v-model="form.sourceTitle" placeholder="例如：微信公众号 / 网页标题 / 书名" />
        </el-form-item>

        <el-form-item v-if="form.sourceType === 'WEB'" label="来源链接">
          <el-input v-model="form.sourceLink" placeholder="https://..." />
        </el-form-item>

        <el-form-item label="专题">
          <el-select
            v-model="form.topicName"
            filterable
            allow-create
            clearable
            default-first-option
            placeholder="可选：选择或输入专题名"
            :loading="topicLoading"
            @focus="loadTopicOptions"
          >
            <el-option
              v-for="t in topicOptions"
              :key="t.topicId"
              :label="t.name"
              :value="t.name"
            />
          </el-select>
        </el-form-item>

        <div class="action-row">
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            保存收集
          </el-button>
        </div>
      </el-form>
    </el-card>

    <!-- 保存后反馈区 -->
    <el-card v-if="pageState === 'saved'" class="feedback-card">
      <div class="feedback-header">
        <el-icon :size="24" color="#67c23a"><CircleCheckFilled /></el-icon>
        <span>已保存</span>
      </div>

      <div class="feedback-preview">{{ savedEntry?.rawContent }}</div>

      <div class="feedback-actions">
        <el-button type="primary" size="large" @click="handleContinueCollect">
          继续记录
        </el-button>
        <el-button size="large" @click="showInsightInput = true" :disabled="showInsightInput">
          补一句思考
        </el-button>
      </div>

      <!-- 补一句思考 -->
      <div v-if="showInsightInput" class="insight-section">
        <el-input
          v-model="insightText"
          type="textarea"
          :rows="3"
          placeholder="写下你的一句话思考..."
          autofocus
        />
        <div class="insight-actions">
          <el-button type="primary" :loading="insightSaving" @click="handleSaveInsight">
            保存思考
          </el-button>
          <el-button @click="showInsightInput = false; insightText = ''">取消</el-button>
        </div>
      </div>

      <!-- 更多处理（弱入口） -->
      <div class="more-actions">
        <el-button text type="info" @click="showTopicSelect = !showTopicSelect">
          {{ showTopicSelect ? '收起' : '更多处理' }}
        </el-button>
      </div>

      <!-- 加入专题 -->
      <div v-if="showTopicSelect" class="topic-section">
        <el-select
          v-model="selectedTopicId"
          filterable
          clearable
          placeholder="选择专题"
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
        <el-button
          type="primary"
          :disabled="!selectedTopicId"
          :loading="topicSaving"
          @click="handleSetTopic"
          style="margin-top: 8px"
        >
          加入专题
        </el-button>
      </div>
    </el-card>

    <!-- 最近收集 -->
    <el-card class="recent-card">
      <template #header>
        <div class="card-header-row">
          <span class="card-header">最近收集</span>
          <router-link v-if="pendingCount > 0" to="/pending" class="pending-hint">
            还有 {{ pendingCount }} 条可去待处理继续处理 →
          </router-link>
        </div>
      </template>

      <el-empty v-if="!recentEntries.length" description="暂无收集记录" />

      <div v-else class="recent-list">
        <div v-for="entry in recentEntries" :key="entry.entryId" class="recent-item">
          <div class="recent-title">{{ entry.sourceTitle || '未命名来源' }}</div>
          <div class="recent-content">{{ entry.rawContent }}</div>
          <div v-if="entry.insightText" class="recent-insight">
            💡 {{ entry.insightText }}
          </div>
          <div class="recent-meta">
            <el-tag v-if="entry.topicName" size="small" type="info">{{ entry.topicName }}</el-tag>
            <span v-else class="meta-text">未归类</span>
            <span class="meta-text">{{ formatTime(entry.capturedAt) }}</span>
          </div>

          <!-- 行内补思考 -->
          <div v-if="recentInsightEditing === entry.entryId" class="recent-inline-edit">
            <el-input
              v-model="recentInsightText"
              type="textarea"
              :rows="2"
              placeholder="写下你的一句话思考..."
              autofocus
            />
            <div class="recent-inline-actions">
              <el-button type="primary" size="small" :loading="recentSaving" @click="handleRecentSaveInsight(entry.entryId)">保存</el-button>
              <el-button size="small" @click="recentInsightEditing = ''">取消</el-button>
            </div>
          </div>

          <!-- 行内加专题 -->
          <div v-if="recentTopicEditing === entry.entryId" class="recent-inline-edit">
            <el-select
              v-model="recentTopicId"
              filterable
              allow-create
              clearable
              default-first-option
              placeholder="选择或输入专题名"
              :loading="topicLoading"
              @focus="loadTopicOptions"
              style="width: 100%"
            >
              <el-option v-for="t in topicOptions" :key="t.topicId" :label="t.name" :value="t.topicId" />
            </el-select>
            <div class="recent-inline-actions">
              <el-button type="primary" size="small" :disabled="!recentTopicId" :loading="recentSaving" @click="handleRecentSetTopic(entry.entryId)">保存</el-button>
              <el-button size="small" @click="recentTopicEditing = ''">取消</el-button>
            </div>
          </div>

          <!-- 快捷操作按钮 -->
          <div v-if="recentInsightEditing !== entry.entryId && recentTopicEditing !== entry.entryId" class="recent-quick-actions">
            <el-button
              v-if="!entry.insightText"
              size="small"
              text
              type="warning"
              @click="recentInsightEditing = entry.entryId; recentInsightText = ''; recentTopicEditing = ''"
            >补思考</el-button>
            <el-button
              v-if="!entry.topicName"
              size="small"
              text
              type="info"
              @click="recentTopicEditing = entry.entryId; recentTopicId = ''; recentInsightEditing = ''"
            >加专题</el-button>
            <el-button size="small" text type="primary" @click="$router.push(`/entry/${entry.entryId}`)">详情</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 值得回看（回顾机制） -->
    <el-card v-if="reviewEntries.length > 0" class="review-card">
      <template #header>
        <span class="card-header">值得回看</span>
      </template>
      <p class="review-desc">这些内容你已经多次取用，可能值得再看一眼</p>
      <div class="review-list">
        <div v-for="entry in reviewEntries" :key="entry.entryId" class="review-item">
          <div class="review-content">{{ entry.rawContent }}</div>
          <div v-if="entry.insightText" class="review-insight">💡 {{ entry.insightText }}</div>
          <div class="review-meta">
            <el-tag v-if="entry.topicName" size="small" type="info">{{ entry.topicName }}</el-tag>
            <span class="meta-text">已取用 {{ entry.reusedCount || 0 }} 次</span>
          </div>
          <div class="review-actions">
            <el-button size="small" text type="primary" @click="$router.push(`/entry/${entry.entryId}`)">详情</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled } from '@element-plus/icons-vue'
import {
  createEntry,
  getRecentEntries,
  updateInsight,
  updateEntryTopic,
  getTopicList,
  getPendingEntries,
  getReviewEntries
} from '@/api/entry'
import type { EntryResponse, TopicResponse } from '@/types/entry'

type PageState = 'input' | 'saved'

const pageState = ref<PageState>('input')
const submitting = ref(false)
const savedEntry = ref<EntryResponse | null>(null)
const recentEntries = ref<EntryResponse[]>([])
const pendingCount = ref(0)
const reviewEntries = ref<EntryResponse[]>([])

const form = ref({
  rawContent: '',
  sourceType: '',
  sourceTitle: '',
  sourceLink: '',
  topicName: ''
})

const showInsightInput = ref(false)
const insightText = ref('')
const insightSaving = ref(false)

const showTopicSelect = ref(false)
const selectedTopicId = ref('')
const topicSaving = ref(false)

const topicOptions = ref<TopicResponse[]>([])
const topicLoading = ref(false)

const recentInsightEditing = ref('')
const recentInsightText = ref('')
const recentTopicEditing = ref('')
const recentTopicId = ref('')
const recentSaving = ref(false)

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

async function loadRecentEntries() {
  recentEntries.value = await getRecentEntries()
}

async function loadPendingCount() {
  try {
    const res = await getPendingEntries(0, 1)
    pendingCount.value = res.total
  } catch {
    pendingCount.value = 0
  }
}

async function loadReviewEntries() {
  try {
    reviewEntries.value = await getReviewEntries(3)
  } catch {
    reviewEntries.value = []
  }
}

async function handleSubmit() {
  if (!form.value.rawContent.trim()) {
    ElMessage.warning('请先输入收集内容')
    return
  }

  submitting.value = true
  try {
    const result = await createEntry({
      rawContent: form.value.rawContent,
      contentType: 'text',
      sourceType: form.value.sourceType || 'MANUAL',
      sourceTitle: form.value.sourceTitle || '手工录入',
      sourceLink: form.value.sourceLink || undefined,
      topicName: form.value.topicName || undefined
    })
    savedEntry.value = result
    pageState.value = 'saved'
    showInsightInput.value = false
    showTopicSelect.value = false
    insightText.value = ''
    selectedTopicId.value = ''
    ElMessage.success('保存成功')
    await Promise.all([loadRecentEntries(), loadPendingCount(), loadReviewEntries()])
  } finally {
    submitting.value = false
  }
}

function handleContinueCollect() {
  form.value = { rawContent: '', sourceType: '', sourceTitle: '', sourceLink: '', topicName: '' }
  savedEntry.value = null
  pageState.value = 'input'
}

async function handleSaveInsight() {
  if (!insightText.value.trim() || !savedEntry.value) return
  insightSaving.value = true
  try {
    await updateInsight(savedEntry.value.entryId, insightText.value.trim())
    ElMessage.success('思考已保存')
    showInsightInput.value = false
    insightText.value = ''
    await Promise.all([loadRecentEntries(), loadPendingCount()])
  } finally {
    insightSaving.value = false
  }
}

async function handleSetTopic() {
  if (!selectedTopicId.value || !savedEntry.value) return
  topicSaving.value = true
  try {
    await updateEntryTopic(savedEntry.value.entryId, selectedTopicId.value)
    ElMessage.success('已加入专题')
    showTopicSelect.value = false
    selectedTopicId.value = ''
    await Promise.all([loadRecentEntries(), loadPendingCount()])
  } finally {
    topicSaving.value = false
  }
}

async function handleRecentSaveInsight(entryId: string) {
  if (!recentInsightText.value.trim()) return
  recentSaving.value = true
  try {
    await updateInsight(entryId, recentInsightText.value.trim())
    ElMessage.success('思考已保存')
    recentInsightEditing.value = ''
    await Promise.all([loadRecentEntries(), loadPendingCount()])
  } finally {
    recentSaving.value = false
  }
}

async function handleRecentSetTopic(entryId: string) {
  if (!recentTopicId.value) return
  recentSaving.value = true
  try {
    await updateEntryTopic(entryId, recentTopicId.value)
    ElMessage.success('已加入专题')
    recentTopicEditing.value = ''
    await Promise.all([loadRecentEntries(), loadPendingCount()])
  } finally {
    recentSaving.value = false
  }
}

function formatTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

onMounted(async () => {
  await Promise.all([loadRecentEntries(), loadPendingCount(), loadReviewEntries()])
})
</script>

<style scoped>
.collect-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 16px 48px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.collect-card, .recent-card, .feedback-card {
  border-radius: 16px;
}
.card-header {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pending-hint {
  font-size: 13px;
  color: #e6a23c;
  text-decoration: none;
}
.pending-hint:hover {
  text-decoration: underline;
}
.action-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.feedback-card {
  text-align: center;
}
.feedback-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 600;
  color: #67c23a;
  margin-bottom: 16px;
}
.feedback-preview {
  font-size: 14px;
  color: #606266;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 20px;
  text-align: left;
  max-height: 120px;
  overflow: hidden;
}
.feedback-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
}
.insight-section {
  margin-top: 12px;
  text-align: left;
}
.insight-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}
.more-actions {
  margin-top: 8px;
}
.topic-section {
  margin-top: 12px;
  text-align: left;
}
.recent-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.recent-item {
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fafafa;
}
.recent-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}
.recent-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  margin-bottom: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.recent-insight {
  font-size: 13px;
  color: #409eff;
  margin-bottom: 6px;
  padding: 6px 10px;
  background: #ecf5ff;
  border-radius: 6px;
}
.recent-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #909399;
}
.meta-text {
  color: #909399;
}
.recent-quick-actions {
  display: flex;
  gap: 4px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #ebeef5;
}
.recent-inline-edit {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #ebeef5;
}
.recent-inline-actions {
  display: flex;
  gap: 8px;
  margin-top: 6px;
}
.review-card {
  border-radius: 16px;
  border-left: 3px solid #e6a23c;
}
.review-desc {
  font-size: 13px;
  color: #909399;
  margin: 0 0 12px;
}
.review-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.review-item {
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fffbf0;
}
.review-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 6px;
}
.review-insight {
  font-size: 13px;
  color: #409eff;
  margin-bottom: 6px;
  padding: 4px 8px;
  background: #ecf5ff;
  border-radius: 4px;
}
.review-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #909399;
}
.review-actions {
  margin-top: 4px;
}
</style>
