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
    switchActiveRole: vi.fn(),
  },
}))

vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
    getConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
  },
}))

import { usersApi } from '@/api/users.api'

describe('Auth Store - Multi-Role', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  describe('canSwitchRole', () => {
    it('should return true when assignedRoles has more than 1 role', () => {
      const auth = useAuthStore()
      auth.user = {
        role: 'TEACHER',
        assignedRoles: ['TEACHER', 'PARENT'],
      } as any

      expect(auth.canSwitchRole).toBe(true)
    })

    it('should return false when only 1 assigned role', () => {
      const auth = useAuthStore()
      auth.user = {
        role: 'TEACHER',
        assignedRoles: ['TEACHER'],
      } as any

      expect(auth.canSwitchRole).toBe(false)
    })

    it('should return false for SUPERADMIN (empty assignedRoles)', () => {
      const auth = useAuthStore()
      auth.user = {
        role: 'SUPERADMIN',
        assignedRoles: [],
      } as any

      expect(auth.canSwitchRole).toBe(false)
    })

    it('should return false when assignedRoles is undefined', () => {
      const auth = useAuthStore()
      auth.user = {
        role: 'PARENT',
      } as any

      expect(auth.canSwitchRole).toBe(false)
    })

    it('should return false when user is null', () => {
      const auth = useAuthStore()
      expect(auth.canSwitchRole).toBe(false)
    })
  })

  describe('assignedRoles', () => {
    it('should return assigned roles from user', () => {
      const auth = useAuthStore()
      auth.user = {
        role: 'TEACHER',
        assignedRoles: ['TEACHER', 'PARENT', 'SECTION_ADMIN'],
      } as any

      expect(auth.assignedRoles).toEqual(['TEACHER', 'PARENT', 'SECTION_ADMIN'])
    })

    it('should return empty array when user is null', () => {
      const auth = useAuthStore()
      expect(auth.assignedRoles).toEqual([])
    })
  })

  describe('switchRole', () => {
    it('should call API and update tokens', async () => {
      setActivePinia(createPinia())
      sessionStorage.setItem('accessToken', 'old-token')
      const auth = useAuthStore()

      vi.mocked(usersApi.switchActiveRole).mockResolvedValue({
        data: {
          data: {
            accessToken: 'new-access-token',
            refreshToken: 'new-refresh-token',
            userId: 'user-1',
            email: 'test@example.com',
            role: 'PARENT',
          },
        },
      } as any)

      vi.mocked(usersApi.getMe).mockResolvedValue({
        data: {
          data: {
            id: 'user-1',
            email: 'test@example.com',
            role: 'PARENT',
            assignedRoles: ['TEACHER', 'PARENT'],
          },
        },
      } as any)

      await auth.switchRole('PARENT')

      expect(usersApi.switchActiveRole).toHaveBeenCalledWith('PARENT')
      expect(sessionStorage.getItem('accessToken')).toBe('new-access-token')
      expect(sessionStorage.getItem('refreshToken')).toBe('new-refresh-token')
    })

    it('should update user role in state after switch', async () => {
      setActivePinia(createPinia())
      sessionStorage.setItem('accessToken', 'old-token')
      const auth = useAuthStore()
      auth.user = {
        id: 'user-1',
        role: 'TEACHER',
        assignedRoles: ['TEACHER', 'PARENT'],
      } as any

      vi.mocked(usersApi.switchActiveRole).mockResolvedValue({
        data: {
          data: {
            accessToken: 'new-token',
            refreshToken: 'new-refresh',
            userId: 'user-1',
            email: 'test@example.com',
            role: 'PARENT',
          },
        },
      } as any)

      vi.mocked(usersApi.getMe).mockResolvedValue({
        data: {
          data: {
            id: 'user-1',
            email: 'test@example.com',
            role: 'PARENT',
            assignedRoles: ['TEACHER', 'PARENT'],
          },
        },
      } as any)

      await auth.switchRole('PARENT')

      expect(auth.user?.role).toBe('PARENT')
    })

    it('should set loading during switch', async () => {
      setActivePinia(createPinia())
      sessionStorage.setItem('accessToken', 'token')
      const auth = useAuthStore()

      let resolveSwitch!: Function
      vi.mocked(usersApi.switchActiveRole).mockReturnValue(
        new Promise((resolve) => { resolveSwitch = resolve }) as any
      )

      const switchPromise = auth.switchRole('PARENT')
      expect(auth.loading).toBe(true)

      resolveSwitch({
        data: { data: { accessToken: 'token', refreshToken: 'refresh' } },
      })

      vi.mocked(usersApi.getMe).mockResolvedValue({
        data: { data: { id: '1', role: 'PARENT', assignedRoles: ['TEACHER', 'PARENT'] } },
      } as any)

      await switchPromise
      expect(auth.loading).toBe(false)
    })
  })

  describe('role computeds after switch', () => {
    it('isTeacher should update when switching from PARENT to TEACHER', () => {
      const auth = useAuthStore()
      auth.user = {
        role: 'PARENT',
        assignedRoles: ['TEACHER', 'PARENT'],
      } as any

      expect(auth.isTeacher).toBe(false)

      // Simulate role change
      auth.user = {
        role: 'TEACHER',
        assignedRoles: ['TEACHER', 'PARENT'],
      } as any

      expect(auth.isTeacher).toBe(true)
    })

    it('isSectionAdmin should update when switching to SECTION_ADMIN', () => {
      const auth = useAuthStore()
      auth.user = {
        role: 'PARENT',
        assignedRoles: ['PARENT', 'SECTION_ADMIN'],
      } as any

      expect(auth.isSectionAdmin).toBe(false)

      auth.user = {
        role: 'SECTION_ADMIN',
        assignedRoles: ['PARENT', 'SECTION_ADMIN'],
      } as any

      expect(auth.isSectionAdmin).toBe(true)
    })

    it('canHaveFamily should change based on role', () => {
      const auth = useAuthStore()

      auth.user = {
        role: 'TEACHER',
        assignedRoles: ['TEACHER', 'PARENT'],
      } as any
      expect(auth.canHaveFamily).toBe(false)

      auth.user = {
        role: 'PARENT',
        assignedRoles: ['TEACHER', 'PARENT'],
      } as any
      expect(auth.canHaveFamily).toBe(true)
    })
  })
})
