import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/admin.api', () => ({
  adminApi: { getPublicConfig: vi.fn() },
}))

import { useAdminStore } from '@/stores/admin'
import { useTheme } from '@/composables/useTheme'

describe('useTheme', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    document.documentElement.style.cssText = ''
  })

  it('should return applyTheme function', () => {
    const { applyTheme } = useTheme()
    expect(typeof applyTheme).toBe('function')
  })

  it('should apply theme CSS variables', () => {
    const { applyTheme } = useTheme()
    applyTheme({ primaryColor: '#3b82f6', bgMain: '#ffffff' })
    expect(document.documentElement.style.getPropertyValue('--mw-primary')).toBe('#3b82f6')
    // bgMain maps to the real --mw-bg token (previously the dead --mw-bg-main)
    expect(document.documentElement.style.getPropertyValue('--mw-bg')).toBe('#ffffff')
  })

  it('should not crash when theme is null', () => {
    const { applyTheme } = useTheme()
    expect(() => applyTheme(null)).not.toThrow()
  })
})
