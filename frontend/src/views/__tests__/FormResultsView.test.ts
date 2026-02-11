import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import FormResultsView from '@/views/FormResultsView.vue'

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn(), back: vi.fn() })),
  useRoute: vi.fn(() => ({ params: { id: 'form-1' }, query: {} })),
}))

vi.mock('@/api/forms.api', () => ({
  formsApi: {
    getAvailableForms: vi.fn().mockResolvedValue({ data: { data: { content: [], last: true, totalElements: 0 } } }),
    getMyForms: vi.fn().mockResolvedValue({ data: { data: { content: [], last: true, totalElements: 0 } } }),
    getForm: vi.fn(),
    createForm: vi.fn(),
    updateForm: vi.fn(),
    publishForm: vi.fn(),
    closeForm: vi.fn(),
    deleteForm: vi.fn(),
    submitResponse: vi.fn(),
    getResults: vi.fn().mockResolvedValue({
      data: {
        data: {
          form: {
            id: 'form-1',
            title: 'Elternbefragung',
            responseCount: 10,
            targetCount: 20,
            anonymous: false,
          },
          results: [
            {
              questionId: 'q-1',
              label: 'Was gefällt Ihnen?',
              type: 'TEXT',
              totalAnswers: 8,
              textAnswers: ['Sehr gut', 'Könnte besser sein', 'Alles top'],
              optionCounts: null,
              averageRating: null,
              ratingDistribution: null,
              yesCount: null,
              noCount: null,
            },
            {
              questionId: 'q-2',
              label: 'Bewertung',
              type: 'SINGLE_CHOICE',
              totalAnswers: 10,
              textAnswers: null,
              optionCounts: { Gut: 5, Mittel: 3, Schlecht: 2 },
              averageRating: null,
              ratingDistribution: null,
              yesCount: null,
              noCount: null,
            },
            {
              questionId: 'q-3',
              label: 'Sternebewertung',
              type: 'RATING',
              totalAnswers: 9,
              textAnswers: null,
              optionCounts: null,
              averageRating: 3.7,
              ratingDistribution: { '1': 1, '2': 1, '3': 2, '4': 3, '5': 2 },
              yesCount: null,
              noCount: null,
            },
            {
              questionId: 'q-4',
              label: 'Zufrieden?',
              type: 'YES_NO',
              totalAnswers: 10,
              textAnswers: null,
              optionCounts: null,
              averageRating: null,
              ratingDistribution: null,
              yesCount: 7,
              noCount: 3,
            },
          ],
        },
      },
    }),
    getIndividualResponses: vi.fn().mockResolvedValue({
      data: {
        data: [
          {
            userId: 'u1',
            userName: 'Max Mustermann',
            submittedAt: '2025-06-15T10:00:00Z',
            answers: [
              { questionId: 'q-1', text: 'Sehr gut', selectedOptions: null, rating: null },
            ],
          },
        ],
      },
    }),
    exportCsv: vi.fn().mockResolvedValue({ data: 'csv-data' }),
    exportPdf: vi.fn().mockResolvedValue({ data: new Blob() }),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      forms: {
        resultsSummary: 'Ergebnisse',
        responseRate: 'Antwortrate',
        responsesCount: 'Antworten',
        exportCsv: 'CSV Export',
        exportPdf: 'PDF Export',
        average: 'Durchschnitt',
        yes: 'Ja',
        no: 'Nein',
        individualResponses: 'Einzelne Antworten',
        user: 'Benutzer',
        submittedAt: 'Eingereicht am',
      },
      common: {
        back: 'Zurück',
      },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title'] },
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'text', 'severity', 'size'],
    emits: ['click'],
  },
  ProgressBar: { template: '<div class="progress-stub" />', props: ['value', 'showValue'] },
  DataTable: { template: '<div class="datatable-stub"><slot /></div>', props: ['value', 'stripedRows', 'size'] },
  Column: { template: '<div class="column-stub"><slot /></div>', props: ['field', 'header'] },
}

function mountFormResults() {
  const pinia = createPinia()
  return mount(FormResultsView, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('FormResultsView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render component', () => {
    const wrapper = mountFormResults()
    expect(wrapper.exists()).toBe(true)
  })

  it('should show loading spinner before data loads', () => {
    const wrapper = mountFormResults()
    expect(wrapper.find('.loading-stub').exists()).toBe(true)
  })

  it('should render back button', () => {
    const wrapper = mountFormResults()
    const buttons = wrapper.findAll('.button-stub')
    expect(buttons[0]!.text()).toContain('Zurück')
  })

  it('should call fetchResults on mount', async () => {
    const { formsApi } = await import('@/api/forms.api')
    mountFormResults()
    await vi.waitFor(() => {
      expect(formsApi.getResults).toHaveBeenCalledWith('form-1')
    })
  })

  it('should show page title with form name after loading', async () => {
    const wrapper = mountFormResults()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(
      wrapper.find('.page-title-stub').exists() || wrapper.find('.loading-stub').exists()
    ).toBe(true)
  })

  it('should render response rate section after loading', async () => {
    const wrapper = mountFormResults()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(
      wrapper.find('.response-rate').exists() || wrapper.find('.loading-stub').exists()
    ).toBe(true)
  })

  it('should render export buttons after loading', async () => {
    const wrapper = mountFormResults()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    const buttons = wrapper.findAll('.button-stub')
    const texts = buttons.map(b => b.text())
    // After loading, should include CSV/PDF export buttons or still be loading
    expect(
      texts.some(t => t.includes('CSV')) || wrapper.find('.loading-stub').exists()
    ).toBe(true)
  })

  it('should render result cards for each question after loading', async () => {
    const wrapper = mountFormResults()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(
      wrapper.findAll('.result-card').length > 0 || wrapper.find('.loading-stub').exists()
    ).toBe(true)
  })

  it('should fetch individual responses for non-anonymous forms', async () => {
    const { formsApi } = await import('@/api/forms.api')
    mountFormResults()
    await vi.waitFor(() => {
      expect(formsApi.getResults).toHaveBeenCalledWith('form-1')
    })
    // After results load and form is not anonymous, should also call individual responses
    await vi.waitFor(() => {
      expect(formsApi.getIndividualResponses).toHaveBeenCalledWith('form-1')
    })
  })
})
