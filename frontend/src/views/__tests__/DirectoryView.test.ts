import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'

// --- mock auth store
const mockAuth = {
  user: { id: 'current-user-id', role: 'TEACHER' },
  isAdmin: false,
}

vi.mock('@/stores/auth', () => ({
  useAuthStore: vi.fn(() => mockAuth),
}))

// --- mock admin store
const mockAdmin = {
  isModuleEnabled: vi.fn((m: string) => m === 'messaging'),
}

vi.mock('@/stores/admin', () => ({
  useAdminStore: vi.fn(() => mockAdmin),
}))

// --- mock messaging store
const mockMessaging = {
  startDirectConversation: vi.fn().mockResolvedValue({ id: 'conv-1' }),
}

vi.mock('@/stores/messaging', () => ({
  useMessagingStore: vi.fn(() => mockMessaging),
}))

// --- mock users API
vi.mock('@/api/users.api', () => ({
  usersApi: {
    directory: vi.fn().mockResolvedValue({
      data: {
        data: {
          content: [
            {
              id: 'user-1',
              email: 'anna@example.com',
              firstName: 'Anna',
              lastName: 'Mueller',
              displayName: 'Anna Mueller',
              phone: null,
              avatarUrl: null,
              role: 'TEACHER',
              specialRoles: [],
              assignedRoles: [],
              active: true,
            },
            {
              id: 'user-2',
              email: 'max@example.com',
              firstName: 'Max',
              lastName: 'Schmidt',
              displayName: 'Max Schmidt',
              phone: null,
              avatarUrl: 'https://example.com/avatar.jpg',
              role: 'PARENT',
              specialRoles: [],
              assignedRoles: [],
              active: true,
            },
          ],
          totalElements: 2,
          totalPages: 1,
          page: 0,
          size: 18,
          last: true,
        },
      },
    }),
  },
}))

// --- mock sections API
vi.mock('@/api/sections.api', () => ({
  sectionsApi: {
    getAll: vi.fn().mockResolvedValue({
      data: {
        data: [
          { id: 'sec-1', name: 'Grundstufe', slug: 'grundstufe', description: null, sortOrder: 1, active: true },
          { id: 'sec-2', name: 'Mittelstufe', slug: 'mittelstufe', description: null, sortOrder: 2, active: true },
        ],
      },
    }),
  },
}))

// --- mock rooms API
vi.mock('@/api/rooms.api', () => ({
  roomsApi: {
    getMine: vi.fn().mockResolvedValue({
      data: {
        data: [
          { id: 'room-1', name: 'Sonnengruppe', sectionId: 'sec-1', type: 'KLASSE', archived: false, memberCount: 10, joinPolicy: 'INVITE_ONLY', tags: [], description: null, publicDescription: null, avatarUrl: null, expiresAt: null },
          { id: 'room-2', name: 'Mondgruppe', sectionId: 'sec-2', type: 'KLASSE', archived: false, memberCount: 8, joinPolicy: 'INVITE_ONLY', tags: [], description: null, publicDescription: null, avatarUrl: null, expiresAt: null },
        ],
      },
    }),
  },
}))

// --- mock family API
vi.mock('@/api/family.api', () => ({
  familyApi: {
    getAll: vi.fn().mockResolvedValue({
      data: {
        data: [],
      },
    }),
  },
}))

// --- mock router
const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: mockPush })),
  useRoute: vi.fn(() => ({ name: 'directory', path: '/directory', params: {}, query: {} })),
}))

import DirectoryView from '@/views/DirectoryView.vue'
import { usersApi } from '@/api/users.api'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      directory: {
        title: 'Verzeichnis',
        tabUsers: 'Benutzer',
        tabFamilies: 'Familien',
        search: 'Name oder E-Mail suchen...',
        searchFamilies: 'Familie oder Mitglied suchen...',
        filterRole: 'Alle Rollen',
        filterSection: 'Alle Bereiche',
        filterRoom: 'Alle Räume',
        noResults: 'Keine Benutzer gefunden',
        noFamilies: 'Keine Familien gefunden',
        showingCount: '{count} Benutzer',
        startChat: 'Nachricht senden',
        memberCount: '{count} Mitglieder',
        sendMessage: 'Nachricht senden',
        roles: {
          SUPERADMIN: 'Superadmin',
          SECTION_ADMIN: 'Bereichsadmin',
          TEACHER: 'Pädagoge',
          PARENT: 'Elternteil',
          STUDENT: 'Schüler/in',
        },
      },
      common: {
        loading: 'Laden...',
        loadingTimeout: 'Dauert länger...',
        retry: 'Erneut',
      },
    },
  },
})

function mountView() {
  return mount(DirectoryView, {
    global: {
      plugins: [i18n],
      stubs: {
        PageTitle: { template: '<div class="page-title-stub"><slot /></div>', props: ['title'] },
        LoadingSpinner: { template: '<div class="loading-stub" />' },
        EmptyState: { template: '<div class="empty-state-stub" />' },
        InputText: { template: '<input class="input-stub" />' },
        Select: { template: '<div class="select-stub" />' },
        Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity'] },
        Paginator: { template: '<div class="paginator-stub" />' },
        Avatar: { template: '<span class="avatar-stub" />' },
        Tabs: { template: '<div class="tabs-stub"><slot /></div>', props: ['value'] },
        TabList: { template: '<div class="tablist-stub"><slot /></div>' },
        Tab: { template: '<div class="tab-stub"><slot /></div>', props: ['value'] },
        TabPanels: { template: '<div class="tabpanels-stub"><slot /></div>' },
        TabPanel: { template: '<div class="tabpanel-stub"><slot /></div>', props: ['value'] },
      },
    },
  })
}

describe('DirectoryView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(usersApi.directory).mockClear()
    mockPush.mockClear()
    mockMessaging.startDirectConversation.mockClear()
    mockAdmin.isModuleEnabled.mockImplementation((m: string) => m === 'messaging')
    mockAuth.user = { id: 'current-user-id', role: 'TEACHER' }
  })

  it('renders page title', async () => {
    const w = mountView()
    await flushPromises()
    expect(w.find('.page-title-stub').exists()).toBe(true)
  })

  it('calls usersApi.directory on mount', async () => {
    mountView()
    await flushPromises()
    expect(usersApi.directory).toHaveBeenCalledWith({
      page: 0,
      size: 18,
    })
  })

  it('renders user cards from API response', async () => {
    const w = mountView()
    await flushPromises()
    const cards = w.findAll('.user-card')
    expect(cards).toHaveLength(2)
  })

  it('displays user names', async () => {
    const w = mountView()
    await flushPromises()
    const names = w.findAll('.user-name')
    expect(names[0].text()).toBe('Anna Mueller')
    expect(names[1].text()).toBe('Max Schmidt')
  })

  it('displays user emails', async () => {
    const w = mountView()
    await flushPromises()
    const emails = w.findAll('.user-email')
    expect(emails[0].text()).toBe('anna@example.com')
    expect(emails[1].text()).toBe('max@example.com')
  })

  it('shows role tags', async () => {
    const w = mountView()
    await flushPromises()
    const tags = w.findAll('.tag-stub')
    expect(tags.length).toBeGreaterThanOrEqual(2)
  })

  it('shows showing count', async () => {
    const w = mountView()
    await flushPromises()
    expect(w.text()).toContain('2 Benutzer')
  })

  it('shows empty state when no users', async () => {
    vi.mocked(usersApi.directory).mockResolvedValueOnce({
      data: { data: { content: [], totalElements: 0, totalPages: 0, page: 0, size: 18, last: true }, message: null, success: true, timestamp: '' },
    } as any)
    const w = mountView()
    await flushPromises()
    expect(w.find('.empty-state').exists()).toBe(true)
    expect(w.text()).toContain('Keine Benutzer gefunden')
  })

  it('navigates to user profile on card click', async () => {
    const w = mountView()
    await flushPromises()
    const cards = w.findAll('.user-card')
    await cards[0].trigger('click')
    expect(mockPush).toHaveBeenCalledWith({ name: 'user-profile', params: { userId: 'user-1' } })
  })

  it('navigates to user profile on any card click', async () => {
    const w = mountView()
    await flushPromises()
    const cards = w.findAll('.user-card')
    await cards[1].trigger('click')
    expect(mockPush).toHaveBeenCalledWith({ name: 'user-profile', params: { userId: 'user-2' } })
  })

  it('navigates to user profile even when messaging disabled', async () => {
    mockAdmin.isModuleEnabled.mockReturnValue(false)
    const w = mountView()
    await flushPromises()
    const cards = w.findAll('.user-card')
    await cards[0].trigger('click')
    expect(mockPush).toHaveBeenCalledWith({ name: 'user-profile', params: { userId: 'user-1' } })
  })

  it('all cards have clickable class', async () => {
    const w = mountView()
    await flushPromises()
    const cards = w.findAll('.user-card')
    expect(cards[0].classes()).toContain('clickable')
    expect(cards[1].classes()).toContain('clickable')
  })

  it('renders filter bar with search and selects', async () => {
    const w = mountView()
    await flushPromises()
    expect(w.find('.filter-bar').exists()).toBe(true)
    expect(w.find('.search-wrapper').exists()).toBe(true)
    expect(w.find('.filter-selects').exists()).toBe(true)
  })

  it('renders chat button for other users when messaging enabled', async () => {
    const w = mountView()
    await flushPromises()
    const chatBtns = w.findAll('.chat-btn')
    expect(chatBtns.length).toBe(2)
  })

  it('does not render chat buttons when messaging disabled', async () => {
    mockAdmin.isModuleEnabled.mockReturnValue(false)
    const w = mountView()
    await flushPromises()
    const chatBtns = w.findAll('.chat-btn')
    expect(chatBtns.length).toBe(0)
  })
})
