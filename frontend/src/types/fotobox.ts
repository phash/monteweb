export type FotoboxPermissionLevel = 'VIEW_ONLY' | 'POST_IMAGES' | 'CREATE_THREADS'

import type { FileAudience } from './files'

export interface FotoboxThreadInfo {
  id: string
  roomId: string
  title: string
  description: string | null
  coverImageId: string | null
  coverImageThumbnailUrl: string | null
  imageCount: number
  audience: FileAudience
  createdBy: string
  createdByName: string
  createdAt: string
}

export interface FotoboxImageInfo {
  id: string
  threadId: string
  uploadedBy: string
  uploadedByName: string
  originalFilename: string
  imageUrl: string
  thumbnailUrl: string
  fileSize: number
  contentType: string
  width: number | null
  height: number | null
  caption: string | null
  sortOrder: number
  createdAt: string
}

export interface FotoboxRoomSettings {
  enabled: boolean
  defaultPermission: FotoboxPermissionLevel
  maxImagesPerThread: number | null
  maxFileSizeMb: number
}

export interface CreateFotoboxThreadRequest {
  title: string
  description?: string
  audience?: FileAudience
}
