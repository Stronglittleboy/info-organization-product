import http from './http'
import type {
  CreateEntryRequest,
  EntryResponse,
  PendingEntryResponse,
  TopicResponse,
  PageResponse
} from '@/types/entry'

export async function createEntry(data: CreateEntryRequest) {
  const response = await http.post<EntryResponse>('/entries', data)
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
