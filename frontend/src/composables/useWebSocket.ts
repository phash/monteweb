import { ref, onUnmounted } from 'vue'
import { useNotificationsStore } from '@/stores/notifications'
import { useMessagingStore } from '@/stores/messaging'
import type { NotificationInfo } from '@/types/notification'
import type { MessageInfo } from '@/types/messaging'

// SockJS + STOMP are loaded via CDN or npm; for now we use a simple approach
// In production, install: npm i @stomp/stompjs sockjs-client

let stompClient: any = null
const connected = ref(false)

export function useWebSocket() {
  function connect(userId: string) {
    if (connected.value) return

    // Dynamic import to avoid build issues if stomp not installed yet
    try {
      const SockJS = (window as any).SockJS
      const Stomp = (window as any).Stomp

      if (!SockJS || !Stomp) {
        console.warn('WebSocket libraries not loaded (SockJS/STOMP). Real-time disabled.')
        return
      }

      const socket = new SockJS('/ws')
      stompClient = Stomp.over(socket)
      stompClient.debug = () => {} // silence debug logs

      stompClient.connect({}, () => {
        connected.value = true

        // Subscribe to personal notification queue
        stompClient.subscribe(`/user/${userId}/queue/notifications`, (msg: any) => {
          const notification: NotificationInfo = JSON.parse(msg.body)
          const store = useNotificationsStore()
          store.addNotification(notification)
        })

        // Subscribe to personal message queue
        stompClient.subscribe(`/user/${userId}/queue/messages`, (msg: any) => {
          const message: MessageInfo = JSON.parse(msg.body)
          const store = useMessagingStore()
          store.addIncomingMessage(message)
        })
      }, (error: any) => {
        console.error('WebSocket connection error:', error)
        connected.value = false
      })
    } catch (e) {
      console.warn('WebSocket setup failed:', e)
    }
  }

  function disconnect() {
    if (stompClient && connected.value) {
      stompClient.disconnect()
      connected.value = false
    }
  }

  onUnmounted(() => {
    // Don't disconnect on unmount - connection is app-wide
  })

  return {
    connected,
    connect,
    disconnect,
  }
}
