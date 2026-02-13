import client from './client'
import type { ApiResponse } from '@/types/api'
import type { FileInfo, FolderInfo } from '@/types/files'

export const filesApi = {
  listFiles(roomId: string, folderId?: string) {
    return client.get<ApiResponse<FileInfo[]>>(`/rooms/${roomId}/files`, {
      params: folderId ? { folderId } : undefined,
    })
  },

  uploadFile(roomId: string, file: File, folderId?: string, audience?: string) {
    const formData = new FormData()
    formData.append('file', file)
    if (folderId) formData.append('folderId', folderId)
    if (audience) formData.append('audience', audience)
    return client.post<ApiResponse<FileInfo>>(`/rooms/${roomId}/files`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  downloadFile(roomId: string, fileId: string) {
    return client.get(`/rooms/${roomId}/files/${fileId}`, {
      responseType: 'blob',
    })
  },

  deleteFile(roomId: string, fileId: string) {
    return client.delete<ApiResponse<void>>(`/rooms/${roomId}/files/${fileId}`)
  },

  listFolders(roomId: string, parentId?: string) {
    return client.get<ApiResponse<FolderInfo[]>>(`/rooms/${roomId}/files/folders`, {
      params: parentId ? { parentId } : undefined,
    })
  },

  createFolder(roomId: string, name: string, parentId?: string, audience?: string) {
    return client.post<ApiResponse<FolderInfo>>(`/rooms/${roomId}/files/folders`, {
      name,
      parentId: parentId ?? null,
      audience: audience ?? null,
    })
  },

  deleteFolder(roomId: string, folderId: string) {
    return client.delete<ApiResponse<void>>(`/rooms/${roomId}/files/folders/${folderId}`)
  },
}
