import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import CalendarView from '@/views/CalendarView.vue'

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn() })),
}))

vi.mock('@/api/calendar.api', () => ({
  calendarApi: {
    getEvents: vi.fn().mockResolvedValue({
      data: { data: { content: [], last: true, totalElements: 0 } },
    }),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      calendar: {
        title: 'Kalender',
        createEvent: 'Termin erstellen',
        noEvents: 'Keine Termine.',
        today: 'Heute',
        cancelled: 'Abgesagt',
        scopes: { ROOM: 'Raum', SECTION: 'Bereich', SCHOOL: 'Schule' },
        attendingCount: '{n} Zusagen',
      },
      common: { loading: 'Laden...' },
      jobboard: { jobCount: '{n} Jobs' },
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
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity', 'size', 'icon'] },
  'router-link': {
    template: '<a class="router-link-stub"><slot /></a>',
    props: ['to'],
  },
}

function mountCalendar() {
  const pinia = createPinia()
  return mount(CalendarView, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('CalendarView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render page title', () => {
    const wrapper = mountCalendar()
    expect(wrapper.find('.page-title-stub').text()).toContain('Kalender')
  })

  it('should render month navigation', () => {
    const wrapper = mountCalendar()
    expect(wrapper.find('.month-nav').exists()).toBe(true)
    expect(wrapper.find('.month-label').exists()).toBe(true)
  })

  it('should show empty state or loading when no events', () => {
    const wrapper = mountCalendar()
    // Component shows either loading or empty state depending on timing
    expect(wrapper.find('.empty-stub').exists() || wrapper.find('.loading-stub').exists()).toBe(true)
  })

  it('should render today button', () => {
    const wrapper = mountCalendar()
    expect(wrapper.text()).toContain('Heute')
  })

  it('should render navigation buttons', () => {
    const wrapper = mountCalendar()
    const buttons = wrapper.findAll('.button-stub')
    expect(buttons.length).toBeGreaterThanOrEqual(3)
  })
})
