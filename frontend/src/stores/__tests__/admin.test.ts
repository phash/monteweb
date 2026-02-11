import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAdminStore } from '@/stores/admin'

vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn(),
    updateModules: vi.fn(),
    updateTheme: vi.fn(),
  },
}))

import { adminApi } from '@/api/admin.api'

describe('Admin Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should start with null config', () => {
    const store = useAdminStore()
    expect(store.config).toBeNull()
    expect(store.loading).toBe(false)
  })

  it('should fetch config', async () => {
    const store = useAdminStore()
    const mockConfig = {
      schoolName: 'Montessori Schule',
      modules: { messaging: true, files: true, jobboard: false },
    }

    vi.mocked(adminApi.getPublicConfig).mockResolvedValue({
      data: { data: mockConfig },
    } as any)

    await store.fetchConfig()

    expect(store.config).toEqual(mockConfig)
  })

  it('should update modules and set config', async () => {
    const store = useAdminStore()
    const updatedConfig = {
      schoolName: 'Montessori Schule',
      modules: { messaging: true, files: true, jobboard: true },
    }

    vi.mocked(adminApi.updateModules).mockResolvedValue({
      data: { data: updatedConfig },
    } as any)

    await store.updateModules({ jobboard: true })

    expect(store.config).toEqual(updatedConfig)
  })

  it('should update theme and set config', async () => {
    const store = useAdminStore()
    const updatedConfig = {
      schoolName: 'Montessori Schule',
      modules: {},
      theme: { primaryColor: '#ff0000' },
    }

    vi.mocked(adminApi.updateTheme).mockResolvedValue({
      data: { data: updatedConfig },
    } as any)

    await store.updateTheme({ primaryColor: '#ff0000' })

    expect(store.config).toEqual(updatedConfig)
  })

  it('should check if module is enabled', async () => {
    const store = useAdminStore()

    vi.mocked(adminApi.getPublicConfig).mockResolvedValue({
      data: {
        data: {
          modules: { messaging: true, jobboard: false, cleaning: true },
        },
      },
    } as any)

    await store.fetchConfig()

    expect(store.isModuleEnabled('messaging')).toBe(true)
    expect(store.isModuleEnabled('jobboard')).toBe(false)
    expect(store.isModuleEnabled('cleaning')).toBe(true)
    expect(store.isModuleEnabled('nonexistent')).toBe(false)
  })
})
