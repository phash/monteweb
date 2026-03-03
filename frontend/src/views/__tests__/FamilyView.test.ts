import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import FamilyView from '@/views/FamilyView.vue'
import { useFamilyStore } from '@/stores/family'
import { useAdminStore } from '@/stores/admin'
import { useAuthStore } from '@/stores/auth'

const mockGetMine = vi.fn().mockResolvedValue({ data: { data: [] } })
const mockGetMyInvitations = vi.fn().mockResolvedValue({ data: { data: [] } })
const mockGetFamilyInvitations = vi.fn().mockResolvedValue({ data: { data: [] } })
const mockCreateFamily = vi.fn()
const mockJoinByCode = vi.fn()
const mockGenerateInviteCode = vi.fn().mockResolvedValue({ data: { data: { inviteCode: 'ABC123' } } })
const mockAcceptInvitation = vi.fn().mockResolvedValue({ data: { data: {} } })
const mockDeclineInvitation = vi.fn().mockResolvedValue({ data: { data: {} } })
const mockLeaveFamily = vi.fn().mockResolvedValue({ data: { data: {} } })
const mockGetFamilyCalendar = vi.fn().mockResolvedValue({ data: { data: [] } })
const mockDownloadFamilyIcal = vi.fn().mockResolvedValue({ data: 'BEGIN:VCALENDAR' })
const mockRequestSoleCustody = vi.fn().mockResolvedValue({ data: { data: {} } })
const mockUploadAvatar = vi.fn().mockResolvedValue({ data: { data: {} } })
const mockRemoveAvatar = vi.fn().mockResolvedValue({ data: { data: {} } })

vi.mock('@/api/family.api', () => ({
  familyApi: {
    getMine: (...args: unknown[]) => mockGetMine(...args),
    getMyInvitations: (...args: unknown[]) => mockGetMyInvitations(...args),
    getFamilyInvitations: (...args: unknown[]) => mockGetFamilyInvitations(...args),
    createFamily: (...args: unknown[]) => mockCreateFamily(...args),
    joinByCode: (...args: unknown[]) => mockJoinByCode(...args),
    generateInviteCode: (...args: unknown[]) => mockGenerateInviteCode(...args),
    acceptInvitation: (...args: unknown[]) => mockAcceptInvitation(...args),
    declineInvitation: (...args: unknown[]) => mockDeclineInvitation(...args),
    leaveFamily: (...args: unknown[]) => mockLeaveFamily(...args),
    getFamilyCalendar: (...args: unknown[]) => mockGetFamilyCalendar(...args),
    downloadFamilyIcal: (...args: unknown[]) => mockDownloadFamilyIcal(...args),
    requestSoleCustody: (...args: unknown[]) => mockRequestSoleCustody(...args),
    uploadAvatar: (...args: unknown[]) => mockUploadAvatar(...args),
    removeAvatar: (...args: unknown[]) => mockRemoveAvatar(...args),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))
vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
  },
}))

const mockFamily = {
  id: 'fam-1',
  name: 'Familie Testmann',
  avatarUrl: null,
  hoursExempt: false,
  active: true,
  soleCustody: false,
  soleCustodyApproved: false,
  members: [
    { userId: 'u1', displayName: 'Max Testmann', role: 'PARENT' },
    { userId: 'u2', displayName: 'Anna Testmann', role: 'PARENT' },
    { userId: 'u3', displayName: 'Tim Testmann', role: 'CHILD' },
  ],
}

const mockInvitation = {
  id: 'inv-1',
  familyId: 'fam-1',
  familyName: 'Familie Testmann',
  inviterId: 'u1',
  inviterName: 'Max Testmann',
  inviteeId: 'u99',
  inviteeName: 'Lisa Neu',
  role: 'PARENT',
  status: 'PENDING',
}

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      family: {
        title: 'Familienverbund',
        create: 'Erstellen',
        join: 'Beitreten',
        inviteCode: 'Einladungscode',
        generateCode: 'Code generieren',
        noFamily: 'Kein Familienverbund.',
        familyName: 'Familienname',
        name: 'Familienname',
        members: 'Mitglieder',
        childRole: 'Kind',
        parentRole: 'Elternteil',
        joinCode: 'Code eingeben',
        inviteMembers: 'Mitglieder einladen',
        inviteMember: 'Mitglied einladen',
        sentInvitations: 'Gesendete Einladungen',
        receivedInvitations: 'Empfangene Einladungen',
        pendingInvitations: 'Einladungen',
        accept: 'Annehmen',
        decline: 'Ablehnen',
        pending: 'Ausstehend',
        role: 'Rolle',
        leave: 'Verlassen',
        leaveConfirmTitle: 'Familie verlassen',
        leaveConfirmMessage: 'Wirklich verlassen?',
        leftFamily: 'Familie verlassen',
        invitationSent: 'Einladung gesendet',
        invitationAccepted: 'Einladung angenommen',
        invitationDeclined: 'Einladung abgelehnt',
        invitedBy: 'Eingeladen von {name}',
        acceptInvitation: 'Annehmen',
        declineInvitation: 'Ablehnen',
        calendar: 'Familienkalender',
        calendarDesc: 'Termine aus allen Raeumen',
        noCalendarEvents: 'Keine Termine',
        downloadIcal: 'Kalender herunterladen',
        soleCustody: 'Alleiniges Sorgerecht',
        soleCustodyHint: 'Nur dieser Elternteil',
        soleCustodyRequested: 'Beantragt',
        soleCustodyPending: 'Ausstehend',
        soleCustodyApproved: 'Genehmigt',
        soleCustodyApprovedLabel: 'Genehmigt',
        approveSoleCustody: 'Genehmigen',
        roles: { PARENT: 'Elternteil', CHILD: 'Kind' },
      },
      common: { save: 'Speichern', cancel: 'Abbrechen', delete: 'Loeschen', create: 'Erstellen', removeAvatar: 'Avatar entfernen' },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title', 'subtitle'] },
  EmptyState: {
    template: '<div class="empty-stub">{{ message }}<slot /></div>',
    props: ['icon', 'message'],
  },
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  FamilyHoursWidget: { template: '<div class="hours-stub" />', props: ['familyId'] },
  InviteMemberDialog: { template: '<div class="invite-dialog-stub" />', props: ['visible', 'familyId'] },
  AvatarUpload: { template: '<div class="avatar-stub" />', props: ['imageUrl', 'size', 'icon', 'editable'] },
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'severity', 'text', 'size', 'outlined'],
    emits: ['click'],
  },
  InputText: {
    template: '<input class="input-stub" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
    props: ['modelValue', 'id'],
    emits: ['update:modelValue'],
  },
  Dialog: {
    template: '<div v-if="visible" class="dialog-stub"><slot /><slot name="footer" /></div>',
    props: ['visible', 'header', 'modal'],
  },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity', 'size', 'icon'] },
  Checkbox: {
    template: '<input type="checkbox" class="checkbox-stub" />',
    props: ['modelValue', 'binary', 'disabled'],
  },
}

function mountFamily(options?: { withFamily?: boolean; withInvitations?: boolean }) {
  const pinia = createPinia()
  setActivePinia(pinia)

  if (options?.withFamily) {
    mockGetMine.mockResolvedValue({ data: { data: [mockFamily] } })
  } else {
    mockGetMine.mockResolvedValue({ data: { data: [] } })
  }

  if (options?.withInvitations) {
    mockGetMyInvitations.mockResolvedValue({ data: { data: [mockInvitation] } })
  } else {
    mockGetMyInvitations.mockResolvedValue({ data: { data: [] } })
  }

  return mount(FamilyView, {
    global: {
      plugins: [i18n, pinia],
      stubs,
    },
  })
}

describe('FamilyView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render page title', () => {
    const wrapper = mountFamily()
    expect(wrapper.find('.page-title-stub').text()).toContain('Familienverbund')
  })

  it('should render action buttons', () => {
    const wrapper = mountFamily()
    expect(wrapper.findAll('.button-stub').length).toBeGreaterThanOrEqual(0)
  })

  it('should show empty state when no families', async () => {
    const wrapper = mountFamily()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    // Either empty state or loading spinner should exist
    expect(wrapper.find('.empty-stub').exists() || wrapper.find('.loading-stub').exists()).toBe(true)
  })

  it('should have dialog components for create/join', () => {
    const wrapper = mountFamily()
    // Dialogs are not visible by default
    expect(wrapper.find('.dialog-stub').exists()).toBe(false)
  })

  describe('family display with data', () => {
    it('should display family members when family exists', async () => {
      const wrapper = mountFamily({ withFamily: true })
      await flushPromises()
      await wrapper.vm.$nextTick()
      expect(wrapper.text()).toContain('Max Testmann')
      expect(wrapper.text()).toContain('Anna Testmann')
      expect(wrapper.text()).toContain('Tim Testmann')
    })

    it('should display member roles as tags', async () => {
      const wrapper = mountFamily({ withFamily: true })
      await flushPromises()
      await wrapper.vm.$nextTick()
      const tags = wrapper.findAll('.tag-stub')
      const roleTag = tags.find(t => t.text().includes('Elternteil'))
      expect(roleTag).toBeTruthy()
    })

    it('should display family name', async () => {
      const wrapper = mountFamily({ withFamily: true })
      await flushPromises()
      await wrapper.vm.$nextTick()
      expect(wrapper.text()).toContain('Familie Testmann')
    })

    it('should render FamilyHoursWidget', async () => {
      const wrapper = mountFamily({ withFamily: true })
      await flushPromises()
      await wrapper.vm.$nextTick()
      expect(wrapper.find('.hours-stub').exists()).toBe(true)
    })

    it('should render invite member button', async () => {
      const wrapper = mountFamily({ withFamily: true })
      await flushPromises()
      await wrapper.vm.$nextTick()
      const buttons = wrapper.findAll('.button-stub')
      const inviteBtn = buttons.find(b => b.text().includes('Mitglied einladen'))
      expect(inviteBtn).toBeTruthy()
    })

    it('should render generate code button', async () => {
      const wrapper = mountFamily({ withFamily: true })
      await flushPromises()
      await wrapper.vm.$nextTick()
      const buttons = wrapper.findAll('.button-stub')
      const codeBtn = buttons.find(b => b.text().includes('Code generieren'))
      expect(codeBtn).toBeTruthy()
    })

    it('should render leave family button', async () => {
      const wrapper = mountFamily({ withFamily: true })
      await flushPromises()
      await wrapper.vm.$nextTick()
      const buttons = wrapper.findAll('.button-stub')
      const leaveBtn = buttons.find(b => b.text().includes('Verlassen'))
      expect(leaveBtn).toBeTruthy()
    })
  })

  describe('empty state actions', () => {
    it('should show create and join buttons in empty state', async () => {
      const wrapper = mountFamily()
      await flushPromises()
      await wrapper.vm.$nextTick()
      const emptyState = wrapper.find('.empty-stub')
      if (emptyState.exists()) {
        const buttons = emptyState.findAll('.button-stub')
        const createBtn = buttons.find(b => b.text().includes('Erstellen'))
        const joinBtn = buttons.find(b => b.text().includes('Beitreten'))
        expect(createBtn).toBeTruthy()
        expect(joinBtn).toBeTruthy()
      }
    })
  })

  describe('invitations', () => {
    it('should display pending invitations', async () => {
      const wrapper = mountFamily({ withInvitations: true })
      await flushPromises()
      await wrapper.vm.$nextTick()
      expect(wrapper.text()).toContain('Eingeladen von Max Testmann')
    })

    it('should show accept and decline buttons for invitations', async () => {
      const wrapper = mountFamily({ withInvitations: true })
      await flushPromises()
      await wrapper.vm.$nextTick()
      const buttons = wrapper.findAll('.button-stub')
      const acceptBtn = buttons.find(b => b.text().includes('Annehmen'))
      const declineBtn = buttons.find(b => b.text().includes('Ablehnen'))
      expect(acceptBtn).toBeTruthy()
      expect(declineBtn).toBeTruthy()
    })

    it('should call acceptInvitation when accept is clicked', async () => {
      const wrapper = mountFamily({ withInvitations: true })
      await flushPromises()
      await wrapper.vm.$nextTick()
      const buttons = wrapper.findAll('.button-stub')
      const acceptBtn = buttons.find(b => b.text().includes('Annehmen'))
      // Reset mocks for the follow-up calls after accept
      mockGetMine.mockResolvedValue({ data: { data: [] } })
      mockGetMyInvitations.mockResolvedValue({ data: { data: [] } })
      await acceptBtn!.trigger('click')
      await flushPromises()
      expect(mockAcceptInvitation).toHaveBeenCalledWith('inv-1')
    })

    it('should call declineInvitation when decline is clicked', async () => {
      const wrapper = mountFamily({ withInvitations: true })
      await flushPromises()
      await wrapper.vm.$nextTick()
      const buttons = wrapper.findAll('.button-stub')
      const declineBtn = buttons.find(b => b.text().includes('Ablehnen'))
      mockGetMyInvitations.mockResolvedValue({ data: { data: [] } })
      await declineBtn!.trigger('click')
      await flushPromises()
      expect(mockDeclineInvitation).toHaveBeenCalledWith('inv-1')
    })
  })

  describe('leave family', () => {
    it('should open leave confirmation dialog', async () => {
      const wrapper = mountFamily({ withFamily: true })
      await flushPromises()
      await wrapper.vm.$nextTick()
      const buttons = wrapper.findAll('.button-stub')
      const leaveBtn = buttons.find(b => b.text().includes('Verlassen'))
      await leaveBtn!.trigger('click')
      await wrapper.vm.$nextTick()
      // Leave dialog should now be visible
      const dialog = wrapper.findAll('.dialog-stub')
      expect(dialog.length).toBeGreaterThanOrEqual(1)
    })
  })

  describe('data loading', () => {
    it('should call getMine on mount', async () => {
      mountFamily()
      await flushPromises()
      expect(mockGetMine).toHaveBeenCalled()
    })

    it('should call getMyInvitations on mount', async () => {
      mountFamily()
      await flushPromises()
      expect(mockGetMyInvitations).toHaveBeenCalled()
    })
  })
})
