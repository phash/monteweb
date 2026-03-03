import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ImpersonationBanner from '@/components/common/ImpersonationBanner.vue'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/auth.api', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    stopImpersonation: vi.fn().mockResolvedValue({ data: { data: { accessToken: 'tok', refreshToken: 'ref' } } }),
  },
}))

vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
    getConfig: vi.fn().mockResolvedValue({ data: { data: {} } }),
  },
}))

vi.mock('@/api/users.api', () => ({
  usersApi: { getMe: vi.fn() },
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      auth: {
        impersonation: {
          banner: 'Sie agieren als {name}',
          stop: 'Beenden',
        },
      },
    },
  },
})

const stubs = {
  Button: {
    template: '<button class="btn-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'size', 'severity'],
    emits: ['click'],
  },
}

function createWrapper(impersonating = false, displayName = 'Max Mustermann') {
  const pinia = createPinia()
  setActivePinia(pinia)
  const auth = useAuthStore()
  if (impersonating) {
    auth.$patch({
      user: {
        id: 'user-2',
        email: 'max@test.de',
        firstName: 'Max',
        lastName: 'Mustermann',
        displayName,
        role: 'PARENT',
        active: true,
      } as any,
      impersonatedBy: 'admin-user-1',
    })
  }
  return mount(ImpersonationBanner, {
    global: {
      plugins: [pinia, i18n],
      stubs,
    },
  })
}

describe('ImpersonationBanner', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should not render when not impersonating', () => {
    const wrapper = createWrapper(false)
    expect(wrapper.find('.impersonation-banner').exists()).toBe(false)
  })

  it('should render banner when impersonating', () => {
    const wrapper = createWrapper(true)
    expect(wrapper.find('.impersonation-banner').exists()).toBe(true)
  })

  it('should display impersonated user name', () => {
    const wrapper = createWrapper(true, 'Anna Schmidt')
    expect(wrapper.text()).toContain('Anna Schmidt')
  })

  it('should display warning message', () => {
    const wrapper = createWrapper(true)
    expect(wrapper.text()).toContain('Sie agieren als Max Mustermann')
  })

  it('should display warning icon', () => {
    const wrapper = createWrapper(true)
    expect(wrapper.find('.pi-exclamation-triangle').exists()).toBe(true)
  })

  it('should display stop impersonation button', () => {
    const wrapper = createWrapper(true)
    expect(wrapper.text()).toContain('Beenden')
  })

  it('should call stopImpersonation on button click', async () => {
    // Mock window.location.reload
    const reloadMock = vi.fn()
    Object.defineProperty(window, 'location', {
      value: { ...window.location, reload: reloadMock },
      writable: true,
      configurable: true,
    })

    const wrapper = createWrapper(true)
    const auth = useAuthStore()
    const stopSpy = vi.spyOn(auth, 'stopImpersonation').mockResolvedValue()

    const stopBtn = wrapper.find('.btn-stub')
    await stopBtn.trigger('click')

    expect(stopSpy).toHaveBeenCalled()
  })
})
