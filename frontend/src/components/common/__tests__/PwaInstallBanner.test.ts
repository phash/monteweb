import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { nextTick, ref } from 'vue'
import PwaInstallBanner from '@/components/common/PwaInstallBanner.vue'

const mockInstall = vi.fn()
const mockDismiss = vi.fn()
const mockIsInstallable = ref(false)
const mockIsInstalled = ref(false)
const mockDismissed = ref(false)

vi.mock('@/composables/usePwaInstall', () => ({
  usePwaInstall: () => ({
    isInstallable: mockIsInstallable,
    isInstalled: mockIsInstalled,
    dismissed: mockDismissed,
    install: mockInstall,
    dismiss: mockDismiss,
    get showBanner() {
      return mockIsInstallable.value && !mockIsInstalled.value && !mockDismissed.value
    },
  }),
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      pwa: {
        installTitle: 'MonteWeb installieren',
        installDescription: 'App auf dem Startbildschirm hinzufügen',
        install: 'Installieren',
      },
      common: {
        close: 'Schließen',
      },
    },
  },
})

function mountBanner() {
  return mount(PwaInstallBanner, {
    global: {
      plugins: [i18n],
      stubs: {
        Button: {
          template: '<button class="btn-stub" @click="$emit(\'click\')">{{ label }}</button>',
          props: ['label', 'icon', 'text', 'severity', 'size', 'ariaLabel'],
          emits: ['click'],
        },
        Transition: {
          template: '<div><slot /></div>',
        },
      },
    },
  })
}

describe('PwaInstallBanner', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockIsInstallable.value = false
    mockIsInstalled.value = false
    mockDismissed.value = false
  })

  it('should not be visible when app is not installable', () => {
    const wrapper = mountBanner()
    expect(wrapper.find('.install-banner').exists()).toBe(false)
  })

  it('should be visible when app is installable', async () => {
    mockIsInstallable.value = true
    const wrapper = mountBanner()
    await nextTick()
    expect(wrapper.find('.install-banner').exists()).toBe(true)
  })

  it('should not be visible when already installed', async () => {
    mockIsInstallable.value = true
    mockIsInstalled.value = true
    const wrapper = mountBanner()
    await nextTick()
    expect(wrapper.find('.install-banner').exists()).toBe(false)
  })

  it('should not be visible when dismissed', async () => {
    mockIsInstallable.value = true
    mockDismissed.value = true
    const wrapper = mountBanner()
    await nextTick()
    expect(wrapper.find('.install-banner').exists()).toBe(false)
  })

  it('should display install title', async () => {
    mockIsInstallable.value = true
    const wrapper = mountBanner()
    await nextTick()
    expect(wrapper.text()).toContain('MonteWeb installieren')
  })

  it('should call install when install button is clicked', async () => {
    mockIsInstallable.value = true
    const wrapper = mountBanner()
    await nextTick()
    const buttons = wrapper.findAll('.btn-stub')
    const installBtn = buttons.find((b) => b.text().includes('Installieren'))
    expect(installBtn).toBeTruthy()
    await installBtn!.trigger('click')
    expect(mockInstall).toHaveBeenCalled()
  })

  it('should call dismiss when close button is clicked', async () => {
    mockIsInstallable.value = true
    const wrapper = mountBanner()
    await nextTick()
    const buttons = wrapper.findAll('.btn-stub')
    // Close button is the second button (no label text)
    const closeBtn = buttons.find((b) => !b.text().includes('Installieren'))
    expect(closeBtn).toBeTruthy()
    await closeBtn!.trigger('click')
    expect(mockDismiss).toHaveBeenCalled()
  })

  it('should have download icon', async () => {
    mockIsInstallable.value = true
    const wrapper = mountBanner()
    await nextTick()
    expect(wrapper.find('.pi-download').exists()).toBe(true)
  })
})
