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
import { profileFieldsApi } from '../profilefields.api'

describe('profileFieldsApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('user endpoints', () => {
    it('should GET /profile-fields for definitions', async () => {
      await profileFieldsApi.getDefinitions()
      expect(client.get).toHaveBeenCalledWith('/profile-fields')
    })

    it('should GET /profile-fields/me for user values', async () => {
      await profileFieldsApi.getMyValues()
      expect(client.get).toHaveBeenCalledWith('/profile-fields/me')
    })

    it('should PUT /profile-fields/me to update values', async () => {
      const values = { f1: 'test', f2: '2024-01-01' }
      await profileFieldsApi.updateMyValues(values)
      expect(client.put).toHaveBeenCalledWith('/profile-fields/me', values)
    })
  })

  describe('admin endpoints', () => {
    it('should GET /admin/profile-fields for all definitions', async () => {
      await profileFieldsApi.listAllDefinitions()
      expect(client.get).toHaveBeenCalledWith('/admin/profile-fields')
    })

    it('should POST /admin/profile-fields to create a field', async () => {
      const data = {
        fieldKey: 'hobby',
        labelDe: 'Hobby',
        labelEn: 'Hobby',
        fieldType: 'TEXT',
        required: false,
        position: 0,
      }
      await profileFieldsApi.createDefinition(data)
      expect(client.post).toHaveBeenCalledWith('/admin/profile-fields', data)
    })

    it('should PUT /admin/profile-fields/:id to update a field', async () => {
      const data = { labelDe: 'Updated' }
      await profileFieldsApi.updateDefinition('f1', data)
      expect(client.put).toHaveBeenCalledWith('/admin/profile-fields/f1', data)
    })

    it('should DELETE /admin/profile-fields/:id to delete a field', async () => {
      await profileFieldsApi.deleteDefinition('f1')
      expect(client.delete).toHaveBeenCalledWith('/admin/profile-fields/f1')
    })
  })
})
