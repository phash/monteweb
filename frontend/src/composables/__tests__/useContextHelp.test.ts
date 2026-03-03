import { describe, it, expect, beforeEach, vi } from 'vitest'
import { ref, reactive } from 'vue'
import { setActivePinia, createPinia } from 'pinia'

const mockRoute = reactive({ name: 'dashboard' as string | undefined })

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
}))

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/client', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn() },
}))
vi.mock('@/api/admin.api', () => ({
  adminApi: { getPublicConfig: vi.fn() },
}))

import { useAuthStore } from '@/stores/auth'
import { useContextHelp } from '../useContextHelp'

describe('useContextHelp', () => {
  let authStore: ReturnType<typeof useAuthStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    authStore = useAuthStore()
    mockRoute.name = 'dashboard'
  })

  function setupUser(role: string) {
    authStore.user = {
      id: 'u1',
      email: 'test@test.com',
      firstName: 'Test',
      lastName: 'User',
      displayName: 'Test User',
      phone: null,
      avatarUrl: null,
      role,
      specialRoles: [],
      assignedRoles: [],
      active: true,
    } as any
  }

  it('should return pageTitle for known route', () => {
    setupUser('PARENT')
    const { pageTitle } = useContextHelp()
    expect(pageTitle.value).toBe('help.pages.dashboard.title')
  })

  it('should return fallback pageTitle for unknown route', () => {
    mockRoute.name = 'some-unknown-route'
    setupUser('PARENT')
    const { pageTitle } = useContextHelp()
    expect(pageTitle.value).toBe('help.contextHelp')
  })

  it('should return actions for PARENT on dashboard', () => {
    setupUser('PARENT')
    const { actions } = useContextHelp()
    expect(actions.value.length).toBeGreaterThan(0)
    expect(actions.value[0]).toBe('help.pages.dashboard.parent.action1')
  })

  it('should return tips for TEACHER on dashboard', () => {
    setupUser('TEACHER')
    const { tips } = useContextHelp()
    expect(tips.value.length).toBeGreaterThan(0)
    expect(tips.value[0]).toBe('help.pages.dashboard.teacher.tip1')
  })

  it('should return SUPERADMIN actions for admin routes', () => {
    mockRoute.name = 'admin-dashboard'
    setupUser('SUPERADMIN')
    const { actions } = useContextHelp()
    expect(actions.value.length).toBeGreaterThan(0)
    expect(actions.value[0]).toBe('help.pages.adminDashboard.action1')
  })

  it('should use general help when role-specific help is not available', () => {
    mockRoute.name = 'discover-rooms'
    setupUser('PARENT')
    const { actions } = useContextHelp()
    // discover-rooms has general help (no role-specific)
    expect(actions.value.length).toBeGreaterThan(0)
    expect(actions.value[0]).toBe('help.pages.discover.action1')
  })

  it('should use general help for profile route (all roles)', () => {
    mockRoute.name = 'profile'
    setupUser('STUDENT')
    const { actions } = useContextHelp()
    expect(actions.value.length).toBeGreaterThan(0)
    expect(actions.value[0]).toBe('help.pages.profile.action1')
  })

  it('should return hasHelp true when actions or tips exist', () => {
    setupUser('PARENT')
    const { hasHelp } = useContextHelp()
    expect(hasHelp.value).toBe(true)
  })

  it('should return hasHelp false for unknown route', () => {
    mockRoute.name = 'nonexistent-page'
    setupUser('PARENT')
    const { hasHelp } = useContextHelp()
    expect(hasHelp.value).toBe(false)
  })

  it('should return empty actions for unknown route', () => {
    mockRoute.name = 'nonexistent-page'
    setupUser('PARENT')
    const { actions } = useContextHelp()
    expect(actions.value).toEqual([])
  })

  it('should return empty tips for unknown route', () => {
    mockRoute.name = 'nonexistent-page'
    setupUser('PARENT')
    const { tips } = useContextHelp()
    expect(tips.value).toEqual([])
  })

  it('should return empty actions/tips when user has no role match and no general help', () => {
    mockRoute.name = 'admin-dashboard'
    setupUser('STUDENT') // STUDENT has no admin-dashboard help
    const { actions, tips, hasHelp } = useContextHelp()
    expect(actions.value).toEqual([])
    expect(tips.value).toEqual([])
    expect(hasHelp.value).toBe(false)
  })

  it('should handle route name being undefined', () => {
    mockRoute.name = undefined
    setupUser('PARENT')
    const { hasHelp } = useContextHelp()
    expect(hasHelp.value).toBe(false)
  })

  it('should return correct content for rooms route', () => {
    mockRoute.name = 'rooms'
    setupUser('TEACHER')
    const { actions, tips } = useContextHelp()
    expect(actions.value[0]).toBe('help.pages.rooms.teacher.action1')
    expect(tips.value[0]).toBe('help.pages.rooms.teacher.tip1')
  })

  it('should return SECTION_ADMIN content for section-admin route', () => {
    mockRoute.name = 'section-admin'
    setupUser('SECTION_ADMIN')
    const { actions } = useContextHelp()
    expect(actions.value[0]).toBe('help.pages.sectionAdmin.action1')
  })
})
