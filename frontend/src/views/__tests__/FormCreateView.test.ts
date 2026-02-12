import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import FormCreateView from '@/views/FormCreateView.vue'

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn(), back: vi.fn() })),
  useRoute: vi.fn(() => ({ name: 'form-create', params: {}, query: {} })),
}))

vi.mock('@/api/forms.api', () => ({
  formsApi: {
    getAvailableForms: vi.fn().mockResolvedValue({ data: { data: { content: [], last: true, totalElements: 0 } } }),
    getMyForms: vi.fn().mockResolvedValue({ data: { data: { content: [], last: true, totalElements: 0 } } }),
    getForm: vi.fn().mockResolvedValue({
      data: {
        data: {
          form: {
            id: 'f-1', title: 'Test Form', description: '', type: 'SURVEY',
            scope: 'ROOM', scopeId: 'r1', anonymous: false, deadline: null,
            status: 'DRAFT', createdBy: 'user-1',
          },
          questions: [],
        },
      },
    }),
    createForm: vi.fn().mockResolvedValue({
      data: {
        data: {
          form: { id: 'f-new', title: 'New Form', status: 'DRAFT' },
          questions: [],
        },
      },
    }),
    updateForm: vi.fn().mockResolvedValue({ data: { data: { form: { id: 'f-1' }, questions: [] } } }),
    publishForm: vi.fn().mockResolvedValue({ data: { data: { id: 'f-1', status: 'PUBLISHED' } } }),
    closeForm: vi.fn(),
    deleteForm: vi.fn(),
    submitResponse: vi.fn(),
    getResults: vi.fn(),
    getIndividualResponses: vi.fn(),
    exportCsv: vi.fn(),
    exportPdf: vi.fn(),
  },
}))

vi.mock('@/api/rooms.api', () => ({
  roomsApi: {
    getMine: vi.fn().mockResolvedValue({
      data: {
        data: [
          { id: 'r1', name: 'Room 1', type: 'KLASSE', memberCount: 5, description: null, publicDescription: null, avatarUrl: null, sectionId: null, archived: false, joinPolicy: 'OPEN', expiresAt: null, tags: [] },
        ],
      },
    }),
    getById: vi.fn(),
    create: vi.fn(),
    discover: vi.fn(),
    joinRoom: vi.fn(),
    leaveRoom: vi.fn(),
    getChatChannels: vi.fn(),
    getOrCreateChatChannel: vi.fn(),
    createInterestRoom: vi.fn(),
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
        createForm: 'Formular erstellen',
        editForm: 'Formular bearbeiten',
        formTitle: 'Titel',
        titlePlaceholder: 'Formulartitel eingeben',
        description: 'Beschreibung',
        formType: 'Formulartyp',
        scope: 'Bereich',
        selectRoom: 'Raum wählen',
        deadline: 'Frist',
        anonymousLabel: 'Anonym',
        questions: 'Fragen',
        addQuestion: 'Frage hinzufügen',
        noQuestions: 'Noch keine Fragen hinzugefügt.',
        questionType: 'Fragetyp',
        questionLabel: 'Frage',
        questionDescription: 'Beschreibung',
        required: 'Pflichtfeld',
        options: 'Optionen',
        option: 'Option',
        addOption: 'Option hinzufügen',
        saveDraft: 'Entwurf speichern',
        publish: 'Veröffentlichen',
        saved: 'Gespeichert',
        published: 'Veröffentlicht',
        types: { SURVEY: 'Umfrage', CONSENT: 'Einwilligung' },
        scopes: { ROOM: 'Raum', SECTION: 'Bereich', SCHOOL: 'Schule' },
        questionTypes: {
          TEXT: 'Text',
          SINGLE_CHOICE: 'Einzelauswahl',
          MULTIPLE_CHOICE: 'Mehrfachauswahl',
          RATING: 'Bewertung',
          YES_NO: 'Ja/Nein',
        },
      },
      common: {
        back: 'Zurück',
        cancel: 'Abbrechen',
        error: 'Fehler',
      },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title'] },
  EmptyState: { template: '<div class="empty-stub">{{ message }}</div>', props: ['icon', 'message'] },
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'text', 'severity', 'size', 'loading', 'disabled', 'outlined'],
    emits: ['click'],
  },
  InputText: { template: '<input class="input-stub" />', props: ['modelValue', 'placeholder', 'type'] },
  Textarea: { template: '<textarea class="textarea-stub" />', props: ['modelValue', 'autoResize', 'rows'] },
  DatePicker: { template: '<input class="datepicker-stub" />', props: ['modelValue', 'dateFormat', 'showIcon'] },
  Select: { template: '<select class="select-stub" />', props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder'] },
  Checkbox: { template: '<input class="checkbox-stub" type="checkbox" />', props: ['modelValue', 'binary', 'inputId'] },
  Divider: { template: '<hr class="divider-stub" />' },
}

function mountFormCreate() {
  const pinia = createPinia()
  return mount(FormCreateView, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('FormCreateView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render component', () => {
    const wrapper = mountFormCreate()
    expect(wrapper.exists()).toBe(true)
  })

  it('should render page title for create mode', () => {
    const wrapper = mountFormCreate()
    expect(wrapper.find('.page-title-stub').text()).toContain('Formular erstellen')
  })

  it('should render back button', () => {
    const wrapper = mountFormCreate()
    const buttons = wrapper.findAll('.button-stub')
    expect(buttons[0]!.text()).toContain('Zurück')
  })

  it('should render form fields: title, description, type, scope', () => {
    const wrapper = mountFormCreate()
    expect(wrapper.findAll('.input-stub').length).toBeGreaterThan(0)
    expect(wrapper.find('.textarea-stub').exists()).toBe(true)
    expect(wrapper.findAll('.select-stub').length).toBeGreaterThanOrEqual(2)
  })

  it('should render deadline datepicker', () => {
    const wrapper = mountFormCreate()
    expect(wrapper.find('.datepicker-stub').exists()).toBe(true)
  })

  it('should render anonymous checkbox', () => {
    const wrapper = mountFormCreate()
    expect(wrapper.find('.checkbox-stub').exists()).toBe(true)
  })

  it('should render divider', () => {
    const wrapper = mountFormCreate()
    expect(wrapper.find('.divider-stub').exists()).toBe(true)
  })

  it('should render questions section with add button', () => {
    const wrapper = mountFormCreate()
    expect(wrapper.find('.questions-section').exists()).toBe(true)
    const buttons = wrapper.findAll('.button-stub')
    const addBtn = buttons.find(b => b.text().includes('Frage hinzufügen'))
    expect(addBtn).toBeTruthy()
  })

  it('should show empty state when no questions', () => {
    const wrapper = mountFormCreate()
    expect(wrapper.find('.empty-stub').text()).toContain('Noch keine Fragen')
  })

  it('should render save draft and publish buttons', () => {
    const wrapper = mountFormCreate()
    const buttons = wrapper.findAll('.button-stub')
    const texts = buttons.map(b => b.text())
    expect(texts).toContain('Entwurf speichern')
    expect(texts).toContain('Veröffentlichen')
  })

  it('should call fetchMyRooms on mount', async () => {
    const { roomsApi } = await import('@/api/rooms.api')
    mountFormCreate()
    await vi.waitFor(() => {
      expect(roomsApi.getMine).toHaveBeenCalled()
    })
  })

  it('should render in edit mode when route name is form-edit', async () => {
    const { useRoute } = await import('vue-router')
    vi.mocked(useRoute).mockReturnValue({
      name: 'form-edit',
      params: { id: 'f-1' },
      query: {},
    } as any)

    const pinia = createPinia()
    const wrapper = mount(FormCreateView, {
      global: { plugins: [i18n, pinia], stubs },
    })
    expect(wrapper.find('.page-title-stub').text()).toContain('Formular bearbeiten')
  })
})
