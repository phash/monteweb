export type SourceType = 'ROOM' | 'SECTION' | 'SCHOOL' | 'BOARD' | 'SYSTEM'

export interface FeedPost {
  id: string
  authorId: string
  authorName: string
  sourceType: SourceType
  sourceId: string | null
  sourceName: string | null
  title: string | null
  content: string
  pinned: boolean
  attachments: FeedAttachment[]
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

export interface CreatePostRequest {
  sourceType: SourceType
  sourceId?: string
  title?: string
  content: string
}

export interface CreateCommentRequest {
  content: string
}
