import client from './client'
import type { ApiResponse } from '@/types/api'
import type {
  FotoboxThreadInfo,
  FotoboxImageInfo,
  FotoboxRoomSettings,
  CreateFotoboxThreadRequest,
} from '@/types/fotobox'
import { authenticatedImageUrl } from '@/composables/useImageToken'

export const fotoboxApi = {
  // Settings
  getSettings(roomId: string) {
    return client.get<ApiResponse<FotoboxRoomSettings>>(`/rooms/${roomId}/fotobox/settings`)
  },
  updateSettings(roomId: string, settings: Partial<FotoboxRoomSettings>) {
    return client.put<ApiResponse<FotoboxRoomSettings>>(`/rooms/${roomId}/fotobox/settings`, settings)
  },

  // Threads
  getThreads(roomId: string) {
    return client.get<ApiResponse<FotoboxThreadInfo[]>>(`/rooms/${roomId}/fotobox/threads`)
  },
  getThread(roomId: string, threadId: string) {
    return client.get<ApiResponse<FotoboxThreadInfo>>(`/rooms/${roomId}/fotobox/threads/${threadId}`)
  },
  getThreadImages(roomId: string, threadId: string) {
    return client.get<ApiResponse<FotoboxImageInfo[]>>(
      `/rooms/${roomId}/fotobox/threads/${threadId}/images`,
    )
  },
  createThread(roomId: string, data: CreateFotoboxThreadRequest) {
    return client.post<ApiResponse<FotoboxThreadInfo>>(`/rooms/${roomId}/fotobox/threads`, data)
  },
  updateThread(roomId: string, threadId: string, data: Partial<CreateFotoboxThreadRequest>) {
    return client.put<ApiResponse<FotoboxThreadInfo>>(
      `/rooms/${roomId}/fotobox/threads/${threadId}`,
      data,
    )
  },
  deleteThread(roomId: string, threadId: string) {
    return client.delete<ApiResponse<void>>(`/rooms/${roomId}/fotobox/threads/${threadId}`)
  },

  // Images
  uploadImages(roomId: string, threadId: string, files: File[], caption?: string) {
    const formData = new FormData()
    files.forEach((f) => formData.append('files', f))
    if (caption) formData.append('caption', caption)
    return client.post<ApiResponse<FotoboxImageInfo[]>>(
      `/rooms/${roomId}/fotobox/threads/${threadId}/images`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } },
    )
  },
  updateImage(imageId: string, data: { caption?: string; sortOrder?: number }) {
    return client.put<ApiResponse<FotoboxImageInfo>>(`/fotobox/images/${imageId}`, data)
  },
  deleteImage(imageId: string) {
    return client.delete<ApiResponse<void>>(`/fotobox/images/${imageId}`)
  },

  // Image URL helpers — uses short-lived image token for <img> tags
  imageUrl(imageId: string) {
    return authenticatedImageUrl(`/api/v1/fotobox/images/${imageId}`)
  },
  thumbnailUrl(imageId: string) {
    return authenticatedImageUrl(`/api/v1/fotobox/images/${imageId}/thumbnail`)
  },
}
