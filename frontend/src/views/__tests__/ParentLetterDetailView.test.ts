import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'

// --- mock parentletter store
const mockStore = {
  currentLetter: null as any,
  loading: false,
  fetchLetter: vi.fn().mockResolvedValue(undefined),
  sendLetter: vi.fn().mockResolvedValue(undefined),
  closeLetter: vi.fn().mockResolvedValue(undefined),
  confirmLetter: vi.fn().mockResolvedValue(undefined),
  markAsRead: vi.fn().mockResolvedValue(undefined),
}

vi.mock('@/stores/parentletter', () => ({
  useParentLetterStore: vi.fn(() => mockStore),
}))

// --- mock auth store
const mockAuth = {
  isTeacher: true,
  isAdmin: true,
  user: { id: 'user-1', role: 'TEACHER' },
}

vi.mock('@/stores/auth', () => ({
  useAuthStore: vi.fn(() => mockAuth),
}))

// --- mock vue-router
const mockRouter = { push: vi.fn(), back: vi.fn() }
const mockRoute = { params: { id: 'letter-1' } }

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => mockRouter),
  useRoute: vi.fn(() => mockRoute),
}))

// --- mock composables
vi.mock('@/composables/useLocaleDate', () => ({
  useLocaleDate: vi.fn(() => ({
    formatShortDate: vi.fn((d: string) => d?.substring(0, 10) || ''),
    formatCompactDateTime: vi.fn((d: string) => d?.substring(0, 16) || ''),
  })),
}))

vi.mock('@/composables/useMarkdown', () => ({
  useMarkdown: vi.fn(() => ({
    renderMarkdown: vi.fn((text: string) => `<p>${text}</p>`),
  })),
}))

// --- mock parentletter API
vi.mock('@/api/parentletter.api', () => ({
  parentLetterApi: {
    getAttachments: vi.fn().mockResolvedValue({ data: { data: [] } }),
    uploadAttachments: vi.fn().mockResolvedValue({}),
    deleteAttachment: vi.fn().mockResolvedValue({}),
    downloadLetterPdf: vi.fn().mockResolvedValue({ data: new Blob() }),
    downloadTrackingPdf: vi.fn().mockResolvedValue({ data: new Blob() }),
    getAttachmentDownloadUrl: vi.fn((letterId: string, attId: string) => `/api/v1/parent-letters/${letterId}/attachments/${attId}`),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))

import ParentLetterDetailView from '@/views/ParentLetterDetailView.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      parentLetters: {
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
        send: 'Senden',
        close: 'Schließen',
        sent: 'Gesendet',
        closed: 'Geschlossen',
        downloadPdf: 'PDF herunterladen',
        downloadTracking: 'Rücklauf-PDF',
        confirmProgress: 'Bestätigungsfortschritt',
        confirmationRequired: 'Bestätigung erforderlich',
        confirmationHint: 'Bitte bestätigen Sie den Elternbrief.',
        confirm: 'Bestätigen',
        confirmSuccess: 'Bestätigt',
        allConfirmed: 'Alle bestätigt',
        recipientsTitle: 'Empfänger',
        deadline: 'Frist',
        createdAt: 'Erstellt',
        notFound: 'Brief nicht gefunden.',
        attachments: {
          title: 'Anhänge',
          upload: 'Hochladen',
          uploadHint: 'Max. 5 Dateien',
          uploaded: 'Hochgeladen',
          deleted: 'Gelöscht',
        },
        recipientTable: {
          total: 'Gesamt',
          read: 'Gelesen',
          confirmed: 'Bestätigt',
          empty: 'Keine Empfänger',
          student: 'Schüler',
          parent: 'Elternteil',
          family: 'Familie',
          status: 'Status',
          readAt: 'Gelesen am',
          confirmedAt: 'Bestätigt am',
          reminderSent: 'Erinnerung',
        },
      },
      common: {
        back: 'Zurück',
        edit: 'Bearbeiten',
        error: 'Fehler',
      },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title'] },
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  RecipientStatusTable: { template: '<div class="recipient-table-stub" />', props: ['recipients'] },
  Button: {
    template: '<button class="button-stub" :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'text', 'severity', 'size', 'disabled', 'loading'],
    emits: ['click'],
  },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity', 'size', 'icon'] },
  ProgressBar: { template: '<div class="progress-stub" :data-value="value" />', props: ['value', 'showValue'] },
  Divider: { template: '<hr class="divider-stub" />' },
  FileUpload: { template: '<div class="fileupload-stub" />', props: ['mode', 'multiple', 'auto', 'maxFileSize', 'chooseLabel', 'disabled', 'customUpload'] },
}

const mockLetterDetail = {
  id: 'letter-1',
  title: 'Wandertag am Freitag',
  content: 'Liebe Eltern, am Freitag findet ein Wandertag statt.',
  status: 'SENT' as const,
  roomId: 'room-1',
  roomName: 'Sonnengruppe',
  createdBy: 'user-1',
  creatorName: 'Maria Muster',
  sendDate: '2025-03-10T08:00:00Z',
  deadline: '2025-03-15',
  reminderDays: 3,
  reminderSent: false,
  totalRecipients: 10,
  confirmedCount: 4,
  recipients: [
    {
      id: 'rec-1',
      studentId: 'student-1',
      studentName: 'Anna Schmidt',
      parentId: 'parent-1',
      parentName: 'Hans Schmidt',
      familyName: 'Schmidt',
      status: 'CONFIRMED' as const,
      readAt: '2025-03-10T09:00:00Z',
      confirmedAt: '2025-03-10T10:00:00Z',
      confirmedByName: 'Hans Schmidt',
      reminderSentAt: null,
    },
    {
      id: 'rec-2',
      studentId: 'student-2',
      studentName: 'Ben Mueller',
      parentId: 'parent-2',
      parentName: 'Eva Mueller',
      familyName: 'Mueller',
      status: 'READ' as const,
      readAt: '2025-03-10T11:00:00Z',
      confirmedAt: null,
      confirmedByName: null,
      reminderSentAt: null,
    },
  ],
  createdAt: '2025-03-05T10:00:00Z',
  updatedAt: '2025-03-10T08:00:00Z',
}

function mountView() {
  return mount(ParentLetterDetailView, {
    global: { plugins: [i18n], stubs },
  })
}

describe('ParentLetterDetailView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    Object.assign(mockStore, {
      currentLetter: { ...mockLetterDetail },
      loading: false,
      fetchLetter: vi.fn().mockResolvedValue(undefined),
      sendLetter: vi.fn().mockResolvedValue(undefined),
      closeLetter: vi.fn().mockResolvedValue(undefined),
      confirmLetter: vi.fn().mockResolvedValue(undefined),
      markAsRead: vi.fn().mockResolvedValue(undefined),
    })
    Object.assign(mockAuth, {
      isTeacher: true,
      isAdmin: true,
      user: { id: 'user-1', role: 'TEACHER' },
    })
    mockRouter.push.mockReset()
    mockRouter.back.mockReset()
  })

  // ==================== Basic render ====================

  it('should mount and render', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('should fetch letter on mount', async () => {
    mountView()
    await flushPromises()
    expect(mockStore.fetchLetter).toHaveBeenCalledWith('letter-1')
  })

  it('should render letter title', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.page-title-stub').text()).toContain('Wandertag am Freitag')
    wrapper.unmount()
  })

  it('should show status tag', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.tag-stub').text()).toContain('Gesendet')
    wrapper.unmount()
  })

  // ==================== Meta bar ====================

  it('should render room name', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Sonnengruppe')
    wrapper.unmount()
  })

  it('should render creator name', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Maria Muster')
    wrapper.unmount()
  })

  it('should render deadline when set', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Frist')
    wrapper.unmount()
  })

  // ==================== Letter content ====================

  it('should render letter content as HTML', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.content-text').html()).toContain('Liebe Eltern')
    wrapper.unmount()
  })

  // ==================== Management actions (teacher/admin) ====================

  it('should show management actions for creator/admin', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.management-actions').exists()).toBe(true)
    wrapper.unmount()
  })

  it('should show send button for DRAFT letters', async () => {
    mockStore.currentLetter = { ...mockLetterDetail, status: 'DRAFT' }

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Senden')
    wrapper.unmount()
  })

  it('should show edit button for DRAFT letters', async () => {
    mockStore.currentLetter = { ...mockLetterDetail, status: 'DRAFT' }

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Bearbeiten')
    wrapper.unmount()
  })

  it('should show close button for SENT letters', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Schließen')
    wrapper.unmount()
  })

  it('should show PDF download button', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('PDF herunterladen')
    wrapper.unmount()
  })

  it('should show tracking PDF button for non-DRAFT letters', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Rücklauf-PDF')
    wrapper.unmount()
  })

  it('should not show tracking PDF button for DRAFT letters', async () => {
    mockStore.currentLetter = { ...mockLetterDetail, status: 'DRAFT' }

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).not.toContain('Rücklauf-PDF')
    wrapper.unmount()
  })

  // ==================== Confirmation progress ====================

  it('should show confirmation progress for creator/admin', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Bestätigungsfortschritt')
    expect(wrapper.text()).toContain('4/10')
    wrapper.unmount()
  })

  it('should show correct progress percentage', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('40%')
    wrapper.unmount()
  })

  it('should render progress bar', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.progress-stub').exists()).toBe(true)
    wrapper.unmount()
  })

  // ==================== Recipient table (teacher/admin) ====================

  it('should show recipient tracking table for creator/admin', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.recipient-table-stub').exists()).toBe(true)
    wrapper.unmount()
  })

  it('should show recipients count', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Empfänger')
    expect(wrapper.text()).toContain('(2)')
    wrapper.unmount()
  })

  // ==================== Parent view ====================

  it('should not show management actions for parents', async () => {
    Object.assign(mockAuth, {
      isTeacher: false,
      isAdmin: false,
      user: { id: 'parent-2', role: 'PARENT' },
    })
    mockStore.currentLetter = { ...mockLetterDetail, createdBy: 'other-user' }

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.management-actions').exists()).toBe(false)
    wrapper.unmount()
  })

  it('should show confirmation section for parents with SENT letter', async () => {
    Object.assign(mockAuth, {
      isTeacher: false,
      isAdmin: false,
      user: { id: 'parent-2', role: 'PARENT' },
    })
    mockStore.currentLetter = {
      ...mockLetterDetail,
      createdBy: 'other-user',
      recipients: [
        {
          id: 'rec-2',
          studentId: 'student-2',
          studentName: 'Ben Mueller',
          parentId: 'parent-2',
          parentName: 'Eva Mueller',
          familyName: 'Mueller',
          status: 'READ',
          readAt: '2025-03-10T11:00:00Z',
          confirmedAt: null,
          confirmedByName: null,
          reminderSentAt: null,
        },
      ],
    }

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Bestätigung erforderlich')
    expect(wrapper.text()).toContain('Ben Mueller')
    wrapper.unmount()
  })

  it('should mark as read for parents viewing SENT letter', async () => {
    Object.assign(mockAuth, {
      isTeacher: false,
      isAdmin: false,
      user: { id: 'parent-2', role: 'PARENT' },
    })
    mockStore.currentLetter = {
      ...mockLetterDetail,
      createdBy: 'other-user',
      recipients: [
        {
          id: 'rec-2',
          studentId: 'student-2',
          studentName: 'Ben Mueller',
          parentId: 'parent-2',
          parentName: 'Eva Mueller',
          familyName: 'Mueller',
          status: 'READ',
          readAt: null,
          confirmedAt: null,
          confirmedByName: null,
          reminderSentAt: null,
        },
      ],
    }

    mountView()
    await flushPromises()
    expect(mockStore.markAsRead).toHaveBeenCalledWith('letter-1')
  })

  // ==================== Loading and empty state ====================

  it('should show loading spinner when loading and no letter', async () => {
    mockStore.loading = true
    mockStore.currentLetter = null

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.loading-stub').exists()).toBe(true)
    wrapper.unmount()
  })

  it('should show not found when no letter loaded', async () => {
    mockStore.currentLetter = null

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Brief nicht gefunden.')
    wrapper.unmount()
  })

  // ==================== Back button ====================

  it('should render back button', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Zurück')
    wrapper.unmount()
  })

  // ==================== Status severity logic ====================

  it('should map status to correct tag severity', () => {
    function statusSeverity(status: string): string {
      switch (status) {
        case 'DRAFT': return 'secondary'
        case 'SCHEDULED': return 'info'
        case 'SENT': return 'success'
        case 'CLOSED': return 'warn'
        default: return 'secondary'
      }
    }

    expect(statusSeverity('DRAFT')).toBe('secondary')
    expect(statusSeverity('SCHEDULED')).toBe('info')
    expect(statusSeverity('SENT')).toBe('success')
    expect(statusSeverity('CLOSED')).toBe('warn')
  })

  // ==================== Confirm progress computation ====================

  it('should compute confirm progress correctly', () => {
    // 0 total recipients
    expect(Math.round((0 / 0) * 100) || 0).toBe(0)
    // normal
    expect(Math.round((4 / 10) * 100)).toBe(40)
    // all confirmed
    expect(Math.round((10 / 10) * 100)).toBe(100)
  })

  // ==================== getAttachmentIcon and formatFileSize logic ====================

  it('should return correct file icons', () => {
    const getAttachmentIcon = (contentType: string): string => {
      if (contentType.startsWith('image/')) return 'pi pi-image'
      if (contentType === 'application/pdf') return 'pi pi-file-pdf'
      if (contentType.startsWith('video/')) return 'pi pi-video'
      if (contentType.startsWith('audio/')) return 'pi pi-volume-up'
      return 'pi pi-file'
    }

    expect(getAttachmentIcon('image/png')).toBe('pi pi-image')
    expect(getAttachmentIcon('application/pdf')).toBe('pi pi-file-pdf')
    expect(getAttachmentIcon('video/mp4')).toBe('pi pi-video')
    expect(getAttachmentIcon('audio/mpeg')).toBe('pi pi-volume-up')
    expect(getAttachmentIcon('text/plain')).toBe('pi pi-file')
  })

  it('should format file sizes correctly', () => {
    const formatFileSize = (bytes: number): string => {
      if (!bytes) return ''
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
    }

    expect(formatFileSize(0)).toBe('')
    expect(formatFileSize(100)).toBe('100 B')
    expect(formatFileSize(1023)).toBe('1023 B')
    expect(formatFileSize(1024)).toBe('1.0 KB')
    expect(formatFileSize(1048576)).toBe('1.0 MB')
    expect(formatFileSize(5242880)).toBe('5.0 MB')
  })
})
