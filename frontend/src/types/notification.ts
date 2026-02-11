export type NotificationType =
  | 'POST'
  | 'COMMENT'
  | 'MESSAGE'
  | 'SYSTEM'
  | 'REMINDER'
  | 'CLEANING_COMPLETED'
  | 'JOB_COMPLETED'
  | 'DISCUSSION_THREAD'
  | 'DISCUSSION_REPLY'
  | 'EVENT_CREATED'
  | 'EVENT_UPDATED'
  | 'EVENT_CANCELLED'
  | 'FORM_PUBLISHED'
  | 'CONSENT_REQUIRED'

export interface NotificationInfo {
  id: string
  userId: string
  type: NotificationType
  title: string
  message: string
  link: string | null
  referenceType: string | null
  referenceId: string | null
  read: boolean
  createdAt: string
}
