import client from './client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type {
  ParentLetterInfo,
  ParentLetterDetailInfo,
  ParentLetterConfigInfo,
  ParentLetterAttachmentInfo,
  ParentLetterStatsInfo,
  CreateParentLetterRequest,
  UpdateParentLetterRequest,
  UpdateParentLetterConfigRequest,
} from '@/types/parentletter'

export const parentLetterApi = {
  // Teacher/Admin endpoints
  getMyLetters(page = 0, size = 20) {
    return client.get<ApiResponse<PageResponse<ParentLetterInfo>>>('/parent-letters/my', {
      params: { page, size },
    })
  },

  getLetter(id: string) {
    return client.get<ApiResponse<ParentLetterDetailInfo>>(`/parent-letters/${id}`)
  },

  createLetter(data: CreateParentLetterRequest) {
    return client.post<ApiResponse<ParentLetterDetailInfo>>('/parent-letters', data)
  },

  updateLetter(id: string, data: UpdateParentLetterRequest) {
    return client.put<ApiResponse<ParentLetterDetailInfo>>(`/parent-letters/${id}`, data)
  },

  sendLetter(id: string) {
    return client.post<ApiResponse<ParentLetterDetailInfo>>(`/parent-letters/${id}/send`)
  },

  closeLetter(id: string) {
    return client.post<ApiResponse<ParentLetterDetailInfo>>(`/parent-letters/${id}/close`)
  },

  deleteLetter(id: string) {
    return client.delete<ApiResponse<void>>(`/parent-letters/${id}`)
  },

  // Parent endpoints
  getInbox(page = 0, size = 20) {
    return client.get<ApiResponse<PageResponse<ParentLetterInfo>>>('/parent-letters/inbox', {
      params: { page, size },
    })
  },

  confirmLetter(id: string, studentId: string) {
    return client.post<ApiResponse<void>>(`/parent-letters/${id}/confirm/${studentId}`)
  },

  markAsRead(id: string) {
    return client.post<ApiResponse<void>>(`/parent-letters/${id}/read`)
  },

  // Stats
  getStats() {
    return client.get<ApiResponse<ParentLetterStatsInfo>>('/parent-letters/stats')
  },

  // Attachments
  uploadAttachments(letterId: string, files: File[]) {
    const formData = new FormData()
    files.forEach(f => formData.append('files', f))
    return client.post<ApiResponse<ParentLetterAttachmentInfo[]>>(
      `/parent-letters/${letterId}/attachments`, formData,
      { headers: { 'Content-Type': 'multipart/form-data' } },
    )
  },

  getAttachments(letterId: string) {
    return client.get<ApiResponse<ParentLetterAttachmentInfo[]>>(
      `/parent-letters/${letterId}/attachments`,
    )
  },

  deleteAttachment(letterId: string, attachmentId: string) {
    return client.delete<ApiResponse<void>>(
      `/parent-letters/${letterId}/attachments/${attachmentId}`,
    )
  },

  getAttachmentDownloadUrl(letterId: string, attachmentId: string) {
    return `/api/v1/parent-letters/${letterId}/attachments/${attachmentId}`
  },

  // PDF
  downloadLetterPdf(id: string, studentId?: string) {
    const params = studentId ? { studentId } : {}
    return client.get(`/parent-letters/${id}/pdf`, { responseType: 'blob', params })
  },

  downloadTrackingPdf(id: string) {
    return client.get(`/parent-letters/${id}/tracking-pdf`, { responseType: 'blob' })
  },

  // Config
  getConfig(sectionId?: string) {
    return client.get<ApiResponse<ParentLetterConfigInfo>>('/parent-letter-config', {
      params: sectionId ? { sectionId } : {},
    })
  },

  updateConfig(data: UpdateParentLetterConfigRequest, sectionId?: string) {
    return client.put<ApiResponse<ParentLetterConfigInfo>>('/parent-letter-config', data, {
      params: sectionId ? { sectionId } : {},
    })
  },

  uploadLetterhead(file: File, sectionId?: string) {
    const formData = new FormData()
    formData.append('file', file)
    if (sectionId) formData.append('sectionId', sectionId)
    return client.post<ApiResponse<ParentLetterConfigInfo>>('/parent-letter-config/letterhead', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  deleteLetterhead(sectionId?: string) {
    return client.delete<ApiResponse<void>>('/parent-letter-config/letterhead', {
      params: sectionId ? { sectionId } : {},
    })
  },
}
