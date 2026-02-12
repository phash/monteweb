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

  describe('refresh', () => {
    it('should POST to /auth/refresh with refresh token', async () => {
      await authApi.refresh('my-refresh-token')
      expect(client.post).toHaveBeenCalledWith('/auth/refresh', { refreshToken: 'my-refresh-token' })
    })
  })

  describe('logout', () => {
    it('should POST to /auth/logout with refresh token', async () => {
      await authApi.logout('my-refresh-token')
      expect(client.post).toHaveBeenCalledWith('/auth/logout', { refreshToken: 'my-refresh-token' })
    })

    it('should POST to /auth/logout with empty body when token is null', async () => {
      await authApi.logout(null)
      expect(client.post).toHaveBeenCalledWith('/auth/logout', {})
    })
  })
})
