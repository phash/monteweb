import { describe, it, expect, beforeEach, vi } from 'vitest'

vi.mock('@/api/notifications.api', () => ({
  notificationsApi: {
    getPushPublicKey: vi.fn(),
    pushSubscribe: vi.fn(),
    pushUnsubscribe: vi.fn(),
  },
}))

import { notificationsApi } from '@/api/notifications.api'
import { usePushNotifications } from '@/composables/usePushNotifications'

describe('usePushNotifications extended', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // ==================== Return shape ====================

  it('should return isSupported ref', () => {
    const { isSupported } = usePushNotifications()
    expect(isSupported).toBeDefined()
    expect(typeof isSupported.value).toBe('boolean')
  })

  it('should return isSubscribed ref', () => {
    const { isSubscribed } = usePushNotifications()
    expect(isSubscribed).toBeDefined()
    expect(typeof isSubscribed.value).toBe('boolean')
  })

  it('should return permission ref', () => {
    const { permission } = usePushNotifications()
    expect(permission).toBeDefined()
  })

  it('should return subscribe function', () => {
    const { subscribe } = usePushNotifications()
    expect(typeof subscribe).toBe('function')
  })

  it('should return unsubscribe function', () => {
    const { unsubscribe } = usePushNotifications()
    expect(typeof unsubscribe).toBe('function')
  })

  it('should return checkSubscription function', () => {
    const { checkSubscription } = usePushNotifications()
    expect(typeof checkSubscription).toBe('function')
  })

  // ==================== isSupported detection ====================

  it('should detect push support based on serviceWorker and PushManager', () => {
    const { isSupported } = usePushNotifications()
    // jsdom lacks serviceWorker and PushManager
    expect(isSupported.value).toBe(false)
  })

  // ==================== subscribe when not supported ====================

  it('should return false from subscribe when not supported', async () => {
    const { subscribe } = usePushNotifications()
    const result = await subscribe()
    expect(result).toBe(false)
  })

  it('should not call any API when subscribing without support', async () => {
    const { subscribe } = usePushNotifications()
    await subscribe()
    expect(notificationsApi.getPushPublicKey).not.toHaveBeenCalled()
    expect(notificationsApi.pushSubscribe).not.toHaveBeenCalled()
  })

  // ==================== unsubscribe when not supported ====================

  it('should do nothing when unsubscribing without support', async () => {
    const { unsubscribe } = usePushNotifications()
    await unsubscribe()
    expect(notificationsApi.pushUnsubscribe).not.toHaveBeenCalled()
  })

  // ==================== checkSubscription when not supported ====================

  it('should do nothing when checking subscription without support', async () => {
    const { checkSubscription } = usePushNotifications()
    await checkSubscription()
    // Should exit early without accessing navigator.serviceWorker
  })

  // ==================== subscribe with supported browser ====================

  it('should call getPushPublicKey when permission is granted', async () => {
    // Mock browser APIs for this test
    const mockSubscription = {
      toJSON: () => ({
        endpoint: 'https://fcm.example.com/test',
        keys: { p256dh: 'key1', auth: 'key2' },
      }),
    }
    const mockPushManager = {
      subscribe: vi.fn().mockResolvedValue(mockSubscription),
      getSubscription: vi.fn().mockResolvedValue(null),
    }
    const mockRegistration = { pushManager: mockPushManager }

    // Override global mocks temporarily
    const origServiceWorker = (navigator as any).serviceWorker
    const origPushManager = (window as any).PushManager
    const origNotification = globalThis.Notification

    Object.defineProperty(navigator, 'serviceWorker', {
      value: { ready: Promise.resolve(mockRegistration) },
      configurable: true,
    })
    Object.defineProperty(window, 'PushManager', {
      value: class PushManager {},
      configurable: true,
    })
    Object.defineProperty(globalThis, 'Notification', {
      value: {
        permission: 'default',
        requestPermission: vi.fn().mockResolvedValue('granted'),
      },
      configurable: true,
    })

    vi.mocked(notificationsApi.getPushPublicKey).mockResolvedValue({
      data: { data: { publicKey: 'BNTest' } },
    } as any)
    vi.mocked(notificationsApi.pushSubscribe).mockResolvedValue({} as any)

    const { subscribe, isSubscribed } = usePushNotifications()
    const result = await subscribe()

    expect(result).toBe(true)
    expect(notificationsApi.getPushPublicKey).toHaveBeenCalled()
    expect(notificationsApi.pushSubscribe).toHaveBeenCalledWith({
      endpoint: 'https://fcm.example.com/test',
      p256dh: 'key1',
      auth: 'key2',
    })
    expect(isSubscribed.value).toBe(true)

    // Restore
    Object.defineProperty(navigator, 'serviceWorker', {
      value: origServiceWorker,
      configurable: true,
    })
    Object.defineProperty(window, 'PushManager', {
      value: origPushManager,
      configurable: true,
    })
    Object.defineProperty(globalThis, 'Notification', {
      value: origNotification,
      configurable: true,
    })
  })

  it('should return false when permission is denied', async () => {
    const origServiceWorker = (navigator as any).serviceWorker
    const origPushManager = (window as any).PushManager
    const origNotification = globalThis.Notification

    Object.defineProperty(navigator, 'serviceWorker', {
      value: { ready: Promise.resolve({ pushManager: { subscribe: vi.fn() } }) },
      configurable: true,
    })
    Object.defineProperty(window, 'PushManager', {
      value: class PushManager {},
      configurable: true,
    })
    Object.defineProperty(globalThis, 'Notification', {
      value: {
        permission: 'default',
        requestPermission: vi.fn().mockResolvedValue('denied'),
      },
      configurable: true,
    })

    const { subscribe } = usePushNotifications()
    const result = await subscribe()

    expect(result).toBe(false)
    expect(notificationsApi.getPushPublicKey).not.toHaveBeenCalled()

    // Restore
    Object.defineProperty(navigator, 'serviceWorker', {
      value: origServiceWorker,
      configurable: true,
    })
    Object.defineProperty(window, 'PushManager', {
      value: origPushManager,
      configurable: true,
    })
    Object.defineProperty(globalThis, 'Notification', {
      value: origNotification,
      configurable: true,
    })
  })

  // ==================== unsubscribe with supported browser ====================

  it('should unsubscribe and call API', async () => {
    const mockSubscription = {
      endpoint: 'https://fcm.example.com/test',
      unsubscribe: vi.fn().mockResolvedValue(true),
    }
    const mockPushManager = {
      getSubscription: vi.fn().mockResolvedValue(mockSubscription),
    }
    const mockRegistration = { pushManager: mockPushManager }

    const origServiceWorker = (navigator as any).serviceWorker
    const origPushManager = (window as any).PushManager

    Object.defineProperty(navigator, 'serviceWorker', {
      value: { ready: Promise.resolve(mockRegistration) },
      configurable: true,
    })
    Object.defineProperty(window, 'PushManager', {
      value: class PushManager {},
      configurable: true,
    })

    vi.mocked(notificationsApi.pushUnsubscribe).mockResolvedValue({} as any)

    const { unsubscribe, isSubscribed } = usePushNotifications()
    await unsubscribe()

    expect(notificationsApi.pushUnsubscribe).toHaveBeenCalledWith({
      endpoint: 'https://fcm.example.com/test',
    })
    expect(mockSubscription.unsubscribe).toHaveBeenCalled()
    expect(isSubscribed.value).toBe(false)

    // Restore
    Object.defineProperty(navigator, 'serviceWorker', {
      value: origServiceWorker,
      configurable: true,
    })
    Object.defineProperty(window, 'PushManager', {
      value: origPushManager,
      configurable: true,
    })
  })

  it('should handle unsubscribe when no subscription exists', async () => {
    const mockPushManager = {
      getSubscription: vi.fn().mockResolvedValue(null),
    }
    const mockRegistration = { pushManager: mockPushManager }

    const origServiceWorker = (navigator as any).serviceWorker
    const origPushManager = (window as any).PushManager

    Object.defineProperty(navigator, 'serviceWorker', {
      value: { ready: Promise.resolve(mockRegistration) },
      configurable: true,
    })
    Object.defineProperty(window, 'PushManager', {
      value: class PushManager {},
      configurable: true,
    })

    const { unsubscribe, isSubscribed } = usePushNotifications()
    await unsubscribe()

    expect(notificationsApi.pushUnsubscribe).not.toHaveBeenCalled()
    expect(isSubscribed.value).toBe(false)

    // Restore
    Object.defineProperty(navigator, 'serviceWorker', {
      value: origServiceWorker,
      configurable: true,
    })
    Object.defineProperty(window, 'PushManager', {
      value: origPushManager,
      configurable: true,
    })
  })

  // ==================== urlBase64ToUint8Array logic ====================

  it('should correctly convert base64 URL-safe string', () => {
    // Test the logic directly
    function urlBase64ToUint8Array(base64String: string): Uint8Array {
      const padding = '='.repeat((4 - (base64String.length % 4)) % 4)
      const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/')
      const rawData = window.atob(base64)
      const outputArray = new Uint8Array(rawData.length)
      for (let i = 0; i < rawData.length; ++i) {
        outputArray[i] = rawData.charCodeAt(i)
      }
      return outputArray
    }

    // Test with a known base64 URL-safe string
    const result = urlBase64ToUint8Array('AAEC') // Should decode to [0, 1, 2]
    expect(result).toBeInstanceOf(Uint8Array)
    expect(result.length).toBe(3)
    expect(result[0]).toBe(0)
    expect(result[1]).toBe(1)
    expect(result[2]).toBe(2)
  })

  it('should handle URL-safe characters in base64 conversion', () => {
    function urlBase64ToUint8Array(base64String: string): Uint8Array {
      const padding = '='.repeat((4 - (base64String.length % 4)) % 4)
      const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/')
      const rawData = window.atob(base64)
      const outputArray = new Uint8Array(rawData.length)
      for (let i = 0; i < rawData.length; ++i) {
        outputArray[i] = rawData.charCodeAt(i)
      }
      return outputArray
    }

    // URL-safe base64 uses - instead of + and _ instead of /
    const result = urlBase64ToUint8Array('AB-C_w')
    expect(result).toBeInstanceOf(Uint8Array)
    expect(result.length).toBeGreaterThan(0)
  })

  it('should add correct padding', () => {
    // Padding logic: (4 - (length % 4)) % 4
    expect((4 - (1 % 4)) % 4).toBe(3) // 1 char -> 3 padding
    expect((4 - (2 % 4)) % 4).toBe(2) // 2 chars -> 2 padding
    expect((4 - (3 % 4)) % 4).toBe(1) // 3 chars -> 1 padding
    expect((4 - (4 % 4)) % 4).toBe(0) // 4 chars -> 0 padding
  })

  // ==================== Error handling ====================

  it('should handle subscribe error gracefully', async () => {
    const origServiceWorker = (navigator as any).serviceWorker
    const origPushManager = (window as any).PushManager
    const origNotification = globalThis.Notification

    Object.defineProperty(navigator, 'serviceWorker', {
      value: { ready: Promise.resolve({ pushManager: { subscribe: vi.fn() } }) },
      configurable: true,
    })
    Object.defineProperty(window, 'PushManager', {
      value: class PushManager {},
      configurable: true,
    })
    Object.defineProperty(globalThis, 'Notification', {
      value: {
        permission: 'default',
        requestPermission: vi.fn().mockRejectedValue(new Error('user dismissed')),
      },
      configurable: true,
    })

    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})

    const { subscribe } = usePushNotifications()
    const result = await subscribe()

    expect(result).toBe(false)
    expect(consoleError).toHaveBeenCalled()

    consoleError.mockRestore()

    // Restore
    Object.defineProperty(navigator, 'serviceWorker', {
      value: origServiceWorker,
      configurable: true,
    })
    Object.defineProperty(window, 'PushManager', {
      value: origPushManager,
      configurable: true,
    })
    Object.defineProperty(globalThis, 'Notification', {
      value: origNotification,
      configurable: true,
    })
  })

  it('should handle unsubscribe error gracefully', async () => {
    const mockPushManager = {
      getSubscription: vi.fn().mockRejectedValue(new Error('not available')),
    }
    const mockRegistration = { pushManager: mockPushManager }

    const origServiceWorker = (navigator as any).serviceWorker
    const origPushManager = (window as any).PushManager

    Object.defineProperty(navigator, 'serviceWorker', {
      value: { ready: Promise.resolve(mockRegistration) },
      configurable: true,
    })
    Object.defineProperty(window, 'PushManager', {
      value: class PushManager {},
      configurable: true,
    })

    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})

    const { unsubscribe } = usePushNotifications()
    // Should not throw
    await unsubscribe()
    expect(consoleError).toHaveBeenCalled()

    consoleError.mockRestore()

    // Restore
    Object.defineProperty(navigator, 'serviceWorker', {
      value: origServiceWorker,
      configurable: true,
    })
    Object.defineProperty(window, 'PushManager', {
      value: origPushManager,
      configurable: true,
    })
  })
})
