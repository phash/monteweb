import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import RoomEvents from '@/components/rooms/RoomEvents.vue'

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn() })),
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
        scopes: { ROOM: 'Raum', SECTION: 'Bereich', SCHOOL: 'Schule' },
      },
      common: { loading: 'Laden...' },
    },
  },
})

const stubs = {
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  EmptyState: { template: '<div class="empty-stub">{{ message }}</div>', props: ['icon', 'message'] },
  Button: { template: '<button class="button-stub">{{ label }}</button>', props: ['label', 'icon', 'text', 'severity', 'size'] },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity', 'size'] },
  'router-link': { template: '<a class="router-link-stub"><slot /></a>', props: ['to'] },
}

function mountRoomEvents() {
  const pinia = createPinia()
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
})
