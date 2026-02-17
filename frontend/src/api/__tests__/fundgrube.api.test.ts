import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('../client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: { data: [] } }),
    post: vi.fn().mockResolvedValue({ data: { data: {} } }),
    put: vi.fn().mockResolvedValue({ data: { data: {} } }),
    delete: vi.fn().mockResolvedValue({ data: { data: null } }),
  },
}))

import client from '../client'
import { fundgrubeApi } from '../fundgrube.api'

describe('fundgrubeApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.setItem('accessToken', 'test-token')
  })

  describe('listItems', () => {
    it('should call GET /fundgrube/items without params', async () => {
      await fundgrubeApi.listItems()
      expect(client.get).toHaveBeenCalledWith('/fundgrube/items', { params: {} })
    })

    it('should call GET /fundgrube/items with sectionId', async () => {
      await fundgrubeApi.listItems('section-1')
      expect(client.get).toHaveBeenCalledWith('/fundgrube/items', {
        params: { sectionId: 'section-1' },
      })
    })
  })

  describe('getItem', () => {
    it('should call GET /fundgrube/items/{id}', async () => {
      await fundgrubeApi.getItem('item-1')
      expect(client.get).toHaveBeenCalledWith('/fundgrube/items/item-1')
    })
  })

  describe('createItem', () => {
    it('should call POST /fundgrube/items', async () => {
      const data = { title: 'Lost bag', description: 'Blue backpack', sectionId: 'sec-1' }
      await fundgrubeApi.createItem(data)
      expect(client.post).toHaveBeenCalledWith('/fundgrube/items', data)
    })

    it('should call POST /fundgrube/items with minimal data', async () => {
      const data = { title: 'Lost key' }
      await fundgrubeApi.createItem(data)
      expect(client.post).toHaveBeenCalledWith('/fundgrube/items', data)
    })
  })

  describe('updateItem', () => {
    it('should call PUT /fundgrube/items/{id}', async () => {
      const data = { title: 'Updated title' }
      await fundgrubeApi.updateItem('item-1', data)
      expect(client.put).toHaveBeenCalledWith('/fundgrube/items/item-1', data)
    })
  })

  describe('deleteItem', () => {
    it('should call DELETE /fundgrube/items/{id}', async () => {
      await fundgrubeApi.deleteItem('item-1')
      expect(client.delete).toHaveBeenCalledWith('/fundgrube/items/item-1')
    })
  })

  describe('claimItem', () => {
    it('should call POST /fundgrube/items/{id}/claim with empty body', async () => {
      await fundgrubeApi.claimItem('item-1')
      expect(client.post).toHaveBeenCalledWith('/fundgrube/items/item-1/claim', {})
    })

    it('should call POST /fundgrube/items/{id}/claim with comment', async () => {
      await fundgrubeApi.claimItem('item-1', { comment: 'My bag has red straps' })
      expect(client.post).toHaveBeenCalledWith('/fundgrube/items/item-1/claim', {
        comment: 'My bag has red straps',
      })
    })
  })

  describe('uploadImages', () => {
    it('should call POST /fundgrube/items/{id}/images with FormData', async () => {
      const file = new File(['data'], 'photo.jpg', { type: 'image/jpeg' })
      await fundgrubeApi.uploadImages('item-1', [file])
      expect(client.post).toHaveBeenCalledWith(
        '/fundgrube/items/item-1/images',
        expect.any(FormData),
        { headers: { 'Content-Type': 'multipart/form-data' } },
      )
    })
  })

  describe('deleteImage', () => {
    it('should call DELETE /fundgrube/images/{id}', async () => {
      await fundgrubeApi.deleteImage('img-1')
      expect(client.delete).toHaveBeenCalledWith('/fundgrube/images/img-1')
    })
  })

  describe('imageUrl', () => {
    it('should return URL with token', () => {
      localStorage.setItem('accessToken', 'abc123')
      const url = fundgrubeApi.imageUrl('img-1')
      expect(url).toBe('/api/v1/fundgrube/images/img-1?token=abc123')
    })

    it('should return URL without token when not logged in', () => {
      localStorage.removeItem('accessToken')
      const url = fundgrubeApi.imageUrl('img-1')
      expect(url).toBe('/api/v1/fundgrube/images/img-1')
    })
  })

  describe('thumbnailUrl', () => {
    it('should return thumbnail URL with token', () => {
      localStorage.setItem('accessToken', 'abc123')
      const url = fundgrubeApi.thumbnailUrl('img-1')
      expect(url).toBe('/api/v1/fundgrube/images/img-1/thumbnail?token=abc123')
    })

    it('should return thumbnail URL without token when not logged in', () => {
      localStorage.removeItem('accessToken')
      const url = fundgrubeApi.thumbnailUrl('img-1')
      expect(url).toBe('/api/v1/fundgrube/images/img-1/thumbnail')
    })
  })
})
