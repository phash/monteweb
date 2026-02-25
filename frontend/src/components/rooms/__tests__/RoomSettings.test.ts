import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'

// --- mock rooms API
const mockUpdateSettings = vi.fn().mockResolvedValue({ data: { data: {} } })

vi.mock('@/api/rooms.api', () => ({
  roomsApi: {
    updateSettings: (...args: any[]) => mockUpdateSettings(...args),
    getMine: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getById: vi.fn().mockResolvedValue({ data: { data: {} } }),
  },
}))

// --- mock toast
const mockToastAdd = vi.fn()
vi.mock('primevue/usetoast', () => ({
  useToast: vi.fn(() => ({ add: mockToastAdd })),
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: { de: {} },
})

const stubs = {
  ToggleSwitch: {
    template: '<input type="checkbox" class="toggleswitch-stub" :checked="modelValue" @change="$emit(\'update:modelValue\', !modelValue)" />',
    props: ['modelValue'],
    emits: ['update:modelValue'],
  },
  Select: {
    template: '<select class="select-stub" :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><option v-for="o in options" :key="o.value" :value="o.value">{{ o.label }}</option></select>',
    props: ['modelValue', 'options', 'optionLabel', 'optionValue'],
    emits: ['update:modelValue'],
  },
  Button: {
    template: '<button class="button-stub" :disabled="loading" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'loading'],
    emits: ['click'],
  },
}

import RoomSettings from '../RoomSettings.vue'
import { useRoomsStore } from '@/stores/rooms'

function mountComponent(props = {}, roomSettings?: any) {
  const pinia = createPinia()
  setActivePinia(pinia)

  // Pre-populate the rooms store with currentRoom if settings provided
  const roomsStore = useRoomsStore()
  if (roomSettings) {
    roomsStore.currentRoom = {
      id: 'room-1',
      name: 'Sonnengruppe',
      description: null,
      publicDescription: null,
      avatarUrl: null,
      type: 'KLASSE',
      sectionId: 'sec-1',
      archived: false,
      memberCount: 10,
      joinPolicy: 'INVITE_ONLY',
      expiresAt: null,
      tags: [],
      settings: roomSettings,
      createdBy: null,
      createdAt: null,
      members: [],
    } as any
  }

  return mount(RoomSettings, {
    props: { roomId: 'room-1', ...props },
    global: {
      plugins: [i18n, pinia],
      stubs,
    },
  })
}

describe('RoomSettings', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockUpdateSettings.mockResolvedValue({ data: { data: {} } })
  })

  it('renders the settings title', () => {
    const w = mountComponent()
    expect(w.text()).toContain('rooms.settings.title')
  })

  it('renders module toggles (chat, files, parentSpace)', () => {
    const w = mountComponent()
    const toggles = w.findAll('.toggleswitch-stub')
    expect(toggles.length).toBeGreaterThanOrEqual(3)
  })

  it('renders visibility select', () => {
    const w = mountComponent()
    const selects = w.findAll('.select-stub')
    expect(selects.length).toBeGreaterThanOrEqual(1)
  })

  it('renders discussion mode select', () => {
    const w = mountComponent()
    const selects = w.findAll('.select-stub')
    expect(selects.length).toBeGreaterThanOrEqual(2)
  })

  it('renders save button', () => {
    const w = mountComponent()
    const btn = w.find('.button-stub')
    expect(btn.exists()).toBe(true)
    expect(btn.text()).toContain('common.save')
  })

  it('loads settings from currentRoom on mount and saves them', async () => {
    const w = mountComponent({}, {
      chatEnabled: false,
      filesEnabled: true,
      parentSpaceEnabled: true,
      visibility: 'ALL',
      discussionMode: 'ANNOUNCEMENTS_ONLY',
      allowMemberThreadCreation: false,
      childDiscussionEnabled: true,
    })

    // Verify settings were loaded by clicking save and checking what was sent
    const btn = w.find('.button-stub')
    await btn.trigger('click')
    await flushPromises()
    expect(mockUpdateSettings).toHaveBeenCalledWith('room-1', {
      chatEnabled: false,
      filesEnabled: true,
      parentSpaceEnabled: true,
      visibility: 'ALL',
      discussionMode: 'ANNOUNCEMENTS_ONLY',
      allowMemberThreadCreation: false,
      childDiscussionEnabled: true,
    })
  })

  it('uses default settings when currentRoom has no settings', () => {
    const w = mountComponent()
    const toggles = w.findAll('.toggleswitch-stub')
    // Default: chatEnabled=true, filesEnabled=true, parentSpaceEnabled=false
    expect((toggles[0].element as HTMLInputElement).checked).toBe(true)
    expect((toggles[1].element as HTMLInputElement).checked).toBe(true)
    expect((toggles[2].element as HTMLInputElement).checked).toBe(false)
  })

  it('calls roomsApi.updateSettings on save', async () => {
    const w = mountComponent()
    const btn = w.find('.button-stub')
    await btn.trigger('click')
    await flushPromises()
    expect(mockUpdateSettings).toHaveBeenCalledWith('room-1', {
      chatEnabled: true,
      filesEnabled: true,
      parentSpaceEnabled: false,
      visibility: 'MEMBERS_ONLY',
      discussionMode: 'FULL',
      allowMemberThreadCreation: true,
      childDiscussionEnabled: false,
    })
  })

  it('shows success toast after saving', async () => {
    const w = mountComponent()
    const btn = w.find('.button-stub')
    await btn.trigger('click')
    await flushPromises()
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'success' })
    )
  })

  it('shows error toast when save fails', async () => {
    mockUpdateSettings.mockRejectedValueOnce(new Error('Server error'))
    const w = mountComponent()
    const btn = w.find('.button-stub')
    await btn.trigger('click')
    await flushPromises()
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'error' })
    )
  })

  it('toggles chatEnabled when toggle is changed', async () => {
    const w = mountComponent()
    const toggles = w.findAll('.toggleswitch-stub')
    await toggles[0].trigger('change')
    await flushPromises()

    // Save and check what was sent
    const btn = w.find('.button-stub')
    await btn.trigger('click')
    await flushPromises()
    expect(mockUpdateSettings).toHaveBeenCalledWith('room-1',
      expect.objectContaining({ chatEnabled: false })
    )
  })

  it('toggles filesEnabled when toggle is changed', async () => {
    const w = mountComponent()
    const toggles = w.findAll('.toggleswitch-stub')
    await toggles[1].trigger('change')
    await flushPromises()

    const btn = w.find('.button-stub')
    await btn.trigger('click')
    await flushPromises()
    expect(mockUpdateSettings).toHaveBeenCalledWith('room-1',
      expect.objectContaining({ filesEnabled: false })
    )
  })

  it('toggles parentSpaceEnabled when toggle is changed', async () => {
    const w = mountComponent()
    const toggles = w.findAll('.toggleswitch-stub')
    await toggles[2].trigger('change')
    await flushPromises()

    const btn = w.find('.button-stub')
    await btn.trigger('click')
    await flushPromises()
    expect(mockUpdateSettings).toHaveBeenCalledWith('room-1',
      expect.objectContaining({ parentSpaceEnabled: true })
    )
  })

  it('renders section headers for modules, access, discussions', () => {
    const w = mountComponent()
    expect(w.text()).toContain('rooms.settings.modules')
    expect(w.text()).toContain('rooms.settings.access')
    expect(w.text()).toContain('discussions.title')
  })

  it('renders setting labels for all options', () => {
    const w = mountComponent()
    expect(w.text()).toContain('chat.title')
    expect(w.text()).toContain('files.title')
    expect(w.text()).toContain('rooms.settings.parentSpace')
    expect(w.text()).toContain('rooms.settings.visibility')
    expect(w.text()).toContain('rooms.settings.discussionMode')
    expect(w.text()).toContain('rooms.settings.allowThreadCreation')
    expect(w.text()).toContain('rooms.settings.childDiscussion')
  })

  it('renders hint texts for toggles', () => {
    const w = mountComponent()
    expect(w.text()).toContain('rooms.settings.chatHint')
    expect(w.text()).toContain('rooms.settings.filesHint')
    expect(w.text()).toContain('rooms.settings.parentSpaceHint')
    expect(w.text()).toContain('rooms.settings.allowThreadCreationHint')
    expect(w.text()).toContain('rooms.settings.childDiscussionHint')
  })

  it('saves with custom roomId prop', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)

    const w = mount(RoomSettings, {
      props: { roomId: 'room-99' },
      global: {
        plugins: [i18n, pinia],
        stubs,
      },
    })

    const btn = w.find('.button-stub')
    await btn.trigger('click')
    await flushPromises()
    expect(mockUpdateSettings).toHaveBeenCalledWith('room-99', expect.any(Object))
  })

  it('fetches room after successful save', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const roomsStore = useRoomsStore()
    const fetchRoomSpy = vi.spyOn(roomsStore, 'fetchRoom')

    const w = mount(RoomSettings, {
      props: { roomId: 'room-1' },
      global: {
        plugins: [i18n, pinia],
        stubs,
      },
    })

    const btn = w.find('.button-stub')
    await btn.trigger('click')
    await flushPromises()
    expect(fetchRoomSpy).toHaveBeenCalledWith('room-1')
  })
})
