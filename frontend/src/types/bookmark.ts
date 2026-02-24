export type BookmarkContentType = 'POST' | 'EVENT' | 'WIKI_PAGE' | 'JOB'

export interface BookmarkInfo {
  id: string
  userId: string
  contentType: BookmarkContentType
  contentId: string
  createdAt: string
}
