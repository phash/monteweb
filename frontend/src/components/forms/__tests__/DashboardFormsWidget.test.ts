import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import DashboardFormsWidget from '@/components/forms/DashboardFormsWidget.vue'
import { useFormsStore } from '@/stores/forms'

vi.mock('@/api/forms.api', () => ({
  formsApi: {
    getAvailableForms: vi.fn().mockResolvedValue({
      data: { data: { content: [], totalElements: 0 } },
    }),
    getMyForms: vi.fn().mockResolvedValue({ data: { data: { content: [] } } }),
    getForm: vi.fn(),
  },
}))

vi.mock('@/composables/useLocaleDate', () => ({
  useLocaleDate: vi.fn(() => ({
    formatShortDate: vi.fn((d: string) => d),
    formatCompactDateTime: vi.fn((d: string) => d),
  })),
}))

const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: mockPush })),
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      forms: {
        pendingForms: 'Offene Formulare',
        viewAll: 'Alle anzeigen',
        types: {
          SURVEY: 'Umfrage',
          CONSENT: 'Einwilligung',
        },
        scopes: {
          SCHOOL: 'Schulweit',
          SECTION: 'Bereich',
          ROOM: 'Raum',
        },
      },
    },
  },
})

const stubs = {
  Tag: {
    template: '<span class="tag-stub" :class="severity">{{ value }}</span>',
    props: ['value', 'severity', 'size'],
  },
  Button: {
    template: '<button class="btn-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'text', 'size'],
    emits: ['click'],
  },
  'router-link': {
    template: '<a class="router-link-stub" :href="to?.name"><slot /></a>',
    props: ['to'],
  },
}

function makePendingForm(overrides = {}) {
  return {
    id: 'form-1',
    title: 'Elternbefragung',
    description: 'Feedback',
    type: 'SURVEY',
    scope: 'SCHOOL',
    scopeId: null,
    scopeName: null,
    sectionIds: [],
    sectionNames: [],
    status: 'PUBLISHED',
    anonymous: false,
    deadline: '2026-04-01',
    questionCount: 5,
    responseCount: 10,
    targetCount: 50,
    hasUserResponded: false,
    createdAt: '2026-01-01',
    ...overrides,
  }
}

function createWrapper(forms: any[] = []) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const store = useFormsStore()
  store.$patch({ availableForms: forms })
  return mount(DashboardFormsWidget, {
    global: {
      plugins: [pinia, i18n],
      stubs,
    },
  })
}

describe('DashboardFormsWidget', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should not render when no pending forms', () => {
    const wrapper = createWrapper([])
    expect(wrapper.find('.forms-widget').exists()).toBe(false)
  })

  it('should not render when all forms are responded', () => {
    const wrapper = createWrapper([makePendingForm({ hasUserResponded: true })])
    expect(wrapper.find('.forms-widget').exists()).toBe(false)
  })

  it('should render widget when pending forms exist', () => {
    const wrapper = createWrapper([makePendingForm()])
    expect(wrapper.find('.forms-widget').exists()).toBe(true)
  })

  it('should display pending forms header', () => {
    const wrapper = createWrapper([makePendingForm()])
    expect(wrapper.text()).toContain('Offene Formulare')
  })

  it('should display form title', () => {
    const wrapper = createWrapper([makePendingForm({ title: 'Einwilligung Fotos' })])
    expect(wrapper.text()).toContain('Einwilligung Fotos')
  })

  it('should display form type tag', () => {
    const wrapper = createWrapper([makePendingForm({ type: 'CONSENT' })])
    expect(wrapper.find('.tag-stub').text()).toBe('Einwilligung')
  })

  it('should show at most 5 pending forms', () => {
    const forms = Array.from({ length: 8 }, (_, i) =>
      makePendingForm({ id: `form-${i}`, hasUserResponded: false }),
    )
    const wrapper = createWrapper(forms)
    const entries = wrapper.findAll('.form-entry')
    expect(entries).toHaveLength(5)
  })

  it('should display scope label for school-scoped form', () => {
    const wrapper = createWrapper([makePendingForm({ scope: 'SCHOOL' })])
    expect(wrapper.text()).toContain('Schulweit')
  })

  it('should display section names when available', () => {
    const wrapper = createWrapper([
      makePendingForm({ scope: 'SECTION', sectionNames: ['Grundschule', 'Oberschule'] }),
    ])
    expect(wrapper.text()).toContain('Grundschule, Oberschule')
  })

  it('should display deadline when set', () => {
    const wrapper = createWrapper([makePendingForm({ deadline: '2026-04-01' })])
    expect(wrapper.text()).toContain('2026-04-01')
  })

  it('should have view all button', () => {
    const wrapper = createWrapper([makePendingForm()])
    expect(wrapper.text()).toContain('Alle anzeigen')
  })

  it('should navigate to forms on view all click', async () => {
    const wrapper = createWrapper([makePendingForm()])
    const viewAllBtn = wrapper.find('.btn-stub')
    await viewAllBtn.trigger('click')
    expect(mockPush).toHaveBeenCalledWith({ name: 'forms' })
  })

  it('should render form entries as router links', () => {
    const wrapper = createWrapper([makePendingForm()])
    expect(wrapper.find('.router-link-stub').exists()).toBe(true)
  })

  it('should use warn severity for CONSENT type', () => {
    const wrapper = createWrapper([makePendingForm({ type: 'CONSENT' })])
    expect(wrapper.find('.tag-stub').classes()).toContain('warn')
  })

  it('should use info severity for SURVEY type', () => {
    const wrapper = createWrapper([makePendingForm({ type: 'SURVEY' })])
    expect(wrapper.find('.tag-stub').classes()).toContain('info')
  })
})
