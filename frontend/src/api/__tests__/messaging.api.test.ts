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
import { messagingApi } from '../messaging.api'

describe('messagingApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  describe('getConversations', () => {
    it('should GET /messages/conversations', async () => {
      await messagingApi.getConversations()
      expect(client.get).toHaveBeenCalledWith('/messages/conversations')
    })
  })

  describe('getConversation', () => {
    it('should GET /messages/conversations/{id}', async () => {
      await messagingApi.getConversation('conv-1')
      expect(client.get).toHaveBeenCalledWith('/messages/conversations/conv-1')
    })
  })

  describe('startConversation', () => {
    it('should POST /messages/conversations', async () => {
      const data = { recipientId: 'user-2', content: 'Hello!' }
      await messagingApi.startConversation(data as any)
      expect(client.post).toHaveBeenCalledWith('/messages/conversations', data)
    })
  })

  describe('getMessages', () => {
    it('should GET /messages/conversations/{id}/messages with pagination', async () => {
      await messagingApi.getMessages('conv-1', 0, 50)
      expect(client.get).toHaveBeenCalledWith('/messages/conversations/conv-1/messages', {
        params: { page: 0, size: 50 },
      })
    })

    it('should use default pagination', async () => {
      await messagingApi.getMessages('conv-1')
      expect(client.get).toHaveBeenCalledWith('/messages/conversations/conv-1/messages', {
        params: { page: 0, size: 50 },
      })
    })
  })

  describe('sendMessage', () => {
    it('should POST FormData with text content', async () => {
      await messagingApi.sendMessage('conv-1', 'Test message')
      expect(client.post).toHaveBeenCalledWith(
        '/messages/conversations/conv-1/messages',
        expect.any(FormData),
        { headers: { 'Content-Type': 'multipart/form-data' } },
      )
      const formData = vi.mocked(client.post).mock.calls[0][1] as FormData
      expect(formData.get('content')).toBe('Test message')
      expect(formData.get('image')).toBeNull()
      expect(formData.get('replyToId')).toBeNull()
    })

    it('should POST FormData with image', async () => {
      const file = new File(['test'], 'photo.jpg', { type: 'image/jpeg' })
      await messagingApi.sendMessage('conv-1', undefined, file)
      const formData = vi.mocked(client.post).mock.calls[0][1] as FormData
      expect(formData.get('image')).toBeTruthy()
      expect(formData.get('content')).toBeNull()
    })

    it('should POST FormData with replyToId', async () => {
      await messagingApi.sendMessage('conv-1', 'Reply text', undefined, 'msg-123')
      const formData = vi.mocked(client.post).mock.calls[0][1] as FormData
      expect(formData.get('content')).toBe('Reply text')
      expect(formData.get('replyToId')).toBe('msg-123')
    })

    it('should POST FormData with image and text and reply', async () => {
      const file = new File(['test'], 'photo.jpg', { type: 'image/jpeg' })
      await messagingApi.sendMessage('conv-1', 'With image', file, 'msg-456')
      const formData = vi.mocked(client.post).mock.calls[0][1] as FormData
      expect(formData.get('content')).toBe('With image')
      expect(formData.get('image')).toBeTruthy()
      expect(formData.get('replyToId')).toBe('msg-456')
    })
  })

  describe('markAsRead', () => {
    it('should PUT /messages/conversations/{id}/read', async () => {
      await messagingApi.markAsRead('conv-1')
      expect(client.put).toHaveBeenCalledWith('/messages/conversations/conv-1/read')
    })
  })

  describe('getUnreadCount', () => {
    it('should GET /messages/unread-count', async () => {
      await messagingApi.getUnreadCount()
      expect(client.get).toHaveBeenCalledWith('/messages/unread-count')
    })
  })

  describe('imageUrl', () => {
    it('should return URL with token when logged in', () => {
      localStorage.setItem('accessToken', 'abc123')
      const url = messagingApi.imageUrl('img-1')
      expect(url).toBe('/api/v1/messages/images/img-1?token=abc123')
    })

    it('should return URL without token when not logged in', () => {
      const url = messagingApi.imageUrl('img-1')
      expect(url).toBe('/api/v1/messages/images/img-1')
    })
  })

  describe('thumbnailUrl', () => {
    it('should return thumbnail URL with token when logged in', () => {
      localStorage.setItem('accessToken', 'abc123')
      const url = messagingApi.thumbnailUrl('img-1')
      expect(url).toBe('/api/v1/messages/images/img-1/thumbnail?token=abc123')
    })

    it('should return thumbnail URL without token when not logged in', () => {
      const url = messagingApi.thumbnailUrl('img-1')
      expect(url).toBe('/api/v1/messages/images/img-1/thumbnail')
    })
  })
})
