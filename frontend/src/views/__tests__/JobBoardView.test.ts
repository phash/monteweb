import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import JobBoardView from '@/views/JobBoardView.vue'

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn() })),
}))

vi.mock('@/api/jobboard.api', () => ({
  jobboardApi: {
    getJobs: vi.fn().mockResolvedValue({ data: { data: { content: [], last: true, totalElements: 0 } } }),
    getMyAssignments: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getCategories: vi.fn().mockResolvedValue({ data: { data: [] } }),
  },
}))

vi.mock('@/api/calendar.api', () => ({
  calendarApi: {
    getEvents: vi.fn().mockResolvedValue({ data: { data: { content: [], last: true, totalElements: 0 } } }),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))
vi.mock('@/api/admin.api', () => ({ adminApi: { getPublicConfig: vi.fn() } }))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      jobboard: {
        title: 'Jobbörse',
        create: 'Job erstellen',
        openJobs: 'Offene Jobs',
        myAssignments: 'Meine Aufgaben',
        noJobs: 'Keine offenen Jobs.',
        allCategories: 'Alle Kategorien',
        filterCategory: 'Kategorie filtern',
        noAssignments: 'Sie haben keine aktiven Aufgaben.',
        allEvents: 'Alle Events',
        jobCount: '{n} Jobs',
      },
      common: { loading: 'Laden...' },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title', 'subtitle'] },
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  EmptyState: { template: '<div class="empty-stub">{{ message }}</div>', props: ['icon', 'message'] },
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'text', 'size'],
    emits: ['click'],
  },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity', 'size'] },
  Select: { template: '<select class="select-stub" />', props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder'] },
  DatePicker: { template: '<input class="datepicker-stub" />', props: ['modelValue', 'placeholder', 'dateFormat', 'showIcon', 'showButtonBar', 'minDate'] },
  Tabs: { template: '<div class="tabs-stub"><slot /></div>', props: ['modelValue'] },
  TabList: { template: '<div class="tablist-stub"><slot /></div>' },
  Tab: { template: '<div class="tab-stub"><slot /></div>', props: ['value'] },
  TabPanels: { template: '<div class="tabpanels-stub"><slot /></div>' },
  TabPanel: { template: '<div class="tabpanel-stub"><slot /></div>', props: ['value'] },
}

function mountJobBoard() {
  const pinia = createPinia()
  return mount(JobBoardView, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('JobBoardView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render page title', () => {
    const wrapper = mountJobBoard()
    expect(wrapper.find('.page-title-stub').text()).toContain('Jobbörse')
  })

  it('should render tabs', () => {
    const wrapper = mountJobBoard()
    expect(wrapper.find('.tabs-stub').exists()).toBe(true)
  })

  it('should render create button', () => {
    const wrapper = mountJobBoard()
    expect(wrapper.text()).toContain('Job erstellen')
  })
})
