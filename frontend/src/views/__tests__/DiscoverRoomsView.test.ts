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
  },
}))

vi.mock('@/api/sections.api', () => ({
  sectionsApi: {
    getAll: vi.fn().mockResolvedValue({ data: { data: [] } }),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))

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
        allRooms: 'Alle Räume',
        noRooms: 'Keine Räume gefunden.',
        allSections: 'Alle Bereiche',
        allTypes: 'Alle Typen',
        otherRooms: 'Sonstige Räume',
        filterBySection: 'Bereich',
        filterByType: 'Typ',
        members: 'Mitglieder',
        tags: 'Tags',
        tagsPlaceholder: 'Tag eingeben',
      },
      rooms: {
        name: 'Name',
        description: 'Beschreibung',
        create: 'Erstellen',
        tags: 'Tags',
        requestJoin: 'Beitritt anfragen',
        joinRequestSent: 'Anfrage gesendet',
        joinRequestMessage: 'Beitrittsanfrage für {room}',
        joinRequestPlaceholder: 'Nachricht an die Leitung (optional)...',
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
      common: { cancel: 'Abbrechen', save: 'Speichern', loading: 'Laden...', create: 'Erstellen' },
    },
  },
})

const stubs = {
  InputText: { template: '<input class="input-stub" />', props: ['modelValue', 'placeholder'] },
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'text', 'severity', 'size', 'loading', 'disabled'],
    emits: ['click'],
  },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity', 'size'] },
  Select: { template: '<select class="select-stub" />', props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder'] },
  Dialog: {
    template: '<div v-if="visible" class="dialog-stub"><slot /><slot name="footer" /></div>',
    props: ['visible', 'header', 'modal'],
  },
  Chips: { template: '<div class="chips-stub" />', props: ['modelValue'] },
  Textarea: { template: '<textarea class="textarea-stub" />', props: ['modelValue', 'placeholder', 'rows'] },
}

function mountDiscover() {
  const pinia = createPinia()
  return mount(DiscoverRoomsView, {
    global: {
      plugins: [i18n, pinia],
      stubs,
    },
  })
}

describe('DiscoverRoomsView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render component', () => {
    const wrapper = mountDiscover()
    expect(wrapper.exists()).toBe(true)
  })

  it('should render search input', () => {
    const wrapper = mountDiscover()
    expect(wrapper.find('.input-stub').exists()).toBe(true)
  })

  it('should render create room button', () => {
    const wrapper = mountDiscover()
    expect(wrapper.text()).toContain('Raum erstellen')
  })
})
