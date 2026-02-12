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
import { sectionsApi } from '../sections.api'

describe('sectionsApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getAll', () => {
    it('should GET /sections', async () => {
      await sectionsApi.getAll()
      expect(client.get).toHaveBeenCalledWith('/sections')
    })
  })

  describe('create', () => {
    it('should POST /sections with data', async () => {
      const data = { name: 'Grundschule', sortOrder: 3 }
      await sectionsApi.create(data)
      expect(client.post).toHaveBeenCalledWith('/sections', data)
    })
  })

  describe('update', () => {
    it('should PUT /sections/{id}', async () => {
      const data = { name: 'Updated Section', sortOrder: 5 }
      await sectionsApi.update('sect-1', data)
      expect(client.put).toHaveBeenCalledWith('/sections/sect-1', data)
    })
  })

  describe('deactivate', () => {
    it('should DELETE /sections/{id}', async () => {
      await sectionsApi.deactivate('sect-1')
      expect(client.delete).toHaveBeenCalledWith('/sections/sect-1')
    })
  })
})
