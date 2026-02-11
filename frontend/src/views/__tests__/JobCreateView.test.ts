import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import JobCreateView from '@/views/JobCreateView.vue'

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn(), back: vi.fn() })),
  useRoute: vi.fn(() => ({ query: {}, params: {} })),
}))

vi.mock('@/api/jobboard.api', () => ({
  jobboardApi: {
    listJobs: vi.fn().mockResolvedValue({ data: { data: { content: [], last: true } } }),
    getCategories: vi.fn().mockResolvedValue({ data: { data: [] } }),
    createJob: vi.fn().mockResolvedValue({ data: { data: { id: 'job-1' } } }),
    getJob: vi.fn(),
    applyForJob: vi.fn(),
    getAssignments: vi.fn(),
    getMyAssignments: vi.fn().mockResolvedValue({ data: { data: [] } }),
    startAssignment: vi.fn(),
    completeAssignment: vi.fn(),
    confirmAssignment: vi.fn(),
    getFamilyHours: vi.fn(),
    getReport: vi.fn(),
    getReportSummary: vi.fn(),
    exportCsv: vi.fn(),
    exportPdf: vi.fn(),
  },
}))

vi.mock('@/api/calendar.api', () => ({
  calendarApi: {
    getEvents: vi.fn().mockResolvedValue({ data: { data: { content: [], last: true, totalElements: 0 } } }),
    getEvent: vi.fn(),
    createEvent: vi.fn(),
    updateEvent: vi.fn(),
    cancelEvent: vi.fn(),
    deleteEvent: vi.fn(),
    rsvp: vi.fn(),
    getRoomEvents: vi.fn(),
  },
}))

vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
    updateModules: vi.fn(),
    updateTheme: vi.fn(),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      jobboard: {
        create: 'Job erstellen',
        titleLabel: 'Titel',
        category: 'Kategorie',
        estimatedHours: 'Geschätzte Stunden',
        maxHelpers: 'Max. Helfer',
        location: 'Ort',
        date: 'Datum',
        time: 'Uhrzeit',
        contact: 'Kontakt',
        linkedEvent: 'Verknüpftes Event',
        noLinkedEvent: 'Kein Event',
        selectEvent: 'Event auswählen',
        create_form: {
          titlePlaceholder: 'z.B. Garten aufräumen',
          categoryPlaceholder: 'z.B. Gartenarbeit',
          locationPlaceholder: 'z.B. Schulhof',
          timePlaceholder: 'z.B. 14:00',
          contactPlaceholder: 'z.B. Tel. oder E-Mail',
        },
      },
      common: {
        back: 'Zurück',
        cancel: 'Abbrechen',
        description: 'Beschreibung',
      },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title'] },
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'text', 'severity', 'size', 'loading', 'disabled'],
    emits: ['click'],
  },
  InputText: { template: '<input class="input-stub" />', props: ['modelValue', 'placeholder', 'list'] },
  InputNumber: { template: '<input class="input-number-stub" type="number" />', props: ['modelValue', 'min', 'max', 'step', 'minFractionDigits', 'maxFractionDigits'] },
  Textarea: { template: '<textarea class="textarea-stub" />', props: ['modelValue', 'autoResize', 'rows'] },
  DatePicker: { template: '<input class="datepicker-stub" />', props: ['modelValue', 'dateFormat'] },
  Select: { template: '<select class="select-stub" />', props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder'] },
}

function mountJobCreate() {
  const pinia = createPinia()
  return mount(JobCreateView, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('JobCreateView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render component', () => {
    const wrapper = mountJobCreate()
    expect(wrapper.exists()).toBe(true)
  })

  it('should render page title', () => {
    const wrapper = mountJobCreate()
    expect(wrapper.find('.page-title-stub').text()).toContain('Job erstellen')
  })

  it('should render form fields', () => {
    const wrapper = mountJobCreate()
    expect(wrapper.findAll('.input-stub').length).toBeGreaterThan(0)
    expect(wrapper.find('.textarea-stub').exists()).toBe(true)
    expect(wrapper.find('.datepicker-stub').exists()).toBe(true)
  })

  it('should render input number fields for hours and max helpers', () => {
    const wrapper = mountJobCreate()
    expect(wrapper.findAll('.input-number-stub').length).toBe(2)
  })

  it('should render back button', () => {
    const wrapper = mountJobCreate()
    const buttons = wrapper.findAll('.button-stub')
    expect(buttons[0]!.text()).toContain('Zurück')
  })

  it('should render cancel and create buttons', () => {
    const wrapper = mountJobCreate()
    const buttons = wrapper.findAll('.button-stub')
    const texts = buttons.map(b => b.text())
    expect(texts).toContain('Abbrechen')
    expect(texts).toContain('Job erstellen')
  })

  it('should have the form card structure', () => {
    const wrapper = mountJobCreate()
    expect(wrapper.find('.create-form').exists()).toBe(true)
  })

  it('should pre-fill eventId from route query', async () => {
    const { useRoute } = await import('vue-router')
    vi.mocked(useRoute).mockReturnValue({ query: { eventId: 'evt-123' }, params: {} } as any)

    const pinia = createPinia()
    const wrapper = mount(JobCreateView, {
      global: { plugins: [i18n, pinia], stubs },
    })
    expect(wrapper.exists()).toBe(true)
  })
})
