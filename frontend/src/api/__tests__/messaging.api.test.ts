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
    it('should POST /messages/conversations/{id}/messages', async () => {
      await messagingApi.sendMessage('conv-1', 'Test message')
      expect(client.post).toHaveBeenCalledWith('/messages/conversations/conv-1/messages', {
        content: 'Test message',
      })
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
})
