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
import { adminApi } from '../admin.api'

describe('adminApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getConfig', () => {
    it('should GET /admin/config', async () => {
      await adminApi.getConfig()
      expect(client.get).toHaveBeenCalledWith('/admin/config')
    })
  })

  describe('updateConfig', () => {
    it('should PUT /admin/config with data', async () => {
      const data = { schoolName: 'Montessori School', targetHoursPerFamily: 20 }
      await adminApi.updateConfig(data)
      expect(client.put).toHaveBeenCalledWith('/admin/config', data)
    })
  })

  describe('updateTheme', () => {
    it('should PUT /admin/config/theme', async () => {
      const theme = { primaryColor: '#ff0000', bgMain: '#ffffff' }
      await adminApi.updateTheme(theme)
      expect(client.put).toHaveBeenCalledWith('/admin/config/theme', theme)
    })
  })

  describe('updateModules', () => {
    it('should PUT /admin/config/modules', async () => {
      const modules = { messaging: true, cleaning: false }
      await adminApi.updateModules(modules)
      expect(client.put).toHaveBeenCalledWith('/admin/config/modules', modules)
    })
  })

  describe('uploadLogo', () => {
    it('should POST FormData to /admin/config/logo', async () => {
      const file = new File(['img'], 'logo.png', { type: 'image/png' })
      await adminApi.uploadLogo(file)

      const [url, formData, config] = vi.mocked(client.post).mock.calls[0]
      expect(url).toBe('/admin/config/logo')
      expect(formData).toBeInstanceOf(FormData)
      expect(config).toEqual({ headers: { 'Content-Type': 'multipart/form-data' } })
    })
  })

  describe('getPublicConfig', () => {
    it('should GET /config', async () => {
      await adminApi.getPublicConfig()
      expect(client.get).toHaveBeenCalledWith('/config')
    })
  })
})
