import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('../client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: { data: {} } }),
    post: vi.fn().mockResolvedValue({ data: { data: {} } }),
    put: vi.fn().mockResolvedValue({ data: { data: {} } }),
    delete: vi.fn().mockResolvedValue({ data: { data: null } }),
  },
}))

import client from '../client'
import { filesApi } from '../files.api'

describe('filesApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('listFiles', () => {
    it('should GET /rooms/{roomId}/files', async () => {
      await filesApi.listFiles('room-1')
      expect(client.get).toHaveBeenCalledWith('/rooms/room-1/files', {
        params: undefined,
      })
    })

    it('should pass folderId when provided', async () => {
      await filesApi.listFiles('room-1', 'folder-1')
      expect(client.get).toHaveBeenCalledWith('/rooms/room-1/files', {
        params: { folderId: 'folder-1' },
      })
    })
  })

  describe('uploadFile', () => {
    it('should POST FormData to /rooms/{roomId}/files', async () => {
      const file = new File(['content'], 'doc.pdf', { type: 'application/pdf' })
      await filesApi.uploadFile('room-1', file)

      const [url, formData, config] = vi.mocked(client.post).mock.calls[0]
      expect(url).toBe('/rooms/room-1/files')
      expect(formData).toBeInstanceOf(FormData)
      expect(config).toEqual({ headers: { 'Content-Type': 'multipart/form-data' } })
    })

    it('should include folderId when provided', async () => {
      const file = new File(['content'], 'doc.pdf', { type: 'application/pdf' })
      await filesApi.uploadFile('room-1', file, 'folder-1')

      const formData = vi.mocked(client.post).mock.calls[0][1] as FormData
      expect(formData.get('folderId')).toBe('folder-1')
    })
  })

  describe('downloadFile', () => {
    it('should GET /rooms/{roomId}/files/{fileId} as blob', async () => {
      await filesApi.downloadFile('room-1', 'file-1')
      expect(client.get).toHaveBeenCalledWith('/rooms/room-1/files/file-1', {
        responseType: 'blob',
      })
    })
  })

  describe('deleteFile', () => {
    it('should DELETE /rooms/{roomId}/files/{fileId}', async () => {
      await filesApi.deleteFile('room-1', 'file-1')
      expect(client.delete).toHaveBeenCalledWith('/rooms/room-1/files/file-1')
    })
  })

  describe('listFolders', () => {
    it('should GET /rooms/{roomId}/files/folders', async () => {
      await filesApi.listFolders('room-1')
      expect(client.get).toHaveBeenCalledWith('/rooms/room-1/files/folders', {
        params: undefined,
      })
    })

    it('should pass parentId when provided', async () => {
      await filesApi.listFolders('room-1', 'parent-1')
      expect(client.get).toHaveBeenCalledWith('/rooms/room-1/files/folders', {
        params: { parentId: 'parent-1' },
      })
    })
  })

  describe('createFolder', () => {
    it('should POST /rooms/{roomId}/files/folders', async () => {
      await filesApi.createFolder('room-1', 'Documents')
      expect(client.post).toHaveBeenCalledWith('/rooms/room-1/files/folders', {
        name: 'Documents',
        parentId: null,
      })
    })

    it('should pass parentId when provided', async () => {
      await filesApi.createFolder('room-1', 'Sub', 'parent-1')
      expect(client.post).toHaveBeenCalledWith('/rooms/room-1/files/folders', {
        name: 'Sub',
        parentId: 'parent-1',
      })
    })
  })

  describe('deleteFolder', () => {
    it('should DELETE /rooms/{roomId}/files/folders/{folderId}', async () => {
      await filesApi.deleteFolder('room-1', 'folder-1')
      expect(client.delete).toHaveBeenCalledWith('/rooms/room-1/files/folders/folder-1')
    })
  })
})
