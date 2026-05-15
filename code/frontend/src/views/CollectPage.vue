<template>
  <div class="collect-page">
    <!-- 主输入区：方案 B 智能识别 -->
    <el-card v-if="pageState !== 'saved'" class="collect-card">
      <template #header>
        <div class="card-header">收集素材</div>
      </template>

      <!-- 初始：统一输入区 -->
      <div
        v-if="!contentDetected"
        class="smart-idle"
        :class="{ 'is-drag-over': dragOverIdle }"
        @dragenter.prevent="dragOverIdle = true"
        @dragover.prevent="dragOverIdle = true"
        @dragleave="onIdleDragLeave"
        @drop.prevent="onIdleDrop"
      >
        <div class="smart-drop-wrap">
          <textarea
            v-model="smartDraft"
            class="smart-input"
            rows="10"
            placeholder="粘贴文本、图片或链接..."
            @paste="handleSmartPaste"
            @keydown="handleSmartKeydown"
          />
        </div>
        <p class="drop-hint">将图片拖到此处也可上传</p>
        <input
          ref="fileInputRef"
          type="file"
          accept="image/jpeg,image/png,image/webp,image/gif"
          class="file-input-hidden"
          @change="handleHiddenFileChange"
        />
        <div class="upload-hint">
          或点击上传图片
          <span
            class="upload-trigger"
            role="button"
            tabindex="0"
            title="选择图片"
            @click="triggerFilePick"
            @keydown.enter.prevent="triggerFilePick"
            @keydown.space.prevent="triggerFilePick"
          >📷</span>
        </div>
        <div class="idle-actions">
          <el-button type="primary" :disabled="!smartDraft.trim()" @click="enterTextModeFromDraft">
            填写详情（文本）
          </el-button>
          <el-button :disabled="!isLikelyUrl(smartDraft)" @click="enterUrlModeFromDraft">
            识别为链接
          </el-button>
        </div>
        <p class="hint-line">
          支持拖拽或粘贴图片；粘贴以 http(s):// 开头的链接将自动识别并拉取网页信息；纯文本可先输入再点「填写详情」或使用 Ctrl/⌘ + Enter。
        </p>
      </div>

      <!-- 识别后：分类型表单 -->
      <div v-else class="editing-area">
        <div class="editing-toolbar">
          <span class="type-chip">{{ typeChipLabel }}</span>
          <el-button text type="primary" @click="resetDetection">← 重新输入</el-button>
        </div>

        <!-- 文本 -->
        <template v-if="contentType === 'text'">
          <div class="section-label">📝 文本</div>
          <el-input
            v-model="form.rawContent"
            type="textarea"
            :rows="8"
            placeholder="正文内容"
          />
          <el-form-item label="你的思考（可选）" class="mt-form">
            <el-input v-model="form.textInsight" type="textarea" :rows="2" placeholder="一句话思考" />
          </el-form-item>
        </template>

        <!-- 链接 -->
        <template v-else-if="contentType === 'url'">
          <div class="section-label">🔗 链接</div>
          <el-input v-model="urlForm.url" placeholder="https://..." clearable />
          <div class="url-actions">
            <el-button type="primary" plain :loading="urlMetaLoading" @click="fetchUrlMeta(false)">
              重新拉取网页信息
            </el-button>
          </div>
          <div v-if="urlMetaLoading" class="url-loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>正在获取网页信息...</span>
          </div>
          <div v-else-if="urlPreview" class="url-preview">
            <div class="url-preview-title">{{ urlPreview.title || '（无标题）' }}</div>
            <p class="url-preview-desc">{{ urlPreview.description || '（无描述）' }}</p>
            <p v-if="urlPreview.extractedText" class="url-preview-text">{{ urlPreview.extractedText }}</p>
          </div>
          <el-alert
            v-else-if="urlExtractFailed"
            type="warning"
            :closable="false"
            title="无法获取网页信息"
            description="仍可填写思考后保存，服务器会再次尝试提取元数据。"
            show-icon
            class="mt-alert"
          />
          <el-form-item label="你的思考（必填）" required class="mt-form">
            <el-input
              v-model="urlForm.insight"
              type="textarea"
              :rows="3"
              placeholder="这篇文章/页面对你意味着什么？"
            />
          </el-form-item>
        </template>

        <!-- 图片 -->
        <template v-else>
          <div class="section-label">📷 图片</div>
          <div
            class="img-stage"
            :class="{ 'is-drag-over': dragOverImage }"
            @dragenter.prevent="dragOverImage = true"
            @dragover.prevent="dragOverImage = true"
            @dragleave="onImageDragLeave"
            @drop.prevent="onImageDrop"
          >
            <div v-if="imageObjectUrl" class="img-preview-wrap">
              <img :src="imageObjectUrl" alt="预览" class="img-preview" />
              <el-button text type="primary" @click="triggerFilePick">更换图片</el-button>
            </div>
            <div v-else class="img-placeholder">
              <span>将图片拖到此处，或点击下方选择文件</span>
              <el-button type="primary" plain @click="triggerFilePick">选择图片</el-button>
            </div>
          </div>
          <div v-if="ocrLoading" class="ocr-loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>正在识别文字...</span>
          </div>
          <template v-else>
            <el-alert
              v-if="ocrFailed"
              type="warning"
              :closable="false"
              title="文字识别失败"
              description="请直接在下方思考中描述图片要点，仍可保存。"
              show-icon
              class="mt-alert"
            />
            <el-form-item v-if="imageOcrPreview && !ocrFailed" label="识别到的文字" class="mt-form">
              <el-input v-model="imageOcrPreview" type="textarea" :rows="4" readonly />
            </el-form-item>
          </template>
          <el-form-item label="这张图说明了什么？（必填）" required class="mt-form">
            <el-input
              v-model="imageForm.insight"
              type="textarea"
              :rows="3"
              placeholder="例如：竞品登录流程、会议白板要点…"
            />
          </el-form-item>
          <el-button :disabled="!imageFile" :loading="ocrLoading" @click="runOcrPreview">
            重新识别文字
          </el-button>
        </template>

        <el-collapse v-model="moreOpen" class="more-collapse">
          <el-collapse-item title="⚙️ 更多选项" name="more">
            <el-form label-position="top">
              <el-form-item v-if="contentType === 'text'" label="来源类型">
                <el-select v-model="form.sourceType" clearable placeholder="可选" style="width: 100%">
                  <el-option label="书籍" value="书籍" />
                  <el-option label="网页" value="网页" />
                  <el-option label="对话" value="对话" />
                  <el-option label="手工录入" value="手工录入" />
                  <el-option label="其他" value="其他" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="contentType === 'text'" label="来源标题">
                <el-input v-model="form.sourceTitle" placeholder="书名 / 标题 / 会话名" />
              </el-form-item>
              <el-form-item v-if="contentType === 'text' && isWebSource(form.sourceType)" label="来源链接">
                <el-input v-model="form.sourceLink" placeholder="https://..." />
              </el-form-item>
              <el-form-item v-if="contentType === 'url'" label="来源类型">
                <el-select v-model="urlForm.sourceType" clearable placeholder="可选" style="width: 100%">
                  <el-option label="网页" value="网页" />
                  <el-option label="文章" value="文章" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="contentType === 'image'" label="来源类型">
                <el-select v-model="imageForm.sourceType" clearable placeholder="可选" style="width: 100%">
                  <el-option label="截图" value="截图" />
                  <el-option label="照片" value="照片" />
                </el-select>
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
                  style="width: 100%"
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
            </el-form>
          </el-collapse-item>
        </el-collapse>

        <div class="action-row">
          <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
        </div>
      </div>
    </el-card>

    <!-- 保存后反馈区 -->
    <el-card v-if="pageState === 'saved'" class="feedback-card">
      <div class="feedback-header">
        <el-icon :size="24" color="#67c23a"><CircleCheckFilled /></el-icon>
        <span>已保存</span>
      </div>

      <div class="feedback-preview">{{ savedPreview }}</div>

      <div class="feedback-actions">
        <el-button type="primary" size="large" @click="handleContinueCollect">
          继续记录
        </el-button>
        <el-button size="large" @click="showInsightInput = true" :disabled="showInsightInput">
          补一句思考
        </el-button>
      </div>

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

      <div class="more-actions">
        <el-button text type="info" @click="showTopicSelect = !showTopicSelect">
          {{ showTopicSelect ? '收起' : '更多处理' }}
        </el-button>
      </div>

      <div v-if="showTopicSelect" class="topic-section">
        <el-select
          v-model="selectedTopicId"
          filterable
          clearable
          placeholder="选择专题"
          :loading="topicLoading"
          style="width: 100%"
          @focus="loadTopicOptions"
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
          <div class="recent-title">{{ recentTitle(entry) }}</div>
          <div class="recent-content">{{ recentBody(entry) }}</div>
          <div v-if="entry.insightText" class="recent-insight">
            💡 {{ entry.insightText }}
          </div>
          <div class="recent-meta">
            <el-tag v-if="entry.topicName" size="small" type="info">{{ entry.topicName }}</el-tag>
            <span v-else class="meta-text">未归类</span>
            <span class="meta-text">{{ formatTime(entry.capturedAt) }}</span>
          </div>

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

          <div v-if="recentTopicEditing === entry.entryId" class="recent-inline-edit">
            <el-select
              v-model="recentTopicId"
              filterable
              allow-create
              clearable
              default-first-option
              placeholder="选择或输入专题名"
              :loading="topicLoading"
              style="width: 100%"
              @focus="loadTopicOptions"
            >
              <el-option v-for="t in topicOptions" :key="t.topicId" :label="t.name" :value="t.topicId" />
            </el-select>
            <div class="recent-inline-actions">
              <el-button type="primary" size="small" :disabled="!recentTopicId" :loading="recentSaving" @click="handleRecentSetTopic(entry.entryId)">保存</el-button>
              <el-button size="small" @click="recentTopicEditing = ''">取消</el-button>
            </div>
          </div>

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

    <!-- 值得回看 -->
    <el-card v-if="reviewEntries.length > 0" class="review-card">
      <template #header>
        <span class="card-header">值得回看</span>
      </template>
      <p class="review-desc">这些内容你已经多次取用，可能值得再看一眼</p>
      <div class="review-list">
        <div v-for="entry in reviewEntries" :key="entry.entryId" class="review-item">
          <div class="recent-title">{{ recentTitle(entry) }}</div>
          <div class="review-content">{{ reviewBody(entry) }}</div>
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
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled, Loading } from '@element-plus/icons-vue'
import {
  collectText,
  collectUrl,
  createEntry,
  createTopic,
  extractUrlMetadata,
  getPendingEntries,
  getRecentEntries,
  getReviewEntries,
  getTopicList,
  ocrImage,
  updateEntryTopic,
  updateInsight,
  uploadImageEntry
} from '@/api/entry'
import type { EntryResponse, TopicResponse, UrlMetadataResponse } from '@/types/entry'

type PageState = 'input' | 'saved'
type ContentKind = 'text' | 'url' | 'image'

const URL_LINE = /^https?:\/\/.+/i

function isWebSource(s?: string | null) {
  if (s == null || !String(s).trim()) return false
  const v = String(s).trim()
  const up = v.toUpperCase()
  return v === '网页' || up === 'WEB'
}

const pageState = ref<PageState>('input')
const contentDetected = ref(false)
const contentType = ref<ContentKind | null>(null)
const smartDraft = ref('')
const moreOpen = ref<string[]>([])
const fileInputRef = ref<HTMLInputElement | null>(null)

const submitting = ref(false)
const savedEntry = ref<EntryResponse | null>(null)
const recentEntries = ref<EntryResponse[]>([])
const pendingCount = ref(0)
const reviewEntries = ref<EntryResponse[]>([])

const form = ref({
  rawContent: '',
  textInsight: '',
  sourceType: '',
  sourceTitle: '',
  sourceLink: '',
  topicName: ''
})

const urlForm = ref({
  url: '',
  insight: '',
  sourceType: '网页'
})
const urlPreview = ref<UrlMetadataResponse | null>(null)
const urlMetaLoading = ref(false)
const urlExtractFailed = ref(false)

const imageForm = ref({
  insight: '',
  sourceType: ''
})
const imageFile = ref<File | undefined>(undefined)
const imageObjectUrl = ref('')
const imageOcrPreview = ref('')
const ocrLoading = ref(false)
const ocrFailed = ref(false)

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

const dragOverIdle = ref(false)
const dragOverImage = ref(false)

watch(
  () => form.value.sourceType,
  (v) => {
    if (!isWebSource(v)) form.value.sourceLink = ''
  }
)

const typeChipLabel = computed(() => {
  if (contentType.value === 'text') return '📝 文本'
  if (contentType.value === 'url') return '🔗 链接'
  if (contentType.value === 'image') return '📷 图片'
  return ''
})

const savedPreview = computed(() => {
  const e = savedEntry.value
  if (!e) return ''
  if (e.contentType === 'url') {
    const parts = [e.urlTitle, e.urlDescription, e.rawContent].filter((x) => !!x && String(x).trim())
    return parts.join('\n\n').slice(0, 4000)
  }
  if (e.contentType === 'image') {
    const t = [e.imageOcrText, e.insightText].filter((x) => !!x && String(x).trim())
    return t.join('\n\n') || e.rawContent || ''
  }
  return e.rawContent || ''
})

function recentTitle(e: EntryResponse) {
  if (e.contentType === 'url' && e.urlTitle) return e.urlTitle
  return e.sourceTitle || '未命名来源'
}

function recentBody(e: EntryResponse) {
  if (e.contentType === 'url') return e.url || e.rawContent || ''
  if (e.contentType === 'image') return e.imageOcrText || e.rawContent || ''
  return e.rawContent || ''
}

function reviewBody(e: EntryResponse) {
  return recentBody(e)
}

function isLikelyUrl(s: string) {
  const t = s.trim()
  return t.length > 0 && URL_LINE.test(t) && !t.includes('\n')
}

function revokeImagePreview() {
  if (imageObjectUrl.value) {
    URL.revokeObjectURL(imageObjectUrl.value)
    imageObjectUrl.value = ''
  }
}

function resetDetection() {
  contentDetected.value = false
  contentType.value = null
  smartDraft.value = ''
  revokeImagePreview()
  imageFile.value = undefined
  imageOcrPreview.value = ''
  ocrFailed.value = false
  ocrLoading.value = false
  urlPreview.value = null
  urlExtractFailed.value = false
  urlMetaLoading.value = false
  form.value = {
    rawContent: '',
    textInsight: '',
    sourceType: '',
    sourceTitle: '',
    sourceLink: '',
    topicName: ''
  }
  urlForm.value = { url: '', insight: '', sourceType: '网页' }
  imageForm.value = { insight: '', sourceType: '' }
  moreOpen.value = []
  dragOverIdle.value = false
  dragOverImage.value = false
}

function enterTextMode(text: string) {
  contentDetected.value = true
  contentType.value = 'text'
  form.value.rawContent = text
  form.value.textInsight = ''
  smartDraft.value = ''
}

function enterTextModeFromDraft() {
  const t = smartDraft.value.trim()
  if (!t) {
    ElMessage.warning('请先输入内容')
    return
  }
  enterTextMode(t)
}

async function enterUrlMode(url: string, silent = false) {
  contentDetected.value = true
  contentType.value = 'url'
  urlForm.value.url = url
  urlForm.value.insight = ''
  urlForm.value.sourceType = urlForm.value.sourceType || '网页'
  urlPreview.value = null
  urlExtractFailed.value = false
  smartDraft.value = ''
  await fetchUrlMeta(silent)
}

async function enterUrlModeFromDraft() {
  const u = smartDraft.value.trim()
  if (!isLikelyUrl(u)) {
    ElMessage.warning('当前内容不是单行链接')
    return
  }
  await enterUrlMode(u, false)
}

async function enterImageMode(file: File) {
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请使用图片文件')
    return
  }
  const ok = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'].includes(file.type)
  if (!ok) {
    ElMessage.warning('仅支持 JPEG、PNG、WebP、GIF')
    return
  }
  revokeImagePreview()
  imageFile.value = file
  imageObjectUrl.value = URL.createObjectURL(file)
  contentDetected.value = true
  contentType.value = 'image'
  imageForm.value.insight = ''
  imageOcrPreview.value = ''
  ocrFailed.value = false
  smartDraft.value = ''
  await runOcrPreview()
}

async function handleSmartPaste(e: ClipboardEvent) {
  const items = e.clipboardData?.items
  if (items) {
    for (let i = 0; i < items.length; i++) {
      const item = items[i]
      if (item.kind === 'file' && item.type.startsWith('image/')) {
        e.preventDefault()
        const f = item.getAsFile()
        if (f) await enterImageMode(f)
        return
      }
    }
  }
  const text = e.clipboardData?.getData('text') ?? ''
  const t = text.trim()
  if (URL_LINE.test(t) && !t.includes('\n')) {
    e.preventDefault()
    await enterUrlMode(t, true)
    return
  }
  if (text) {
    enterTextMode(text)
  }
}

function handleSmartDrop(e: DragEvent) {
  const files = e.dataTransfer?.files
  if (files && files.length > 0 && files[0].type.startsWith('image/')) {
    void enterImageMode(files[0])
  }
}

function onIdleDragLeave(e: DragEvent) {
  const cur = e.currentTarget as HTMLElement | null
  const rel = e.relatedTarget as Node | null
  if (cur && rel && cur.contains(rel)) return
  dragOverIdle.value = false
}

function onIdleDrop(e: DragEvent) {
  dragOverIdle.value = false
  handleSmartDrop(e)
}

function onImageDragLeave(e: DragEvent) {
  const cur = e.currentTarget as HTMLElement | null
  const rel = e.relatedTarget as Node | null
  if (cur && rel && cur.contains(rel)) return
  dragOverImage.value = false
}

function onImageDrop(e: DragEvent) {
  dragOverImage.value = false
  const files = e.dataTransfer?.files
  if (files && files.length > 0 && files[0].type.startsWith('image/')) {
    void enterImageMode(files[0])
  }
}

function handleHiddenFileChange(ev: Event) {
  const input = ev.target as HTMLInputElement
  const f = input.files?.[0]
  input.value = ''
  if (f) void enterImageMode(f)
}

function triggerFilePick() {
  fileInputRef.value?.click()
}

function handleSmartKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    if (contentDetected.value) {
      resetDetection()
    }
    return
  }
  if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
    e.preventDefault()
    if (contentDetected.value) {
      void handleSubmit()
    } else if (smartDraft.value.trim()) {
      enterTextModeFromDraft()
    }
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

async function resolveTopicIdFromName(name?: string): Promise<string | undefined> {
  const n = name?.trim()
  if (!n) return undefined
  const existing = topicOptions.value.find((t) => t.name === n)
  if (existing) return existing.topicId
  const created = await createTopic(n)
  topicOptions.value.push(created)
  return created.topicId
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

async function fetchUrlMeta(silent: boolean) {
  const u = urlForm.value.url.trim()
  if (!u) {
    if (!silent) ElMessage.warning('请先填写网页地址')
    return
  }
  urlMetaLoading.value = true
  urlExtractFailed.value = false
  try {
    urlPreview.value = await extractUrlMetadata(u)
    if (!silent) ElMessage.success('已获取网页信息')
  } catch {
    urlPreview.value = null
    urlExtractFailed.value = true
    if (!silent) ElMessage.error('拉取网页信息失败')
  } finally {
    urlMetaLoading.value = false
  }
}

async function runOcrPreview() {
  if (!imageFile.value) return
  ocrLoading.value = true
  ocrFailed.value = false
  try {
    const r = await ocrImage(imageFile.value)
    imageOcrPreview.value = r.text || ''
  } catch {
    imageOcrPreview.value = ''
    ocrFailed.value = true
  } finally {
    ocrLoading.value = false
  }
}

async function handleSubmit() {
  submitting.value = true
  try {
    const topicId = await resolveTopicIdFromName(form.value.topicName)

    if (contentType.value === 'text') {
      if (!form.value.rawContent.trim()) {
        ElMessage.warning('请先输入正文')
        return
      }
      const hasStructuredSource = !!(
        form.value.sourceTitle?.trim() ||
        (isWebSource(form.value.sourceType) && form.value.sourceLink?.trim())
      )

      if (hasStructuredSource) {
        const result = await createEntry({
          rawContent: form.value.rawContent.trim(),
          contentType: 'text',
          sourceType: form.value.sourceType || 'MANUAL',
          sourceTitle: form.value.sourceTitle?.trim() || '手工录入',
          sourceLink: form.value.sourceLink || undefined,
          topicName: form.value.topicName || undefined
        })
        savedEntry.value = result
      } else {
        const result = await collectText({
          rawContent: form.value.rawContent.trim(),
          insight: form.value.textInsight.trim() || undefined,
          sourceType: form.value.sourceType || undefined,
          topicId
        })
        savedEntry.value = result
      }
    } else if (contentType.value === 'url') {
      const u = urlForm.value.url.trim()
      if (!u) {
        ElMessage.warning('请填写链接地址')
        return
      }
      if (!urlForm.value.insight.trim()) {
        ElMessage.warning('请填写你的思考')
        return
      }
      const result = await collectUrl({
        url: u,
        insight: urlForm.value.insight.trim(),
        sourceType: urlForm.value.sourceType || undefined,
        topicId
      })
      savedEntry.value = result
    } else if (contentType.value === 'image') {
      if (!imageFile.value) {
        ElMessage.warning('请先选择图片')
        return
      }
      if (!imageForm.value.insight.trim()) {
        ElMessage.warning('请填写这张图说明了什么')
        return
      }
      const result = await uploadImageEntry({
        file: imageFile.value,
        insight: imageForm.value.insight.trim(),
        sourceType: imageForm.value.sourceType || undefined,
        topicId
      })
      savedEntry.value = result
    } else {
      return
    }

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
  resetDetection()
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
  max-width: 800px;
  margin: 0 auto;
  padding: 24px 16px 48px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.collect-card,
.recent-card,
.feedback-card {
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

/* 方案 B：智能输入 */
.smart-idle {
  padding: 12px;
  border-radius: 12px;
  border: 2px dashed transparent;
  transition: border-color 0.2s, background 0.2s;
}
.smart-idle.is-drag-over {
  border-color: #409eff;
  background: #ecf5ff;
}
.smart-drop-wrap {
  width: 100%;
}
.smart-input {
  width: 100%;
  min-height: 200px;
  padding: 16px;
  border: 2px dashed #dcdfe6;
  border-radius: 12px;
  font-size: 16px;
  line-height: 1.6;
  resize: vertical;
  font-family: inherit;
  box-sizing: border-box;
  transition: border-color 0.2s, border-style 0.2s;
}
.smart-input:focus {
  outline: none;
  border-color: #409eff;
  border-style: solid;
}
.upload-hint {
  margin-top: 16px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}
.upload-trigger {
  cursor: pointer;
  font-size: 20px;
  margin-left: 4px;
  user-select: none;
}
.drop-hint {
  margin: 8px 0 0;
  font-size: 13px;
  color: #909399;
  text-align: center;
}
.file-input-hidden {
  display: none;
}
.idle-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}
.hint-line {
  margin: 12px 0 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.editing-area {
  padding-top: 4px;
}
.editing-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.type-chip {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.section-label {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 10px;
  color: #303133;
}
.mt-form {
  margin-top: 12px;
}
.mt-alert {
  margin-top: 12px;
}
.url-actions {
  margin-top: 10px;
}
.url-loading,
.ocr-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  color: #606266;
  font-size: 14px;
}
.url-preview {
  padding: 12px 14px;
  margin-top: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}
.url-preview-title {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
  margin-bottom: 8px;
}
.url-preview-desc,
.url-preview-text {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 8px;
}
.url-preview-text {
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 160px;
  overflow: auto;
}
.img-stage {
  min-height: 160px;
  padding: 16px;
  border: 2px dashed #dcdfe6;
  border-radius: 12px;
  margin-bottom: 12px;
  transition: border-color 0.2s, background 0.2s;
}
.img-stage.is-drag-over {
  border-color: #409eff;
  background: #ecf5ff;
}
.img-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 140px;
  color: #909399;
  font-size: 14px;
  text-align: center;
}
.img-preview-wrap {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}
.img-preview {
  max-width: 100%;
  max-height: 280px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}
.more-collapse {
  margin-top: 16px;
  border: none;
}
.more-collapse :deep(.el-collapse-item__header) {
  font-size: 14px;
  color: #606266;
}
.action-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
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
  max-height: 200px;
  overflow: auto;
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
