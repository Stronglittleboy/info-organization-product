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
