import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import DiscoverRoomsView from '@/views/DiscoverRoomsView.vue'

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn() })),
}))

vi.mock('@/api/rooms.api', () => ({
  roomsApi: {
    getMine: vi.fn().mockResolvedValue({ data: { data: [] } }),
    discover: vi.fn().mockResolvedValue({ data: { data: { content: [], totalPages: 0, last: true } } }),
    browse: vi.fn().mockResolvedValue({ data: { data: { content: [], totalPages: 0, last: true } } }),
    create: vi.fn(),
    createInterestRoom: vi.fn(),
    requestJoin: vi.fn(),
    getMyJoinRequests: vi.fn().mockResolvedValue({ data: { data: [] } }),
    joinRoom: vi.fn(),
    leaveRoom: vi.fn(),
    muteRoom: vi.fn(),
    unmuteRoom: vi.fn(),
  },
}))

vi.mock('@/api/sections.api', () => ({
  sectionsApi: {
    getAll: vi.fn().mockResolvedValue({ data: { data: [] } }),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))

import { roomsApi } from '@/api/rooms.api'
import { sectionsApi } from '@/api/sections.api'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      discover: {
        title: 'Räume entdecken',
        searchPlaceholder: 'Nach Räumen suchen...',
        createRoom: 'Raum erstellen',
        join: 'Beitreten',
        joined: 'Beigetreten',
        created: 'Raum erstellt',
        members: 'Mitglieder',
        noRooms: 'Keine Räume gefunden.',
        tags: 'Tags',
        tagsPlaceholder: 'Tag eingeben',
        allSections: 'Alle Bereiche',
        allTypes: 'Alle Typen',
        otherRooms: 'Sonstige Räume',
        filterBySection: 'Bereich',
        filterByType: 'Typ',
      },
      rooms: {
        name: 'Name',
        description: 'Beschreibung',
        create: 'Erstellen',
        tags: 'Tags',
        closedRooms: 'Weitere Räume',
        requestJoin: 'Beitritt anfragen',
        joinRequestSent: 'Anfrage gesendet',
        joinRequestMessage: 'Beitrittsanfrage für {room}',
        joinRequestPlaceholder: 'Nachricht...',
        inviteOnly: 'Nur auf Einladung',
        types: {
          KLASSE: 'Klasse',
          GRUPPE: 'Gruppe',
          PROJEKT: 'Projekt',
          INTEREST: 'Interessengruppe',
          CUSTOM: 'Sonstige',
        },
        joinPolicies: {
          OPEN: 'Offen',
          REQUEST: 'Auf Anfrage',
          INVITE_ONLY: 'Nur auf Einladung',
        },
      },
      common: { cancel: 'Abbrechen', create: 'Erstellen', previous: 'Zurück', next: 'Weiter' },
    },
  },
})

const stubs = {
  InputText: { template: '<input class="input-stub" />', props: ['modelValue', 'placeholder'] },
  Button: {
    template: '<button class="button-stub" :data-severity="severity" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'text', 'severity', 'size', 'loading', 'disabled'],
    emits: ['click'],
  },
  Tag: { template: '<span class="tag-stub" :data-severity="severity">{{ value }}</span>', props: ['value', 'severity', 'size'] },
  Select: { template: '<select class="select-stub" />', props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder'] },
  Dialog: {
    template: '<div v-if="visible" class="dialog-stub"><slot /><slot name="footer" /></div>',
    props: ['visible', 'header', 'modal'],
  },
  Chips: { template: '<div class="chips-stub" />', props: ['modelValue'] },
  Textarea: { template: '<textarea class="textarea-stub" />', props: ['modelValue', 'placeholder', 'rows'] },
}

function mountWithBrowseRooms(browseRooms: any[] = []) {
  vi.mocked(roomsApi.browse).mockResolvedValue({
    data: { data: { content: browseRooms, totalPages: 1, last: true } },
  } as any)
  vi.mocked(sectionsApi.getAll).mockResolvedValue({
    data: { data: [] },
  } as any)

  const pinia = createPinia()
  return mount(DiscoverRoomsView, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('DiscoverRoomsView – JoinPolicy', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render join button for OPEN joinPolicy rooms in browse list', async () => {
    const wrapper = mountWithBrowseRooms([
      { id: 'r1', name: 'Open Room', type: 'GRUPPE', joinPolicy: 'OPEN', memberCount: 5, publicDescription: null, avatarUrl: null, sectionId: null, tags: [] },
    ])

    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    const buttons = wrapper.findAll('.button-stub')
    const joinButton = buttons.find(b => b.text() === 'Beitreten')
    expect(joinButton).toBeDefined()
  })

  it('should render request join button for REQUEST joinPolicy rooms', async () => {
    const wrapper = mountWithBrowseRooms([
      { id: 'r2', name: 'Request Room', type: 'PROJEKT', joinPolicy: 'REQUEST', memberCount: 10, publicDescription: 'A project', avatarUrl: null, sectionId: null, tags: [] },
    ])

    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    const buttons = wrapper.findAll('.button-stub')
    const requestButton = buttons.find(b => b.text() === 'Beitritt anfragen')
    expect(requestButton).toBeDefined()
  })

  it('should render invite-only tag for INVITE_ONLY joinPolicy rooms', async () => {
    const wrapper = mountWithBrowseRooms([
      { id: 'r3', name: 'Invite Room', type: 'KLASSE', joinPolicy: 'INVITE_ONLY', memberCount: 20, publicDescription: null, avatarUrl: null, sectionId: null, tags: [] },
    ])

    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    const tags = wrapper.findAll('.tag-stub')
    const inviteTag = tags.find(t => t.text() === 'Nur auf Einladung')
    expect(inviteTag).toBeDefined()
  })

  it('should show mixed joinPolicy rooms correctly', async () => {
    const wrapper = mountWithBrowseRooms([
      { id: 'r1', name: 'Open Room', type: 'GRUPPE', joinPolicy: 'OPEN', memberCount: 5, publicDescription: null, avatarUrl: null, sectionId: null, tags: [] },
      { id: 'r2', name: 'Request Room', type: 'PROJEKT', joinPolicy: 'REQUEST', memberCount: 10, publicDescription: null, avatarUrl: null, sectionId: null, tags: [] },
      { id: 'r3', name: 'Invite Room', type: 'KLASSE', joinPolicy: 'INVITE_ONLY', memberCount: 20, publicDescription: null, avatarUrl: null, sectionId: null, tags: [] },
    ])

    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    const text = wrapper.text()
    expect(text).toContain('Open Room')
    expect(text).toContain('Request Room')
    expect(text).toContain('Invite Room')
  })

  it('should call joinRoom when clicking join on OPEN room', async () => {
    vi.mocked(roomsApi.joinRoom).mockResolvedValue({} as any)

    const wrapper = mountWithBrowseRooms([
      { id: 'r1', name: 'Open Room', type: 'GRUPPE', joinPolicy: 'OPEN', memberCount: 5, publicDescription: null, avatarUrl: null, sectionId: null, tags: [] },
    ])

    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    const buttons = wrapper.findAll('.button-stub')
    const joinButton = buttons.find(b => b.text() === 'Beitreten')
    if (joinButton) {
      await joinButton.trigger('click')
      await wrapper.vm.$nextTick()
      expect(roomsApi.joinRoom).toHaveBeenCalledWith('r1')
    }
  })
})
