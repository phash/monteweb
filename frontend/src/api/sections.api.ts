import client from './client'
import type { ApiResponse } from '@/types/api'
import type { SchoolSectionInfo } from '@/types/family'

export const sectionsApi = {
  getAll() {
    return client.get<ApiResponse<SchoolSectionInfo[]>>('/sections')
  },

  create(data: { name: string; description?: string; sortOrder: number }) {
    return client.post<ApiResponse<SchoolSectionInfo>>('/sections', data)
  },

  update(id: string, data: { name?: string; description?: string; sortOrder?: number }) {
    return client.put<ApiResponse<SchoolSectionInfo>>(`/sections/${id}`, data)
  },

  deactivate(id: string) {
    return client.delete<ApiResponse<void>>(`/sections/${id}`)
  },
}
