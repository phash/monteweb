import { describe, it, expect, beforeEach, vi } from 'vitest'

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

import { usePushNotifications } from '@/composables/usePushNotifications'

describe('usePushNotifications', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should return expected properties', () => {
    const result = usePushNotifications()
    expect(result).toHaveProperty('isSupported')
    expect(result).toHaveProperty('isSubscribed')
    expect(result).toHaveProperty('permission')
    expect(result).toHaveProperty('subscribe')
    expect(result).toHaveProperty('unsubscribe')
    expect(result).toHaveProperty('checkSubscription')
  })

  it('should detect push support based on browser APIs', () => {
    const { isSupported } = usePushNotifications()
    // jsdom doesn't have serviceWorker or PushManager
    expect(isSupported.value).toBe(false)
  })

  it('should return false from subscribe when not supported', async () => {
    const { subscribe } = usePushNotifications()
    const result = await subscribe()
    expect(result).toBe(false)
  })
})
