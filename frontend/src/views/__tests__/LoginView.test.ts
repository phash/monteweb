import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import LoginView from '@/views/LoginView.vue'

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn() })),
  useRoute: vi.fn(() => ({ query: {} })),
}))

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn().mockRejectedValue(new Error('not available')),
    post: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}))

vi.mock('@/api/auth.api', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
  },
}))

vi.mock('@/api/users.api', () => ({
  usersApi: { getMe: vi.fn() },
}))

vi.mock('@/api/admin.api', () => ({
  adminApi: { getPublicConfig: vi.fn() },
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      auth: {
        login: 'Anmelden',
        register: 'Registrieren',
        email: 'E-Mail',
        password: 'Passwort',
        firstName: 'Vorname',
        lastName: 'Nachname',
        phone: 'Telefon',
        noAccount: 'Noch kein Konto?',
        hasAccount: 'Schon ein Konto?',
        or: 'oder',
        loginSso: 'SSO Login',
        loginError: 'Anmeldung fehlgeschlagen',
        registerError: 'Registrierung fehlgeschlagen',
      },
    },
  },
})

const stubs = {
  InputText: { template: '<input class="input-stub" />', props: ['modelValue', 'type', 'id', 'disabled'] },
  Password: { template: '<input class="password-stub" />', props: ['modelValue', 'feedback', 'toggleMask', 'id'] },
  Button: {
    template: '<button class="button-stub" :type="type" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'type', 'loading', 'severity', 'outlined'],
    emits: ['click'],
  },
  Divider: { template: '<hr class="divider-stub" />', props: ['align'] },
  Message: { template: '<div class="message-stub"><slot /></div>', props: ['severity', 'closable'] },
}

function mountLogin() {
  const pinia = createPinia()
  return mount(LoginView, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('LoginView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render MonteWeb title', () => {
    const wrapper = mountLogin()
    expect(wrapper.find('.login-title').text()).toBe('MonteWeb')
  })

  it('should show login mode by default', () => {
    const wrapper = mountLogin()
    expect(wrapper.text()).toContain('Anmelden')
  })

  it('should render email and password fields', () => {
    const wrapper = mountLogin()
    expect(wrapper.find('.input-stub').exists()).toBe(true)
    expect(wrapper.find('.password-stub').exists()).toBe(true)
  })

  it('should have toggle link to register', () => {
    const wrapper = mountLogin()
    expect(wrapper.text()).toContain('Noch kein Konto?')
  })

  it('should switch to register mode on toggle click', async () => {
    const wrapper = mountLogin()
    const link = wrapper.find('.login-toggle a')
    await link.trigger('click')
    expect(wrapper.text()).toContain('Registrieren')
  })

  it('should render form', () => {
    const wrapper = mountLogin()
    expect(wrapper.find('form').exists()).toBe(true)
  })
})
