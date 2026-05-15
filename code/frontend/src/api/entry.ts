import http from './http'
import type {
  CollectTextPayload,
  CollectUrlPayload,
  CreateEntryRequest,
  EntryResponse,
  PendingEntryResponse,
  TopicResponse,
  PageResponse,
  UrlMetadataResponse
} from '@/types/entry'

export async function createEntry(data: CreateEntryRequest) {
  const response = await http.post<EntryResponse>('/entries', data)
  return response.data
}

export async function collectText(data: CollectTextPayload) {
  const response = await http.post<EntryResponse>('/entries/collect-text', data)
  return response.data
}

export async function collectUrl(data: CollectUrlPayload) {
  const response = await http.post<EntryResponse>('/entries/collect-url', data)
  return response.data
}

export async function extractUrlMetadata(url: string) {
  const response = await http.post<UrlMetadataResponse>(
    '/entries/extract-url',
    { url },
    { timeout: 60000 }
  )
  return response.data
}

const multipartTimeoutMs = 120000

export async function ocrImage(file: File) {
  const fd = new FormData()
  fd.append('file', file)
  const response = await http.post<{ text: string }>('/entries/ocr', fd, { timeout: multipartTimeoutMs })
  return response.data
}

export async function uploadImageEntry(params: {
  file: File
  insight: string
  sourceType?: string
  topicId?: string
}) {
  const fd = new FormData()
  fd.append('file', params.file)
  fd.append('insight', params.insight)
  if (params.sourceType) fd.append('sourceType', params.sourceType)
  if (params.topicId) fd.append('topicId', params.topicId)
  const response = await http.post<EntryResponse>('/entries/upload-image', fd, { timeout: multipartTimeoutMs })
  return response.data
}

export async function getRecentEntries() {
  const response = await http.get<EntryResponse[]>('/entries/recent')
  return response.data
}

export async function getEntry(entryId: string) {
  const response = await http.get<EntryResponse>(`/entries/${entryId}`)
  return response.data
}

export async function updateInsight(entryId: string, insightText: string) {
  const response = await http.patch<{ success: boolean }>(`/entries/${entryId}/insight`, { insightText })
  return response.data
}

export async function updateEntryTopic(entryId: string, topicId: string) {
  const response = await http.patch<{ success: boolean }>(`/entries/${entryId}/topic`, { topicId })
  return response.data
}

export async function getPendingEntries(offset = 0, limit = 20) {
  const response = await http.get<PageResponse<PendingEntryResponse>>('/entries/pending', {
    params: { offset, limit }
  })
  return response.data
}

export async function skipEntry(entryId: string) {
  const response = await http.post<{ success: boolean }>(`/entries/${entryId}/skip`)
  return response.data
}

export async function getTopicList(offset = 0, limit = 50) {
  const response = await http.get<PageResponse<TopicResponse>>('/topics', {
    params: { offset, limit }
  })
  return response.data
}

export async function searchTopics(keyword: string, limit = 10) {
  const response = await http.get<TopicResponse[]>('/topics/search', {
    params: { keyword, limit }
  })
  return response.data
}

export async function createTopic(name: string, description?: string) {
  const response = await http.post<TopicResponse>('/topics', { name, description })
  return response.data
}

export async function searchEntries(params: {
  keyword: string
  topicId?: string
  hasInsight?: boolean
  startDate?: string
  endDate?: string
  cursor?: string
  limit?: number
}) {
  const response = await http.get<PageResponse<EntryResponse>>('/entries/search', { params })
  return response.data
}

export async function recordReuse(entryId: string, reuseType: string) {
  const response = await http.post<{ success: boolean; totalCount: number }>(`/entries/${entryId}/reuse`, { reuseType })
  return response.data
}

export async function getReviewEntries(limit = 5) {
  const response = await http.get<EntryResponse[]>('/entries/review', { params: { limit } })
  return response.data
}

export async function getTopicDetail(topicId: string) {
  const response = await http.get<TopicResponse>(`/topics/${topicId}`)
  return response.data
}

export async function getTopicEntries(topicId: string, cursor?: string, limit = 20) {
  const response = await http.get<PageResponse<EntryResponse>>(`/topics/${topicId}/entries`, {
    params: { cursor, limit }
  })
  return response.data
}
