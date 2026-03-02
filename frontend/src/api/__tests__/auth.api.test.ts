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
import { authApi } from '../auth.api'

describe('authApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('login', () => {
    it('should POST to /auth/login with credentials', async () => {
      const data = { email: 'user@example.com', password: 'secret123' }
      await authApi.login(data)
      expect(client.post).toHaveBeenCalledWith('/auth/login', data)
    })
  })

  describe('register', () => {
    it('should POST to /auth/register with user data', async () => {
      const data = { email: 'new@example.com', password: 'pass', firstName: 'New', lastName: 'User' }
      await authApi.register(data)
      expect(client.post).toHaveBeenCalledWith('/auth/register', data)
    })
  })

  describe('logout', () => {
    it('should POST to /auth/logout with empty body', async () => {
      await authApi.logout()
      expect(client.post).toHaveBeenCalledWith('/auth/logout', {})
    })

    it('should POST to /auth/logout with empty body when token is null', async () => {
      await authApi.logout(null)
      expect(client.post).toHaveBeenCalledWith('/auth/logout', {})
    })
  })
})
