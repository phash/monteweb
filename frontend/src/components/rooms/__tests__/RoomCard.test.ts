import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import RoomCard from '@/components/rooms/RoomCard.vue'
import type { RoomInfo } from '@/types/room'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      rooms: {
        members: 'Mitglieder',
        types: {
          KLASSE: 'Klasse',
          GRUPPE: 'Gruppe',
          PROJEKT: 'Projekt',
          INTEREST: 'Interessengruppe',
          CUSTOM: 'Benutzerdefiniert',
        },
      },
    },
  },
})

const stubs = {
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity'] },
  'router-link': { template: '<a class="router-link-stub"><slot /></a>', props: ['to'] },
}

const mockRoom: RoomInfo = {
  id: 'room-1',
  name: 'Klasse 3a',
  description: 'Die beste Klasse',
  publicDescription: null,
  avatarUrl: null,
  type: 'KLASSE',
  sectionId: 'sec-1',
  archived: false,
  memberCount: 25,
  discoverable: true,
  expiresAt: null,
  tags: [],
}

function mountRoomCard(room: Partial<RoomInfo> = {}) {
  const pinia = createPinia()
  return mount(RoomCard, {
    props: { room: { ...mockRoom, ...room } },
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('RoomCard', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render component', () => {
    const wrapper = mountRoomCard()
    expect(wrapper.exists()).toBe(true)
  })

  it('should display room name', () => {
    const wrapper = mountRoomCard()
    expect(wrapper.find('.room-name').text()).toBe('Klasse 3a')
  })

  it('should display room description', () => {
    const wrapper = mountRoomCard()
    expect(wrapper.find('.room-desc').text()).toBe('Die beste Klasse')
  })

  it('should not display description when null', () => {
    const wrapper = mountRoomCard({ description: null })
    expect(wrapper.find('.room-desc').exists()).toBe(false)
  })

  it('should display member count', () => {
    const wrapper = mountRoomCard()
    expect(wrapper.find('.room-members').text()).toContain('25')
    expect(wrapper.find('.room-members').text()).toContain('Mitglieder')
  })

  it('should display room type tag', () => {
    const wrapper = mountRoomCard()
    expect(wrapper.find('.tag-stub').text()).toBe('Klasse')
  })

  it('should display GRUPPE type correctly', () => {
    const wrapper = mountRoomCard({ type: 'GRUPPE' })
    expect(wrapper.find('.tag-stub').text()).toBe('Gruppe')
  })

  it('should display PROJEKT type correctly', () => {
    const wrapper = mountRoomCard({ type: 'PROJEKT' })
    expect(wrapper.find('.tag-stub').text()).toBe('Projekt')
  })

  it('should show default icon when no avatar', () => {
    const wrapper = mountRoomCard({ avatarUrl: null })
    expect(wrapper.find('.pi-home').exists()).toBe(true)
    expect(wrapper.find('.room-card-avatar-img').exists()).toBe(false)
  })

  it('should show avatar image when avatarUrl is set', () => {
    const wrapper = mountRoomCard({ avatarUrl: 'https://example.com/avatar.png' })
    expect(wrapper.find('.room-card-avatar-img').exists()).toBe(true)
    expect(wrapper.find('.pi-home').exists()).toBe(false)
  })

  it('should link to room detail', () => {
    const wrapper = mountRoomCard()
    expect(wrapper.find('.router-link-stub').exists()).toBe(true)
  })
})
