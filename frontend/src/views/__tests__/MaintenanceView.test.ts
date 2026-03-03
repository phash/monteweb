import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'

// --- mock admin store
const mockAdminStore = {
  config: null as any,
  loading: false,
  fetchConfig: vi.fn().mockResolvedValue(undefined),
  isModuleEnabled: vi.fn((name: string) => name === 'maintenance'),
}

vi.mock('@/stores/admin', () => ({
  useAdminStore: vi.fn(() => mockAdminStore),
}))

const mockRouterPush = vi.fn()
const mockRouterReplace = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: mockRouterPush, replace: mockRouterReplace })),
  useRoute: vi.fn(() => ({ params: {}, query: {} })),
}))

import MaintenanceView from '@/views/MaintenanceView.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      maintenance: {
        title: 'Wartungsmodus',
        defaultMessage: 'Das System wird gerade gewartet.',
        loginAsAdmin: 'Als Admin anmelden',
      },
    },
  },
})

const globalStubs = {
  Button: {
    template: '<button @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'text'],
    emits: ['click'],
  },
}

function mountView() {
  return mount(MaintenanceView, {
    global: { plugins: [i18n], stubs: globalStubs },
  })
}

describe('MaintenanceView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    mockAdminStore.config = { maintenanceMessage: 'Wartung bis 18:00 Uhr', modules: { maintenance: true } }
    mockAdminStore.fetchConfig.mockResolvedValue(undefined)
    mockAdminStore.isModuleEnabled = vi.fn((name: string) => name === 'maintenance')
  })

  it('should mount without crashing', () => {
    const wrapper = mountView()
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('should render the maintenance title', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('h1').text()).toBe('Wartungsmodus')
    wrapper.unmount()
  })

  it('should fetch config on mount', async () => {
    mountView()
    await flushPromises()
    expect(mockAdminStore.fetchConfig).toHaveBeenCalled()
  })

  it('should show custom maintenance message from config', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.maintenance-message').text()).toContain('Wartung bis 18:00 Uhr')
    wrapper.unmount()
  })

  it('should show default message when no custom message is set', async () => {
    mockAdminStore.config = { maintenanceMessage: null, modules: { maintenance: true } }
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.maintenance-message').text()).toContain('Das System wird gerade gewartet.')
    wrapper.unmount()
  })

  it('should show default message on fetch error', async () => {
    mockAdminStore.fetchConfig.mockRejectedValue(new Error('fail'))
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.maintenance-message').text()).toContain('Das System wird gerade gewartet.')
    wrapper.unmount()
  })

  it('should render login button', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Als Admin anmelden')
    wrapper.unmount()
  })

  it('should navigate to login when login button is clicked', async () => {
    const wrapper = mountView()
    await flushPromises()
    await wrapper.find('button').trigger('click')
    expect(mockRouterPush).toHaveBeenCalledWith('/login')
    wrapper.unmount()
  })

  it('should render wrench icon', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.pi-wrench').exists()).toBe(true)
    wrapper.unmount()
  })

  it('should redirect to home when maintenance is not enabled', async () => {
    mockAdminStore.isModuleEnabled = vi.fn(() => false)
    mountView()
    await flushPromises()
    expect(mockRouterReplace).toHaveBeenCalledWith('/')
  })
})
