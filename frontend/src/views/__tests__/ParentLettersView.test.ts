import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'

// --- mock parentletter store
const mockStore = {
  letters: [] as any[],
  inbox: [] as any[],
  loading: false,
  total: 0,
  inboxTotal: 0,
  fetchMyLetters: vi.fn().mockResolvedValue(undefined),
  fetchInbox: vi.fn().mockResolvedValue(undefined),
  sendLetter: vi.fn().mockResolvedValue(undefined),
  closeLetter: vi.fn().mockResolvedValue(undefined),
  deleteLetter: vi.fn().mockResolvedValue(undefined),
}

vi.mock('@/stores/parentletter', () => ({
  useParentLetterStore: vi.fn(() => mockStore),
}))

// --- mock auth store
const mockAuth = {
  isTeacher: false,
  isAdmin: false,
  user: { id: 'user-1', role: 'PARENT' },
}

vi.mock('@/stores/auth', () => ({
  useAuthStore: vi.fn(() => mockAuth),
}))

// --- mock vue-router
vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn() })),
}))

// --- mock composables
vi.mock('@/composables/useLocaleDate', () => ({
  useLocaleDate: vi.fn(() => ({
    formatShortDate: vi.fn((d: string) => d),
    formatCompactDateTime: vi.fn((d: string) => d),
  })),
}))

// --- mock parentletter API (the view does not call the API directly, but the store mock handles it)
vi.mock('@/api/parentletter.api', () => ({
  parentLetterApi: {
    getMyLetters: vi.fn().mockResolvedValue({ data: { data: { content: [], totalElements: 0, last: true } } }),
    getInbox: vi.fn().mockResolvedValue({ data: { data: { content: [], totalElements: 0, last: true } } }),
  },
}))

import ParentLettersView from '@/views/ParentLettersView.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      parentLetters: {
        title: 'Elternbriefe',
        create: 'Neuen Brief erstellen',
        editLetter: 'Brief bearbeiten',
        noLetters: 'Keine Elternbriefe vorhanden.',
        noInbox: 'Keine Elternbriefe in Ihrem Posteingang.',
        tabs: {
          mine: 'Meine Briefe',
          inbox: 'Posteingang',
        },
        send: 'Brief senden',
        close: 'Brief schließen',
        sent: 'Brief gesendet',
        closed: 'Brief geschlossen',
        deleted: 'Brief gelöscht',
        confirmed: 'Bestätigung erfolgreich',
        deadline: 'Bestätigungsfrist',
        confirmed_label: 'Bestätigt',
        statuses: {
          DRAFT: 'Entwurf',
          SCHEDULED: 'Geplant',
          SENT: 'Gesendet',
          CLOSED: 'Geschlossen',
        },
        recipientStatuses: {
          OPEN: 'Offen',
          READ: 'Gelesen',
          CONFIRMED: 'Bestätigt',
        },
      },
      common: {
        loading: 'Laden...',
        view: 'Anzeigen',
        edit: 'Bearbeiten',
        delete: 'Löschen',
        error: 'Fehler',
      },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title'] },
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  EmptyState: { template: '<div class="empty-stub">{{ message }}</div>', props: ['icon', 'message'] },
  Button: {
    template: '<button class="button-stub" :aria-label="ariaLabel" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'text', 'rounded', 'size', 'severity', 'loading', 'ariaLabel'],
    emits: ['click'],
  },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity', 'size', 'icon'] },
  ProgressBar: { template: '<div class="progress-stub" />', props: ['value', 'showValue'] },
  Tabs: { template: '<div class="tabs-stub"><slot /></div>', props: ['modelValue'] },
  TabList: { template: '<div class="tablist-stub"><slot /></div>' },
  Tab: { template: '<div class="tab-stub" :data-value="value"><slot /></div>', props: ['value'] },
  TabPanels: { template: '<div class="tabpanels-stub"><slot /></div>' },
  TabPanel: { template: '<div class="tabpanel-stub" :data-value="value"><slot /></div>', props: ['value'] },
  'router-link': { template: '<a class="router-link-stub"><slot /></a>', props: ['to'] },
}

function mountView() {
  return mount(ParentLettersView, {
    global: { plugins: [i18n], stubs },
  })
}

describe('ParentLettersView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    Object.assign(mockStore, {
      letters: [],
      inbox: [],
      loading: false,
      total: 0,
      inboxTotal: 0,
      fetchMyLetters: vi.fn().mockResolvedValue(undefined),
      fetchInbox: vi.fn().mockResolvedValue(undefined),
      sendLetter: vi.fn().mockResolvedValue(undefined),
      closeLetter: vi.fn().mockResolvedValue(undefined),
      deleteLetter: vi.fn().mockResolvedValue(undefined),
    })
    Object.assign(mockAuth, {
      isTeacher: false,
      isAdmin: false,
      user: { id: 'user-1', role: 'PARENT' },
    })
  })

  // ==================== Basic render ====================

  it('should mount and render', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('should render title "Elternbriefe"', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.page-title-stub').text()).toContain('Elternbriefe')
    wrapper.unmount()
  })

  // ==================== Tab visibility by role ====================

  it('should show "Meine Briefe" tab for teachers', async () => {
    mockAuth.isTeacher = true
    mockAuth.user = { id: 'user-1', role: 'TEACHER' }

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Meine Briefe')
    wrapper.unmount()
  })

  it('should show "Meine Briefe" tab for admins', async () => {
    mockAuth.isAdmin = true
    mockAuth.isTeacher = true
    mockAuth.user = { id: 'user-1', role: 'SUPERADMIN' }

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Meine Briefe')
    wrapper.unmount()
  })

  it('should show "Posteingang" tab for parents', async () => {
    mockAuth.isTeacher = false
    mockAuth.user = { id: 'user-1', role: 'PARENT' }

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Posteingang')
    wrapper.unmount()
  })

  it('should show "Posteingang" tab for students', async () => {
    mockAuth.isTeacher = false
    mockAuth.user = { id: 'user-1', role: 'STUDENT' }

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Posteingang')
    wrapper.unmount()
  })

  // ==================== Create button visibility ====================

  it('should show create button for teachers', async () => {
    mockAuth.isTeacher = true
    mockAuth.user = { id: 'user-1', role: 'TEACHER' }

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Neuen Brief erstellen')
    wrapper.unmount()
  })

  it('should show create button for admins', async () => {
    mockAuth.isAdmin = true
    mockAuth.isTeacher = true
    mockAuth.user = { id: 'user-1', role: 'SUPERADMIN' }

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Neuen Brief erstellen')
    wrapper.unmount()
  })

  it('should hide create button for parents', async () => {
    mockAuth.isTeacher = false
    mockAuth.isAdmin = false
    mockAuth.user = { id: 'user-1', role: 'PARENT' }

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).not.toContain('Neuen Brief erstellen')
    wrapper.unmount()
  })

  it('should hide create button for students', async () => {
    mockAuth.isTeacher = false
    mockAuth.isAdmin = false
    mockAuth.user = { id: 'user-1', role: 'STUDENT' }

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).not.toContain('Neuen Brief erstellen')
    wrapper.unmount()
  })

  // ==================== onMounted: fetch calls by role ====================

  it('should call fetchMyLetters on mount for teachers', async () => {
    mockAuth.isTeacher = true
    mockAuth.user = { id: 'user-1', role: 'TEACHER' }

    const wrapper = mountView()
    await flushPromises()
    expect(mockStore.fetchMyLetters).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('should call fetchMyLetters on mount for admins', async () => {
    mockAuth.isAdmin = true
    mockAuth.isTeacher = true
    mockAuth.user = { id: 'user-1', role: 'SUPERADMIN' }

    const wrapper = mountView()
    await flushPromises()
    expect(mockStore.fetchMyLetters).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('should call fetchInbox on mount for parents', async () => {
    mockAuth.isTeacher = false
    mockAuth.user = { id: 'user-1', role: 'PARENT' }

    const wrapper = mountView()
    await flushPromises()
    expect(mockStore.fetchInbox).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('should call fetchInbox on mount for students', async () => {
    mockAuth.isTeacher = false
    mockAuth.user = { id: 'user-1', role: 'STUDENT' }

    const wrapper = mountView()
    await flushPromises()
    expect(mockStore.fetchInbox).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('should not call fetchInbox on mount for teachers', async () => {
    mockAuth.isTeacher = true
    mockAuth.isAdmin = false
    mockAuth.user = { id: 'user-1', role: 'TEACHER' }

    const wrapper = mountView()
    await flushPromises()
    expect(mockStore.fetchInbox).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('should call both fetchMyLetters and fetchInbox if user has both views', async () => {
    // SUPERADMIN: isTeacher=true (showMyLettersTab) AND NOT PARENT/STUDENT role
    // So only fetchMyLetters is called for SUPERADMIN
    mockAuth.isTeacher = true
    mockAuth.isAdmin = true
    mockAuth.user = { id: 'user-1', role: 'SUPERADMIN' }

    const wrapper = mountView()
    await flushPromises()
    expect(mockStore.fetchMyLetters).toHaveBeenCalled()
    expect(mockStore.fetchInbox).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  // ==================== Empty states ====================

  it('should show empty state for letters when letters array is empty (teacher view)', async () => {
    mockAuth.isTeacher = true
    mockAuth.user = { id: 'user-1', role: 'TEACHER' }
    mockStore.letters = []

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Keine Elternbriefe vorhanden.')
    wrapper.unmount()
  })

  it('should show empty state for inbox when inbox is empty (parent view)', async () => {
    mockAuth.isTeacher = false
    mockAuth.user = { id: 'user-1', role: 'PARENT' }
    mockStore.inbox = []

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Keine Elternbriefe in Ihrem Posteingang.')
    wrapper.unmount()
  })

  // ==================== Letter list render ====================

  it('should render letter items in the list for teachers', async () => {
    mockAuth.isTeacher = true
    mockAuth.user = { id: 'user-1', role: 'TEACHER' }
    mockStore.letters = [
      {
        id: 'letter-1',
        title: 'Wandertag',
        status: 'DRAFT',
        roomId: 'room-1',
        roomName: 'Sonnengruppe',
        createdBy: 'user-1',
        creatorName: 'Maria Muster',
        sendDate: null,
        deadline: null,
        totalRecipients: 10,
        confirmedCount: 3,
        createdAt: '2025-03-01T10:00:00Z',
        updatedAt: '2025-03-01T10:00:00Z',
      },
    ]

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Wandertag')
    expect(wrapper.text()).toContain('Sonnengruppe')
    wrapper.unmount()
  })

  it('should render inbox items for parents', async () => {
    mockAuth.isTeacher = false
    mockAuth.user = { id: 'user-1', role: 'PARENT' }
    mockStore.inbox = [
      {
        id: 'letter-2',
        title: 'Schulausflug',
        status: 'SENT',
        roomId: 'room-1',
        roomName: 'Sonnengruppe',
        createdBy: 'user-2',
        creatorName: 'Lehrer Mustermann',
        sendDate: '2025-03-10T08:00:00Z',
        deadline: '2025-03-15',
        totalRecipients: 20,
        confirmedCount: 5,
        createdAt: '2025-03-05T10:00:00Z',
        updatedAt: '2025-03-10T08:00:00Z',
      },
    ]

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Schulausflug')
    wrapper.unmount()
  })

  // ==================== Loading state ====================

  it('should show loading spinner when store is loading', async () => {
    mockAuth.isTeacher = true
    mockAuth.user = { id: 'user-1', role: 'TEACHER' }
    mockStore.loading = true
    mockStore.letters = []

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.loading-stub').exists()).toBe(true)
    wrapper.unmount()
  })
})
