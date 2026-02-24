import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ProfileView from '@/views/ProfileView.vue'

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({
  usersApi: {
    getMe: vi.fn(),
    updateMe: vi.fn().mockResolvedValue({}),
    uploadAvatar: vi.fn().mockResolvedValue({}),
    removeAvatar: vi.fn().mockResolvedValue({}),
  },
}))
vi.mock('@/api/client', () => ({
  default: { get: vi.fn(), post: vi.fn() },
}))

vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {}, availableLanguages: ['de', 'en'] } } }),
  },
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      profile: {
        title: 'Mein Profil',
        saved: 'Profil gespeichert',
        pushNotifications: 'Push-Benachrichtigungen',
        enablePush: 'Push aktivieren',
        pushDenied: 'Push blockiert',
      },
      auth: { email: 'E-Mail', firstName: 'Vorname', lastName: 'Nachname', phone: 'Telefon' },
      common: { save: 'Speichern', removeAvatar: 'Avatar entfernen' },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title', 'subtitle'] },
  AvatarUpload: { template: '<div class="avatar-stub" />', props: ['imageUrl', 'size', 'icon', 'editable'] },
  InputText: { template: '<input class="input-stub" />', props: ['modelValue', 'id', 'disabled'] },
  ToggleSwitch: { template: '<input type="checkbox" class="toggle-stub" />', props: ['modelValue'] },
  Button: {
    template: '<button class="button-stub" :type="type">{{ label }}</button>',
    props: ['label', 'type'],
  },
  Message: { template: '<div class="message-stub"><slot /></div>', props: ['severity', 'closable'] },
  LanguageSwitcher: { template: '<div class="language-switcher-stub" />' },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity'] },
  Select: { template: '<select class="select-stub" />', props: ['modelValue', 'options', 'optionLabel', 'optionValue'] },
  Dialog: { template: '<div class="dialog-stub"><slot /><slot name="footer" /></div>', props: ['visible', 'header', 'modal'] },
  Password: { template: '<input type="password" class="password-stub" />', props: ['modelValue', 'feedback', 'toggleMask'] },
}

function mountProfile() {
  const pinia = createPinia()
  return mount(ProfileView, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('ProfileView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render page title', () => {
    const wrapper = mountProfile()
    expect(wrapper.find('.page-title-stub').text()).toContain('Profil')
  })

  it('should render avatar upload', () => {
    const wrapper = mountProfile()
    expect(wrapper.find('.avatar-stub').exists()).toBe(true)
  })

  it('should render form fields', () => {
    const wrapper = mountProfile()
    expect(wrapper.findAll('.input-stub').length).toBeGreaterThanOrEqual(3)
  })

  it('should render save button', () => {
    const wrapper = mountProfile()
    expect(wrapper.find('.button-stub').text()).toContain('Speichern')
  })

  it('should render form element', () => {
    const wrapper = mountProfile()
    expect(wrapper.find('form').exists()).toBe(true)
  })
})
