import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/auth.api', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
  },
}))

vi.mock('@/api/users.api', () => ({
  usersApi: {
    getMe: vi.fn(),
  },
}))

vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
    getConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
  },
}))

import { authApi } from '@/api/auth.api'
import { usersApi } from '@/api/users.api'

describe('Auth Store - Extended', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  describe('register', () => {
    it('should register and return PENDING_APPROVAL without storing tokens', async () => {
      const auth = useAuthStore()

      vi.mocked(authApi.register).mockResolvedValue({
        data: { data: null, message: 'PENDING_APPROVAL' },
      } as any)

      const result = await auth.register({
        email: 'new@example.com',
        password: 'pass123',
        firstName: 'New',
        lastName: 'User',
      } as any)

      expect(result).toBe('PENDING_APPROVAL')
      expect(auth.isAuthenticated).toBe(false)
      expect(auth.user).toBeNull()
      expect(sessionStorage.getItem('accessToken')).toBeNull()
      expect(sessionStorage.getItem('refreshToken')).toBeNull()
    })

    it('should set loading during registration', async () => {
      const auth = useAuthStore()

      let resolveRegister!: Function
      vi.mocked(authApi.register).mockReturnValue(
        new Promise((resolve) => { resolveRegister = resolve }) as any
      )

      const registerPromise = auth.register({ email: 'test@test.com', password: 'pw' } as any)
      expect(auth.loading).toBe(true)

      resolveRegister({
        data: { data: null, message: 'PENDING_APPROVAL' },
      })

      await registerPromise
      expect(auth.loading).toBe(false)
    })
  })

  describe('fetchUser', () => {
    it('should not fetch if no access token', async () => {
      const auth = useAuthStore()
      await auth.fetchUser()
      expect(usersApi.getMe).not.toHaveBeenCalled()
    })

    it('should clear tokens on fetch failure', async () => {
      setActivePinia(createPinia())
      sessionStorage.setItem('accessToken', 'expired-token')
      const auth = useAuthStore()

      vi.mocked(usersApi.getMe).mockRejectedValue(new Error('401'))

      await auth.fetchUser()

      expect(auth.user).toBeNull()
    })

    it('should set user on successful fetch', async () => {
      setActivePinia(createPinia())
      sessionStorage.setItem('accessToken', 'valid-token')
      const auth = useAuthStore()

      vi.mocked(usersApi.getMe).mockResolvedValue({
        data: {
          data: {
            id: 'user-1',
            email: 'test@example.com',
            firstName: 'Test',
            lastName: 'User',
            role: 'TEACHER',
          },
        },
      } as any)

      await auth.fetchUser()

      expect(auth.user).not.toBeNull()
      expect(auth.user?.role).toBe('TEACHER')
    })
  })

  describe('computed roles', () => {
    it('should detect TEACHER role', () => {
      const auth = useAuthStore()
      auth.user = { role: 'TEACHER' } as any

      expect(auth.isTeacher).toBe(true)
      expect(auth.isAdmin).toBe(false)
      expect(auth.isStudent).toBe(false)
    })

    it('should detect SECTION_ADMIN as teacher', () => {
      const auth = useAuthStore()
      auth.user = { role: 'SECTION_ADMIN' } as any

      expect(auth.isTeacher).toBe(true)
      expect(auth.isAdmin).toBe(false)
    })

    it('should detect STUDENT role', () => {
      const auth = useAuthStore()
      auth.user = { role: 'STUDENT' } as any

      expect(auth.isStudent).toBe(true)
      expect(auth.isTeacher).toBe(false)
      expect(auth.isAdmin).toBe(false)
    })

    it('should detect PARENT role (none of the special roles)', () => {
      const auth = useAuthStore()
      auth.user = { role: 'PARENT' } as any

      expect(auth.isStudent).toBe(false)
      expect(auth.isTeacher).toBe(false)
      expect(auth.isAdmin).toBe(false)
    })

    it('should detect isPutzOrga from specialRoles', () => {
      const auth = useAuthStore()
      auth.user = {
        role: 'PARENT',
        specialRoles: ['PUTZORGA:section-1'],
      } as any

      expect(auth.isPutzOrga).toBe(true)
    })

    it('should not be isPutzOrga without PUTZORGA role', () => {
      const auth = useAuthStore()
      auth.user = {
        role: 'PARENT',
        specialRoles: ['OTHER_ROLE'],
      } as any

      expect(auth.isPutzOrga).toBe(false)
    })

    it('should not be isPutzOrga with no specialRoles', () => {
      const auth = useAuthStore()
      auth.user = {
        role: 'PARENT',
        specialRoles: undefined,
      } as any

      expect(auth.isPutzOrga).toBe(false)
    })

    it('should not be isPutzOrga when user is null', () => {
      const auth = useAuthStore()
      expect(auth.isPutzOrga).toBe(false)
    })
  })

  describe('logout error handling', () => {
    it('should clear state even if logout API fails', async () => {
      const auth = useAuthStore()
      sessionStorage.setItem('accessToken', 'token')
      sessionStorage.setItem('refreshToken', 'refresh')

      vi.mocked(authApi.logout).mockRejectedValue(new Error('Network error'))

      try {
        await auth.logout()
      } catch {
        // Error is re-thrown from try/finally
      }

      expect(auth.user).toBeNull()
      expect(sessionStorage.getItem('accessToken')).toBeNull()
      expect(sessionStorage.getItem('refreshToken')).toBeNull()
    })
  })

  describe('login error handling', () => {
    it('should reset loading on login failure', async () => {
      const auth = useAuthStore()

      vi.mocked(authApi.login).mockRejectedValue(new Error('Invalid credentials'))

      try {
        await auth.login({ email: 'test@test.com', password: 'wrong' })
      } catch {
        // Expected
      }

      expect(auth.loading).toBe(false)
    })

    it('should not be authenticated after login failure', async () => {
      const auth = useAuthStore()

      vi.mocked(authApi.login).mockRejectedValue(new Error('401'))

      try {
        await auth.login({ email: 'bad@test.com', password: 'wrong' })
      } catch {
        // Expected
      }

      expect(auth.isAuthenticated).toBe(false)
      expect(auth.user).toBeNull()
    })
  })

  describe('register error handling', () => {
    it('should reset loading on register failure', async () => {
      const auth = useAuthStore()

      vi.mocked(authApi.register).mockRejectedValue(new Error('Email taken'))

      try {
        await auth.register({ email: 'taken@test.com', password: 'pw' } as any)
      } catch {
        // Expected
      }

      expect(auth.loading).toBe(false)
    })
  })
})
