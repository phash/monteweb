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
import { usersApi } from '../users.api'

describe('usersApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getMe', () => {
    it('should GET /users/me', async () => {
      await usersApi.getMe()
      expect(client.get).toHaveBeenCalledWith('/users/me')
    })
  })

  describe('updateMe', () => {
    it('should PUT /users/me with profile data', async () => {
      const data = { firstName: 'Updated', lastName: 'Name', phone: '123' }
      await usersApi.updateMe(data)
      expect(client.put).toHaveBeenCalledWith('/users/me', data)
    })
  })

  describe('uploadAvatar', () => {
    it('should POST FormData to /users/me/avatar', async () => {
      const file = new File(['img'], 'avatar.jpg', { type: 'image/jpeg' })
      await usersApi.uploadAvatar(file)
      const [url, formData] = vi.mocked(client.post).mock.calls[0]
      expect(url).toBe('/users/me/avatar')
      expect(formData).toBeInstanceOf(FormData)
    })
  })

  describe('removeAvatar', () => {
    it('should DELETE /users/me/avatar', async () => {
      await usersApi.removeAvatar()
      expect(client.delete).toHaveBeenCalledWith('/users/me/avatar')
    })
  })

  describe('getById', () => {
    it('should GET /users/{id}', async () => {
      await usersApi.getById('user-123')
      expect(client.get).toHaveBeenCalledWith('/users/user-123')
    })
  })

  describe('search', () => {
    it('should GET /users/search with query params', async () => {
      await usersApi.search('John', 1, 10)
      expect(client.get).toHaveBeenCalledWith('/users/search', {
        params: { q: 'John', page: 1, size: 10 },
      })
    })

    it('should use default page and size', async () => {
      await usersApi.search('test')
      expect(client.get).toHaveBeenCalledWith('/users/search', {
        params: { q: 'test', page: 0, size: 20 },
      })
    })
  })

  describe('list (admin)', () => {
    it('should GET /admin/users with params', async () => {
      await usersApi.list({ page: 0, size: 10 })
      expect(client.get).toHaveBeenCalledWith('/admin/users', { params: { page: 0, size: 10 } })
    })
  })

  describe('updateRole', () => {
    it('should PUT /admin/users/{id}/roles', async () => {
      await usersApi.updateRole('user-1', 'TEACHER')
      expect(client.put).toHaveBeenCalledWith('/admin/users/user-1/roles', { role: 'TEACHER' })
    })
  })

  describe('setActive', () => {
    it('should PUT /admin/users/{id}/status with active param', async () => {
      await usersApi.setActive('user-1', false)
      expect(client.put).toHaveBeenCalledWith('/admin/users/user-1/status', null, {
        params: { active: false },
      })
    })
  })

  describe('addSpecialRole', () => {
    it('should POST /admin/users/{id}/special-roles', async () => {
      await usersApi.addSpecialRole('user-1', 'CLEANING_MANAGER')
      expect(client.post).toHaveBeenCalledWith('/admin/users/user-1/special-roles', { role: 'CLEANING_MANAGER' })
    })
  })

  describe('removeSpecialRole', () => {
    it('should DELETE /admin/users/{id}/special-roles/{role} with encoding', async () => {
      await usersApi.removeSpecialRole('user-1', 'CLEANING_MANAGER')
      expect(client.delete).toHaveBeenCalledWith('/admin/users/user-1/special-roles/CLEANING_MANAGER')
    })
  })

  describe('getDarkMode', () => {
    it('should GET /users/me/dark-mode', async () => {
      await usersApi.getDarkMode()
      expect(client.get).toHaveBeenCalledWith('/users/me/dark-mode')
    })
  })

  describe('updateDarkMode', () => {
    it('should PUT /users/me/dark-mode with preference', async () => {
      await usersApi.updateDarkMode('DARK')
      expect(client.put).toHaveBeenCalledWith('/users/me/dark-mode', { darkMode: 'DARK' })
    })

    it('should send SYSTEM preference', async () => {
      await usersApi.updateDarkMode('SYSTEM')
      expect(client.put).toHaveBeenCalledWith('/users/me/dark-mode', { darkMode: 'SYSTEM' })
    })

    it('should send LIGHT preference', async () => {
      await usersApi.updateDarkMode('LIGHT')
      expect(client.put).toHaveBeenCalledWith('/users/me/dark-mode', { darkMode: 'LIGHT' })
    })
  })
})
