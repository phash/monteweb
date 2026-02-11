import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useFormsStore } from '@/stores/forms'

vi.mock('@/api/forms.api', () => ({
  formsApi: {
    getAvailableForms: vi.fn(),
    getMyForms: vi.fn(),
    getForm: vi.fn(),
    createForm: vi.fn(),
    updateForm: vi.fn(),
    deleteForm: vi.fn(),
    publishForm: vi.fn(),
    closeForm: vi.fn(),
    submitResponse: vi.fn(),
    getResults: vi.fn(),
    getIndividualResponses: vi.fn(),
    exportCsv: vi.fn(),
    exportPdf: vi.fn(),
  },
}))

import { formsApi } from '@/api/forms.api'

describe('Forms Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should start with empty state', () => {
    const store = useFormsStore()
    expect(store.availableForms).toEqual([])
    expect(store.myForms).toEqual([])
    expect(store.currentForm).toBeNull()
    expect(store.currentResults).toBeNull()
    expect(store.loading).toBe(false)
  })

  it('should fetch available forms', async () => {
    const store = useFormsStore()
    const mockForms = [
      { id: '1', title: 'Survey 1', type: 'SURVEY' },
      { id: '2', title: 'Consent 1', type: 'CONSENT' },
    ]

    vi.mocked(formsApi.getAvailableForms).mockResolvedValue({
      data: { data: { content: mockForms, totalElements: 2, last: true } },
    } as any)

    await store.fetchAvailableForms()

    expect(store.availableForms).toHaveLength(2)
    expect(store.totalAvailable).toBe(2)
    expect(store.hasMore).toBe(false)
  })

  it('should fetch my forms', async () => {
    const store = useFormsStore()
    const mockForms = [{ id: '1', title: 'My Form', status: 'DRAFT' }]

    vi.mocked(formsApi.getMyForms).mockResolvedValue({
      data: { data: { content: mockForms, totalElements: 1, last: true } },
    } as any)

    await store.fetchMyForms()

    expect(store.myForms).toHaveLength(1)
    expect(store.totalMine).toBe(1)
  })

  it('should fetch form detail', async () => {
    const store = useFormsStore()
    const mockDetail = {
      form: { id: '1', title: 'Survey', status: 'PUBLISHED' },
      questions: [{ id: 'q1', type: 'TEXT', label: 'Question 1' }],
    }

    vi.mocked(formsApi.getForm).mockResolvedValue({
      data: { data: mockDetail },
    } as any)

    await store.fetchForm('1')

    expect(store.currentForm).toBeTruthy()
    expect(store.currentForm?.form.title).toBe('Survey')
    expect(store.currentForm?.questions).toHaveLength(1)
  })

  it('should create form and prepend to myForms', async () => {
    const store = useFormsStore()
    const created = {
      form: { id: 'new', title: 'New Form', status: 'DRAFT' },
      questions: [],
    }

    vi.mocked(formsApi.createForm).mockResolvedValue({
      data: { data: created },
    } as any)

    const result = await store.createForm({
      title: 'New Form',
      type: 'SURVEY',
      scope: 'ROOM',
      anonymous: false,
      questions: [],
    } as any)

    expect(result.form.id).toBe('new')
    expect(store.myForms[0].id).toBe('new')
  })

  it('should publish form and update status', async () => {
    const store = useFormsStore()
    store.myForms = [{ id: '1', status: 'DRAFT' }] as any
    store.currentForm = { form: { id: '1', status: 'DRAFT' }, questions: [] } as any

    const published = { id: '1', status: 'PUBLISHED' }
    vi.mocked(formsApi.publishForm).mockResolvedValue({
      data: { data: published },
    } as any)

    await store.publishForm('1')

    expect(store.myForms[0].status).toBe('PUBLISHED')
    expect(store.currentForm?.form.status).toBe('PUBLISHED')
  })

  it('should close form and update status', async () => {
    const store = useFormsStore()
    store.myForms = [{ id: '1', status: 'PUBLISHED' }] as any

    const closed = { id: '1', status: 'CLOSED' }
    vi.mocked(formsApi.closeForm).mockResolvedValue({
      data: { data: closed },
    } as any)

    await store.closeForm('1')

    expect(store.myForms[0].status).toBe('CLOSED')
  })

  it('should delete form and remove from list', async () => {
    const store = useFormsStore()
    store.myForms = [{ id: '1' }, { id: '2' }] as any

    vi.mocked(formsApi.deleteForm).mockResolvedValue({} as any)

    await store.deleteForm('1')

    expect(store.myForms).toHaveLength(1)
    expect(store.myForms[0].id).toBe('2')
  })

  it('should submit response and update hasUserResponded', async () => {
    const store = useFormsStore()
    store.availableForms = [{ id: '1', hasUserResponded: false, responseCount: 5 }] as any
    store.currentForm = { form: { id: '1', hasUserResponded: false, responseCount: 5 }, questions: [] } as any

    vi.mocked(formsApi.submitResponse).mockResolvedValue({} as any)

    await store.submitResponse('1', { answers: [] })

    expect(store.availableForms[0].hasUserResponded).toBe(true)
    expect(store.availableForms[0].responseCount).toBe(6)
    expect(store.currentForm?.form.hasUserResponded).toBe(true)
  })

  it('should fetch results', async () => {
    const store = useFormsStore()
    const mockResults = {
      form: { id: '1', title: 'Survey', responseCount: 10 },
      results: [
        { questionId: 'q1', label: 'Q1', type: 'TEXT', totalAnswers: 10 },
      ],
    }

    vi.mocked(formsApi.getResults).mockResolvedValue({
      data: { data: mockResults },
    } as any)

    await store.fetchResults('1')

    expect(store.currentResults).toBeTruthy()
    expect(store.currentResults?.results).toHaveLength(1)
    expect(store.currentResults?.form.responseCount).toBe(10)
  })

  it('should fetch individual responses', async () => {
    const store = useFormsStore()
    const mockResponses = [
      { responseId: 'r1', userName: 'User 1', answers: [] },
    ]

    vi.mocked(formsApi.getIndividualResponses).mockResolvedValue({
      data: { data: mockResponses },
    } as any)

    await store.fetchIndividualResponses('1')

    expect(store.individualResponses).toHaveLength(1)
    expect(store.individualResponses[0].userName).toBe('User 1')
  })
})
