export type JobStatus = 'OPEN' | 'ASSIGNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
export type AssignmentStatus = 'ASSIGNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

export interface JobInfo {
  id: string
  title: string
  description: string | null
  category: string
  location: string | null
  sectionId: string | null
  estimatedHours: number
  maxAssignees: number
  currentAssignees: number
  status: JobStatus
  scheduledDate: string | null
  scheduledTime: string | null
  createdBy: string
  creatorName: string
  contactInfo: string | null
  eventId: string | null
  eventTitle: string | null
  createdAt: string
}

export interface JobAssignmentInfo {
  id: string
  jobId: string
  jobTitle: string
  userId: string
  userName: string
  familyId: string
  familyName: string
  status: AssignmentStatus
  actualHours: number | null
  confirmed: boolean
  confirmedBy: string | null
  confirmedAt: string | null
  notes: string | null
  assignedAt: string
  completedAt: string | null
}

export interface FamilyHoursInfo {
  familyId: string
  familyName: string
  targetHours: number
  completedHours: number
  pendingHours: number
  cleaningHours: number
  totalHours: number
  remainingHours: number
  trafficLight: 'GREEN' | 'YELLOW' | 'RED'
  targetCleaningHours: number
  remainingCleaningHours: number
  cleaningTrafficLight: 'GREEN' | 'YELLOW' | 'RED'
  hoursExempt: boolean
}

export interface ReportSummary {
  openJobs: number
  activeJobs: number
  completedJobs: number
  greenFamilies: number
  yellowFamilies: number
  redFamilies: number
}

export interface CreateJobRequest {
  title: string
  description?: string
  category: string
  location?: string
  sectionId?: string
  estimatedHours: number
  maxAssignees: number
  scheduledDate?: string
  scheduledTime?: string
  contactInfo?: string
  eventId?: string
}
