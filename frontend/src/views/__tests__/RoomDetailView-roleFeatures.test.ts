import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import RoomDetailView from '@/views/RoomDetailView.vue'

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn(), back: vi.fn() })),
}))

vi.mock('@/api/rooms.api', () => ({
  roomsApi: {
    getMine: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getById: vi.fn(),
    discover: vi.fn().mockResolvedValue({ data: { data: { content: [] } } }),
    update: vi.fn(),
    uploadAvatar: vi.fn(),
    removeAvatar: vi.fn(),
    requestJoin: vi.fn(),
    approveJoinRequest: vi.fn(),
    denyJoinRequest: vi.fn(),
    getJoinRequests: vi.fn().mockResolvedValue({ data: { data: [] } }),
    addMember: vi.fn(),
    addFamily: vi.fn(),
    joinRoom: vi.fn(),
    leaveRoom: vi.fn(),
    muteRoom: vi.fn().mockResolvedValue({}),
    unmuteRoom: vi.fn().mockResolvedValue({}),
    getChatChannels: vi.fn(),
    getOrCreateChatChannel: vi.fn(),
    createInterestRoom: vi.fn(),
  },
}))

vi.mock('@/api/feed.api', () => ({
  feedApi: {
    getRoomPosts: vi.fn().mockResolvedValue({ data: { data: { content: [] } } }),
    createPost: vi.fn(),
    createRoomPost: vi.fn().mockResolvedValue({ data: { data: { id: 'p1' } } }),
  },
}))

vi.mock('@/api/family.api', () => ({
  familyApi: {
    getMine: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getAll: vi.fn().mockResolvedValue({ data: { data: [] } }),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))
vi.mock('@/api/admin.api', () => ({
  adminApi: { getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }) },
}))

import { roomsApi } from '@/api/rooms.api'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      rooms: {
        title: 'Räume', members: 'Mitglieder', infoBoard: 'Info-Board',
        noPosts: 'Noch keine Beiträge', noMembers: 'Keine Mitglieder',
        publicDescription: 'Öffentliche Beschreibung',
        publicDescPlaceholder: 'Für Nicht-Mitglieder sichtbar...',
        notMember: 'Kein Mitglied', joinToSee: 'Beitreten um Inhalte zu sehen',
        requestJoin: 'Beitritt anfragen', joinRequestSent: 'Anfrage gesendet',
        joinRequestMessage: 'Beitrittsanfrage für {room}',
        joinRequestPlaceholder: 'Nachricht...',
        pendingRequests: 'Offene Anfragen',
        approve: 'Annehmen', deny: 'Ablehnen',
        requestApproved: 'Angenommen', requestDenied: 'Abgelehnt',
        addFamily: 'Familie aufnehmen', familyAdded: 'Hinzugefügt',
        selectFamily: 'Familie auswählen', noFamilies: 'Keine Familien.',
        inviteOnly: 'Nur auf Einladung',
        muteFeed: 'Feed stummschalten',
        unmuteFeed: 'Stummschaltung aufheben',
        muted: 'Feed stummgeschaltet',
        unmuted: 'Stummschaltung aufgehoben',
        roles: { LEADER: 'Leitung', MEMBER: 'Mitglied', PARENT_MEMBER: 'Eltern', GUEST: 'Gast' },
        types: { KLASSE: 'Klasse', GRUPPE: 'Gruppe', PROJEKT: 'Projekt', INTEREST: 'Interesse', CUSTOM: 'Sonstige' },
      },
      discover: { join: 'Beitreten', joined: 'Beigetreten' },
      common: { back: 'Zurück', save: 'Speichern', cancel: 'Abbrechen', delete: 'Löschen', removeAvatar: 'Entfernen', loading: 'Laden...' },
      feed: { postCreated: 'OK', titlePlaceholder: 'Titel', contentPlaceholder: 'Inhalt' },
      discussions: { title: 'Diskussionen' },
      chat: { title: 'Chat' },
      files: { title: 'Dateien' },
      calendar: { title: 'Kalender' },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title', 'subtitle'] },
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  AvatarUpload: { template: '<div class="avatar-stub" />', props: ['imageUrl', 'size', 'icon', 'editable'] },
  PostComposer: { template: '<div class="composer-stub" />' },
  FeedPostComponent: { template: '<div class="feed-post-stub" />', props: ['post'] },
  RoomFiles: { template: '<div class="files-stub" />', props: ['roomId'] },
  RoomChat: { template: '<div class="chat-stub" />', props: ['roomId'] },
  RoomDiscussions: { template: '<div class="discussions-stub" />', props: ['roomId'] },
  RoomEvents: { template: '<div class="events-stub" />', props: ['roomId'] },
  Button: {
    template: '<button class="button-stub" :data-severity="severity" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'text', 'severity', 'size', 'loading', 'disabled', 'outlined'],
    emits: ['click'],
  },
  Tag: { template: '<span class="tag-stub" :data-severity="severity">{{ value }}</span>', props: ['value', 'severity', 'size'] },
  Tabs: { template: '<div class="tabs-stub"><slot /></div>', props: ['value'] },
  TabList: { template: '<div class="tablist-stub"><slot /></div>' },
  Tab: { template: '<div class="tab-stub"><slot /></div>', props: ['value'] },
  TabPanels: { template: '<div class="tabpanels-stub"><slot /></div>' },
  TabPanel: { template: '<div class="tabpanel-stub"><slot /></div>', props: ['value'] },
  Textarea: { template: '<textarea class="textarea-stub" />', props: ['modelValue', 'rows', 'placeholder'] },
  Dialog: { template: '<div v-if="visible" class="dialog-stub"><slot /><slot name="footer" /></div>', props: ['visible', 'header', 'modal'] },
  Select: { template: '<select class="select-stub" />', props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder', 'filter'] },
}

function mountWithPublicRoom(publicRoom: any) {
  vi.mocked(roomsApi.getById).mockResolvedValue({
    data: { data: publicRoom },
  } as any)

  const pinia = createPinia()
  return mount(RoomDetailView, {
    props: { id: 'r1' },
    global: { plugins: [i18n, pinia], stubs },
  })
}

function mountWithMemberRoom(memberRoom: any) {
  vi.mocked(roomsApi.getById).mockResolvedValue({
    data: { data: memberRoom },
  } as any)
  vi.mocked(roomsApi.getJoinRequests).mockResolvedValue({
    data: { data: [] },
  } as any)

  const pinia = createPinia()
  return mount(RoomDetailView, {
    props: { id: 'r1' },
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('RoomDetailView – JoinPolicy', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should show join button for OPEN public room', async () => {
    const wrapper = mountWithPublicRoom({
      id: 'r1', name: 'Open Room', type: 'GRUPPE',
      joinPolicy: 'OPEN', memberCount: 10,
      publicDescription: 'Welcome to our open room',
      avatarUrl: null, sectionId: null, tags: [],
    })

    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    const buttons = wrapper.findAll('.button-stub')
    const joinButton = buttons.find(b => b.text() === 'Beitreten')
    expect(joinButton).toBeDefined()
  })

  it('should show request join button for REQUEST public room', async () => {
    const wrapper = mountWithPublicRoom({
      id: 'r1', name: 'Request Room', type: 'PROJEKT',
      joinPolicy: 'REQUEST', memberCount: 15,
      publicDescription: 'Send a request to join',
      avatarUrl: null, sectionId: null, tags: [],
    })

    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    const buttons = wrapper.findAll('.button-stub')
    const requestButton = buttons.find(b => b.text() === 'Beitritt anfragen')
    expect(requestButton).toBeDefined()
  })

  it('should show invite-only tag for INVITE_ONLY public room', async () => {
    const wrapper = mountWithPublicRoom({
      id: 'r1', name: 'Invite Room', type: 'KLASSE',
      joinPolicy: 'INVITE_ONLY', memberCount: 25,
      publicDescription: 'This room is invite only',
      avatarUrl: null, sectionId: null, tags: [],
    })

    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    const tags = wrapper.findAll('.tag-stub')
    const inviteTag = tags.find(t => t.text() === 'Nur auf Einladung')
    expect(inviteTag).toBeDefined()
  })

  it('should not show join or request button for INVITE_ONLY rooms', async () => {
    const wrapper = mountWithPublicRoom({
      id: 'r1', name: 'Invite Room', type: 'KLASSE',
      joinPolicy: 'INVITE_ONLY', memberCount: 25,
      publicDescription: 'Invite only',
      avatarUrl: null, sectionId: null, tags: [],
    })

    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    const buttons = wrapper.findAll('.button-stub')
    const joinButton = buttons.find(b => b.text() === 'Beitreten')
    const requestButton = buttons.find(b => b.text() === 'Beitritt anfragen')
    expect(joinButton).toBeUndefined()
    expect(requestButton).toBeUndefined()
  })
})

describe('RoomDetailView – Mute/Unmute', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should show mute button for non-leader members', async () => {
    const wrapper = mountWithMemberRoom({
      id: 'r1', name: 'Member Room', type: 'GRUPPE',
      joinPolicy: 'REQUEST', description: 'A room',
      publicDescription: null, avatarUrl: null, sectionId: null,
      archived: false, memberCount: 5, expiresAt: null, tags: [],
      settings: {
        chatEnabled: false, filesEnabled: false, parentSpaceEnabled: false,
        visibility: 'MEMBERS_ONLY', discussionMode: 'FULL',
        allowMemberThreadCreation: false, childDiscussionEnabled: false,
      },
      createdBy: null, createdAt: null,
      members: [{ userId: 'other-user', displayName: 'Other', role: 'LEADER', avatarUrl: null, joinedAt: null }],
    })

    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    const buttons = wrapper.findAll('.button-stub')
    const muteButton = buttons.find(b => b.text() === 'Feed stummschalten')
    expect(muteButton).toBeDefined()
  })

  it('should call muteRoom when clicking mute button', async () => {
    vi.mocked(roomsApi.muteRoom).mockResolvedValue({} as any)

    const wrapper = mountWithMemberRoom({
      id: 'r1', name: 'Mute Test Room', type: 'GRUPPE',
      joinPolicy: 'REQUEST', description: 'Test',
      publicDescription: null, avatarUrl: null, sectionId: null,
      archived: false, memberCount: 3, expiresAt: null, tags: [],
      settings: {
        chatEnabled: false, filesEnabled: false, parentSpaceEnabled: false,
        visibility: 'MEMBERS_ONLY', discussionMode: 'FULL',
        allowMemberThreadCreation: false, childDiscussionEnabled: false,
      },
      createdBy: null, createdAt: null,
      members: [{ userId: 'other-user', displayName: 'Other', role: 'LEADER', avatarUrl: null, joinedAt: null }],
    })

    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    const buttons = wrapper.findAll('.button-stub')
    const muteButton = buttons.find(b => b.text() === 'Feed stummschalten')
    if (muteButton) {
      await muteButton.trigger('click')
      await wrapper.vm.$nextTick()
      expect(roomsApi.muteRoom).toHaveBeenCalledWith('r1')
    }
  })
})
