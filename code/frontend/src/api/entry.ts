import http from './http'
import type { CreateEntryRequest, EntryResponse } from '@/types/entry'

export async function createEntry(data: CreateEntryRequest) {
  const response = await http.post<EntryResponse>('/entries', data)
  return response.data
}

export async function getRecentEntries() {
  const response = await http.get<EntryResponse[]>('/entries/recent')
  return response.data
}
