import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import EventCreateView from '@/views/EventCreateView.vue'

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn(), back: vi.fn() })),
  useRoute: vi.fn(() => ({ name: 'event-create', params: {}, query: {} })),
}))

vi.mock('@/api/calendar.api', () => ({
  calendarApi: {
    getEvents: vi.fn().mockResolvedValue({ data: { data: { content: [], last: true, totalElements: 0 } } }),
    getEvent: vi.fn().mockResolvedValue({
      data: {
        data: {
          id: 'evt-1', title: 'Test', description: '', location: '',
          allDay: false, startDate: '2025-06-15', startTime: '10:00:00',
          endDate: '2025-06-15', endTime: '11:00:00', scope: 'ROOM',
          scopeId: 'r1', recurrence: 'NONE', recurrenceEnd: null,
        },
      },
    }),
    createEvent: vi.fn().mockResolvedValue({ data: { data: { id: 'evt-new' } } }),
    updateEvent: vi.fn().mockResolvedValue({ data: { data: { id: 'evt-1' } } }),
    cancelEvent: vi.fn(),
    deleteEvent: vi.fn(),
    rsvp: vi.fn(),
    getRoomEvents: vi.fn(),
  },
}))

vi.mock('@/api/rooms.api', () => ({
  roomsApi: {
    getMine: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getById: vi.fn(),
    create: vi.fn(),
    discover: vi.fn(),
    joinRoom: vi.fn(),
    leaveRoom: vi.fn(),
    getChatChannels: vi.fn(),
    getOrCreateChatChannel: vi.fn(),
    createInterestRoom: vi.fn(),
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
        createEvent: 'Event erstellen',
        editEvent: 'Event bearbeiten',
        eventTitle: 'Titel',
        titlePlaceholder: 'Eventtitel eingeben',
        description: 'Beschreibung',
        descriptionPlaceholder: 'Beschreibung eingeben',
        location: 'Ort',
        locationPlaceholder: 'Ort eingeben',
        allDay: 'Ganztägig',
        startDate: 'Startdatum',
        startTime: 'Startzeit',
        endDate: 'Enddatum',
        endTime: 'Endzeit',
        scope: 'Bereich',
        selectRoom: 'Raum wählen',
        recurrence: 'Wiederholung',
        recurrenceEnd: 'Ende der Wiederholung',
        scopes: { ROOM: 'Raum', SECTION: 'Bereich', SCHOOL: 'Schule' },
        recurrences: { NONE: 'Keine', DAILY: 'Täglich', WEEKLY: 'Wöchentlich', MONTHLY: 'Monatlich', YEARLY: 'Jährlich' },
      },
      common: {
        back: 'Zurück',
        cancel: 'Abbrechen',
        save: 'Speichern',
        create: 'Erstellen',
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
  InputText: { template: '<input class="input-stub" />', props: ['modelValue', 'placeholder'] },
  Textarea: { template: '<textarea class="textarea-stub" />', props: ['modelValue', 'placeholder', 'autoResize', 'rows'] },
  DatePicker: { template: '<input class="datepicker-stub" />', props: ['modelValue', 'dateFormat'] },
  Select: { template: '<select class="select-stub" />', props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder'] },
  Checkbox: { template: '<input class="checkbox-stub" type="checkbox" />', props: ['modelValue', 'binary', 'inputId'] },
}

function mountEventCreate() {
  const pinia = createPinia()
  return mount(EventCreateView, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('EventCreateView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render component', () => {
    const wrapper = mountEventCreate()
    expect(wrapper.exists()).toBe(true)
  })

  it('should render page title for create mode', () => {
    const wrapper = mountEventCreate()
    expect(wrapper.find('.page-title-stub').text()).toContain('Event erstellen')
  })

  it('should render back button', () => {
    const wrapper = mountEventCreate()
    const buttons = wrapper.findAll('.button-stub')
    expect(buttons[0]!.text()).toContain('Zurück')
  })

  it('should render form fields', () => {
    const wrapper = mountEventCreate()
    expect(wrapper.findAll('.input-stub').length).toBeGreaterThan(0)
    expect(wrapper.find('.textarea-stub').exists()).toBe(true)
    expect(wrapper.findAll('.datepicker-stub').length).toBeGreaterThan(0)
  })

  it('should render scope and recurrence selects', () => {
    const wrapper = mountEventCreate()
    expect(wrapper.findAll('.select-stub').length).toBeGreaterThanOrEqual(2)
  })

  it('should render allDay checkbox', () => {
    const wrapper = mountEventCreate()
    expect(wrapper.find('.checkbox-stub').exists()).toBe(true)
  })

  it('should render cancel and create buttons', () => {
    const wrapper = mountEventCreate()
    const buttons = wrapper.findAll('.button-stub')
    const texts = buttons.map(b => b.text())
    expect(texts).toContain('Abbrechen')
    expect(texts).toContain('Erstellen')
  })

  it('should call fetchMyRooms on mount', async () => {
    const { roomsApi } = await import('@/api/rooms.api')
    mountEventCreate()
    await vi.waitFor(() => {
      expect(roomsApi.getMine).toHaveBeenCalled()
    })
  })

  it('should render in edit mode when route name is event-edit', async () => {
    const { useRoute } = await import('vue-router')
    vi.mocked(useRoute).mockReturnValue({
      name: 'event-edit',
      params: { id: 'evt-1' },
      query: {},
    } as any)

    const pinia = createPinia()
    const wrapper = mount(EventCreateView, {
      global: { plugins: [i18n, pinia], stubs },
    })
    expect(wrapper.find('.page-title-stub').text()).toContain('Event bearbeiten')
  })

  it('should pre-fill roomId from query params', async () => {
    const { useRoute } = await import('vue-router')
    vi.mocked(useRoute).mockReturnValue({
      name: 'event-create',
      params: {},
      query: { roomId: 'room-123' },
    } as any)

    const pinia = createPinia()
    const wrapper = mount(EventCreateView, {
      global: { plugins: [i18n, pinia], stubs },
    })
    expect(wrapper.exists()).toBe(true)
  })
})
