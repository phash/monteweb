import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, nextTick } from 'vue'

// Helper component to test the composable (needs lifecycle hooks)
function createTestComponent(composableFn: () => any) {
  return defineComponent({
    setup() {
      return composableFn()
    },
    template: '<div />',
  })
}

describe('usePwaInstall', () => {
  let matchMediaMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    vi.clearAllMocks()
    // Reset modules to clear shared module-level refs between tests
    vi.resetModules()
    localStorage.clear()

    matchMediaMock = vi.fn(() => ({
      matches: false,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    }))
    Object.defineProperty(window, 'matchMedia', {
      value: matchMediaMock,
      writable: true,
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  async function getComposable() {
    const mod = await import('@/composables/usePwaInstall')
    return mod.usePwaInstall
  }

  it('should return expected properties', async () => {
    const usePwaInstall = await getComposable()
    const wrapper = mount(createTestComponent(usePwaInstall))
    const vm = wrapper.vm as any
    expect(vm).toHaveProperty('isInstallable')
    expect(vm).toHaveProperty('isInstalled')
    expect(vm).toHaveProperty('dismissed')
    expect(vm).toHaveProperty('install')
    expect(vm).toHaveProperty('dismiss')
    expect(vm).toHaveProperty('showBanner')
  })

  it('should detect standalone mode as already installed', async () => {
    matchMediaMock.mockReturnValue({ matches: true })
    const usePwaInstall = await getComposable()
    const wrapper = mount(createTestComponent(usePwaInstall))
    expect((wrapper.vm as any).isInstalled).toBe(true)
  })

  it('should not be installable by default', async () => {
    const usePwaInstall = await getComposable()
    const wrapper = mount(createTestComponent(usePwaInstall))
    expect((wrapper.vm as any).isInstallable).toBe(false)
    expect((wrapper.vm as any).showBanner).toBe(false)
  })

  it('should become installable on beforeinstallprompt event', async () => {
    const usePwaInstall = await getComposable()
    const wrapper = mount(createTestComponent(usePwaInstall))
    const event = new Event('beforeinstallprompt')
    Object.assign(event, {
      prompt: vi.fn(),
      userChoice: Promise.resolve({ outcome: 'accepted' }),
    })
    window.dispatchEvent(event)
    await nextTick()
    expect((wrapper.vm as any).isInstallable).toBe(true)
    expect((wrapper.vm as any).showBanner).toBe(true)
  })

  it('should set dismissed flag and store timestamp', async () => {
    const usePwaInstall = await getComposable()
    const wrapper = mount(createTestComponent(usePwaInstall))
    // First make installable
    const event = new Event('beforeinstallprompt')
    Object.assign(event, { prompt: vi.fn(), userChoice: Promise.resolve({ outcome: 'accepted' }) })
    window.dispatchEvent(event)
    await nextTick()

    ;(wrapper.vm as any).dismiss()
    expect((wrapper.vm as any).dismissed).toBe(true)
    expect(localStorage.getItem('pwa-install-dismissed')).toBeTruthy()
    expect((wrapper.vm as any).showBanner).toBe(false)
  })

  it('should respect 7-day dismiss window', async () => {
    // Set dismissed 3 days ago
    const threeDaysAgo = Date.now() - 3 * 24 * 60 * 60 * 1000
    localStorage.setItem('pwa-install-dismissed', threeDaysAgo.toString())

    const usePwaInstall = await getComposable()
    const wrapper = mount(createTestComponent(usePwaInstall))
    const event = new Event('beforeinstallprompt')
    Object.assign(event, { prompt: vi.fn(), userChoice: Promise.resolve({ outcome: 'accepted' }) })
    window.dispatchEvent(event)
    await nextTick()

    // Should still be dismissed (within 7 days)
    expect((wrapper.vm as any).dismissed).toBe(true)
    expect((wrapper.vm as any).showBanner).toBe(false)
  })

  it('should clear dismiss after 7 days', async () => {
    // Set dismissed 8 days ago
    const eightDaysAgo = Date.now() - 8 * 24 * 60 * 60 * 1000
    localStorage.setItem('pwa-install-dismissed', eightDaysAgo.toString())

    const usePwaInstall = await getComposable()
    const wrapper = mount(createTestComponent(usePwaInstall))
    const event = new Event('beforeinstallprompt')
    Object.assign(event, { prompt: vi.fn(), userChoice: Promise.resolve({ outcome: 'accepted' }) })
    window.dispatchEvent(event)
    await nextTick()

    // Should not be dismissed anymore (past 7 days)
    expect((wrapper.vm as any).dismissed).toBe(false)
    expect(localStorage.getItem('pwa-install-dismissed')).toBeNull()
    expect((wrapper.vm as any).showBanner).toBe(true)
  })

  it('should call prompt() on install', async () => {
    const usePwaInstall = await getComposable()
    const wrapper = mount(createTestComponent(usePwaInstall))
    const promptMock = vi.fn()
    const event = new Event('beforeinstallprompt')
    Object.assign(event, {
      prompt: promptMock,
      userChoice: Promise.resolve({ outcome: 'accepted' }),
    })
    window.dispatchEvent(event)
    await nextTick()

    await (wrapper.vm as any).install()
    expect(promptMock).toHaveBeenCalled()
    expect((wrapper.vm as any).isInstalled).toBe(true)
  })

  it('should handle dismissed install outcome', async () => {
    const usePwaInstall = await getComposable()
    const wrapper = mount(createTestComponent(usePwaInstall))
    const event = new Event('beforeinstallprompt')
    Object.assign(event, {
      prompt: vi.fn(),
      userChoice: Promise.resolve({ outcome: 'dismissed' }),
    })
    window.dispatchEvent(event)
    await nextTick()

    await (wrapper.vm as any).install()
    expect((wrapper.vm as any).isInstalled).toBe(false)
  })

  it('should handle appinstalled event', async () => {
    const usePwaInstall = await getComposable()
    const wrapper = mount(createTestComponent(usePwaInstall))
    window.dispatchEvent(new Event('appinstalled'))
    await nextTick()
    expect((wrapper.vm as any).isInstalled).toBe(true)
    expect((wrapper.vm as any).isInstallable).toBe(false)
  })

  it('should not call prompt if no deferred event', async () => {
    const usePwaInstall = await getComposable()
    const wrapper = mount(createTestComponent(usePwaInstall))
    // install() without beforeinstallprompt should be a no-op
    await (wrapper.vm as any).install()
    expect((wrapper.vm as any).isInstalled).toBe(false)
  })
})
