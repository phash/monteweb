import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { nextTick } from 'vue'
import { flushPromises } from '@vue/test-utils'

// We dynamically import useDarkMode after vi.resetModules() to get fresh module state
let useDarkMode: typeof import('@/composables/useDarkMode').useDarkMode
let usersApi: typeof import('@/api/users.api').usersApi
let useAuthStore: typeof import('@/stores/auth').useAuthStore

describe('useDarkMode', () => {
  beforeEach(async () => {
    vi.resetModules()

    vi.doMock('@/api/users.api', () => ({
      usersApi: {
        updateDarkMode: vi.fn().mockResolvedValue({ data: { data: { darkMode: 'DARK' } } }),
        getDarkMode: vi.fn().mockResolvedValue({ data: { data: { darkMode: 'SYSTEM' } } }),
      },
    }))
    vi.doMock('@/api/auth.api', () => ({ authApi: {} }))
    vi.doMock('@/api/client', () => ({
      default: { get: vi.fn(), post: vi.fn(), put: vi.fn() },
    }))
    vi.doMock('@/api/admin.api', () => ({
      adminApi: { getPublicConfig: vi.fn() },
    }))

    localStorage.clear()
    document.documentElement.classList.remove('dark')

    const darkMod = await import('@/composables/useDarkMode')
    useDarkMode = darkMod.useDarkMode
    const apiMod = await import('@/api/users.api')
    usersApi = apiMod.usersApi
    const authMod = await import('@/stores/auth')
    useAuthStore = authMod.useAuthStore

    setActivePinia(createPinia())
  })

  afterEach(() => {
    document.documentElement.classList.remove('dark')
    localStorage.clear()
  })

  function setupAuth(darkMode = 'SYSTEM') {
    // accessToken must be in localStorage BEFORE creating the store
    // because isAuthenticated = computed(() => !!accessToken.value)
    // and accessToken reads from localStorage on init
    localStorage.setItem('accessToken', 'test-token')

    // Re-create pinia so the store initializes with the token
    const pinia = createPinia()
    setActivePinia(pinia)

    const auth = useAuthStore()
    auth.user = {
      id: 'u1',
      email: 'test@test.com',
      firstName: 'Test',
      lastName: 'User',
      displayName: 'Test User',
      phone: null,
      avatarUrl: null,
      role: 'PARENT',
      specialRoles: [],
      assignedRoles: [],
      active: true,
      darkMode,
    } as any
    return auth
  }

  it('should return preference, loadFromUser, and setPreference', () => {
    const { preference, loadFromUser, setPreference } = useDarkMode()
    expect(preference).toBeDefined()
    expect(typeof loadFromUser).toBe('function')
    expect(typeof setPreference).toBe('function')
  })

  it('should default preference to SYSTEM when localStorage is empty', () => {
    const { preference } = useDarkMode()
    expect(preference.value).toBe('SYSTEM')
  })

  it('should read preference from localStorage on init', async () => {
    vi.resetModules()
    localStorage.setItem('darkMode', 'DARK')

    vi.doMock('@/api/users.api', () => ({
      usersApi: {
        updateDarkMode: vi.fn().mockResolvedValue({}),
        getDarkMode: vi.fn().mockResolvedValue({}),
      },
    }))
    vi.doMock('@/api/auth.api', () => ({ authApi: {} }))
    vi.doMock('@/api/client', () => ({ default: { get: vi.fn(), post: vi.fn(), put: vi.fn() } }))
    vi.doMock('@/api/admin.api', () => ({ adminApi: { getPublicConfig: vi.fn() } }))
    setActivePinia(createPinia())

    const mod = await import('@/composables/useDarkMode')
    const { preference } = mod.useDarkMode()
    expect(preference.value).toBe('DARK')
  })

  it('should persist preference to localStorage when changed via setPreference', async () => {
    const { setPreference } = useDarkMode()
    await setPreference('DARK')
    await nextTick()
    expect(localStorage.getItem('darkMode')).toBe('DARK')
  })

  it('should apply dark class when setPreference is DARK', async () => {
    const { setPreference } = useDarkMode()
    await setPreference('DARK')
    await nextTick()
    expect(document.documentElement.classList.contains('dark')).toBe(true)
  })

  it('should remove dark class when setPreference is LIGHT', async () => {
    document.documentElement.classList.add('dark')
    const { setPreference } = useDarkMode()
    await setPreference('LIGHT')
    await nextTick()
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })

  it('should call API when authenticated and setPreference is called', async () => {
    setupAuth()
    const { setPreference } = useDarkMode()

    await setPreference('DARK')
    await flushPromises()
    expect(usersApi.updateDarkMode).toHaveBeenCalledWith('DARK')
  })

  it('should not call API when not authenticated', async () => {
    const { setPreference } = useDarkMode()
    await setPreference('LIGHT')
    await flushPromises()
    expect(usersApi.updateDarkMode).not.toHaveBeenCalled()
  })

  it('loadFromUser should update preference from auth user darkMode', async () => {
    setupAuth('DARK')
    const { preference, loadFromUser } = useDarkMode()

    await loadFromUser()
    expect(preference.value).toBe('DARK')
  })

  it('loadFromUser should do nothing when not authenticated', async () => {
    const { preference, loadFromUser } = useDarkMode()
    await loadFromUser()
    expect(preference.value).toBe('SYSTEM')
  })

  it('loadFromUser should ignore invalid dark mode values', async () => {
    setupAuth('INVALID')
    const { preference, loadFromUser } = useDarkMode()

    await loadFromUser()
    expect(preference.value).toBe('SYSTEM')
  })

  it('should handle API error gracefully in setPreference', async () => {
    vi.mocked(usersApi.updateDarkMode).mockRejectedValueOnce(new Error('Network error'))
    setupAuth()
    const { preference, setPreference } = useDarkMode()

    // Should not throw
    await setPreference('DARK')
    await flushPromises()
    expect(preference.value).toBe('DARK')
    expect(localStorage.getItem('darkMode')).toBe('DARK')
  })

  it('should call API with SYSTEM preference', async () => {
    setupAuth()
    const { setPreference } = useDarkMode()

    // First change to DARK to ensure preference changes
    await setPreference('DARK')
    await flushPromises()
    vi.mocked(usersApi.updateDarkMode).mockClear()

    await setPreference('SYSTEM')
    await flushPromises()
    expect(usersApi.updateDarkMode).toHaveBeenCalledWith('SYSTEM')
  })
})
