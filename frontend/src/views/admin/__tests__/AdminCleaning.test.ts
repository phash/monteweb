import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import AdminCleaning from '@/views/admin/AdminCleaning.vue'

vi.mock('@/api/cleaning.api', () => ({
  getConfigs: vi.fn().mockResolvedValue({
    data: {
      data: [
        {
          id: 'cfg-1', sectionId: 'sec-1', sectionName: 'Grundschule', title: 'Montag Putzen',
          description: null, dayOfWeek: 1, startTime: '14:00', endTime: '16:00',
          minParticipants: 3, maxParticipants: 6, hoursCredit: 2.0, active: true,
        },
        {
          id: 'cfg-2', sectionId: 'sec-2', sectionName: 'Mittelschule', title: 'Mittwoch Putzen',
          description: 'Midweek', dayOfWeek: 3, startTime: '15:00', endTime: '17:00',
          minParticipants: 2, maxParticipants: 4, hoursCredit: 2.0, active: false,
        },
      ],
    },
  }),
  createConfig: vi.fn().mockResolvedValue({
    data: { data: { id: 'cfg-3', title: 'New Config', active: true } },
  }),
  updateConfig: vi.fn().mockResolvedValue({
    data: { data: { id: 'cfg-1', active: false } },
  }),
  generateSlots: vi.fn().mockResolvedValue({ data: { data: [] } }),
  exportQrCodesPdf: vi.fn().mockResolvedValue({ data: new Blob() }),
  getUpcomingSlots: vi.fn().mockResolvedValue({ data: { data: { content: [], totalPages: 0 } } }),
  getMySlots: vi.fn().mockResolvedValue({ data: { data: [] } }),
  getDashboard: vi.fn().mockResolvedValue({ data: { data: {} } }),
}))

vi.mock('@/api/sections.api', () => ({
  sectionsApi: {
    getAll: vi.fn().mockResolvedValue({
      data: {
        data: [
          { id: 'sec-1', name: 'Grundschule', slug: 'gs', description: null, sortOrder: 1, active: true },
          { id: 'sec-2', name: 'Mittelschule', slug: 'ms', description: null, sortOrder: 2, active: true },
        ],
      },
    }),
  },
}))

vi.mock('@/api/users.api', () => ({
  usersApi: {
    search: vi.fn().mockResolvedValue({
      data: { data: { content: [{ id: 'u-2', displayName: 'Search Result', email: 'sr@test.de', role: 'PARENT', active: true }] } },
    }),
    findBySpecialRole: vi.fn().mockResolvedValue({
      data: { data: [{ id: 'u-1', displayName: 'Putz Parent', email: 'putz@test.de', role: 'PARENT', active: true }] },
    }),
    addSpecialRole: vi.fn().mockResolvedValue({ data: { data: { id: 'u-1' } } }),
    removeSpecialRole: vi.fn().mockResolvedValue({ data: { data: null } }),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
  },
}))

import * as cleaningApi from '@/api/cleaning.api'
import { sectionsApi } from '@/api/sections.api'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      cleaning: {
        admin: {
          title: 'Putzverwaltung',
          newConfig: 'Neue Konfiguration',
          configTitle: 'Titel',
          section: 'Bereich',
          sectionId: 'Bereichs-ID',
          sectionIdPlaceholder: 'ID eingeben',
          day: 'Tag',
          timeRange: 'Zeitraum',
          startTime: 'Beginn',
          endTime: 'Ende',
          participants: 'Teilnehmer',
          minParticipants: 'Min. Teilnehmer',
          maxParticipants: 'Max. Teilnehmer',
          hoursCredit: 'Stundengutschrift',
          status: 'Status',
          generate: 'Termine generieren',
          generateTitle: 'Termine generieren',
          generateHint: 'Termine für {title} generieren',
          fromDate: 'Von',
          toDate: 'Bis',
          slotsGenerated: '{n} Termine generiert',
          exportQrCodes: 'QR-Codes',
          exportQrCodesTitle: 'QR-Codes exportieren',
          exportQrCodesHint: 'QR-Codes für {title} exportieren',
          configCreated: 'Konfiguration erstellt',
          putzOrgaManagement: 'PutzOrga-Verwaltung',
          putzOrgaHint: 'Verwalten Sie die PutzOrga-Verantwortlichen.',
          selectSection: 'Bereich wählen',
          assignPutzOrga: 'PutzOrga zuweisen',
          searchParent: 'Elternteil suchen',
          assign: 'Zuweisen',
          putzOrgaAssigned: 'PutzOrga zugewiesen',
          putzOrgaRemoved: 'PutzOrga entfernt',
          noPutzOrga: 'Keine PutzOrga-Verantwortlichen',
        },
        days: {
          monday: 'Montag', tuesday: 'Dienstag', wednesday: 'Mittwoch',
          thursday: 'Donnerstag', friday: 'Freitag',
        },
      },
      admin: { columnName: 'Name', columnEmail: 'E-Mail' },
      common: {
        active: 'Aktiv', inactive: 'Inaktiv', actions: 'Aktionen',
        cancel: 'Abbrechen', create: 'Erstellen',
      },
    },
  },
})

const stubs = {
  DataTable: {
    template: '<div class="datatable-stub"><slot /><slot name="empty" /></div>',
    props: ['value', 'loading', 'stripedRows'],
  },
  Column: { template: '<div class="column-stub"><slot /></div>', props: ['field', 'header'] },
  Button: {
    template: '<button class="button-stub" :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'severity', 'text', 'rounded', 'size', 'disabled'],
    emits: ['click'],
  },
  Dialog: {
    template: '<div v-if="visible" class="dialog-stub"><slot /><slot name="footer" /></div>',
    props: ['visible', 'header', 'modal'],
  },
  InputText: { template: '<input class="input-stub" />', props: ['modelValue', 'placeholder'] },
  InputNumber: { template: '<input class="input-number-stub" />', props: ['modelValue', 'min', 'max', 'step', 'minFractionDigits', 'maxFractionDigits'] },
  Select: { template: '<select class="select-stub" />', props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder'] },
  DatePicker: { template: '<input class="datepicker-stub" />', props: ['modelValue', 'dateFormat'] },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity'] },
  AutoComplete: {
    template: '<input class="autocomplete-stub" />',
    props: ['modelValue', 'suggestions', 'optionLabel', 'placeholder', 'minLength'],
    emits: ['update:modelValue', 'complete'],
  },
}

function mountComponent() {
  const pinia = createPinia()
  return mount(AdminCleaning, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('AdminCleaning', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render component', () => {
    const wrapper = mountComponent()
    expect(wrapper.exists()).toBe(true)
  })

  it('should render title', () => {
    const wrapper = mountComponent()
    expect(wrapper.text()).toContain('Putzverwaltung')
  })

  it('should render new config button', () => {
    const wrapper = mountComponent()
    const buttons = wrapper.findAll('.button-stub')
    const newBtn = buttons.find(b => b.text().includes('Neue Konfiguration'))
    expect(newBtn).toBeTruthy()
  })

  it('should render DataTable for configs', () => {
    const wrapper = mountComponent()
    expect(wrapper.find('.datatable-stub').exists()).toBe(true)
  })

  it('should load configs and sections on mount', async () => {
    mountComponent()
    await flushPromises()
    expect(cleaningApi.getConfigs).toHaveBeenCalled()
    expect(sectionsApi.getAll).toHaveBeenCalled()
  })

  it('should render PutzOrga management section', () => {
    const wrapper = mountComponent()
    expect(wrapper.text()).toContain('PutzOrga-Verwaltung')
  })

  it('should render section select for PutzOrga', () => {
    const wrapper = mountComponent()
    const selects = wrapper.findAll('.select-stub')
    expect(selects.length).toBeGreaterThanOrEqual(1)
  })

  it('should show create dialog on button click', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    const buttons = wrapper.findAll('.button-stub')
    const newBtn = buttons.find(b => b.text().includes('Neue Konfiguration'))
    await newBtn!.trigger('click')
    await flushPromises()

    expect(wrapper.find('.dialog-stub').exists()).toBe(true)
  })

  it('should render config columns', () => {
    const wrapper = mountComponent()
    const columns = wrapper.findAll('.column-stub')
    // title, section, day, time, participants, hoursCredit, status, actions
    expect(columns.length).toBe(8)
  })

  it('should render PutzOrga hint text', () => {
    const wrapper = mountComponent()
    expect(wrapper.text()).toContain('Verwalten Sie die PutzOrga-Verantwortlichen.')
  })
})
