export type ParentLetterStatus = 'DRAFT' | 'SCHEDULED' | 'SENT' | 'CLOSED'
export type RecipientStatus = 'OPEN' | 'READ' | 'CONFIRMED'

export interface ParentLetterInfo {
  id: string
  title: string
  status: ParentLetterStatus
  roomId: string
  roomName: string
  createdBy: string
  creatorName: string
  sendDate: string | null
  deadline: string | null
  totalRecipients: number
  confirmedCount: number
  createdAt: string
  updatedAt: string
}

export interface ParentLetterRecipientInfo {
  id: string
  studentId: string
  studentName: string
  parentId: string
  parentName: string
  familyName: string
  status: RecipientStatus
  readAt: string | null
  confirmedAt: string | null
  confirmedByName: string | null
  reminderSentAt: string | null
}

export interface ParentLetterDetailInfo {
  id: string
  title: string
  content: string
  status: ParentLetterStatus
  roomId: string
  roomName: string
  createdBy: string
  creatorName: string
  sendDate: string | null
  deadline: string | null
  reminderDays: number
  reminderSent: boolean
  totalRecipients: number
  confirmedCount: number
  recipients: ParentLetterRecipientInfo[]
  attachments?: ParentLetterAttachmentInfo[]
  createdAt: string
  updatedAt: string
}

export interface ParentLetterConfigInfo {
  id: string
  sectionId: string | null
  sectionName: string | null
  letterheadPath: string | null
  signatureTemplate: string | null
  reminderDays: number
}

export interface CreateParentLetterRequest {
  roomId: string
  title: string
  content: string
  sendDate?: string | null
  deadline?: string | null
  reminderDays?: number | null
  studentIds?: string[] | null
}

export interface UpdateParentLetterRequest {
  title: string
  content: string
  sendDate?: string | null
  deadline?: string | null
  reminderDays?: number | null
  studentIds?: string[] | null
}

export interface UpdateParentLetterConfigRequest {
  signatureTemplate?: string | null
  reminderDays?: number | null
}

export interface ParentLetterStatsInfo {
  activeCount: number
  totalRecipients: number
  totalConfirmed: number
  totalRead: number
  overdueCount: number
}

export interface ParentLetterAttachmentInfo {
  id: string
  originalFilename: string
  storagePath: string
  fileSize: number
  contentType: string
  sortOrder: number
  createdAt: string
}
