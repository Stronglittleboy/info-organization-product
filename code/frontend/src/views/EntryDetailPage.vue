<template>
  <div class="detail-page">
    <div class="page-header">
      <a class="back-link" @click.prevent="$router.back()">← 返回</a>
      <span class="page-title">详情</span>
    </div>

    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="28"><Loading /></el-icon>
    </div>

    <template v-else-if="entry">
      <!-- 原文区 -->
      <el-card class="section-card">
        <template #header><span class="section-title">原文</span></template>
        <div class="raw-content">{{ entry.rawContent }}</div>
      </el-card>

      <!-- 来源区 -->
      <el-card class="section-card">
        <template #header><span class="section-title">来源</span></template>
        <div class="source-info">
          <div v-if="entry.sourceType"><span class="label">类型：</span>{{ entry.sourceType }}</div>
          <div v-if="entry.sourceTitle"><span class="label">标题：</span>{{ entry.sourceTitle }}</div>
          <div v-if="entry.sourceLink">
            <span class="label">链接：</span>
            <a :href="entry.sourceLink" target="_blank" class="source-link">{{ entry.sourceLink }}</a>
          </div>
          <div><span class="label">记录时间：</span>{{ formatTime(entry.capturedAt) }}</div>
        </div>
      </el-card>

      <!-- 思考区 -->
      <el-card class="section-card">
        <template #header><span class="section-title">我的一句思考</span></template>

        <template v-if="!editingInsight">
          <div v-if="entry.insightText" class="insight-text">{{ entry.insightText }}</div>
          <div v-else class="empty-hint">还没有添加思考</div>
          <el-button size="small" text type="primary" @click="startEditInsight">
            {{ entry.insightText ? '编辑' : '添加思考' }}
          </el-button>
        </template>

        <template v-else>
          <el-input
            v-model="insightDraft"
            type="textarea"
            :rows="3"
            placeholder="写下你的一句话思考..."
            autofocus
          />
          <div class="edit-actions">
            <el-button type="primary" size="small" :loading="savingInsight" @click="handleSaveInsight">
              保存
            </el-button>
            <el-button size="small" @click="editingInsight = false">取消</el-button>
          </div>
        </template>
      </el-card>

      <!-- 专题区 -->
      <el-card class="section-card">
        <template #header><span class="section-title">所属专题</span></template>

        <template v-if="!editingTopic">
          <div v-if="entry.topicName" class="topic-display">
            <el-tag>{{ entry.topicName }}</el-tag>
          </div>
          <div v-else class="empty-hint">未归类</div>
          <el-button size="small" text type="primary" @click="startEditTopic">
            {{ entry.topicId ? '更换专题' : '加入专题' }}
          </el-button>
        </template>

        <template v-else>
          <el-select
            v-model="topicDraft"
            filterable
            clearable
            placeholder="选择专题"
            :loading="topicLoading"
            @focus="loadTopicOptions"
            style="width: 100%"
          >
            <el-option v-for="t in topicOptions" :key="t.topicId" :label="t.name" :value="t.topicId" />
          </el-select>
          <div class="edit-actions">
            <el-button
              type="primary"
              size="small"
              :disabled="!topicDraft"
              :loading="savingTopic"
              @click="handleSaveTopic"
            >
              保存
            </el-button>
            <el-button size="small" @click="editingTopic = false">取消</el-button>
          </div>
        </template>
      </el-card>

      <!-- 底部稳定动作区 -->
      <div class="bottom-actions">
        <el-button type="primary" @click="handleCopy('raw')">复制原文</el-button>
        <el-button
          v-if="entry.insightText"
          @click="handleCopy('with_insight')"
        >
          复制原文+思考
        </el-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getEntry,
  updateInsight,
  updateEntryTopic,
  recordReuse,
  getTopicList
} from '@/api/entry'
import type { EntryResponse, TopicResponse } from '@/types/entry'

const route = useRoute()
const entryId = route.params.entryId as string

const entry = ref<EntryResponse | null>(null)
const loading = ref(true)

const editingInsight = ref(false)
const insightDraft = ref('')
const savingInsight = ref(false)

const editingTopic = ref(false)
const topicDraft = ref('')
const savingTopic = ref(false)
const topicOptions = ref<TopicResponse[]>([])
const topicLoading = ref(false)

async function loadEntry() {
  loading.value = true
  try {
    entry.value = await getEntry(entryId)
  } finally {
    loading.value = false
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

function startEditInsight() {
  insightDraft.value = entry.value?.insightText || ''
  editingInsight.value = true
}

async function handleSaveInsight() {
  if (!insightDraft.value.trim()) return
  savingInsight.value = true
  try {
    await updateInsight(entryId, insightDraft.value.trim())
    entry.value!.insightText = insightDraft.value.trim()
    editingInsight.value = false
    ElMessage.success('思考已保存')
  } finally {
    savingInsight.value = false
  }
}

function startEditTopic() {
  topicDraft.value = entry.value?.topicId || ''
  editingTopic.value = true
}

async function handleSaveTopic() {
  if (!topicDraft.value) return
  savingTopic.value = true
  try {
    await updateEntryTopic(entryId, topicDraft.value)
    const selected = topicOptions.value.find(t => t.topicId === topicDraft.value)
    entry.value!.topicId = topicDraft.value
    entry.value!.topicName = selected?.name || ''
    editingTopic.value = false
    ElMessage.success('专题已更新')
  } finally {
    savingTopic.value = false
  }
}

async function handleCopy(type: 'raw' | 'with_insight') {
  if (!entry.value) return
  const text = type === 'with_insight' && entry.value.insightText
    ? `${entry.value.rawContent}\n\n💡 ${entry.value.insightText}`
    : entry.value.rawContent
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
    recordReuse(entryId, type === 'raw' ? 'COPY_RAW' : 'COPY_WITH_INSIGHT').catch(() => {})
  } catch {
    ElMessage.error('复制失败')
  }
}

function formatTime(value?: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 19)
}

onMounted(() => loadEntry())
</script>

<style scoped>
.detail-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 16px 120px;
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
  cursor: pointer;
}
.back-link:hover { text-decoration: underline; }
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.loading-state {
  text-align: center;
  padding: 48px 0;
  color: #909399;
}
.section-card {
  margin-bottom: 16px;
  border-radius: 12px;
}
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.raw-content {
  font-size: 15px;
  color: #303133;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}
.source-info {
  font-size: 14px;
  color: #606266;
  line-height: 2;
}
.label {
  color: #909399;
}
.source-link {
  color: #409eff;
  text-decoration: none;
}
.source-link:hover { text-decoration: underline; }
.insight-text {
  font-size: 15px;
  color: #303133;
  line-height: 1.7;
  margin-bottom: 8px;
}
.empty-hint {
  font-size: 14px;
  color: #c0c4cc;
  margin-bottom: 8px;
}
.edit-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.topic-display {
  margin-bottom: 8px;
}
.bottom-actions {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 1px solid #ebeef5;
  padding: 12px 16px;
  display: flex;
  justify-content: center;
  gap: 12px;
  z-index: 100;
}
</style>
