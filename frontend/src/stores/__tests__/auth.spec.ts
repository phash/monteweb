import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/auth.api', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    impersonate: vi.fn(),
    stopImpersonation: vi.fn(),
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

vi.mock('@/composables/useImageToken', () => ({
  useImageToken: () => ({
    fetchImageToken: vi.fn().mockResolvedValue('mock-image-token'),
    clearImageToken: vi.fn(),
  }),
  authenticatedImageUrl: (path: string) => path,
}))

import { authApi } from '@/api/auth.api'
import { usersApi } from '@/api/users.api'

function fakeJwt(claims: Record<string, string> = {}): string {
  const header = btoa(JSON.stringify({ alg: 'HS256' }))
  const payload = btoa(JSON.stringify({ sub: 'user-id', role: 'TEACHER', ...claims }))
  return `${header}.${payload}.signature`
}

describe('Auth Store - Impersonation', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    sessionStorage.clear()
    vi.clearAllMocks()
  })

  describe('impersonation', () => {
    it('impersonate() sets impersonatedBy and calls fetchUser', async () => {
      const auth = useAuthStore()

      const impersonationToken = fakeJwt({ impersonatedBy: 'admin-user-id' })

      vi.mocked(authApi.impersonate).mockResolvedValue({
        data: {
          data: {
            accessToken: impersonationToken,
            refreshToken: 'imp-refresh-token',
          },
        },
      } as any)

      vi.mocked(usersApi.getMe).mockResolvedValue({
        data: {
          data: {
            id: 'target-id',
            email: 'target@example.com',
            firstName: 'Target',
            lastName: 'User',
            displayName: 'Target User',
            role: 'TEACHER',
            active: true,
          },
        },
      } as any)

      await auth.impersonate('target-id')

      expect(authApi.impersonate).toHaveBeenCalledWith('target-id')
      expect(auth.impersonatedBy).toBe('admin-user-id')
      expect(usersApi.getMe).toHaveBeenCalled()
      expect(sessionStorage.getItem('accessToken')).toBe(impersonationToken)
    })

    it('stopImpersonation() clears impersonatedBy and calls fetchUser', async () => {
      // Set up impersonation state first
      setActivePinia(createPinia())
      const impersonationToken = fakeJwt({ impersonatedBy: 'admin-user-id' })
      sessionStorage.setItem('accessToken', impersonationToken)
      const auth = useAuthStore()

      // Verify impersonation is active initially
      expect(auth.impersonatedBy).toBe('admin-user-id')

      const regularToken = fakeJwt()

      vi.mocked(authApi.stopImpersonation).mockResolvedValue({
        data: {
          data: {
            accessToken: regularToken,
            refreshToken: 'regular-refresh-token',
          },
        },
      } as any)

      vi.mocked(usersApi.getMe).mockResolvedValue({
        data: {
          data: {
            id: 'admin-user-id',
            email: 'admin@example.com',
            firstName: 'Admin',
            lastName: 'User',
            displayName: 'Admin User',
            role: 'SUPERADMIN',
            active: true,
          },
        },
      } as any)

      await auth.stopImpersonation()

      expect(authApi.stopImpersonation).toHaveBeenCalled()
      expect(auth.impersonatedBy).toBeNull()
      expect(usersApi.getMe).toHaveBeenCalled()
      expect(auth.user?.role).toBe('SUPERADMIN')
    })

    it('isImpersonating computed returns true when impersonatedBy is set', () => {
      // Case 1: with impersonation token
      setActivePinia(createPinia())
      const impersonationToken = fakeJwt({ impersonatedBy: 'admin-user-id' })
      sessionStorage.setItem('accessToken', impersonationToken)
      const authWithImpersonation = useAuthStore()

      expect(authWithImpersonation.isImpersonating).toBe(true)
      expect(authWithImpersonation.impersonatedBy).toBe('admin-user-id')

      // Case 2: without impersonation token
      setActivePinia(createPinia())
      sessionStorage.clear()
      const regularToken = fakeJwt()
      sessionStorage.setItem('accessToken', regularToken)
      const authWithoutImpersonation = useAuthStore()

      expect(authWithoutImpersonation.isImpersonating).toBe(false)
      expect(authWithoutImpersonation.impersonatedBy).toBeNull()
    })

    it('clearTokens() also clears impersonatedBy', async () => {
      // Set up impersonation state
      setActivePinia(createPinia())
      const impersonationToken = fakeJwt({ impersonatedBy: 'admin-user-id' })
      sessionStorage.setItem('accessToken', impersonationToken)
      const auth = useAuthStore()

      // Verify impersonation is active
      expect(auth.impersonatedBy).toBe('admin-user-id')
      expect(auth.isImpersonating).toBe(true)

      // Logout calls clearTokens internally
      vi.mocked(authApi.logout).mockResolvedValue({} as any)
      await auth.logout()

      expect(auth.impersonatedBy).toBeNull()
      expect(auth.isImpersonating).toBe(false)
      expect(sessionStorage.getItem('accessToken')).toBeNull()
    })

    it('impersonate() sets loading during the call', async () => {
      const auth = useAuthStore()

      let resolveImpersonate!: Function
      vi.mocked(authApi.impersonate).mockReturnValue(
        new Promise((resolve) => { resolveImpersonate = resolve }) as any
      )

      const impersonatePromise = auth.impersonate('target-id')
      expect(auth.loading).toBe(true)

      const impersonationToken = fakeJwt({ impersonatedBy: 'admin-user-id' })
      resolveImpersonate({
        data: { data: { accessToken: impersonationToken, refreshToken: 'refresh' } },
      })

      vi.mocked(usersApi.getMe).mockResolvedValue({
        data: { data: { id: 'target-id', role: 'TEACHER' } },
      } as any)

      await impersonatePromise
      expect(auth.loading).toBe(false)
    })

    it('impersonatedBy is restored from sessionStorage token on store init', () => {
      // Store a token with impersonatedBy claim in sessionStorage before creating the store
      const impersonationToken = fakeJwt({ impersonatedBy: 'admin-user-id' })
      sessionStorage.setItem('accessToken', impersonationToken)

      setActivePinia(createPinia())
      const auth = useAuthStore()

      expect(auth.impersonatedBy).toBe('admin-user-id')
      expect(auth.isImpersonating).toBe(true)
    })

    it('impersonatedBy is null on store init when no token in sessionStorage', () => {
      sessionStorage.clear()

      setActivePinia(createPinia())
      const auth = useAuthStore()

      expect(auth.impersonatedBy).toBeNull()
      expect(auth.isImpersonating).toBe(false)
    })
  })
})
