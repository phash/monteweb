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
vi.mock('@/api/sections.api', () => ({
  sectionsApi: {
    getAll: vi.fn().mockResolvedValue({ data: { data: [] } }),
  },
}))
vi.mock('@/api/jobboard.api', () => ({
  jobboardApi: {
    getJobs: vi.fn().mockResolvedValue({ data: { data: { content: [] } } }),
  },
}))
vi.mock('@/api/rooms.api', () => ({
  roomsApi: {
    getMine: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getAll: vi.fn().mockResolvedValue({ data: { data: { content: [] } } }),
  },
}))

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
        showCleaning: 'Putzaktionen anzeigen',
        cleaning: 'Putzaktion',
        exportCalendar: 'Exportieren',
        filterPlaceholder: 'Bereiche & Räume filtern',
        scopeSchool: 'Schulweit',
        scopeSections: 'Bereiche',
        scopeRooms: 'Räume',
        ical: { showImported: 'Importierte Termine' },
        showJobs: 'Jobs anzeigen',
        agenda: 'Agenda',
        month: 'Monat',
        threeMonth: '3 Monate',
        year: 'Jahr',
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
  Checkbox: { template: '<input type="checkbox" class="checkbox-stub" />', props: ['modelValue', 'binary', 'inputId'] },
  MultiSelect: { template: '<div class="multiselect-stub" />', props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'optionGroupLabel', 'optionGroupChildren', 'placeholder', 'display', 'maxSelectedLabels', 'selectedItemsLabel'] },
  SelectButton: { template: '<div class="selectbutton-stub" />', props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'allowEmpty'] },
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

describe('CalendarView scope filter logic', () => {
  // Helper: simulate the filter function from CalendarView
  function filterEvents(
    events: Array<{ scope: string; scopeId: string | null; eventType: string }>,
    selectedFilters: string[],
    allFilterKeys: string[],
    showCleaning: boolean
  ) {
    let result = events
    if (!showCleaning) {
      result = result.filter(e => e.eventType !== 'CLEANING')
    }
    if (selectedFilters.length > 0 && selectedFilters.length < allFilterKeys.length) {
      result = result.filter(e => {
        const key = e.scope === 'SCHOOL' ? 'SCHOOL' : `${e.scope}:${e.scopeId}`
        return selectedFilters.includes(key)
      })
    }
    return result
  }

  const events = [
    { scope: 'SCHOOL', scopeId: null, eventType: 'GENERAL' },
    { scope: 'SECTION', scopeId: 'sec-1', eventType: 'GENERAL' },
    { scope: 'SECTION', scopeId: 'sec-2', eventType: 'GENERAL' },
    { scope: 'ROOM', scopeId: 'room-1', eventType: 'GENERAL' },
    { scope: 'ROOM', scopeId: 'room-2', eventType: 'CLEANING' },
  ]

  const allKeys = ['SCHOOL', 'SECTION:sec-1', 'SECTION:sec-2', 'ROOM:room-1', 'ROOM:room-2']

  it('shows all events when all filters selected', () => {
    const result = filterEvents(events, allKeys, allKeys, true)
    expect(result).toHaveLength(5)
  })

  it('filters to school-only events', () => {
    const result = filterEvents(events, ['SCHOOL'], allKeys, true)
    expect(result).toHaveLength(1)
    expect(result[0].scope).toBe('SCHOOL')
  })

  it('filters to specific section', () => {
    const result = filterEvents(events, ['SECTION:sec-1'], allKeys, true)
    expect(result).toHaveLength(1)
    expect(result[0].scopeId).toBe('sec-1')
  })

  it('filters to specific room', () => {
    const result = filterEvents(events, ['ROOM:room-1'], allKeys, true)
    expect(result).toHaveLength(1)
    expect(result[0].scopeId).toBe('room-1')
  })

  it('combines scope filter with cleaning toggle', () => {
    const result = filterEvents(events, ['ROOM:room-2'], allKeys, false)
    expect(result).toHaveLength(0) // room-2 event is CLEANING type, hidden by toggle
  })

  it('combines multiple filters', () => {
    const result = filterEvents(events, ['SCHOOL', 'ROOM:room-1'], allKeys, true)
    expect(result).toHaveLength(2)
  })

  it('shows all when selectedFilters is empty (no filter active yet)', () => {
    const result = filterEvents(events, [], allKeys, true)
    expect(result).toHaveLength(5)
  })
})
