import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'

// --- mock auth store
const mockAuth = {
  user: { id: 'current-user-id', role: 'SUPERADMIN' } as any,
  isAdmin: true,
}

vi.mock('@/stores/auth', () => ({
  useAuthStore: vi.fn(() => mockAuth),
}))

// --- mock admin store
const mockAdmin = {
  config: {
    modules: { messaging: true },
  },
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
const mockGetById = vi.fn().mockResolvedValue({
  data: {
    data: {
      id: 'user-42',
      email: 'anna@example.com',
      firstName: 'Anna',
      lastName: 'Mueller',
      displayName: 'Anna Mueller',
      phone: '+49 170 1234567',
      avatarUrl: null,
      role: 'TEACHER',
      specialRoles: [],
      assignedRoles: [],
      active: true,
      darkMode: 'SYSTEM',
    },
  },
})

const mockGetUserRooms = vi.fn().mockResolvedValue({
  data: {
    data: [
      { id: 'room-1', name: 'Sonnengruppe', sectionId: 'sec-1', type: 'KLASSE', archived: false, memberCount: 10, joinPolicy: 'INVITE_ONLY', tags: [], description: null, publicDescription: null, avatarUrl: null, expiresAt: null },
      { id: 'room-2', name: 'Mondgruppe', sectionId: 'sec-2', type: 'KLASSE', archived: false, memberCount: 8, joinPolicy: 'INVITE_ONLY', tags: [], description: null, publicDescription: null, avatarUrl: null, expiresAt: null },
    ],
  },
})

const mockGetUserFamilies = vi.fn().mockResolvedValue({
  data: {
    data: [
      { id: 'fam-1', name: 'Familie Mueller', avatarUrl: null, hoursExempt: false, active: true, members: [] },
    ],
  },
})

vi.mock('@/api/users.api', () => ({
  usersApi: {
    getById: (...args: any[]) => mockGetById(...args),
    getUserRooms: (...args: any[]) => mockGetUserRooms(...args),
    getUserFamilies: (...args: any[]) => mockGetUserFamilies(...args),
  },
}))

// --- mock router
const mockPush = vi.fn()
const mockBack = vi.fn()
const mockRoute = { params: { userId: 'user-42' } }

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: mockPush, back: mockBack })),
  useRoute: vi.fn(() => mockRoute),
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: { de: {} },
})

const stubs = {
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  Avatar: { template: '<span class="avatar-stub">{{ label }}</span>', props: ['image', 'label', 'shape', 'size'] },
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'text', 'severity', 'size', 'loading'],
    emits: ['click'],
  },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity'] },
}

import UserProfileView from '@/views/UserProfileView.vue'

function mountView() {
  return mount(UserProfileView, {
    global: {
      plugins: [i18n],
      stubs,
    },
  })
}

describe('UserProfileView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()

    // Reset to admin viewing another user
    mockAuth.user = { id: 'current-user-id', role: 'SUPERADMIN' }
    mockAuth.isAdmin = true
    mockRoute.params = { userId: 'user-42' }
    mockAdmin.config = { modules: { messaging: true } }

    mockGetById.mockResolvedValue({
      data: {
        data: {
          id: 'user-42',
          email: 'anna@example.com',
          firstName: 'Anna',
          lastName: 'Mueller',
          displayName: 'Anna Mueller',
          phone: '+49 170 1234567',
          avatarUrl: null,
          role: 'TEACHER',
          specialRoles: [],
          assignedRoles: [],
          active: true,
          darkMode: 'SYSTEM',
        },
      },
    })

    mockGetUserRooms.mockResolvedValue({
      data: {
        data: [
          { id: 'room-1', name: 'Sonnengruppe' },
          { id: 'room-2', name: 'Mondgruppe' },
        ],
      },
    })

    mockGetUserFamilies.mockResolvedValue({
      data: {
        data: [
          { id: 'fam-1', name: 'Familie Mueller' },
        ],
      },
    })
  })

  it('shows loading spinner initially', () => {
    const w = mountView()
    expect(w.find('.loading-stub').exists()).toBe(true)
  })

  it('calls usersApi.getById on mount with userId from route', async () => {
    mountView()
    await flushPromises()
    expect(mockGetById).toHaveBeenCalledWith('user-42')
  })

  it('fetches rooms and families when user is admin', async () => {
    mountView()
    await flushPromises()
    expect(mockGetUserRooms).toHaveBeenCalledWith('user-42')
    expect(mockGetUserFamilies).toHaveBeenCalledWith('user-42')
  })

  it('does not fetch rooms/families when user is not admin', async () => {
    mockAuth.isAdmin = false
    mountView()
    await flushPromises()
    expect(mockGetUserRooms).not.toHaveBeenCalled()
    expect(mockGetUserFamilies).not.toHaveBeenCalled()
  })

  it('displays user name after loading', async () => {
    const w = mountView()
    await flushPromises()
    expect(w.text()).toContain('Anna Mueller')
  })

  it('displays user email', async () => {
    const w = mountView()
    await flushPromises()
    expect(w.text()).toContain('anna@example.com')
  })

  it('displays user phone when available', async () => {
    const w = mountView()
    await flushPromises()
    expect(w.text()).toContain('+49 170 1234567')
  })

  it('does not show phone section when phone is null', async () => {
    mockGetById.mockResolvedValueOnce({
      data: {
        data: {
          id: 'user-42',
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
          darkMode: 'SYSTEM',
        },
      },
    })
    const w = mountView()
    await flushPromises()
    expect(w.find('.profile-detail').exists()).toBe(false)
  })

  it('shows role tag', async () => {
    const w = mountView()
    await flushPromises()
    expect(w.find('.tag-stub').exists()).toBe(true)
  })

  it('shows initials avatar when no avatarUrl', async () => {
    const w = mountView()
    await flushPromises()
    const avatars = w.findAll('.avatar-stub')
    // The one with the initials label
    const initialsAvatar = avatars.find(a => a.text() === 'AM')
    expect(initialsAvatar).toBeTruthy()
  })

  it('shows image avatar when avatarUrl is set', async () => {
    mockGetById.mockResolvedValueOnce({
      data: {
        data: {
          id: 'user-42',
          email: 'anna@example.com',
          firstName: 'Anna',
          lastName: 'Mueller',
          displayName: 'Anna Mueller',
          phone: null,
          avatarUrl: 'https://example.com/avatar.jpg',
          role: 'TEACHER',
          specialRoles: [],
          assignedRoles: [],
          active: true,
          darkMode: 'SYSTEM',
        },
      },
    })
    const w = mountView()
    await flushPromises()
    // When avatarUrl is set, the v-if branch renders the image avatar (no label/initials)
    const avatars = w.findAll('.avatar-stub')
    // The first avatar should NOT contain initials text (it's the image version)
    expect(avatars[0].text()).toBe('')
  })

  it('shows rooms section for admin when rooms are present', async () => {
    const w = mountView()
    await flushPromises()
    expect(w.text()).toContain('Sonnengruppe')
    expect(w.text()).toContain('Mondgruppe')
  })

  it('shows families section for admin when families are present', async () => {
    const w = mountView()
    await flushPromises()
    expect(w.text()).toContain('Familie Mueller')
  })

  it('hides rooms section when user is not admin', async () => {
    mockAuth.isAdmin = false
    const w = mountView()
    await flushPromises()
    expect(w.text()).not.toContain('Sonnengruppe')
  })

  it('shows send message button when messaging is enabled and not own profile', async () => {
    const w = mountView()
    await flushPromises()
    const buttons = w.findAll('.button-stub')
    const msgButton = buttons.find(b => b.text().includes('directory.sendMessage'))
    expect(msgButton).toBeTruthy()
  })

  it('hides message button when messaging is disabled', async () => {
    mockAdmin.config = { modules: { messaging: false } }
    const w = mountView()
    await flushPromises()
    const buttons = w.findAll('.button-stub')
    const msgButton = buttons.find(b => b.text().includes('directory.sendMessage'))
    expect(msgButton).toBeUndefined()
  })

  it('hides message button when viewing own profile', async () => {
    mockAuth.user = { id: 'user-42', role: 'SUPERADMIN' }
    const w = mountView()
    await flushPromises()
    const buttons = w.findAll('.button-stub')
    const msgButton = buttons.find(b => b.text().includes('directory.sendMessage'))
    expect(msgButton).toBeUndefined()
  })

  it('starts conversation when message button is clicked', async () => {
    const w = mountView()
    await flushPromises()
    const buttons = w.findAll('.button-stub')
    const msgButton = buttons.find(b => b.text().includes('directory.sendMessage'))
    await msgButton!.trigger('click')
    await flushPromises()
    expect(mockMessaging.startDirectConversation).toHaveBeenCalledWith('user-42')
    expect(mockPush).toHaveBeenCalledWith({ name: 'messages', params: { conversationId: 'conv-1' } })
  })

  it('navigates back when back button is clicked', async () => {
    const w = mountView()
    await flushPromises()
    const buttons = w.findAll('.button-stub')
    const backButton = buttons.find(b => b.text().includes('common.back'))
    await backButton!.trigger('click')
    expect(mockBack).toHaveBeenCalled()
  })

  it('navigates to room detail when room is clicked', async () => {
    const w = mountView()
    await flushPromises()
    const listItems = w.findAll('.list-item')
    // Rooms should be first section's items
    await listItems[0].trigger('click')
    expect(mockPush).toHaveBeenCalledWith({ name: 'room-detail', params: { id: 'room-1' } })
  })

  it('shows not-found state when user fetch fails', async () => {
    mockGetById.mockRejectedValueOnce(new Error('Not found'))
    const w = mountView()
    await flushPromises()
    expect(w.find('.not-found').exists()).toBe(true)
  })

  it('hides loading spinner after data loads', async () => {
    const w = mountView()
    await flushPromises()
    expect(w.find('.loading-stub').exists()).toBe(false)
  })

  it('renders role tag for TEACHER role', async () => {
    const w = mountView()
    await flushPromises()
    const tag = w.find('.tag-stub')
    expect(tag.exists()).toBe(true)
    // i18n key passthrough: directory.roles.TEACHER
    expect(tag.text()).toContain('directory.roles.TEACHER')
  })

  it('renders role tag for different roles', async () => {
    mockGetById.mockResolvedValueOnce({
      data: {
        data: {
          id: 'user-42',
          email: 'test@example.com',
          firstName: 'Test',
          lastName: 'User',
          displayName: 'Test User',
          phone: null,
          avatarUrl: null,
          role: 'PARENT',
          specialRoles: [],
          assignedRoles: [],
          active: true,
          darkMode: 'SYSTEM',
        },
      },
    })
    const w = mountView()
    await flushPromises()
    const tag = w.find('.tag-stub')
    expect(tag.text()).toContain('directory.roles.PARENT')
  })

  it('handles conversation start failure gracefully', async () => {
    mockMessaging.startDirectConversation.mockRejectedValueOnce(new Error('Not allowed'))
    const w = mountView()
    await flushPromises()
    const buttons = w.findAll('.button-stub')
    const msgButton = buttons.find(b => b.text().includes('directory.sendMessage'))
    await msgButton!.trigger('click')
    await flushPromises()
    // Should not navigate
    expect(mockPush).not.toHaveBeenCalled()
  })

  it('does not start conversation when viewing own profile', async () => {
    mockAuth.user = { id: 'user-42', role: 'SUPERADMIN' }
    mockAdmin.config = { modules: { messaging: true } }
    const w = mountView()
    await flushPromises()
    // Even if we somehow call startConversation, it should bail
    expect(mockMessaging.startDirectConversation).not.toHaveBeenCalled()
  })

  it('handles rooms fetch failure gracefully', async () => {
    mockGetUserRooms.mockRejectedValueOnce(new Error('forbidden'))
    const w = mountView()
    await flushPromises()
    // Should still render the user profile
    expect(w.text()).toContain('Anna Mueller')
  })

  it('handles families fetch failure gracefully', async () => {
    mockGetUserFamilies.mockRejectedValueOnce(new Error('forbidden'))
    const w = mountView()
    await flushPromises()
    // Should still render the user profile
    expect(w.text()).toContain('Anna Mueller')
  })
})
