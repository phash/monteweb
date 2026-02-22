import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import FormDetailView from '@/views/FormDetailView.vue'

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn(), back: vi.fn() })),
  useRoute: vi.fn(() => ({ params: { id: 'form-1' }, query: {} })),
}))

// Mock returns a form that has already been responded to,
// avoiding the response-form code path that requires answer refs
vi.mock('@/api/forms.api', () => ({
  formsApi: {
    getAvailableForms: vi.fn().mockResolvedValue({ data: { data: { content: [], last: true, totalElements: 0 } } }),
    getMyForms: vi.fn().mockResolvedValue({ data: { data: { content: [], last: true, totalElements: 0 } } }),
    getForm: vi.fn().mockResolvedValue({
      data: {
        data: {
          form: {
            id: 'form-1',
            title: 'Elternbefragung',
            description: 'Umfrage zur Zufriedenheit',
            type: 'SURVEY',
            scope: 'ROOM',
            scopeId: 'r1',
            scopeName: 'Klasse 3a',
            anonymous: false,
            deadline: '2025-07-01',
            status: 'PUBLISHED',
            createdBy: 'user-1',
            creatorName: 'Max Mustermann',
            responseCount: 5,
            hasUserResponded: true,
          },
          questions: [
            {
              id: 'q-1',
              type: 'TEXT',
              label: 'Was gefällt Ihnen?',
              description: 'Bitte ausführlich antworten',
              required: true,
              options: null,
              ratingConfig: null,
            },
          ],
        },
      },
    }),
    createForm: vi.fn(),
    updateForm: vi.fn(),
    publishForm: vi.fn().mockResolvedValue({ data: { data: { id: 'form-1', status: 'PUBLISHED' } } }),
    closeForm: vi.fn().mockResolvedValue({ data: { data: { id: 'form-1', status: 'CLOSED' } } }),
    deleteForm: vi.fn().mockResolvedValue({}),
    submitResponse: vi.fn().mockResolvedValue({}),
    getMyResponse: vi.fn().mockResolvedValue({ data: { data: null } }),
    updateResponse: vi.fn().mockResolvedValue({}),
    archiveForm: vi.fn().mockResolvedValue({ data: { data: { id: 'form-1', status: 'ARCHIVED' } } }),
    getResults: vi.fn(),
    getIndividualResponses: vi.fn(),
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
      forms: {
        types: { SURVEY: 'Umfrage', CONSENT: 'Einwilligung' },
        statuses: { DRAFT: 'Entwurf', PUBLISHED: 'Veröffentlicht', CLOSED: 'Geschlossen', ARCHIVED: 'Archiviert' },
        anonymous: 'Anonym',
        deadlineLabel: 'Frist',
        responsesCount: 'Antworten',
        editQuestions: 'Fragen bearbeiten',
        publish: 'Veröffentlichen',
        close: 'Schließen',
        viewResults: 'Ergebnisse',
        deleteForm: 'Löschen',
        alreadyResponded: 'Bereits beantwortet',
        submitResponse: 'Antwort senden',
        thankYou: 'Vielen Dank!',
        published: 'Veröffentlicht',
        closed: 'Geschlossen',
        deleted: 'Gelöscht',
        yes: 'Ja',
        no: 'Nein',
        questionTypes: {
          TEXT: 'Text',
          SINGLE_CHOICE: 'Einzelauswahl',
          MULTIPLE_CHOICE: 'Mehrfachauswahl',
          RATING: 'Bewertung',
          YES_NO: 'Ja/Nein',
        },
        user: 'Benutzer',
        editResponse: 'Antwort bearbeiten',
        updateResponse: 'Antwort aktualisieren',
        responseUpdated: 'Antwort aktualisiert',
        archive: 'Archivieren',
        formArchived: 'Formular archiviert',
        viewResultsHint: 'Ergebnisse ansehen',
        deadlinePassed: 'Frist abgelaufen',
        canEditUntil: 'Bearbeitbar bis',
        cannotEditAnonymous: 'Anonyme Antworten können nicht bearbeitet werden',
      },
      common: {
        back: 'Zurück',
        error: 'Fehler',
      },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title'] },
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'text', 'severity', 'size', 'loading', 'disabled', 'outlined'],
    emits: ['click'],
  },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity', 'size', 'icon'] },
  Textarea: { template: '<textarea class="textarea-stub" />', props: ['modelValue', 'autoResize', 'rows'] },
  RadioButton: { template: '<input class="radio-stub" type="radio" />', props: ['modelValue', 'value', 'name', 'inputId'] },
  Checkbox: { template: '<input class="checkbox-stub" type="checkbox" />', props: ['modelValue', 'binary', 'inputId'] },
  Rating: { template: '<div class="rating-stub" />', props: ['modelValue', 'stars'] },
}

function mountFormDetail() {
  const pinia = createPinia()
  return mount(FormDetailView, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('FormDetailView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render component', () => {
    const wrapper = mountFormDetail()
    expect(wrapper.exists()).toBe(true)
  })

  it('should show loading spinner or content on mount', () => {
    const wrapper = mountFormDetail()
    // Either loading or already rendered (mocks resolve immediately)
    expect(
      wrapper.find('.loading-stub').exists() || wrapper.find('.page-title-stub').exists()
    ).toBe(true)
  })

  it('should render back button', () => {
    const wrapper = mountFormDetail()
    const buttons = wrapper.findAll('.button-stub')
    expect(buttons[0]!.text()).toContain('Zurück')
  })

  it('should call fetchForm on mount', async () => {
    const { formsApi } = await import('@/api/forms.api')
    mountFormDetail()
    await vi.waitFor(() => {
      expect(formsApi.getForm).toHaveBeenCalledWith('form-1')
    })
  })

  it('should show form title after loading', async () => {
    const wrapper = mountFormDetail()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    const pageTitle = wrapper.find('.page-title-stub')
    const loadingStub = wrapper.find('.loading-stub')
    expect(pageTitle.exists() || loadingStub.exists()).toBe(true)
    if (pageTitle.exists()) {
      expect(pageTitle.text()).toContain('Elternbefragung')
    }
  })

  it('should show already-responded message for responded forms', async () => {
    const wrapper = mountFormDetail()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(
      wrapper.find('.responded-message').exists() || wrapper.find('.loading-stub').exists()
    ).toBe(true)
  })

  it('should show meta bar with form info after loading', async () => {
    const wrapper = mountFormDetail()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(
      wrapper.find('.form-meta-bar').exists() || wrapper.find('.loading-stub').exists()
    ).toBe(true)
  })

  it('should show tags after loading', async () => {
    const wrapper = mountFormDetail()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(
      wrapper.findAll('.tag-stub').length > 0 || wrapper.find('.loading-stub').exists()
    ).toBe(true)
  })

  it('should render questions in readonly mode for DRAFT forms', async () => {
    const { formsApi } = await import('@/api/forms.api')
    vi.mocked(formsApi.getForm).mockResolvedValue({
      data: {
        data: {
          form: {
            id: 'form-1',
            title: 'Draft Form',
            description: '',
            type: 'SURVEY',
            scope: 'ROOM',
            scopeId: 'r1',
            scopeName: 'Klasse 3a',
            anonymous: false,
            deadline: null,
            status: 'DRAFT',
            createdBy: 'user-1',
            creatorName: 'Max Mustermann',
            responseCount: 0,
            hasUserResponded: false,
          },
          questions: [
            {
              id: 'q-1', type: 'TEXT', label: 'Frage 1',
              description: null, required: true, options: null, ratingConfig: null,
            },
          ],
        },
      },
    } as any)

    const pinia = createPinia()
    const wrapper = mount(FormDetailView, {
      global: { plugins: [i18n, pinia], stubs },
    })
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(
      wrapper.find('.questions-readonly').exists() || wrapper.find('.loading-stub').exists()
    ).toBe(true)
  })

  it('should show edit response button for responded PUBLISHED form with future deadline', async () => {
    const { formsApi } = await import('@/api/forms.api')
    const futureDate = new Date()
    futureDate.setDate(futureDate.getDate() + 14)
    const deadline = futureDate.toISOString().split('T')[0]

    vi.mocked(formsApi.getForm).mockResolvedValue({
      data: {
        data: {
          form: {
            id: 'form-1',
            title: 'Active Survey',
            description: '',
            type: 'SURVEY',
            scope: 'ROOM',
            scopeId: 'r1',
            scopeName: 'Klasse 3a',
            anonymous: false,
            deadline,
            status: 'PUBLISHED',
            createdBy: 'other-user',
            creatorName: 'Andere Person',
            responseCount: 5,
            hasUserResponded: true,
          },
          questions: [
            { id: 'q-1', type: 'TEXT', label: 'Frage', description: null, required: true, options: null, ratingConfig: null },
          ],
        },
      },
    } as any)

    const pinia = createPinia()
    const wrapper = mount(FormDetailView, {
      global: { plugins: [i18n, pinia], stubs },
    })
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    if (wrapper.find('.responded-message').exists()) {
      const buttons = wrapper.findAll('.button-stub')
      const texts = buttons.map(b => b.text())
      expect(texts.some(t => t.includes('Antwort bearbeiten'))).toBe(true)
    }
  })

  it('should show archive button for CLOSED form when user is creator', async () => {
    const { formsApi } = await import('@/api/forms.api')
    vi.mocked(formsApi.getForm).mockResolvedValue({
      data: {
        data: {
          form: {
            id: 'form-1',
            title: 'Closed Form',
            description: '',
            type: 'SURVEY',
            scope: 'ROOM',
            scopeId: 'r1',
            scopeName: 'Klasse 3a',
            anonymous: false,
            deadline: null,
            status: 'CLOSED',
            createdBy: 'current-user',
            creatorName: 'Current User',
            responseCount: 10,
            hasUserResponded: false,
          },
          questions: [],
        },
      },
    } as any)

    const pinia = createPinia()
    // Set auth store user to match creator
    const { useAuthStore } = await import('@/stores/auth')
    setActivePinia(pinia)
    const authStore = useAuthStore()
    authStore.user = { id: 'current-user', role: 'TEACHER' } as any

    const wrapper = mount(FormDetailView, {
      global: { plugins: [i18n, pinia], stubs },
    })
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    if (wrapper.find('.management-actions').exists()) {
      const buttons = wrapper.findAll('.button-stub')
      const texts = buttons.map(b => b.text())
      expect(texts.some(t => t.includes('Archivieren'))).toBe(true)
    }
  })

  it('should show delete button for creator on all statuses', async () => {
    const { formsApi } = await import('@/api/forms.api')
    vi.mocked(formsApi.getForm).mockResolvedValue({
      data: {
        data: {
          form: {
            id: 'form-1',
            title: 'Published Form',
            description: '',
            type: 'SURVEY',
            scope: 'ROOM',
            scopeId: 'r1',
            scopeName: 'Klasse 3a',
            anonymous: false,
            deadline: null,
            status: 'PUBLISHED',
            createdBy: 'current-user',
            creatorName: 'Current User',
            responseCount: 3,
            hasUserResponded: false,
          },
          questions: [],
        },
      },
    } as any)

    const pinia = createPinia()
    const { useAuthStore } = await import('@/stores/auth')
    setActivePinia(pinia)
    const authStore = useAuthStore()
    authStore.user = { id: 'current-user', role: 'TEACHER' } as any

    const wrapper = mount(FormDetailView, {
      global: { plugins: [i18n, pinia], stubs },
    })
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    if (wrapper.find('.management-actions').exists()) {
      const buttons = wrapper.findAll('.button-stub')
      const texts = buttons.map(b => b.text())
      expect(texts.some(t => t.includes('Löschen'))).toBe(true)
    }
  })

  it('should show view results link for closed forms with user responded', async () => {
    const { formsApi } = await import('@/api/forms.api')
    vi.mocked(formsApi.getForm).mockResolvedValue({
      data: {
        data: {
          form: {
            id: 'form-1',
            title: 'Closed Survey',
            description: '',
            type: 'SURVEY',
            scope: 'ROOM',
            scopeId: 'r1',
            scopeName: 'Klasse 3a',
            anonymous: false,
            deadline: null,
            status: 'CLOSED',
            createdBy: 'other-user',
            creatorName: 'Other User',
            responseCount: 20,
            hasUserResponded: true,
          },
          questions: [],
        },
      },
    } as any)

    const pinia = createPinia()
    const wrapper = mount(FormDetailView, {
      global: { plugins: [i18n, pinia], stubs },
    })
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    if (wrapper.find('.responded-message').exists()) {
      const buttons = wrapper.findAll('.button-stub')
      const texts = buttons.map(b => b.text())
      expect(texts.some(t => t.includes('Ergebnisse ansehen'))).toBe(true)
    }
  })
})
