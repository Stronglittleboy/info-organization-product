<template>
  <div class="topic-list-page">
    <h2 class="page-title">专题</h2>
    <p class="page-desc">围绕持续问题积累的内容</p>

    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="28"><Loading /></el-icon>
    </div>

    <el-empty v-else-if="topics.length === 0" description="还没有专题，在收集时创建你的第一个专题" />

    <div v-else class="topic-grid">
      <router-link
        v-for="t in topics"
        :key="t.topicId"
        :to="`/topic/${t.topicId}`"
        class="topic-card"
      >
        <div class="topic-name">{{ t.name }}</div>
        <div class="topic-count">{{ t.entryCount }} 条素材</div>
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { getTopicList } from '@/api/entry'
import type { TopicResponse } from '@/types/entry'

const topics = ref<TopicResponse[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await getTopicList(0, 100)
    topics.value = res.items
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.topic-list-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 4px;
}
.page-desc {
  font-size: 14px;
  color: #909399;
  margin: 0 0 24px;
}
.loading-state {
  text-align: center;
  padding: 48px 0;
  color: #909399;
}
.topic-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 14px;
}
.topic-card {
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fff;
  text-decoration: none;
  transition: box-shadow .2s, border-color .2s;
}
.topic-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, .1);
}
.topic-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}
.topic-count {
  font-size: 13px;
  color: #909399;
}
</style>
