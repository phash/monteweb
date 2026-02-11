import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import FormsView from '@/views/FormsView.vue'

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn() })),
}))

vi.mock('@/api/forms.api', () => ({
  formsApi: {
    getAvailableForms: vi.fn().mockResolvedValue({ data: { data: { content: [], last: true, totalElements: 0 } } }),
    getMyForms: vi.fn().mockResolvedValue({ data: { data: { content: [], last: true, totalElements: 0 } } }),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      forms: {
        title: 'Formulare',
        create: 'Erstellen',
        noForms: 'Keine Formulare verfügbar.',
        noMyForms: 'Keine eigenen Formulare.',
        tabs: { available: 'Verfügbar', mine: 'Meine' },
        types: { SURVEY: 'Umfrage', CONSENT: 'Einwilligung' },
        scopes: { ROOM: 'Raum', SECTION: 'Bereich', SCHOOL: 'Schule' },
        statuses: { DRAFT: 'Entwurf', PUBLISHED: 'Veröffentlicht', CLOSED: 'Geschlossen', ARCHIVED: 'Archiviert' },
        responded: 'Beantwortet',
        anonymous: 'Anonym',
        questionsCount: 'Fragen',
        responsesCount: 'Antworten',
      },
      common: { loading: 'Laden...' },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title'] },
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  EmptyState: { template: '<div class="empty-stub">{{ message }}</div>', props: ['icon', 'message'] },
  Button: { template: '<button class="button-stub">{{ label }}</button>', props: ['label', 'icon'] },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity', 'size', 'icon'] },
  Tabs: { template: '<div class="tabs-stub"><slot /></div>', props: ['modelValue'] },
  TabList: { template: '<div class="tablist-stub"><slot /></div>' },
  Tab: { template: '<div class="tab-stub"><slot /></div>', props: ['value'] },
  TabPanels: { template: '<div class="tabpanels-stub"><slot /></div>' },
  TabPanel: { template: '<div class="tabpanel-stub"><slot /></div>', props: ['value'] },
  ProgressBar: { template: '<div class="progress-stub" />', props: ['value', 'showValue'] },
  'router-link': { template: '<a class="router-link-stub"><slot /></a>', props: ['to'] },
}

function mountForms() {
  const pinia = createPinia()
  return mount(FormsView, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('FormsView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render component', () => {
    const wrapper = mountForms()
    expect(wrapper.exists()).toBe(true)
  })

  it('should render page title', () => {
    const wrapper = mountForms()
    expect(wrapper.find('.page-title-stub').text()).toContain('Formulare')
  })

  it('should render create button', () => {
    const wrapper = mountForms()
    expect(wrapper.find('.button-stub').text()).toContain('Erstellen')
  })

  it('should have tabs', () => {
    const wrapper = mountForms()
    expect(wrapper.find('.tabs-stub').exists()).toBe(true)
  })
})
