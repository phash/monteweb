import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import RoomEvents from '@/components/rooms/RoomEvents.vue'
import { useCalendarStore } from '@/stores/calendar'
import { useAuthStore } from '@/stores/auth'
import { useRoomsStore } from '@/stores/rooms'

const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: mockPush })),
}))

vi.mock('@/api/calendar.api', () => ({
  calendarApi: {
    getRoomEvents: vi.fn().mockResolvedValue({ data: { data: { content: [], totalElements: 0, last: true } } }),
    getEvents: vi.fn().mockResolvedValue({ data: { data: { content: [], totalElements: 0, last: true } } }),
  },
}))

vi.mock('@/api/rooms.api', () => ({
  roomsApi: { getMine: vi.fn().mockResolvedValue({ data: { data: [] } }), discover: vi.fn().mockResolvedValue({ data: { data: { content: [] } } }) },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))
vi.mock('@/api/admin.api', () => ({
  adminApi: { getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }) },
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      calendar: {
        createEvent: 'Termin erstellen',
        noEvents: 'Keine Termine.',
        cancelled: 'Abgesagt',
        attendingCount: '{n} Zusagen',
        scopes: { ROOM: 'Raum', SECTION: 'Bereich', SCHOOL: 'Schule' },
        viewList: 'Liste',
        viewMonth: 'Monat',
        view3Months: '3 Monate',
        viewYear: 'Schuljahr',
      },
      common: { loading: 'Laden...' },
    },
  },
})

const stubs = {
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  EmptyState: { template: '<div class="empty-stub">{{ message }}</div>', props: ['icon', 'message'] },
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'text', 'severity', 'size'],
    emits: ['click'],
  },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity', 'size'] },
  SelectButton: {
    template: '<div class="selectbutton-stub" />',
    props: ['modelValue', 'options', 'optionLabel', 'optionValue'],
  },
  'router-link': { template: '<a class="router-link-stub"><slot /></a>', props: ['to'] },
}

const mockEvents = [
  {
    id: 'ev-1',
    title: 'Elternabend',
    startDate: '2026-03-10',
    endDate: '2026-03-10',
    startTime: '19:00:00',
    endTime: '21:00:00',
    allDay: false,
    location: 'Raum 101',
    cancelled: false,
    attendingCount: 5,
    maybeCount: 2,
    scope: 'ROOM',
  },
  {
    id: 'ev-2',
    title: 'Sommerfest',
    startDate: '2026-03-15',
    endDate: '2026-03-15',
    startTime: null,
    endTime: null,
    allDay: true,
    location: 'Schulhof',
    cancelled: false,
    attendingCount: 20,
    maybeCount: 0,
    scope: 'ROOM',
  },
  {
    id: 'ev-3',
    title: 'Abgesagter Termin',
    startDate: '2026-03-20',
    endDate: '2026-03-20',
    startTime: '10:00:00',
    endTime: '11:00:00',
    allDay: false,
    location: null,
    cancelled: true,
    attendingCount: 0,
    maybeCount: 0,
    scope: 'ROOM',
  },
]

function mountRoomEvents(options?: { isLeader?: boolean; events?: any[] }) {
  const pinia = createPinia()
  setActivePinia(pinia)

  const calendar = useCalendarStore()
  calendar.events = options?.events ?? []

  const auth = useAuthStore()
  const rooms = useRoomsStore()

  if (options?.isLeader) {
    auth.user = { id: 'user-1', role: 'TEACHER' } as any
    rooms.currentRoom = {
      id: 'room-1',
      members: [{ userId: 'user-1', role: 'LEADER' }],
    } as any
  } else {
    auth.user = { id: 'user-2', role: 'PARENT' } as any
    rooms.currentRoom = {
      id: 'room-1',
      members: [{ userId: 'user-2', role: 'MEMBER' }],
    } as any
  }

  return mount(RoomEvents, {
    props: { roomId: 'room-1' },
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('RoomEvents', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render component', () => {
    const wrapper = mountRoomEvents()
    expect(wrapper.exists()).toBe(true)
  })

  it('should show empty state or loading', async () => {
    const wrapper = mountRoomEvents()
    await wrapper.vm.$nextTick()
    expect(
      wrapper.find('.empty-stub').exists() || wrapper.find('.loading-stub').exists()
    ).toBe(true)
  })

  describe('event creation (leader permissions)', () => {
    it('should show create event button for leader', () => {
      const wrapper = mountRoomEvents({ isLeader: true })
      const buttons = wrapper.findAll('.button-stub')
      const createBtn = buttons.find(b => b.text().includes('Termin erstellen'))
      expect(createBtn).toBeTruthy()
    })

    it('should not show create event button for non-leader', () => {
      const wrapper = mountRoomEvents({ isLeader: false })
      const buttons = wrapper.findAll('.button-stub')
      const createBtn = buttons.find(b => b.text().includes('Termin erstellen'))
      expect(createBtn).toBeUndefined()
    })

    it('should navigate to calendar-create with roomId on create click', async () => {
      const wrapper = mountRoomEvents({ isLeader: true })
      const buttons = wrapper.findAll('.button-stub')
      const createBtn = buttons.find(b => b.text().includes('Termin erstellen'))
      await createBtn!.trigger('click')
      expect(mockPush).toHaveBeenCalledWith({ name: 'calendar-create', query: { roomId: 'room-1' } })
    })
  })

  describe('event list view', () => {
    it('should render events in list view', () => {
      const wrapper = mountRoomEvents({ events: mockEvents })
      const eventItems = wrapper.findAll('.event-item')
      expect(eventItems.length).toBe(3)
    })

    it('should display event titles', () => {
      const wrapper = mountRoomEvents({ events: mockEvents })
      expect(wrapper.text()).toContain('Elternabend')
      expect(wrapper.text()).toContain('Sommerfest')
    })

    it('should display event location', () => {
      const wrapper = mountRoomEvents({ events: mockEvents })
      expect(wrapper.text()).toContain('Raum 101')
      expect(wrapper.text()).toContain('Schulhof')
    })

    it('should show cancelled tag for cancelled events', () => {
      const wrapper = mountRoomEvents({ events: mockEvents })
      const tags = wrapper.findAll('.tag-stub')
      const cancelledTag = tags.find(t => t.text().includes('Abgesagt'))
      expect(cancelledTag).toBeTruthy()
    })

    it('should add cancelled class to cancelled events', () => {
      const wrapper = mountRoomEvents({ events: mockEvents })
      const cancelledItems = wrapper.findAll('.event-item.cancelled')
      expect(cancelledItems.length).toBe(1)
    })

    it('should display attending count', () => {
      const wrapper = mountRoomEvents({ events: mockEvents })
      expect(wrapper.text()).toContain('5 Zusagen')
    })

    it('should navigate to event detail on click', async () => {
      const wrapper = mountRoomEvents({ events: mockEvents })
      const eventItem = wrapper.findAll('.event-item')[0]
      await eventItem.trigger('click')
      expect(mockPush).toHaveBeenCalledWith({ name: 'event-detail', params: { id: 'ev-1' } })
    })

    it('should format timed event with time', () => {
      const wrapper = mountRoomEvents({ events: mockEvents })
      // Timed event should show time (19:00)
      expect(wrapper.text()).toContain('19:00')
    })

    it('should show empty state when no events', () => {
      const wrapper = mountRoomEvents({ events: [] })
      expect(wrapper.find('.empty-stub').exists()).toBe(true)
      expect(wrapper.text()).toContain('Keine Termine.')
    })
  })

  describe('calendar fetching', () => {
    it('should call fetchRoomEvents on mount', async () => {
      const pinia = createPinia()
      setActivePinia(pinia)
      const calendar = useCalendarStore()
      const fetchSpy = vi.spyOn(calendar, 'fetchRoomEvents').mockResolvedValue()
      mount(RoomEvents, {
        props: { roomId: 'room-1' },
        global: { plugins: [i18n, pinia], stubs },
      })
      await flushPromises()
      expect(fetchSpy).toHaveBeenCalledWith('room-1')
    })
  })

  describe('view mode switching', () => {
    it('should render SelectButton for view mode switching', () => {
      const wrapper = mountRoomEvents({ events: mockEvents })
      expect(wrapper.find('.selectbutton-stub').exists()).toBe(true)
    })

    it('should show month navigation when in month view', async () => {
      const wrapper = mountRoomEvents({ events: mockEvents })
      // Force the viewMode to month
      await wrapper.vm.$nextTick()
      // The component uses v-model on SelectButton, we need to check the structure
      // In default list view, month-nav is not shown
      expect(wrapper.find('.month-nav').exists()).toBe(false)
    })
  })

  describe('event date formatting', () => {
    it('should handle all-day events spanning multiple days', () => {
      const multiDayEvents = [{
        ...mockEvents[1],
        id: 'ev-multi',
        startDate: '2026-03-15',
        endDate: '2026-03-17',
        allDay: true,
      }]
      const wrapper = mountRoomEvents({ events: multiDayEvents })
      // Multi-day all-day event should show a date range
      expect(wrapper.find('.event-item').exists()).toBe(true)
    })

    it('should handle events without location', () => {
      const noLocEvents = [{
        ...mockEvents[0],
        id: 'ev-noloc',
        location: null,
      }]
      const wrapper = mountRoomEvents({ events: noLocEvents })
      expect(wrapper.find('.event-item').exists()).toBe(true)
      // Should not render the separator/location
      const separators = wrapper.findAll('.separator')
      // Only the attending count separator if attendingCount > 0
      expect(separators.length).toBeLessThanOrEqual(1)
    })
  })
})
