import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useParentLetterStore } from '@/stores/parentletter'

vi.mock('@/api/parentletter.api', () => ({
  parentLetterApi: {
    getMyLetters: vi.fn(),
    getInbox: vi.fn(),
    getLetter: vi.fn(),
    createLetter: vi.fn(),
    updateLetter: vi.fn(),
    sendLetter: vi.fn(),
    closeLetter: vi.fn(),
    deleteLetter: vi.fn(),
    confirmLetter: vi.fn(),
    markAsRead: vi.fn(),
    getConfig: vi.fn(),
    updateConfig: vi.fn(),
    uploadLetterhead: vi.fn(),
    deleteLetterhead: vi.fn(),
  },
}))

import { parentLetterApi } from '@/api/parentletter.api'

const mockLetterInfo = {
  id: 'letter-1',
  title: 'Wandertag',
  status: 'DRAFT' as const,
  roomId: 'room-1',
  roomName: 'Sonnengruppe',
  createdBy: 'user-1',
  creatorName: 'Maria Muster',
  sendDate: null,
  deadline: null,
  totalRecipients: 10,
  confirmedCount: 0,
  createdAt: '2025-03-01T10:00:00Z',
  updatedAt: '2025-03-01T10:00:00Z',
}

const mockLetterDetail = {
  ...mockLetterInfo,
  content: 'Liebe Eltern, ...',
  reminderDays: 3,
  reminderSent: false,
  recipients: [],
}

describe('ParentLetter Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should start with empty state', () => {
    const store = useParentLetterStore()
    expect(store.letters).toEqual([])
    expect(store.inbox).toEqual([])
    expect(store.currentLetter).toBeNull()
    expect(store.config).toBeNull()
    expect(store.loading).toBe(false)
    expect(store.total).toBe(0)
    expect(store.inboxTotal).toBe(0)
  })

  // ==================== fetchMyLetters ====================

  it('should fetch my letters and populate letters array and total', async () => {
    const store = useParentLetterStore()
    const mockLetters = [mockLetterInfo, { ...mockLetterInfo, id: 'letter-2', title: 'Schulausflug' }]

    vi.mocked(parentLetterApi.getMyLetters).mockResolvedValue({
      data: { data: { content: mockLetters, totalElements: 2, last: true } },
    } as any)

    await store.fetchMyLetters()

    expect(parentLetterApi.getMyLetters).toHaveBeenCalledWith(0, 20)
    expect(store.letters).toHaveLength(2)
    expect(store.total).toBe(2)
    expect(store.letters[0]!.title).toBe('Wandertag')
    expect(store.loading).toBe(false)
  })

  it('should pass page and size to getMyLetters', async () => {
    const store = useParentLetterStore()

    vi.mocked(parentLetterApi.getMyLetters).mockResolvedValue({
      data: { data: { content: [], totalElements: 0, last: true } },
    } as any)

    await store.fetchMyLetters(2, 10)

    expect(parentLetterApi.getMyLetters).toHaveBeenCalledWith(2, 10)
  })

  it('should set loading to false after fetchMyLetters even on error', async () => {
    const store = useParentLetterStore()
    vi.mocked(parentLetterApi.getMyLetters).mockRejectedValue(new Error('network'))

    await expect(store.fetchMyLetters()).rejects.toThrow()
    expect(store.loading).toBe(false)
  })

  // ==================== fetchInbox ====================

  it('should fetch inbox and populate inbox array and inboxTotal', async () => {
    const store = useParentLetterStore()
    const mockInbox = [{ ...mockLetterInfo, status: 'SENT' as const }]

    vi.mocked(parentLetterApi.getInbox).mockResolvedValue({
      data: { data: { content: mockInbox, totalElements: 1, last: true } },
    } as any)

    await store.fetchInbox()

    expect(parentLetterApi.getInbox).toHaveBeenCalledWith(0, 20)
    expect(store.inbox).toHaveLength(1)
    expect(store.inboxTotal).toBe(1)
    expect(store.inbox[0]!.status).toBe('SENT')
    expect(store.loading).toBe(false)
  })

  it('should pass page and size to getInbox', async () => {
    const store = useParentLetterStore()

    vi.mocked(parentLetterApi.getInbox).mockResolvedValue({
      data: { data: { content: [], totalElements: 0, last: true } },
    } as any)

    await store.fetchInbox(1, 5)

    expect(parentLetterApi.getInbox).toHaveBeenCalledWith(1, 5)
  })

  it('should set loading to false after fetchInbox even on error', async () => {
    const store = useParentLetterStore()
    vi.mocked(parentLetterApi.getInbox).mockRejectedValue(new Error('network'))

    await expect(store.fetchInbox()).rejects.toThrow()
    expect(store.loading).toBe(false)
  })

  // ==================== fetchLetter ====================

  it('should fetch a single letter and set currentLetter', async () => {
    const store = useParentLetterStore()

    vi.mocked(parentLetterApi.getLetter).mockResolvedValue({
      data: { data: mockLetterDetail },
    } as any)

    await store.fetchLetter('letter-1')

    expect(parentLetterApi.getLetter).toHaveBeenCalledWith('letter-1')
    expect(store.currentLetter).toBeTruthy()
    expect(store.currentLetter!.title).toBe('Wandertag')
    expect(store.currentLetter!.content).toBe('Liebe Eltern, ...')
    expect(store.loading).toBe(false)
  })

  it('should reset currentLetter to null before fetching', async () => {
    const store = useParentLetterStore()
    store.currentLetter = { ...mockLetterDetail } as any

    vi.mocked(parentLetterApi.getLetter).mockResolvedValue({
      data: { data: mockLetterDetail },
    } as any)

    // Spy on loading sequence: currentLetter should be null while loading
    let wasNullDuringLoad = false
    const originalGetLetter = vi.mocked(parentLetterApi.getLetter)
    originalGetLetter.mockImplementation(async () => {
      wasNullDuringLoad = store.currentLetter === null
      return { data: { data: mockLetterDetail } } as any
    })

    await store.fetchLetter('letter-1')

    expect(wasNullDuringLoad).toBe(true)
  })

  it('should set loading to false after fetchLetter even on error', async () => {
    const store = useParentLetterStore()
    vi.mocked(parentLetterApi.getLetter).mockRejectedValue(new Error('not found'))

    await expect(store.fetchLetter('letter-x')).rejects.toThrow()
    expect(store.loading).toBe(false)
  })

  // ==================== createLetter ====================

  it('should create a letter, set currentLetter, and prepend to letters array', async () => {
    const store = useParentLetterStore()
    store.letters = [{ ...mockLetterInfo, id: 'letter-existing' }] as any

    const newDetail = { ...mockLetterDetail, id: 'letter-new', title: 'Neuer Brief' }
    vi.mocked(parentLetterApi.createLetter).mockResolvedValue({
      data: { data: newDetail },
    } as any)

    const result = await store.createLetter({
      roomId: 'room-1',
      title: 'Neuer Brief',
      content: 'Inhalt...',
    })

    expect(result.id).toBe('letter-new')
    expect(store.currentLetter!.id).toBe('letter-new')
    expect(store.letters).toHaveLength(2)
    expect(store.letters[0]!.id).toBe('letter-new')
    expect(store.letters[1]!.id).toBe('letter-existing')
  })

  it('should call createLetter API with the provided data', async () => {
    const store = useParentLetterStore()
    const createData = {
      roomId: 'room-2',
      title: 'Ausflug',
      content: 'Liebe Eltern...',
      deadline: '2025-06-01',
    }

    vi.mocked(parentLetterApi.createLetter).mockResolvedValue({
      data: { data: { ...mockLetterDetail, ...createData } },
    } as any)

    await store.createLetter(createData)

    expect(parentLetterApi.createLetter).toHaveBeenCalledWith(createData)
  })

  // ==================== sendLetter ====================

  it('should send a letter and update currentLetter status', async () => {
    const store = useParentLetterStore()
    const sentDetail = { ...mockLetterDetail, status: 'SENT' as const }

    store.currentLetter = { ...mockLetterDetail } as any
    store.letters = [{ ...mockLetterInfo }] as any

    vi.mocked(parentLetterApi.sendLetter).mockResolvedValue({
      data: { data: sentDetail },
    } as any)

    const result = await store.sendLetter('letter-1')

    expect(parentLetterApi.sendLetter).toHaveBeenCalledWith('letter-1')
    expect(store.currentLetter!.status).toBe('SENT')
    expect(result.status).toBe('SENT')
  })

  it('should sync the letters list item after sendLetter', async () => {
    const store = useParentLetterStore()
    const sentDetail = { ...mockLetterDetail, id: 'letter-1', status: 'SENT' as const }

    store.letters = [{ ...mockLetterInfo, id: 'letter-1', status: 'DRAFT' as const }] as any

    vi.mocked(parentLetterApi.sendLetter).mockResolvedValue({
      data: { data: sentDetail },
    } as any)

    await store.sendLetter('letter-1')

    expect(store.letters[0]!.status).toBe('SENT')
  })

  // ==================== deleteLetter ====================

  it('should delete a letter and remove it from the letters array', async () => {
    const store = useParentLetterStore()
    store.letters = [
      { ...mockLetterInfo, id: 'letter-1' },
      { ...mockLetterInfo, id: 'letter-2', title: 'Zweiter Brief' },
    ] as any

    vi.mocked(parentLetterApi.deleteLetter).mockResolvedValue({} as any)

    await store.deleteLetter('letter-1')

    expect(parentLetterApi.deleteLetter).toHaveBeenCalledWith('letter-1')
    expect(store.letters).toHaveLength(1)
    expect(store.letters[0]!.id).toBe('letter-2')
  })

  it('should clear currentLetter if the deleted letter is currently open', async () => {
    const store = useParentLetterStore()
    store.currentLetter = { ...mockLetterDetail, id: 'letter-1' } as any
    store.letters = [{ ...mockLetterInfo, id: 'letter-1' }] as any

    vi.mocked(parentLetterApi.deleteLetter).mockResolvedValue({} as any)

    await store.deleteLetter('letter-1')

    expect(store.currentLetter).toBeNull()
  })

  it('should not clear currentLetter if a different letter is deleted', async () => {
    const store = useParentLetterStore()
    store.currentLetter = { ...mockLetterDetail, id: 'letter-1' } as any
    store.letters = [
      { ...mockLetterInfo, id: 'letter-1' },
      { ...mockLetterInfo, id: 'letter-2' },
    ] as any

    vi.mocked(parentLetterApi.deleteLetter).mockResolvedValue({} as any)

    await store.deleteLetter('letter-2')

    expect(store.currentLetter!.id).toBe('letter-1')
  })

  // ==================== confirmLetter ====================

  it('should call confirmLetter API successfully', async () => {
    const store = useParentLetterStore()

    vi.mocked(parentLetterApi.confirmLetter).mockResolvedValue({} as any)
    vi.mocked(parentLetterApi.getLetter).mockResolvedValue({
      data: { data: mockLetterDetail },
    } as any)

    await store.confirmLetter('letter-1', 'student-1')

    expect(parentLetterApi.confirmLetter).toHaveBeenCalledWith('letter-1', 'student-1')
  })

  it('should refresh currentLetter after confirm if currently viewing that letter', async () => {
    const store = useParentLetterStore()
    store.currentLetter = { ...mockLetterDetail, id: 'letter-1' } as any

    const refreshedDetail = { ...mockLetterDetail, id: 'letter-1', confirmedCount: 1 }
    vi.mocked(parentLetterApi.confirmLetter).mockResolvedValue({} as any)
    vi.mocked(parentLetterApi.getLetter).mockResolvedValue({
      data: { data: refreshedDetail },
    } as any)

    await store.confirmLetter('letter-1', 'student-1')

    expect(parentLetterApi.getLetter).toHaveBeenCalledWith('letter-1')
    expect(store.currentLetter!.confirmedCount).toBe(1)
  })

  it('should not refresh currentLetter after confirm if viewing a different letter', async () => {
    const store = useParentLetterStore()
    store.currentLetter = { ...mockLetterDetail, id: 'letter-2' } as any

    vi.mocked(parentLetterApi.confirmLetter).mockResolvedValue({} as any)

    await store.confirmLetter('letter-1', 'student-1')

    expect(parentLetterApi.getLetter).not.toHaveBeenCalled()
  })

  it('should update confirmedCount in inbox after confirm', async () => {
    const store = useParentLetterStore()
    store.currentLetter = null
    store.inbox = [{ ...mockLetterInfo, id: 'letter-1', confirmedCount: 0 }] as any

    vi.mocked(parentLetterApi.confirmLetter).mockResolvedValue({} as any)

    await store.confirmLetter('letter-1', 'student-1')

    expect(store.inbox[0]!.confirmedCount).toBe(1)
  })

  // ==================== markAsRead ====================

  it('should call markAsRead API successfully', async () => {
    const store = useParentLetterStore()

    vi.mocked(parentLetterApi.markAsRead).mockResolvedValue({} as any)

    await store.markAsRead('letter-1')

    expect(parentLetterApi.markAsRead).toHaveBeenCalledWith('letter-1')
  })

  // ==================== fetchConfig ====================

  it('should fetch config and set config state', async () => {
    const store = useParentLetterStore()
    const mockConfig = {
      id: 'cfg-1',
      sectionId: 'sec-1',
      sectionName: 'Grundstufe',
      letterheadPath: null,
      signatureTemplate: 'Mit freundlichen Grüßen',
      reminderDays: 3,
    }

    vi.mocked(parentLetterApi.getConfig).mockResolvedValue({
      data: { data: mockConfig },
    } as any)

    await store.fetchConfig('sec-1')

    expect(parentLetterApi.getConfig).toHaveBeenCalledWith('sec-1')
    expect(store.config).toEqual(mockConfig)
  })

  it('should set config to null when fetchConfig fails', async () => {
    const store = useParentLetterStore()
    vi.mocked(parentLetterApi.getConfig).mockRejectedValue(new Error('not found'))

    await store.fetchConfig()

    expect(store.config).toBeNull()
  })
})
