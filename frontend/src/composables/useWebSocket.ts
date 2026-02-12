import { ref, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import { useNotificationsStore } from '@/stores/notifications'
import { useMessagingStore } from '@/stores/messaging'
import type { NotificationInfo } from '@/types/notification'
import type { MessageInfo } from '@/types/messaging'

let stompClient: Client | null = null
const connected = ref(false)

export function useWebSocket() {
  function connect(userId: string) {
    if (connected.value) return

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/ws/websocket`

    stompClient = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {},
    })

    stompClient.onConnect = () => {
      connected.value = true

      stompClient?.subscribe(`/user/${userId}/queue/notifications`, (msg) => {
        const notification: NotificationInfo = JSON.parse(msg.body)
        const store = useNotificationsStore()
        store.addNotification(notification)
      })

      stompClient?.subscribe(`/user/${userId}/queue/messages`, (msg) => {
        const message: MessageInfo = JSON.parse(msg.body)
        const store = useMessagingStore()
        store.addIncomingMessage(message)
      })
    }

    stompClient.onStompError = () => {
      connected.value = false
    }

    stompClient.activate()
  }

  function disconnect() {
    if (stompClient && connected.value) {
      stompClient.deactivate()
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
