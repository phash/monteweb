export type NotificationType = 'POST' | 'COMMENT' | 'MESSAGE' | 'SYSTEM' | 'REMINDER'

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
