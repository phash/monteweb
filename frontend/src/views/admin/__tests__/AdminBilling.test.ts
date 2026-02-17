import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'

const mockBillingStore = {
  periods: [] as any[],
  activePeriod: null as any,
  report: null as any,
  loading: false,
  fetchPeriods: vi.fn().mockResolvedValue(undefined),
  fetchReport: vi.fn().mockResolvedValue(undefined),
  createPeriod: vi.fn().mockResolvedValue({}),
  closePeriod: vi.fn().mockResolvedValue({}),
  exportPdf: vi.fn().mockResolvedValue(new Blob()),
  exportCsv: vi.fn().mockResolvedValue(new Blob()),
}

vi.mock('@/stores/billing', () => ({
  useBillingStore: vi.fn(() => mockBillingStore),
}))

const mockToastAdd = vi.fn()
vi.mock('primevue/usetoast', () => ({
  useToast: vi.fn(() => ({ add: mockToastAdd })),
}))

import AdminBilling from '@/views/admin/AdminBilling.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      billing: {
        title: 'Jahresabrechnung',
        subtitle: 'Abrechnungsperioden verwalten',
        periods: 'Perioden',
        newPeriod: 'Neue Periode',
        activePeriod: 'Aktive Periode',
        noActivePeriod: 'Keine aktive Periode',
        periodName: 'Name',
        periodNamePlaceholder: 'z.B. Schuljahr 2025/26',
        startDate: 'Startdatum',
        endDate: 'Enddatum',
        status: 'Status',
        statusActive: 'Aktiv',
        create: 'Erstellen',
        createPeriod: 'Neue Periode erstellen',
        periodCreated: 'Periode erstellt',
        periodClosed: 'Periode geschlossen',
        createError: 'Fehler beim Erstellen',
        closeError: 'Fehler beim Schließen',
        closePeriod: 'Periode schließen',
        closeConfirm: 'Schließen',
        closeConfirmTitle: 'Periode schließen?',
        closeConfirmMessage: 'Wirklich schließen?',
        exportPdf: 'PDF',
        family: 'Familie',
        families: 'Familien',
        average: 'Durchschnitt',
        totalHours: 'Gesamt',
        targetHours: 'Soll-Stunden',
        actualHours: 'Ist-Stunden',
        balance: 'Differenz',
        balanceCol: 'Differenz',
        history: 'Verlauf',
        viewReport: 'Bericht anzeigen',
        members: 'Mitglieder',
        roleParent: 'Elternteil',
        roleChild: 'Kind',
      },
      admin: {
        familyCol: 'Familie',
        progressCol: 'Fortschritt',
        jobHoursCol: 'Job-Stunden',
        cleaningHoursCol: 'Putz-Stunden',
        trafficLightCol: 'Ampel',
        pdfExport: 'PDF Export',
        csvExport: 'CSV Export',
        trafficLight: { green: 'Grün', yellow: 'Gelb', red: 'Rot' },
      },
      common: { save: 'Speichern', cancel: 'Abbrechen', confirm: 'Bestätigen' },
    },
  },
})

const globalStubs = {
  PageTitle: { template: '<div />', props: ['title', 'subtitle'] },
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  Button: {
    template: '<button @click="$emit(\'click\')" :disabled="disabled">{{ label }}</button>',
    props: ['label', 'icon', 'severity', 'loading', 'disabled', 'size', 'text'],
    emits: ['click'],
  },
  DataTable: {
    template: '<div class="datatable-stub"><slot /><slot name="expansion" v-bind="{ data: {} }" /></div>',
    props: ['value', 'loading', 'expandedRows', 'dataKey', 'stripedRows'],
    emits: ['update:expandedRows'],
  },
  Column: { template: '<div />', props: ['field', 'header', 'expander', 'sortable', 'sortField', 'style'] },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity'] },
  DatePicker: {
    template: '<input type="date" />',
    props: ['modelValue', 'dateFormat', 'showIcon'],
    emits: ['update:modelValue'],
  },
  InputText: {
    template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
    props: ['modelValue', 'placeholder'],
    emits: ['update:modelValue'],
  },
  Dialog: {
    template: '<div v-if="visible"><slot /><slot name="footer" /></div>',
    props: ['visible', 'header', 'modal', 'style'],
    emits: ['update:visible', 'hide'],
  },
}

function mountBilling() {
  return mount(AdminBilling, {
    global: { plugins: [i18n], stubs: globalStubs },
  })
}

describe('AdminBilling', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    Object.assign(mockBillingStore, {
      periods: [],
      activePeriod: null,
      report: null,
      loading: false,
    })
  })

  // --- Mount & lifecycle ---

  it('should mount and call fetchPeriods on mount', async () => {
    const wrapper = mountBilling()
    await flushPromises()
    expect(mockBillingStore.fetchPeriods).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('should call fetchReport when activePeriod exists on mount', async () => {
    mockBillingStore.activePeriod = { id: 'p1', name: 'SJ 24/25', startDate: '2024-09-01', endDate: '2025-07-31', status: 'ACTIVE' }
    const wrapper = mountBilling()
    await flushPromises()
    expect(mockBillingStore.fetchReport).toHaveBeenCalledWith('p1')
    wrapper.unmount()
  })

  it('should not call fetchReport when no activePeriod', async () => {
    mockBillingStore.activePeriod = null
    const wrapper = mountBilling()
    await flushPromises()
    expect(mockBillingStore.fetchReport).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  // --- createPeriod ---

  it('should not call createPeriod when form is incomplete', async () => {
    const wrapper = mountBilling()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.createPeriod()
    expect(mockBillingStore.createPeriod).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('should call createPeriod when form is complete', async () => {
    mockBillingStore.createPeriod.mockResolvedValue({ id: 'p-new' })
    const wrapper = mountBilling()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.newName = 'Schuljahr 2025/26'
    vm.newStartDate = new Date('2025-09-01')
    vm.newEndDate = new Date('2026-07-31')
    await vm.createPeriod()
    expect(mockBillingStore.createPeriod).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'Schuljahr 2025/26' }),
    )
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))
    // Form should be reset
    expect(vm.newName).toBe('')
    expect(vm.newStartDate).toBeNull()
    expect(vm.newEndDate).toBeNull()
    wrapper.unmount()
  })

  it('should fetch report after creating period if activePeriod exists', async () => {
    mockBillingStore.createPeriod.mockResolvedValue({ id: 'p-new' })
    mockBillingStore.activePeriod = { id: 'p-new', name: 'SJ', startDate: '2025-09-01', endDate: '2026-07-31', status: 'ACTIVE' }
    const wrapper = mountBilling()
    await flushPromises()
    vi.clearAllMocks()
    const vm = wrapper.vm as any
    vm.newName = 'Test'
    vm.newStartDate = new Date('2025-09-01')
    vm.newEndDate = new Date('2026-07-31')
    await vm.createPeriod()
    expect(mockBillingStore.fetchReport).toHaveBeenCalledWith('p-new')
    wrapper.unmount()
  })

  it('should show error toast when createPeriod fails', async () => {
    mockBillingStore.createPeriod.mockRejectedValue(new Error('fail'))
    const wrapper = mountBilling()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.newName = 'Test'
    vm.newStartDate = new Date('2025-09-01')
    vm.newEndDate = new Date('2026-07-31')
    await vm.createPeriod()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'error' }))
    wrapper.unmount()
  })

  // --- closePeriod ---

  it('should not call closePeriod when no activePeriod', async () => {
    mockBillingStore.activePeriod = null
    const wrapper = mountBilling()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.closePeriod()
    expect(mockBillingStore.closePeriod).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('should call closePeriod and show success toast', async () => {
    mockBillingStore.activePeriod = { id: 'p1', name: 'SJ', startDate: '2024-09-01', endDate: '2025-07-31', status: 'ACTIVE' }
    const wrapper = mountBilling()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.showCloseDialog = true
    await vm.closePeriod()
    expect(mockBillingStore.closePeriod).toHaveBeenCalledWith('p1')
    expect(vm.showCloseDialog).toBe(false)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))
    wrapper.unmount()
  })

  it('should show error toast when closePeriod fails', async () => {
    mockBillingStore.activePeriod = { id: 'p1', name: 'SJ', startDate: '2024-09-01', endDate: '2025-07-31', status: 'ACTIVE' }
    mockBillingStore.closePeriod.mockRejectedValue(new Error('fail'))
    const wrapper = mountBilling()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.closePeriod()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'error' }))
    wrapper.unmount()
  })

  // --- loadClosedReport ---

  it('should call fetchReport with period id', async () => {
    const wrapper = mountBilling()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.loadClosedReport('p-closed')
    expect(mockBillingStore.fetchReport).toHaveBeenCalledWith('p-closed')
    wrapper.unmount()
  })

  // --- Helper functions ---

  it('trafficLightSeverity should map correctly', async () => {
    const wrapper = mountBilling()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.trafficLightSeverity('GREEN')).toBe('success')
    expect(vm.trafficLightSeverity('YELLOW')).toBe('warn')
    expect(vm.trafficLightSeverity('RED')).toBe('danger')
    expect(vm.trafficLightSeverity('UNKNOWN')).toBe('info')
    wrapper.unmount()
  })

  it('trafficLightLabel should return translated labels', async () => {
    const wrapper = mountBilling()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.trafficLightLabel('GREEN')).toBe('Grün')
    expect(vm.trafficLightLabel('YELLOW')).toBe('Gelb')
    expect(vm.trafficLightLabel('RED')).toBe('Rot')
    expect(vm.trafficLightLabel('OTHER')).toBe('OTHER')
    wrapper.unmount()
  })

  it('progressPercent should calculate correctly', async () => {
    const wrapper = mountBilling()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.progressPercent(15, 30)).toBe(50)
    expect(vm.progressPercent(30, 30)).toBe(100)
    expect(vm.progressPercent(45, 30)).toBe(100) // capped at 100
    expect(vm.progressPercent(0, 0)).toBe(100) // edge case
    wrapper.unmount()
  })

  it('formatDateISO should format date to ISO string', async () => {
    const wrapper = mountBilling()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.formatDateISO(new Date(2025, 8, 1))).toBe('2025-09-01')
    expect(vm.formatDateISO(new Date(2026, 0, 15))).toBe('2026-01-15')
    wrapper.unmount()
  })

  it('formatDateDisplay should convert ISO to German format', async () => {
    const wrapper = mountBilling()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.formatDateDisplay('2025-09-01')).toBe('01.09.2025')
    expect(vm.formatDateDisplay('2026-01-15')).toBe('15.01.2026')
    wrapper.unmount()
  })

  it('translateRole should return translated role names', async () => {
    const wrapper = mountBilling()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.translateRole('PARENT')).toBe('Elternteil')
    expect(vm.translateRole('CHILD')).toBe('Kind')
    expect(vm.translateRole('OTHER')).toBe('OTHER')
    wrapper.unmount()
  })

  // --- Template rendering with active period ---

  it('should render create form when no active period', async () => {
    mockBillingStore.activePeriod = null
    const wrapper = mountBilling()
    await flushPromises()
    expect(wrapper.text()).toContain('Neue Periode erstellen')
    wrapper.unmount()
  })

  it('should render period info when active period exists', async () => {
    mockBillingStore.activePeriod = {
      id: 'p1',
      name: 'Schuljahr 2024/25',
      startDate: '2024-09-01',
      endDate: '2025-07-31',
      status: 'ACTIVE',
    }
    mockBillingStore.report = {
      summary: { totalFamilies: 5, averageHours: 20, totalHoursAll: 100, greenCount: 3, yellowCount: 1, redCount: 1 },
      families: [],
    }
    const wrapper = mountBilling()
    await flushPromises()
    expect(wrapper.text()).toContain('Schuljahr 2024/25')
    expect(wrapper.text()).toContain('Aktiv')
    wrapper.unmount()
  })

  it('should render summary cards when report exists', async () => {
    mockBillingStore.activePeriod = { id: 'p1', name: 'SJ', startDate: '2024-09-01', endDate: '2025-07-31', status: 'ACTIVE' }
    mockBillingStore.report = {
      summary: { totalFamilies: 10, averageHours: 22, totalHoursAll: 220, greenCount: 6, yellowCount: 3, redCount: 1 },
      families: [],
    }
    const wrapper = mountBilling()
    await flushPromises()
    expect(wrapper.text()).toContain('10')
    expect(wrapper.text()).toContain('22')
    wrapper.unmount()
  })

  it('should render history of closed periods', async () => {
    mockBillingStore.periods = [
      { id: 'p-closed', name: 'SJ 2023/24', startDate: '2023-09-01', endDate: '2024-07-31', status: 'CLOSED' },
    ]
    const wrapper = mountBilling()
    await flushPromises()
    expect(wrapper.text()).toContain('Verlauf')
    expect(wrapper.text()).toContain('SJ 2023/24')
    wrapper.unmount()
  })
})
