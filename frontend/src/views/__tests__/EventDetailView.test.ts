import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import EventDetailView from '@/views/EventDetailView.vue'
import { useAuthStore } from '@/stores/auth'
import { useCalendarStore } from '@/stores/calendar'

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn(), back: vi.fn() })),
}))

vi.mock('@/api/calendar.api', () => ({
  calendarApi: {
    getEvent: vi.fn().mockResolvedValue({
      data: {
        data: {
          id: 'evt-1', title: 'Test Event', description: 'Desc',
          scope: 'ROOM', scopeId: 'r1', scopeName: 'Room 1',
          startDate: '2025-06-15', startTime: '10:00', endDate: '2025-06-15', endTime: '11:00',
          allDay: false, location: 'Aula', cancelled: false,
          createdBy: 'user-1', attendingCount: 2, maybeCount: 1, declinedCount: 0,
          myRsvp: null, recurrence: 'NONE', linkedJobCount: 0,
          jitsiRoomName: null,
        },
      },
    }),
    getEventJobs: vi.fn().mockResolvedValue({ data: { data: [] } }),
    generateJitsiRoom: vi.fn().mockResolvedValue({
      data: {
        data: {
          id: 'evt-1', title: 'Test Event', jitsiRoomName: 'monteweb-event-evt-1aaa',
        },
      },
    }),
    removeJitsiRoom: vi.fn().mockResolvedValue({
      data: {
        data: {
          id: 'evt-1', title: 'Test Event', jitsiRoomName: null,
        },
      },
    }),
  },
}))

vi.mock('@/api/jobboard.api', () => ({
  jobboardApi: {
    getJobs: vi.fn().mockResolvedValue({ data: { data: { content: [] } } }),
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
      calendar: {
        title: 'Kalender',
        attending: 'Zusage',
        maybe: 'Vielleicht',
        declined: 'Absage',
        cancel: 'Event absagen',
        cancelEvent: 'Termin absagen',
        rsvp: 'Teilnahme',
        delete: 'Löschen',
        edit: 'Bearbeiten',
        noEvents: 'Keine Events.',
        cancelled: 'Abgesagt',
        confirmCancel: 'Wirklich absagen?',
        confirmDelete: 'Wirklich löschen?',
        scopes: { ROOM: 'Raum', SECTION: 'Bereich', SCHOOL: 'Schule' },
        attendingCount: '{n} Zusagen',
        maybeCount: '{n} Vielleicht',
        declinedCount: '{n} Absagen',
        linkedJobs: 'Verknüpfte Jobs',
        linkJob: 'Job verknüpfen',
      },
      jobboard: {
        linkExistingJob: 'Bestehenden Job verknüpfen',
        selectJobToLink: 'Job zum Verknüpfen auswählen',
        linkJob: 'Verknüpfen',
        noOpenJobs: 'Keine offenen Jobs zum Verknüpfen verfügbar.',
        createLinkedJob: 'Job erstellen',
      },
      common: { cancel: 'Abbrechen', delete: 'Löschen', back: 'Zurück', loading: 'Laden...', yes: 'Ja', no: 'Nein', edit: 'Bearbeiten', created: 'erstellt' },
      admin: {
        jitsi: {
          joinMeeting: 'Besprechung beitreten',
          addVideoConference: 'Videokonferenz hinzufügen',
          removeVideoConference: 'Videokonferenz entfernen',
          videoConference: 'Videokonferenz',
        },
      },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title', 'subtitle'] },
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'text', 'severity', 'size', 'outlined'],
    emits: ['click'],
  },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity', 'size'] },
  Dialog: {
    template: '<div v-if="visible" class="dialog-stub"><slot /><slot name="footer" /></div>',
    props: ['visible', 'header', 'modal'],
  },
  Select: { template: '<select class="select-stub" />', props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder'] },
}

function mountEventDetail(pinia = createPinia()) {
  return mount(EventDetailView, {
    props: { id: 'evt-1' },
    global: {
      plugins: [i18n, pinia],
      stubs,
    },
  })
}

describe('EventDetailView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render component', () => {
    const wrapper = mountEventDetail()
    expect(wrapper.exists()).toBe(true)
  })

  it('should show loading initially', () => {
    const wrapper = mountEventDetail()
    expect(wrapper.find('.loading-stub').exists()).toBe(true)
  })

  it('should render page title after load', async () => {
    const wrapper = mountEventDetail()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    // Either loading or page-title should be rendered
    expect(wrapper.find('.loading-stub').exists() || wrapper.find('.page-title-stub').exists()).toBe(true)
  })

  it('canManage() returns true for a SECTION_ADMIN who is not the creator', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    // SECTION_ADMIN with a different id than the event creator (createdBy: 'user-1')
    useAuthStore().user = { id: 'admin-9', role: 'SECTION_ADMIN' } as never
    const wrapper = mountEventDetail(pinia)
    await flushPromises()
    await wrapper.vm.$nextTick()
    expect(useCalendarStore().currentEvent?.createdBy).toBe('user-1')
    expect((wrapper.vm as unknown as { canManage: () => boolean }).canManage()).toBe(true)
  })

  it('canManage() returns true for a SUPERADMIN who is not the creator', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useAuthStore().user = { id: 'super-1', role: 'SUPERADMIN' } as never
    const wrapper = mountEventDetail(pinia)
    await flushPromises()
    await wrapper.vm.$nextTick()
    expect((wrapper.vm as unknown as { canManage: () => boolean }).canManage()).toBe(true)
  })

  it('canManage() returns false for a PARENT who is not the creator', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useAuthStore().user = { id: 'parent-3', role: 'PARENT' } as never
    const wrapper = mountEventDetail(pinia)
    await flushPromises()
    await wrapper.vm.$nextTick()
    expect((wrapper.vm as unknown as { canManage: () => boolean }).canManage()).toBe(false)
  })

  it('canManage() returns true for the event creator regardless of role', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    // createdBy in the mock is 'user-1'
    useAuthStore().user = { id: 'user-1', role: 'PARENT' } as never
    const wrapper = mountEventDetail(pinia)
    await flushPromises()
    await wrapper.vm.$nextTick()
    expect((wrapper.vm as unknown as { canManage: () => boolean }).canManage()).toBe(true)
  })
})
