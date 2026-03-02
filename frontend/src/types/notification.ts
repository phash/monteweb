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
  | 'ROOM_JOIN_REQUEST'
  | 'ROOM_JOIN_APPROVED'
  | 'ROOM_JOIN_DENIED'
  | 'FAMILY_INVITATION'
  | 'FAMILY_INVITATION_ACCEPTED'
  | 'MENTION'
  | 'PARENT_LETTER'
  | 'PARENT_LETTER_REMINDER'

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
