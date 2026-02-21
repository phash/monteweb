import { ref, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import { useNotificationsStore } from '@/stores/notifications'
import { useMessagingStore } from '@/stores/messaging'
import type { NotificationInfo } from '@/types/notification'
import type { MessageInfo } from '@/types/messaging'

let stompClient: Client | null = null
const connected = ref(false)
let currentUserId: string | null = null
let visibilityHandler: (() => void) | null = null

export function useWebSocket() {
  function connect(userId: string) {
    if (connected.value && stompClient?.connected) return

    currentUserId = userId

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/ws`

    stompClient = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {},
      connectHeaders: {
        Authorization: `Bearer ${localStorage.getItem('accessToken') || ''}`,
      },
      beforeConnect: () => {
        // Update token before each (re)connect attempt
        if (stompClient) {
          stompClient.connectHeaders = {
            Authorization: `Bearer ${localStorage.getItem('accessToken') || ''}`,
          }
        }
      },
    })

    stompClient.onConnect = () => {
      connected.value = true

      stompClient?.subscribe('/user/queue/notifications', (msg) => {
        const notification: NotificationInfo = JSON.parse(msg.body)
        const store = useNotificationsStore()
        store.addNotification(notification)
      })

      stompClient?.subscribe('/user/queue/messages', (msg) => {
        const message: MessageInfo = JSON.parse(msg.body)
        const store = useMessagingStore()
        store.addIncomingMessage(message)
      })
    }

    stompClient.onDisconnect = () => {
      connected.value = false
    }

    stompClient.onWebSocketClose = () => {
      connected.value = false
    }

    stompClient.onStompError = () => {
      connected.value = false
    }

    stompClient.activate()

    // Handle mobile visibility changes — refresh data when returning to foreground
    if (!visibilityHandler) {
      visibilityHandler = () => {
        if (document.visibilityState === 'visible' && currentUserId) {
          // Force reconnect if connection was lost
          if (!stompClient?.connected) {
            connected.value = false
            stompClient?.activate()
          }
          // Refresh messaging data to catch any missed messages
          const messagingStore = useMessagingStore()
          messagingStore.fetchConversations()
          if (messagingStore.currentConversation) {
            messagingStore.fetchMessages(messagingStore.currentConversation.id)
          }
        }
      }
      document.addEventListener('visibilitychange', visibilityHandler)
    }
  }

  function disconnect() {
    if (stompClient) {
      stompClient.deactivate()
      connected.value = false
    }
    currentUserId = null
    if (visibilityHandler) {
      document.removeEventListener('visibilitychange', visibilityHandler)
      visibilityHandler = null
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
