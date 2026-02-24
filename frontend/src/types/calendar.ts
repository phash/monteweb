export type EventScope = 'ROOM' | 'SECTION' | 'SCHOOL'

export type EventRecurrence = 'NONE' | 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY'

export type RsvpStatus = 'ATTENDING' | 'MAYBE' | 'DECLINED'

export interface CalendarEvent {
  id: string
  title: string
  description: string | null
  location: string | null
  allDay: boolean
  startDate: string
  startTime: string | null
  endDate: string
  endTime: string | null
  scope: EventScope
  scopeId: string | null
  scopeName: string | null
  recurrence: EventRecurrence
  recurrenceEnd: string | null
  cancelled: boolean
  createdBy: string
  creatorName: string
  attendingCount: number
  maybeCount: number
  declinedCount: number
  currentUserRsvp: RsvpStatus | null
  eventType: string
  color: string | null
  jitsiRoomName: string | null
  linkedJobCount: number
  createdAt: string
  updatedAt: string
}

export interface CreateEventRequest {
  title: string
  description?: string
  location?: string
  allDay: boolean
  startDate: string
  startTime?: string
  endDate: string
  endTime?: string
  scope: EventScope
  scopeId?: string
  recurrence?: EventRecurrence
  recurrenceEnd?: string
  eventType?: string
  color?: string
}

export interface UpdateEventRequest {
  title?: string
  description?: string
  location?: string
  allDay?: boolean
  startDate?: string
  startTime?: string
  endDate?: string
  endTime?: string
  recurrence?: EventRecurrence
  recurrenceEnd?: string
  color?: string
}

export interface RsvpRequest {
  status: RsvpStatus
}

export interface ICalSubscription {
  id: string
  name: string
  url: string
  color: string | null
  lastSyncedAt: string | null
  active: boolean
  createdAt: string | null
}

export interface ICalEvent {
  id: string
  subscriptionId: string
  title: string
  description: string | null
  location: string | null
  startDate: string
  endDate: string | null
  startTime: string | null
  endTime: string | null
  allDay: boolean
}
