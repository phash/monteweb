import { defineStore } from 'pinia'
import { ref } from 'vue'
import { messagingApi } from '@/api/messaging.api'
import type { ConversationInfo, MessageInfo } from '@/types/messaging'

export const useMessagingStore = defineStore('messaging', () => {
  const conversations = ref<ConversationInfo[]>([])
  const currentConversation = ref<ConversationInfo | null>(null)
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
    loading.value = true
    try {
      const res = await messagingApi.getMessages(conversationId)
      messages.value = res.data.data.content.reverse()
    } finally {
      loading.value = false
    }
  }

  async function sendMessage(conversationId: string, content?: string, image?: File, replyToId?: string) {
    const res = await messagingApi.sendMessage(conversationId, content, image, replyToId)
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

  async function deleteConversation(conversationId: string) {
    await messagingApi.deleteConversation(conversationId)
    conversations.value = conversations.value.filter(c => c.id !== conversationId)
    if (currentConversation.value?.id === conversationId) {
      currentConversation.value = null
      messages.value = []
    }
  }

  function addIncomingMessage(message: MessageInfo) {
    if (currentConversation.value?.id === message.conversationId) {
      messages.value.push(message)
    }
    // Update conversation list
    const conv = conversations.value.find(c => c.id === message.conversationId)
    if (conv) {
      conv.lastMessage = message.content ?? (message.images?.length ? '\uD83D\uDDBC Bild' : null)
      conv.lastMessageAt = message.createdAt
      conv.unreadCount++
      unreadCount.value++
    }
  }

  return {
    conversations,
    currentConversation,
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
    deleteConversation,
    addIncomingMessage,
  }
})
