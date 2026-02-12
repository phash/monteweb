export type RoomType = 'KLASSE' | 'GRUPPE' | 'PROJEKT' | 'INTEREST' | 'CUSTOM'
export type RoomRole = 'LEADER' | 'MEMBER' | 'PARENT_MEMBER' | 'GUEST'
export type ChannelType = 'MAIN' | 'PARENTS' | 'STUDENTS'
export type JoinPolicy = 'OPEN' | 'REQUEST' | 'INVITE_ONLY'
export type DiscussionMode = 'FULL' | 'ANNOUNCEMENTS_ONLY' | 'DISABLED'

export interface RoomInfo {
  id: string
  name: string
  description: string | null
  publicDescription: string | null
  avatarUrl: string | null
  type: RoomType
  sectionId: string | null
  archived: boolean
  memberCount: number
  joinPolicy: JoinPolicy
  expiresAt: string | null
  tags: string[]
}

export interface RoomSettings {
  chatEnabled: boolean
  filesEnabled: boolean
  parentSpaceEnabled: boolean
  visibility: 'MEMBERS_ONLY' | 'SECTION' | 'ALL'
  discussionMode: DiscussionMode
  allowMemberThreadCreation: boolean
  childDiscussionEnabled: boolean
}

export interface RoomDetail extends RoomInfo {
  settings: RoomSettings
  createdBy: string | null
  createdAt: string | null
  members: RoomMember[]
}

export interface RoomPublicInfo {
  id: string
  name: string
  publicDescription: string | null
  avatarUrl: string | null
  type: RoomType
  sectionId: string | null
  memberCount: number
  joinPolicy: JoinPolicy
  tags: string[]
}

export interface RoomMember {
  userId: string
  displayName: string
  avatarUrl: string | null
  role: RoomRole
  joinedAt: string
}

export interface CreateRoomRequest {
  name: string
  description?: string
  type: RoomType
  sectionId?: string
}

export interface CreateInterestRoomRequest {
  name: string
  description?: string
  tags?: string[]
  expiresAt?: string
}

export interface RoomChatChannelInfo {
  id: string
  roomId: string
  conversationId: string
  channelType: ChannelType
  lastMessage: string | null
  unreadCount: number
}

export interface JoinRequestInfo {
  id: string
  roomId: string
  roomName: string
  userId: string
  userName: string
  message: string | null
  status: 'PENDING' | 'APPROVED' | 'DENIED'
  createdAt: string
  resolvedAt: string | null
}
