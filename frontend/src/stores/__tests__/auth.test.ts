import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

// Mock API modules
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

describe('Auth Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('should start with no user and not authenticated', () => {
    const auth = useAuthStore()
    expect(auth.user).toBeNull()
    expect(auth.isAuthenticated).toBe(false)
    expect(auth.isAdmin).toBe(false)
  })

  it('should login and store tokens', async () => {
    const auth = useAuthStore()

    vi.mocked(authApi.login).mockResolvedValue({
      data: {
        data: {
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        },
      },
    } as any)

    vi.mocked(usersApi.getMe).mockResolvedValue({
      data: {
        data: {
          id: '123',
          email: 'test@example.com',
          firstName: 'Test',
          lastName: 'User',
          displayName: 'Test User',
          role: 'PARENT',
          active: true,
        },
      },
    } as any)

    await auth.login({ email: 'test@example.com', password: 'password' })

    expect(auth.isAuthenticated).toBe(true)
    expect(auth.user?.email).toBe('test@example.com')
    expect(localStorage.getItem('accessToken')).toBe('test-access-token')
    expect(localStorage.getItem('refreshToken')).toBe('test-refresh-token')
  })

  it('should clear state on logout', async () => {
    const auth = useAuthStore()

    // Set up initial authenticated state
    localStorage.setItem('accessToken', 'token')
    localStorage.setItem('refreshToken', 'refresh')

    vi.mocked(authApi.logout).mockResolvedValue({} as any)

    await auth.logout()

    expect(auth.user).toBeNull()
    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(localStorage.getItem('refreshToken')).toBeNull()
  })

  it('should detect admin role', async () => {
    const auth = useAuthStore()

    vi.mocked(authApi.login).mockResolvedValue({
      data: {
        data: { accessToken: 'token', refreshToken: 'refresh' },
      },
    } as any)

    vi.mocked(usersApi.getMe).mockResolvedValue({
      data: {
        data: {
          id: '123',
          email: 'admin@example.com',
          firstName: 'Admin',
          lastName: 'User',
          displayName: 'Admin User',
          role: 'SUPERADMIN',
          active: true,
        },
      },
    } as any)

    await auth.login({ email: 'admin@example.com', password: 'password' })

    expect(auth.isAdmin).toBe(true)
    expect(auth.isTeacher).toBe(true) // SUPERADMIN implies teacher permissions
  })

  it('should set loading during login', async () => {
    const auth = useAuthStore()

    let resolveLogin: Function
    vi.mocked(authApi.login).mockReturnValue(
      new Promise((resolve) => { resolveLogin = resolve }) as any
    )

    const loginPromise = auth.login({ email: 'test@example.com', password: 'pw' })
    expect(auth.loading).toBe(true)

    resolveLogin!({
      data: { data: { accessToken: 'token', refreshToken: 'refresh' } },
    })

    vi.mocked(usersApi.getMe).mockResolvedValue({
      data: { data: { id: '1', email: 'test@example.com', role: 'PARENT' } },
    } as any)

    await loginPromise
    expect(auth.loading).toBe(false)
  })
})
