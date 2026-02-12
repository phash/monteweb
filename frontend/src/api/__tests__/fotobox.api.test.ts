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
import { fotoboxApi } from '../fotobox.api'

describe('fotoboxApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // Settings
  describe('getSettings', () => {
    it('should call GET /rooms/{roomId}/fotobox/settings', async () => {
      await fotoboxApi.getSettings('room-1')
      expect(client.get).toHaveBeenCalledWith('/rooms/room-1/fotobox/settings')
    })
  })

  describe('updateSettings', () => {
    it('should call PUT /rooms/{roomId}/fotobox/settings', async () => {
      const settings = { enabled: true, defaultPermission: 'POST_IMAGES' }
      await fotoboxApi.updateSettings('room-1', settings)
      expect(client.put).toHaveBeenCalledWith('/rooms/room-1/fotobox/settings', settings)
    })
  })

  // Threads
  describe('getThreads', () => {
    it('should call GET /rooms/{roomId}/fotobox/threads', async () => {
      await fotoboxApi.getThreads('room-1')
      expect(client.get).toHaveBeenCalledWith('/rooms/room-1/fotobox/threads')
    })
  })

  describe('getThread', () => {
    it('should call GET /rooms/{roomId}/fotobox/threads/{threadId}', async () => {
      await fotoboxApi.getThread('room-1', 'thread-1')
      expect(client.get).toHaveBeenCalledWith('/rooms/room-1/fotobox/threads/thread-1')
    })
  })

  describe('getThreadImages', () => {
    it('should call GET /rooms/{roomId}/fotobox/threads/{threadId}/images', async () => {
      await fotoboxApi.getThreadImages('room-1', 'thread-1')
      expect(client.get).toHaveBeenCalledWith('/rooms/room-1/fotobox/threads/thread-1/images')
    })
  })

  describe('createThread', () => {
    it('should call POST /rooms/{roomId}/fotobox/threads', async () => {
      const data = { title: 'New Thread', description: 'Desc' }
      await fotoboxApi.createThread('room-1', data)
      expect(client.post).toHaveBeenCalledWith('/rooms/room-1/fotobox/threads', data)
    })
  })

  describe('updateThread', () => {
    it('should call PUT /rooms/{roomId}/fotobox/threads/{threadId}', async () => {
      await fotoboxApi.updateThread('room-1', 'thread-1', { title: 'Updated' })
      expect(client.put).toHaveBeenCalledWith('/rooms/room-1/fotobox/threads/thread-1', { title: 'Updated' })
    })
  })

  describe('deleteThread', () => {
    it('should call DELETE /rooms/{roomId}/fotobox/threads/{threadId}', async () => {
      await fotoboxApi.deleteThread('room-1', 'thread-1')
      expect(client.delete).toHaveBeenCalledWith('/rooms/room-1/fotobox/threads/thread-1')
    })
  })

  // Images
  describe('uploadImages', () => {
    it('should call POST with FormData and multipart header', async () => {
      const files = [new File([''], 'photo.jpg', { type: 'image/jpeg' })]
      await fotoboxApi.uploadImages('room-1', 'thread-1', files, 'A caption')

      expect(client.post).toHaveBeenCalledTimes(1)
      const [url, formData, config] = vi.mocked(client.post).mock.calls[0]
      expect(url).toBe('/rooms/room-1/fotobox/threads/thread-1/images')
      expect(formData).toBeInstanceOf(FormData)
      expect(config).toEqual({ headers: { 'Content-Type': 'multipart/form-data' } })
    })

    it('should include files in FormData', async () => {
      const file1 = new File(['a'], 'img1.jpg', { type: 'image/jpeg' })
      const file2 = new File(['b'], 'img2.png', { type: 'image/png' })
      await fotoboxApi.uploadImages('room-1', 'thread-1', [file1, file2])

      const formData = vi.mocked(client.post).mock.calls[0][1] as FormData
      const files = formData.getAll('files')
      expect(files).toHaveLength(2)
    })

    it('should include caption when provided', async () => {
      const files = [new File([''], 'photo.jpg', { type: 'image/jpeg' })]
      await fotoboxApi.uploadImages('room-1', 'thread-1', files, 'My caption')

      const formData = vi.mocked(client.post).mock.calls[0][1] as FormData
      expect(formData.get('caption')).toBe('My caption')
    })

    it('should not include caption when not provided', async () => {
      const files = [new File([''], 'photo.jpg', { type: 'image/jpeg' })]
      await fotoboxApi.uploadImages('room-1', 'thread-1', files)

      const formData = vi.mocked(client.post).mock.calls[0][1] as FormData
      expect(formData.get('caption')).toBeNull()
    })
  })

  describe('updateImage', () => {
    it('should call PUT /fotobox/images/{imageId}', async () => {
      await fotoboxApi.updateImage('img-1', { caption: 'Updated', sortOrder: 3 })
      expect(client.put).toHaveBeenCalledWith('/fotobox/images/img-1', { caption: 'Updated', sortOrder: 3 })
    })
  })

  describe('deleteImage', () => {
    it('should call DELETE /fotobox/images/{imageId}', async () => {
      await fotoboxApi.deleteImage('img-1')
      expect(client.delete).toHaveBeenCalledWith('/fotobox/images/img-1')
    })
  })

  // URL helpers
  describe('imageUrl', () => {
    it('should return URL with token when accessToken exists', () => {
      localStorage.setItem('accessToken', 'test-jwt-token')
      expect(fotoboxApi.imageUrl('img-1')).toBe('/api/v1/fotobox/images/img-1?token=test-jwt-token')
    })

    it('should return URL without token when no accessToken', () => {
      localStorage.removeItem('accessToken')
      expect(fotoboxApi.imageUrl('img-1')).toBe('/api/v1/fotobox/images/img-1')
    })
  })

  describe('thumbnailUrl', () => {
    it('should return URL with token when accessToken exists', () => {
      localStorage.setItem('accessToken', 'test-jwt-token')
      expect(fotoboxApi.thumbnailUrl('img-1')).toBe(
        '/api/v1/fotobox/images/img-1/thumbnail?token=test-jwt-token',
      )
    })

    it('should return URL without token when no accessToken', () => {
      localStorage.removeItem('accessToken')
      expect(fotoboxApi.thumbnailUrl('img-1')).toBe('/api/v1/fotobox/images/img-1/thumbnail')
    })
  })
})
