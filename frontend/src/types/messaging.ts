export interface ConversationInfo {
  id: string
  title: string | null
  isGroup: boolean
  participants: ParticipantInfo[]
  lastMessage: string | null
  lastMessageAt: string | null
  unreadCount: number
  muted: boolean
  createdAt: string
}

export interface ParticipantInfo {
  userId: string
  displayName: string
  lastReadAt: string | null
}

export interface MessageImageInfo {
  imageId: string
  originalFilename: string
  contentType: string
  fileSize: number
}

export interface ReplyInfo {
  messageId: string
  senderId: string
  senderName: string
  contentPreview: string | null
  hasImage: boolean
}

export interface MessageInfo {
  id: string
  conversationId: string
  senderId: string
  senderName: string
  content: string | null
  createdAt: string
  images: MessageImageInfo[]
  replyTo: ReplyInfo | null
}

export interface StartConversationRequest {
  title?: string
  isGroup: boolean
  participantIds: string[]
}
