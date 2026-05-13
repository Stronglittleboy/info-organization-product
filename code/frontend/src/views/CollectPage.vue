<template>
  <div class="collect-page">
    <el-card class="collect-card">
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
          />
        </el-form-item>

        <el-form-item label="来源标题">
          <el-input v-model="form.sourceTitle" placeholder="例如：微信公众号 / 网页标题 / 手工录入" />
        </el-form-item>

        <el-form-item label="专题名称">
          <el-input v-model="form.topicName" placeholder="可选：输入专题名自动归类" />
        </el-form-item>

        <div class="action-row">
          <el-button type="primary" :loading="submitting" @click="handleSubmit">保存收集</el-button>
          <span v-if="savedEntryId" class="success-text">已保存，Entry ID：{{ savedEntryId }}</span>
        </div>
      </el-form>
    </el-card>

    <el-card class="recent-card">
      <template #header>
        <div class="card-header">最近收集</div>
      </template>

      <el-empty v-if="!recentEntries.length" description="暂无收集记录" />

      <div v-else class="recent-list">
        <div v-for="entry in recentEntries" :key="entry.entryId" class="recent-item">
          <div class="recent-title">{{ entry.sourceTitle || '未命名来源' }}</div>
          <div class="recent-content">{{ entry.rawContent }}</div>
          <div class="recent-meta">
            <span>{{ entry.contentType || 'text' }}</span>
            <span>{{ entry.topicName || '未归类' }}</span>
            <span>{{ formatTime(entry.capturedAt) }}</span>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createEntry, getRecentEntries } from '@/api/entry'
import type { EntryResponse } from '@/types/entry'

const submitting = ref(false)
const savedEntryId = ref('')
const recentEntries = ref<EntryResponse[]>([])

const form = reactive({
  rawContent: '',
  sourceTitle: '',
  topicName: ''
})

async function loadRecentEntries() {
  recentEntries.value = await getRecentEntries()
}

async function handleSubmit() {
  if (!form.rawContent.trim()) {
    ElMessage.warning('请先输入收集内容')
    return
  }

  submitting.value = true
  try {
    const result = await createEntry({
      rawContent: form.rawContent,
      contentType: 'text',
      sourceType: 'manual',
      sourceTitle: form.sourceTitle || '手工录入',
      topicName: form.topicName || undefined
    })

    savedEntryId.value = result.entryId
    ElMessage.success('保存成功')
    form.rawContent = ''
    form.sourceTitle = ''
    form.topicName = ''
    await loadRecentEntries()
  } finally {
    submitting.value = false
  }
}

function formatTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

onMounted(async () => {
  await loadRecentEntries()
})
</script>

<style scoped>
.collect-page {
  max-width: 960px;
  margin: 32px auto;
  padding: 0 16px 32px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.collect-card,.recent-card { border-radius: 16px; }
.card-header { font-size: 18px; font-weight: 600; color: #303133; }
.action-row { display: flex; align-items: center; gap: 12px; }
.success-text { color: #67c23a; font-size: 14px; }
.recent-list { display: flex; flex-direction: column; gap: 12px; }
.recent-item { padding: 14px 16px; border: 1px solid #ebeef5; border-radius: 12px; background: #fafafa; }
.recent-title { font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 8px; }
.recent-content { font-size: 14px; color: #606266; line-height: 1.7; white-space: pre-wrap; word-break: break-word; margin-bottom: 8px; }
.recent-meta { display: flex; gap: 12px; font-size: 12px; color: #909399; }
</style>
