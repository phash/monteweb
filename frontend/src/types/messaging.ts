export interface ConversationInfo {
  id: string
  title: string | null
  isGroup: boolean
  participants: ParticipantInfo[]
  lastMessage: string | null
  lastMessageAt: string | null
  unreadCount: number
  createdAt: string
}

export interface ParticipantInfo {
  userId: string
  displayName: string
  lastReadAt: string | null
}

export interface MessageInfo {
  id: string
  conversationId: string
  senderId: string
  senderName: string
  content: string
  createdAt: string
}

export interface StartConversationRequest {
  title?: string
  isGroup: boolean
  participantIds: string[]
}
