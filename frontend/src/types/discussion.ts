export type ThreadStatus = 'ACTIVE' | 'ARCHIVED'

export interface DiscussionThread {
  id: string
  roomId: string
  createdBy: string
  creatorName: string
  title: string
  content: string | null
  status: ThreadStatus
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
