import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import AdminJobReport from '@/views/admin/AdminJobReport.vue'

vi.mock('@/api/jobboard.api', () => ({
  jobboardApi: {
    getReport: vi.fn().mockResolvedValue({
      data: {
        data: [
          {
            familyName: 'Familie Mustermann', totalHours: 15, targetHours: 30,
            completedHours: 10, cleaningHours: 5, pendingHours: 3, remainingHours: 15,
            trafficLight: 'YELLOW',
          },
          {
            familyName: 'Familie Schmidt', totalHours: 28, targetHours: 30,
            completedHours: 22, cleaningHours: 6, pendingHours: 0, remainingHours: 2,
            trafficLight: 'GREEN',
          },
          {
            familyName: 'Familie Braun', totalHours: 2, targetHours: 30,
            completedHours: 2, cleaningHours: 0, pendingHours: 0, remainingHours: 28,
            trafficLight: 'RED',
          },
        ],
      },
    }),
    getReportSummary: vi.fn().mockResolvedValue({
      data: {
        data: {
          openJobs: 5, activeJobs: 12, completedJobs: 45,
          greenFamilies: 20, yellowFamilies: 10, redFamilies: 5,
        },
      },
    }),
    exportCsv: vi.fn().mockResolvedValue({ data: new Blob(['csv-data']) }),
    exportPdf: vi.fn().mockResolvedValue({ data: new Blob(['pdf-data'], { type: 'application/pdf' }) }),
    listJobs: vi.fn().mockResolvedValue({ data: { data: { content: [], last: true } } }),
    getCategories: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getMyAssignments: vi.fn().mockResolvedValue({ data: { data: [] } }),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
  },
}))

import { jobboardApi } from '@/api/jobboard.api'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      admin: {
        familyReport: 'Familien-Stundenbericht',
        reportSubtitle: 'Übersicht aller Familien',
        pdfExport: 'PDF-Export',
        csvExport: 'CSV-Export',
        openJobs: 'Offene Jobs',
        activeJobs: 'Aktive Jobs',
        completed: 'Abgeschlossen',
        familyCol: 'Familie',
        progressCol: 'Fortschritt',
        jobHoursCol: 'Jobstunden',
        cleaningHoursCol: 'Putzstunden',
        pendingCol: 'Ausstehend',
        remainingCol: 'Verbleibend',
        trafficLightCol: 'Ampel',
        trafficLight: {
          green: 'Grün',
          yellow: 'Gelb',
          red: 'Rot',
        },
      },
      common: { loading: 'Laden...' },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title', 'subtitle'] },
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'severity'],
    emits: ['click'],
  },
  DataTable: {
    template: '<div class="datatable-stub"><slot /></div>',
    props: ['value', 'stripedRows'],
  },
  Column: { template: '<div class="column-stub"><slot /></div>', props: ['field', 'header', 'sortable', 'sortField'] },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity'] },
}

function mountComponent() {
  const pinia = createPinia()
  return mount(AdminJobReport, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('AdminJobReport', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render component', () => {
    const wrapper = mountComponent()
    expect(wrapper.exists()).toBe(true)
  })

  it('should render page title', () => {
    const wrapper = mountComponent()
    expect(wrapper.find('.page-title-stub').text()).toContain('Familien-Stundenbericht')
  })

  it('should render PDF export button', () => {
    const wrapper = mountComponent()
    const buttons = wrapper.findAll('.button-stub')
    const pdfBtn = buttons.find(b => b.text().includes('PDF-Export'))
    expect(pdfBtn).toBeTruthy()
  })

  it('should render CSV export button', () => {
    const wrapper = mountComponent()
    const buttons = wrapper.findAll('.button-stub')
    const csvBtn = buttons.find(b => b.text().includes('CSV-Export'))
    expect(csvBtn).toBeTruthy()
  })

  it('should fetch report on mount', async () => {
    mountComponent()
    await flushPromises()
    expect(jobboardApi.getReport).toHaveBeenCalled()
    expect(jobboardApi.getReportSummary).toHaveBeenCalled()
  })

  it('should render DataTable', () => {
    const wrapper = mountComponent()
    expect(wrapper.find('.datatable-stub').exists()).toBe(true)
  })

  it('should render report columns', () => {
    const wrapper = mountComponent()
    const columns = wrapper.findAll('.column-stub')
    // familyName, progress, completedHours, cleaningHours, pendingHours, remainingHours, trafficLight
    expect(columns.length).toBe(7)
  })

  it('should render summary cards after loading', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    expect(wrapper.text()).toContain('5')  // openJobs
    expect(wrapper.text()).toContain('12') // activeJobs
    expect(wrapper.text()).toContain('45') // completedJobs
    expect(wrapper.text()).toContain('20') // greenFamilies
    expect(wrapper.text()).toContain('10') // yellowFamilies
  })

  it('should render summary card labels', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    expect(wrapper.text()).toContain('Offene Jobs')
    expect(wrapper.text()).toContain('Aktive Jobs')
    expect(wrapper.text()).toContain('Abgeschlossen')
    expect(wrapper.text()).toContain('Grün')
    expect(wrapper.text()).toContain('Gelb')
    expect(wrapper.text()).toContain('Rot')
  })

  it('should not show loading spinner after data loads', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.find('.loading-stub').exists()).toBe(false)
  })
})
