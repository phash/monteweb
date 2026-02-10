export interface FileInfo {
  id: string
  roomId: string
  folderId: string | null
  originalName: string
  contentType: string
  fileSize: number
  uploadedBy: string
  uploaderName: string
  createdAt: string
}

export interface FolderInfo {
  id: string
  roomId: string
  parentId: string | null
  name: string
  createdAt: string
}
