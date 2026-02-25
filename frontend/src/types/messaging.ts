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

export interface MessageAttachmentInfo {
  id: string
  attachmentType: 'FILE' | 'FILE_LINK'
  originalFilename: string | null
  contentType: string | null
  fileSize: number | null
  linkedFileId: string | null
  linkedFileName: string | null
  linkedRoomId: string | null
}

export interface ReplyInfo {
  messageId: string
  senderId: string
  senderName: string
  contentPreview: string | null
  hasImage: boolean
  hasAttachment: boolean
}

export interface MessageReactionSummary {
  emoji: string
  count: number
  userReacted: boolean
}

export interface PollOptionInfo {
  id: string
  label: string
  voteCount: number
  userVoted: boolean
}

export interface PollInfo {
  id: string
  question: string
  multiple: boolean
  closed: boolean
  totalVotes: number
  options: PollOptionInfo[]
  closesAt: string | null
}

export interface MessageInfo {
  id: string
  conversationId: string
  senderId: string
  senderName: string
  content: string | null
  createdAt: string
  images: MessageImageInfo[]
  attachments: MessageAttachmentInfo[]
  replyTo: ReplyInfo | null
  reactions: MessageReactionSummary[]
  poll: PollInfo | null
}

export interface StartConversationRequest {
  title?: string
  isGroup: boolean
  participantIds: string[]
}
