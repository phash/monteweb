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
import { feedApi } from '../feed.api'

describe('feedApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getFeed', () => {
    it('should GET /feed with default pagination', async () => {
      await feedApi.getFeed()
      expect(client.get).toHaveBeenCalledWith('/feed', { params: { page: 0, size: 20 } })
    })

    it('should GET /feed with custom pagination', async () => {
      await feedApi.getFeed(2, 10)
      expect(client.get).toHaveBeenCalledWith('/feed', { params: { page: 2, size: 10 } })
    })
  })

  describe('getBanners', () => {
    it('should GET /feed/banners', async () => {
      await feedApi.getBanners()
      expect(client.get).toHaveBeenCalledWith('/feed/banners')
    })
  })

  describe('getPost', () => {
    it('should GET /feed/posts/{id}', async () => {
      await feedApi.getPost('post-1')
      expect(client.get).toHaveBeenCalledWith('/feed/posts/post-1')
    })
  })

  describe('createPost', () => {
    it('should POST /feed/posts', async () => {
      const data = { title: 'News', content: 'Hello world' }
      await feedApi.createPost(data as any)
      expect(client.post).toHaveBeenCalledWith('/feed/posts', data)
    })
  })

  describe('updatePost', () => {
    it('should PUT /feed/posts/{id}', async () => {
      const data = { content: 'Updated content' }
      await feedApi.updatePost('post-1', data)
      expect(client.put).toHaveBeenCalledWith('/feed/posts/post-1', data)
    })
  })

  describe('deletePost', () => {
    it('should DELETE /feed/posts/{id}', async () => {
      await feedApi.deletePost('post-1')
      expect(client.delete).toHaveBeenCalledWith('/feed/posts/post-1')
    })
  })

  describe('pinPost', () => {
    it('should POST /feed/posts/{id}/pin', async () => {
      await feedApi.pinPost('post-1')
      expect(client.post).toHaveBeenCalledWith('/feed/posts/post-1/pin')
    })
  })

  describe('getComments', () => {
    it('should GET /feed/posts/{id}/comments with pagination', async () => {
      await feedApi.getComments('post-1', 0, 20)
      expect(client.get).toHaveBeenCalledWith('/feed/posts/post-1/comments', {
        params: { page: 0, size: 20 },
      })
    })
  })

  describe('addComment', () => {
    it('should POST /feed/posts/{id}/comments', async () => {
      const data = { content: 'Nice post!' }
      await feedApi.addComment('post-1', data as any)
      expect(client.post).toHaveBeenCalledWith('/feed/posts/post-1/comments', data)
    })
  })

  describe('getRoomPosts', () => {
    it('should GET /feed/rooms/{roomId}/posts', async () => {
      await feedApi.getRoomPosts('room-1', 0, 20)
      expect(client.get).toHaveBeenCalledWith('/feed/rooms/room-1/posts', {
        params: { page: 0, size: 20 },
      })
    })
  })

  describe('createRoomPost', () => {
    it('should POST /feed/rooms/{roomId}/posts', async () => {
      const data = { title: 'Room Update', content: 'New info' }
      await feedApi.createRoomPost('room-1', data)
      expect(client.post).toHaveBeenCalledWith('/feed/rooms/room-1/posts', data)
    })
  })
})
