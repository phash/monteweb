import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import NotificationBell from '@/components/common/NotificationBell.vue'

vi.mock('@/api/notifications.api', () => ({
  notificationsApi: {
    getNotifications: vi.fn().mockResolvedValue({ data: { data: { content: [] } } }),
    getUnreadCount: vi.fn().mockResolvedValue({ data: { data: { count: 0 } } }),
    markAsRead: vi.fn().mockResolvedValue({}),
    markAllAsRead: vi.fn().mockResolvedValue({}),
    deleteNotification: vi.fn().mockResolvedValue({}),
  },
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      notifications: {
        title: 'Benachrichtigungen',
        markAllRead: 'Alle gelesen',
        noNotifications: 'Keine Benachrichtigungen',
        justNow: 'Gerade eben',
        minutesAgo: 'vor {n}m',
        hoursAgo: 'vor {n}h',
        daysAgo: 'vor {n}d',
        bellWithCount: '{count} ungelesene Benachrichtigungen',
      },
    },
  },
})

const stubs = {
  Badge: { template: '<span class="badge-stub">{{ value }}</span>', props: ['value', 'severity'] },
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\', $event)">{{ label }}</button>',
    props: ['label', 'icon', 'text', 'rounded', 'severity', 'size', 'ariaLabel'],
    emits: ['click'],
  },
  Popover: {
    template: '<div class="popover-stub"><slot /></div>',
    methods: { toggle() {}, hide() {} },
  },
}

function mountBell() {
  return mount(NotificationBell, {
    global: {
      plugins: [i18n, createPinia()],
      stubs,
    },
  })
}

describe('NotificationBell', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.useFakeTimers()
  })

  it('should render bell button', () => {
    const wrapper = mountBell()
    expect(wrapper.find('.button-stub').exists()).toBe(true)
    vi.useRealTimers()
  })

  it('should not show badge when unread count is 0', () => {
    const wrapper = mountBell()
    expect(wrapper.find('.badge-stub').exists()).toBe(false)
    vi.useRealTimers()
  })

  it('should render popover', () => {
    const wrapper = mountBell()
    expect(wrapper.find('.popover-stub').exists()).toBe(true)
    vi.useRealTimers()
  })

  it('should show empty state when no notifications', () => {
    const wrapper = mountBell()
    expect(wrapper.find('.empty-state').exists()).toBe(true)
    vi.useRealTimers()
  })
})
