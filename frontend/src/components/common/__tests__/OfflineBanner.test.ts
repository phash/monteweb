import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import OfflineBanner from '@/components/common/OfflineBanner.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      pwa: {
        offline: 'Keine Internetverbindung',
      },
    },
  },
})

const stubs = {
  Transition: {
    template: '<div><slot /></div>',
  },
}

function createWrapper() {
  return mount(OfflineBanner, {
    global: {
      plugins: [i18n],
      stubs,
    },
  })
}

describe('OfflineBanner', () => {
  let originalOnLine: boolean

  beforeEach(() => {
    vi.clearAllMocks()
    originalOnLine = navigator.onLine
  })

  afterEach(() => {
    // Restore onLine
    Object.defineProperty(navigator, 'onLine', {
      value: originalOnLine,
      writable: true,
      configurable: true,
    })
  })

  it('should not show banner when online', () => {
    Object.defineProperty(navigator, 'onLine', { value: true, configurable: true })
    const wrapper = createWrapper()
    expect(wrapper.find('.offline-banner').exists()).toBe(false)
  })

  it('should show banner when offline', () => {
    Object.defineProperty(navigator, 'onLine', { value: false, configurable: true })
    const wrapper = createWrapper()
    expect(wrapper.find('.offline-banner').exists()).toBe(true)
  })

  it('should display offline message text', () => {
    Object.defineProperty(navigator, 'onLine', { value: false, configurable: true })
    const wrapper = createWrapper()
    expect(wrapper.text()).toContain('Keine Internetverbindung')
  })

  it('should have role=alert for accessibility', () => {
    Object.defineProperty(navigator, 'onLine', { value: false, configurable: true })
    const wrapper = createWrapper()
    expect(wrapper.find('[role="alert"]').exists()).toBe(true)
  })

  it('should show banner when going offline', async () => {
    Object.defineProperty(navigator, 'onLine', { value: true, configurable: true })
    const wrapper = createWrapper()
    expect(wrapper.find('.offline-banner').exists()).toBe(false)
    window.dispatchEvent(new Event('offline'))
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.offline-banner').exists()).toBe(true)
  })

  it('should hide banner when coming back online', async () => {
    Object.defineProperty(navigator, 'onLine', { value: false, configurable: true })
    const wrapper = createWrapper()
    expect(wrapper.find('.offline-banner').exists()).toBe(true)
    window.dispatchEvent(new Event('online'))
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.offline-banner').exists()).toBe(false)
  })

  it('should have wifi-off icon when offline', () => {
    Object.defineProperty(navigator, 'onLine', { value: false, configurable: true })
    const wrapper = createWrapper()
    expect(wrapper.find('.pi-wifi-off').exists()).toBe(true)
  })
})
