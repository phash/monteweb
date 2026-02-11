import { ref } from 'vue'
import client from '@/api/client'

const isSupported = ref(false)
const isSubscribed = ref(false)
const permission = ref<NotificationPermission>('default')

export function usePushNotifications() {
  isSupported.value = 'serviceWorker' in navigator && 'PushManager' in window

  async function checkSubscription() {
    if (!isSupported.value) return

    permission.value = Notification.permission

    const registration = await navigator.serviceWorker.ready
    const subscription = await registration.pushManager.getSubscription()
    isSubscribed.value = !!subscription
  }

  async function subscribe() {
    if (!isSupported.value) return false

    try {
      const perm = await Notification.requestPermission()
      permission.value = perm
      if (perm !== 'granted') return false

      // Get VAPID public key from backend
      const keyRes = await client.get<{ data: { publicKey: string } }>(
        '/notifications/push/public-key'
      )
      const vapidPublicKey = keyRes.data.data.publicKey

      const registration = await navigator.serviceWorker.ready
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(vapidPublicKey).buffer as ArrayBuffer,
      })

      const json = subscription.toJSON()
      await client.post('/notifications/push/subscribe', {
        endpoint: json.endpoint,
        p256dh: json.keys?.p256dh,
        auth: json.keys?.auth,
      })

      isSubscribed.value = true
      return true
    } catch (e) {
      console.error('Failed to subscribe to push notifications:', e)
      return false
    }
  }

  async function unsubscribe() {
    if (!isSupported.value) return

    try {
      const registration = await navigator.serviceWorker.ready
      const subscription = await registration.pushManager.getSubscription()
      if (subscription) {
        await client.post('/notifications/push/unsubscribe', {
          endpoint: subscription.endpoint,
        })
        await subscription.unsubscribe()
      }
      isSubscribed.value = false
    } catch (e) {
      console.error('Failed to unsubscribe from push notifications:', e)
    }
  }

  return {
    isSupported,
    isSubscribed,
    permission,
    checkSubscription,
    subscribe,
    unsubscribe,
  }
}

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
