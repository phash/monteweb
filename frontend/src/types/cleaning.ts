export type CleaningSlotStatus = 'OPEN' | 'FULL' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

export interface CleaningConfigInfo {
  id: string
  sectionId: string
  sectionName: string
  title: string
  description: string | null
  dayOfWeek: number
  startTime: string
  endTime: string
  minParticipants: number
  maxParticipants: number
  hoursCredit: number
  active: boolean
  specificDate?: string | null
  calendarEventId?: string | null
  jobId?: string | null
}

export interface RegistrationInfo {
  id: string
  userId: string
  userName: string
  familyId: string
  checkedIn: boolean
  checkedOut: boolean
  actualMinutes: number | null
  noShow: boolean
  swapOffered: boolean
  confirmed: boolean
  confirmedBy?: string | null
  confirmedAt?: string | null
}

export interface CleaningSlotInfo {
  id: string
  configId: string
  sectionId: string
  sectionName: string
  configTitle: string
  slotDate: string
  startTime: string
  endTime: string
  minParticipants: number
  maxParticipants: number
  currentRegistrations: number
  status: CleaningSlotStatus
  cancelled: boolean
  registrations: RegistrationInfo[]
}

export interface DashboardInfo {
  totalSlots: number
  completedSlots: number
  noShows: number
  slotsNeedingParticipants: number
}

export interface CreateConfigRequest {
  sectionId: string
  title: string
  description?: string
  dayOfWeek: number
  startTime: string
  endTime: string
  minParticipants: number
  maxParticipants: number
  hoursCredit: number
  specificDate?: string
}

export interface GenerateSlotsRequest {
  from: string
  to: string
}
