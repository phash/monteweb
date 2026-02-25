import { defineStore } from 'pinia'
import { ref } from 'vue'
import { messagingApi } from '@/api/messaging.api'
import type { ConversationInfo, MessageInfo } from '@/types/messaging'

export const useMessagingStore = defineStore('messaging', () => {
  const conversations = ref<ConversationInfo[]>([])
  const currentConversation = ref<ConversationInfo | null>(null)
  const activeConversationId = ref<string | null>(null)
  const messages = ref<MessageInfo[]>([])
  const unreadCount = ref(0)
  const loading = ref(false)
  const replyToMessage = ref<MessageInfo | null>(null)

  async function fetchConversations() {
    loading.value = true
    try {
      const res = await messagingApi.getConversations()
      conversations.value = res.data.data
    } catch {
      conversations.value = []
    } finally {
      loading.value = false
    }
  }

  async function fetchConversation(id: string) {
    const res = await messagingApi.getConversation(id)
    currentConversation.value = res.data.data
  }

  async function fetchMessages(conversationId: string) {
    activeConversationId.value = conversationId
    loading.value = true
    try {
      const res = await messagingApi.getMessages(conversationId)
      messages.value = res.data.data.content.reverse()
    } finally {
      loading.value = false
    }
  }

  async function sendMessage(conversationId: string, content?: string, image?: File, replyToId?: string, attachment?: File, linkedFileId?: string, linkedRoomId?: string, linkedFileName?: string) {
    const res = await messagingApi.sendMessage(conversationId, content, image, replyToId, attachment, linkedFileId, linkedRoomId, linkedFileName)
    messages.value.push(res.data.data)
    replyToMessage.value = null
    return res.data.data
  }

  function setReplyTo(message: MessageInfo | null) {
    replyToMessage.value = message
  }

  async function startDirectConversation(userId: string) {
    const res = await messagingApi.startConversation({
      isGroup: false,
      participantIds: [userId],
    })
    const conv = res.data.data
    if (!conversations.value.find(c => c.id === conv.id)) {
      conversations.value.unshift(conv)
    }
    return conv
  }

  async function fetchUnreadCount() {
    try {
      const res = await messagingApi.getUnreadCount()
      unreadCount.value = res.data.data.count
    } catch {
      // ignore
    }
  }

  async function markAsRead(conversationId: string) {
    await messagingApi.markAsRead(conversationId)
    const conv = conversations.value.find(c => c.id === conversationId)
    if (conv) {
      unreadCount.value = Math.max(0, unreadCount.value - conv.unreadCount)
      conv.unreadCount = 0
    }
  }

  async function muteConversation(conversationId: string) {
    await messagingApi.muteConversation(conversationId)
    const conv = conversations.value.find(c => c.id === conversationId)
    if (conv) conv.muted = true
    if (currentConversation.value?.id === conversationId) {
      currentConversation.value = { ...currentConversation.value, muted: true }
    }
  }

  async function unmuteConversation(conversationId: string) {
    await messagingApi.unmuteConversation(conversationId)
    const conv = conversations.value.find(c => c.id === conversationId)
    if (conv) conv.muted = false
    if (currentConversation.value?.id === conversationId) {
      currentConversation.value = { ...currentConversation.value, muted: false }
    }
  }

  async function deleteConversation(conversationId: string) {
    await messagingApi.deleteConversation(conversationId)
    conversations.value = conversations.value.filter(c => c.id !== conversationId)
    if (currentConversation.value?.id === conversationId) {
      currentConversation.value = null
      activeConversationId.value = null
      messages.value = []
    }
  }

  function addIncomingMessage(message: MessageInfo) {
    if (activeConversationId.value === message.conversationId) {
      // Avoid duplicates (own messages already added via sendMessage)
      if (!messages.value.some(m => m.id === message.id)) {
        messages.value.push(message)
      }
    }
    // Update conversation list
    const conv = conversations.value.find(c => c.id === message.conversationId)
    if (conv) {
      conv.lastMessage = message.content ?? (message.images?.length ? '\uD83D\uDDBC Bild' : (message.attachments?.length ? '\uD83D\uDCCE Datei' : null))
      conv.lastMessageAt = message.createdAt
      conv.unreadCount++
      unreadCount.value++
    }
  }

  return {
    conversations,
    currentConversation,
    activeConversationId,
    messages,
    unreadCount,
    loading,
    replyToMessage,
    fetchConversations,
    fetchConversation,
    fetchMessages,
    sendMessage,
    setReplyTo,
    startDirectConversation,
    fetchUnreadCount,
    markAsRead,
    muteConversation,
    unmuteConversation,
    deleteConversation,
    addIncomingMessage,
  }
})
