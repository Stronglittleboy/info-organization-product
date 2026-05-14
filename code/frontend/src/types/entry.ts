export interface CreateEntryRequest {
  rawContent: string
  contentType?: string
  sourceType?: string
  sourceTitle?: string
  sourceLink?: string
  topicName?: string
}

export interface EntryResponse {
  entryId: string
  rawContent: string
  contentType?: string
  sourceType?: string
  sourceTitle?: string
  sourceLink?: string
  capturedAt?: string
  insightText?: string
  topicId?: string
  topicName?: string
}

export interface PendingEntryResponse {
  entryId: string
  contentPreview: string
  sourceType?: string
  capturedAt?: string
  currentSuggestedAction: 'ADD_INSIGHT' | 'ADD_TOPIC'
  insightText?: string
  topicId?: string
  topicName?: string
}

export interface TopicResponse {
  topicId: string
  name: string
  description?: string
  entryCount: number
}

export interface PageResponse<T> {
  items: T[]
  total: number
  hasMore: boolean
}
