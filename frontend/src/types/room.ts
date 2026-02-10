export type RoomType = 'KLASSE' | 'GRUPPE' | 'PROJEKT' | 'INTEREST' | 'CUSTOM'
export type RoomRole = 'LEADER' | 'MEMBER' | 'PARENT_MEMBER' | 'GUEST'
export type ChannelType = 'MAIN' | 'PARENTS' | 'STUDENTS'

export interface RoomInfo {
  id: string
  name: string
  description: string | null
  type: RoomType
  sectionId: string | null
  archived: boolean
  memberCount: number
  discoverable: boolean
  expiresAt: string | null
  tags: string[]
}

export interface RoomSettings {
  chatEnabled: boolean
  filesEnabled: boolean
  parentSpaceEnabled: boolean
  visibility: 'MEMBERS_ONLY' | 'SECTION' | 'ALL'
}

export interface RoomDetail extends RoomInfo {
  settings: RoomSettings
  createdBy: string | null
  createdAt: string | null
  members: RoomMember[]
}

export interface RoomMember {
  userId: string
  displayName: string
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
