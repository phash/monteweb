export interface WikiPageResponse {
  id: string
  roomId: string
  parentId: string | null
  title: string
  slug: string
  content: string
  createdBy: string
  createdByName: string
  lastEditedBy: string | null
  lastEditedByName: string | null
  children: WikiPageSummary[]
  createdAt: string
  updatedAt: string
}

export interface WikiPageSummary {
  id: string
  title: string
  slug: string
  parentId: string | null
  hasChildren: boolean
  updatedAt: string
}

export interface WikiPageVersionResponse {
  id: string
  title: string
  content: string
  editedBy: string
  editedByName: string
  createdAt: string
}

export interface CreatePageRequest {
  title: string
  content?: string
  parentId?: string
}

export interface UpdatePageRequest {
  title: string
  content: string
}
