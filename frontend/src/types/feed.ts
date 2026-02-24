export type SourceType = 'ROOM' | 'SECTION' | 'SCHOOL' | 'BOARD' | 'SYSTEM'

export interface ReactionSummary {
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

export interface FeedPost {
  id: string
  authorId: string
  authorName: string
  sourceType: SourceType
  sourceId: string | null
  sourceName: string | null
  title: string | null
  content: string | null
  pinned: boolean
  attachments: FeedAttachment[]
  reactions: ReactionSummary[]
  poll: PollInfo | null
  commentCount: number
  createdAt: string
  updatedAt: string | null
}

export interface FeedAttachment {
  id: string
  fileUrl: string
  fileName: string
  contentType: string
}

export interface FeedComment {
  id: string
  postId: string
  authorId: string
  authorName: string
  content: string
  reactions: ReactionSummary[]
  createdAt: string
}

export interface SystemBanner {
  id: string
  type: string
  title: string
  message: string
  link: string | null
  priority: number
}

export interface CreatePollRequest {
  question: string
  options: string[]
  multiple?: boolean
  closesAt?: string
}

export interface CreatePostRequest {
  sourceType: SourceType
  sourceId?: string
  title?: string
  content?: string
  poll?: CreatePollRequest
}

export interface CreateCommentRequest {
  content: string
}

export interface LinkPreviewInfo {
  url: string
  title: string | null
  description: string | null
  imageUrl: string | null
  siteName: string | null
}
