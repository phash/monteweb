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
import { discussionsApi } from '../discussions.api'

describe('discussionsApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getThreads', () => {
    it('should GET /rooms/{roomId}/threads with params', async () => {
      await discussionsApi.getThreads('room-1', 'ACTIVE', 0, 20)
      expect(client.get).toHaveBeenCalledWith('/rooms/room-1/threads', {
        params: { status: 'ACTIVE', page: 0, size: 20 },
      })
    })

    it('should use default pagination', async () => {
      await discussionsApi.getThreads('room-1')
      expect(client.get).toHaveBeenCalledWith('/rooms/room-1/threads', {
        params: { status: undefined, page: 0, size: 20 },
      })
    })
  })

  describe('getThread', () => {
    it('should GET /rooms/{roomId}/threads/{threadId}', async () => {
      await discussionsApi.getThread('room-1', 'thread-1')
      expect(client.get).toHaveBeenCalledWith('/rooms/room-1/threads/thread-1')
    })
  })

  describe('createThread', () => {
    it('should POST /rooms/{roomId}/threads', async () => {
      await discussionsApi.createThread('room-1', 'My Topic', 'Some content', 'ALL')
      expect(client.post).toHaveBeenCalledWith('/rooms/room-1/threads', {
        title: 'My Topic',
        content: 'Some content',
        audience: 'ALL',
      })
    })
  })

  describe('archiveThread', () => {
    it('should PUT /rooms/{roomId}/threads/{id}/archive', async () => {
      await discussionsApi.archiveThread('room-1', 'thread-1')
      expect(client.put).toHaveBeenCalledWith('/rooms/room-1/threads/thread-1/archive')
    })
  })

  describe('deleteThread', () => {
    it('should DELETE /rooms/{roomId}/threads/{id}', async () => {
      await discussionsApi.deleteThread('room-1', 'thread-1')
      expect(client.delete).toHaveBeenCalledWith('/rooms/room-1/threads/thread-1')
    })
  })

  describe('getReplies', () => {
    it('should GET /rooms/{roomId}/threads/{id}/replies', async () => {
      await discussionsApi.getReplies('room-1', 'thread-1', 0, 50)
      expect(client.get).toHaveBeenCalledWith('/rooms/room-1/threads/thread-1/replies', {
        params: { page: 0, size: 50 },
      })
    })
  })

  describe('addReply', () => {
    it('should POST /rooms/{roomId}/threads/{id}/replies', async () => {
      await discussionsApi.addReply('room-1', 'thread-1', 'Great idea!')
      expect(client.post).toHaveBeenCalledWith('/rooms/room-1/threads/thread-1/replies', {
        content: 'Great idea!',
      })
    })
  })
})
