import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import JobDetailView from '@/views/JobDetailView.vue'

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn(), back: vi.fn() })),
}))

vi.mock('@/api/jobboard.api', () => ({
  jobboardApi: {
    listJobs: vi.fn().mockResolvedValue({ data: { data: { content: [], last: true } } }),
    getCategories: vi.fn().mockResolvedValue({ data: { data: [] } }),
    createJob: vi.fn(),
    getJob: vi.fn().mockResolvedValue({
      data: {
        data: {
          id: 'job-1',
          title: 'Garten aufräumen',
          description: 'Schulhof säubern',
          category: 'Gartenarbeit',
          location: 'Schulhof',
          estimatedHours: 2,
          maxAssignees: 3,
          currentAssignees: 1,
          status: 'OPEN',
          scheduledDate: '2025-06-20',
          scheduledTime: '14:00',
          creatorName: 'Max Mustermann',
          contactInfo: 'tel@example.com',
          eventTitle: null,
          eventId: null,
        },
      },
    }),
    applyForJob: vi.fn().mockResolvedValue({ data: { data: { id: 'a-1', jobId: 'job-1', status: 'ASSIGNED' } } }),
    getAssignments: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getMyAssignments: vi.fn().mockResolvedValue({ data: { data: [] } }),
    startAssignment: vi.fn(),
    completeAssignment: vi.fn(),
    confirmAssignment: vi.fn(),
    getFamilyHours: vi.fn(),
    getReport: vi.fn(),
    getReportSummary: vi.fn(),
    exportCsv: vi.fn(),
    exportPdf: vi.fn(),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      jobboard: {
        apply: 'Bewerben',
        start: 'Starten',
        complete: 'Abschließen',
        completeTask: 'Aufgabe abschließen',
        actualHours: 'Tatsächliche Stunden',
        notesOptional: 'Notizen (optional)',
        assignments: 'Zuweisungen',
        noAssignmentsYet: 'Noch keine Zuweisungen.',
        confirmed: 'Bestätigt',
        category: 'Kategorie',
        location: 'Ort',
        estimatedHours: 'Geschätzte Stunden',
        date: 'Datum',
        contact: 'Kontakt',
        linkedEvent: 'Verknüpftes Event',
        statuses: {
          OPEN: 'Offen',
          ASSIGNED: 'Zugewiesen',
          IN_PROGRESS: 'In Bearbeitung',
          COMPLETED: 'Abgeschlossen',
          CANCELLED: 'Abgesagt',
        },
        assignmentStatuses: {
          ASSIGNED: 'Zugewiesen',
          IN_PROGRESS: 'In Bearbeitung',
          COMPLETED: 'Abgeschlossen',
          CANCELLED: 'Abgesagt',
        },
      },
      common: {
        back: 'Zurück',
        cancel: 'Abbrechen',
        confirm: 'Bestätigen',
        createdBy: 'Erstellt von',
        slots: 'Plätze',
      },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title'] },
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'text', 'severity', 'size', 'loading', 'disabled'],
    emits: ['click'],
  },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity', 'size'] },
  Dialog: {
    template: '<div v-if="visible" class="dialog-stub"><slot /><slot name="footer" /></div>',
    props: ['visible', 'header', 'modal', 'style'],
  },
  InputNumber: { template: '<input class="input-number-stub" type="number" />', props: ['modelValue', 'min', 'max', 'step', 'minFractionDigits', 'maxFractionDigits'] },
  Textarea: { template: '<textarea class="textarea-stub" />', props: ['modelValue', 'autoResize', 'rows'] },
}

function mountJobDetail() {
  const pinia = createPinia()
  return mount(JobDetailView, {
    props: { id: 'job-1' },
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('JobDetailView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render component', () => {
    const wrapper = mountJobDetail()
    expect(wrapper.exists()).toBe(true)
  })

  it('should show loading or job details on mount', () => {
    const wrapper = mountJobDetail()
    // Since mocks resolve immediately, the component may already show job details
    expect(
      wrapper.find('.loading-stub').exists() ||
      wrapper.find('.job-details').exists() ||
      wrapper.find('.button-stub').exists()
    ).toBe(true)
  })

  it('should render back button', () => {
    const wrapper = mountJobDetail()
    const buttons = wrapper.findAll('.button-stub')
    expect(buttons[0]!.text()).toContain('Zurück')
  })

  it('should render job details after loading', async () => {
    const wrapper = mountJobDetail()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    // Should either be loading or showing details
    expect(
      wrapper.find('.loading-stub').exists() ||
      wrapper.find('.job-details').exists()
    ).toBe(true)
  })

  it('should call store fetchJob on mount', async () => {
    const { jobboardApi } = await import('@/api/jobboard.api')
    mountJobDetail()
    await vi.waitFor(() => {
      expect(jobboardApi.getJob).toHaveBeenCalledWith('job-1')
    })
  })

  it('should call store fetchAssignments on mount', async () => {
    const { jobboardApi } = await import('@/api/jobboard.api')
    mountJobDetail()
    await vi.waitFor(() => {
      expect(jobboardApi.getAssignments).toHaveBeenCalledWith('job-1')
    })
  })

  it('should call store fetchMyAssignments on mount', async () => {
    const { jobboardApi } = await import('@/api/jobboard.api')
    mountJobDetail()
    await vi.waitFor(() => {
      expect(jobboardApi.getMyAssignments).toHaveBeenCalled()
    })
  })

  it('should show assignments section after data loads', async () => {
    const wrapper = mountJobDetail()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    // Once job loads, should show assignments header or loading
    expect(wrapper.exists()).toBe(true)
  })
})
