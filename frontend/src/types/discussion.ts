export type ThreadStatus = 'ACTIVE' | 'ARCHIVED'
export type ThreadAudience = 'ALLE' | 'ELTERN' | 'KINDER'

export interface DiscussionThread {
  id: string
  roomId: string
  createdBy: string
  creatorName: string
  title: string
  content: string | null
  status: ThreadStatus
  audience: ThreadAudience
  replyCount: number
  createdAt: string
  updatedAt: string
}

export interface DiscussionReply {
  id: string
  threadId: string
  authorId: string
  authorName: string
  content: string
  createdAt: string
}
