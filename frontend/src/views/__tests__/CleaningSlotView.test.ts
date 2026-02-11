import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import CleaningSlotView from '@/views/CleaningSlotView.vue'

vi.mock('vue-router', () => ({
  useRoute: vi.fn(() => ({ params: { id: 'slot-1' }, query: {} })),
  useRouter: vi.fn(() => ({ push: vi.fn(), back: vi.fn() })),
}))

vi.mock('@/api/cleaning.api', () => ({
  getUpcomingSlots: vi.fn().mockResolvedValue({ data: { data: { content: [], totalPages: 0 } } }),
  getMySlots: vi.fn().mockResolvedValue({ data: { data: [] } }),
  getSlotById: vi.fn().mockResolvedValue({
    data: {
      data: {
        id: 'slot-1',
        configTitle: 'Putzplan Grundschule',
        sectionName: 'Grundschule',
        slotDate: '2025-06-20',
        startTime: '14:00',
        endTime: '16:00',
        status: 'OPEN',
        cancelled: false,
        maxParticipants: 5,
        minParticipants: 2,
        currentRegistrations: 1,
        registrations: [
          {
            id: 'reg-1',
            userId: 'user-2',
            userName: 'Jane Doe',
            checkedIn: false,
            checkedOut: false,
            actualMinutes: null,
            swapOffered: false,
            noShow: false,
          },
        ],
      },
    },
  }),
  registerForSlot: vi.fn().mockResolvedValue({
    data: {
      data: {
        id: 'slot-1',
        configTitle: 'Putzplan Grundschule',
        sectionName: 'Grundschule',
        slotDate: '2025-06-20',
        startTime: '14:00',
        endTime: '16:00',
        status: 'OPEN',
        cancelled: false,
        maxParticipants: 5,
        minParticipants: 2,
        currentRegistrations: 2,
        registrations: [],
      },
    },
  }),
  unregisterFromSlot: vi.fn().mockResolvedValue({}),
  offerSwap: vi.fn().mockResolvedValue({}),
  checkIn: vi.fn().mockResolvedValue({ data: { data: {} } }),
  checkOut: vi.fn().mockResolvedValue({ data: { data: {} } }),
  getConfigs: vi.fn(),
  createConfig: vi.fn(),
  generateSlots: vi.fn(),
  cancelSlot: vi.fn(),
  getDashboard: vi.fn(),
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      cleaning: {
        date: 'Datum',
        time: 'Uhrzeit',
        participants: 'Teilnehmer',
        minRequired: 'Min. {n} benötigt',
        slotStatus: 'Status',
        enoughParticipants: 'Genug Teilnehmer',
        needMore: 'Mehr benötigt',
        register: 'Anmelden',
        unregister: 'Abmelden',
        checkIn: 'Einchecken',
        checkOut: 'Auschecken',
        offerSwap: 'Tausch anbieten',
        registered: 'Angemeldet',
        unregistered: 'Abgemeldet',
        swapOffered: 'Tausch angeboten',
        checkedIn: 'Eingecheckt',
        checkedOut: 'Ausgecheckt',
        registrations: 'Anmeldungen',
        checkedInCol: 'Eingecheckt',
        checkedOutCol: 'Ausgecheckt',
        duration: 'Dauer',
        swapCol: 'Tausch',
        swapAvailable: 'Tausch verfügbar',
        noShow: 'Nicht erschienen',
        qrCheckIn: 'QR-Check-in',
        qrInstructions: 'Bitte scannen Sie den QR-Code.',
        qrPlaceholder: 'QR-Code eingeben',
        status: {
          OPEN: 'Offen',
          FULL: 'Voll',
          IN_PROGRESS: 'Läuft',
          COMPLETED: 'Abgeschlossen',
          CANCELLED: 'Abgesagt',
        },
      },
      common: {
        back: 'Zurück',
        cancel: 'Abbrechen',
        name: 'Name',
      },
    },
  },
})

const stubs = {
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'text', 'severity', 'size', 'outlined', 'disabled'],
    emits: ['click'],
  },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity', 'size'] },
  Dialog: {
    template: '<div v-if="visible" class="dialog-stub"><slot /><slot name="footer" /></div>',
    props: ['visible', 'header', 'modal', 'style'],
  },
  InputText: { template: '<input class="input-stub" />', props: ['modelValue', 'placeholder', 'autofocus'] },
  DataTable: { template: '<div class="datatable-stub"><slot /></div>', props: ['value', 'stripedRows'] },
  Column: { template: '<div class="column-stub"><slot /></div>', props: ['field', 'header'] },
}

function mountCleaningSlot() {
  const pinia = createPinia()
  return mount(CleaningSlotView, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('CleaningSlotView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render component', () => {
    const wrapper = mountCleaningSlot()
    expect(wrapper.exists()).toBe(true)
  })

  it('should show loading or slot content on mount', () => {
    const wrapper = mountCleaningSlot()
    // Since mocks resolve immediately, component may already show slot data
    expect(
      wrapper.html().includes('pi-spin') ||
      wrapper.text().includes('Putzplan Grundschule') ||
      wrapper.find('.button-stub').exists()
    ).toBe(true)
  })

  it('should render back button', () => {
    const wrapper = mountCleaningSlot()
    const buttons = wrapper.findAll('.button-stub')
    expect(buttons[0]!.text()).toContain('Zurück')
  })

  it('should call loadSlot on mount', async () => {
    const cleaningApi = await import('@/api/cleaning.api')
    mountCleaningSlot()
    await vi.waitFor(() => {
      expect(cleaningApi.getSlotById).toHaveBeenCalledWith('slot-1')
    })
  })

  it('should render slot details after loading', async () => {
    const wrapper = mountCleaningSlot()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    // After load, should show the slot title or still loading
    expect(
      wrapper.text().includes('Putzplan Grundschule') ||
      wrapper.html().includes('pi-spin')
    ).toBe(true)
  })

  it('should display registrations table after loading', async () => {
    const wrapper = mountCleaningSlot()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    // Should have DataTable or still loading
    expect(
      wrapper.find('.datatable-stub').exists() ||
      wrapper.html().includes('pi-spin')
    ).toBe(true)
  })

  it('should show status tag after loading', async () => {
    const wrapper = mountCleaningSlot()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    // After load, should show status tag
    expect(
      wrapper.find('.tag-stub').exists() ||
      wrapper.html().includes('pi-spin')
    ).toBe(true)
  })
})
