export interface BillingPeriodInfo {
  id: string
  name: string
  startDate: string
  endDate: string
  status: 'ACTIVE' | 'CLOSED'
  closedAt: string | null
  closedBy: string | null
  notes: string | null
  createdAt: string
}

export interface FamilyMember {
  userId: string
  displayName: string
  role: string
}

export interface FamilyBillingEntry {
  familyId: string
  familyName: string
  members: FamilyMember[]
  jobHours: number
  cleaningHours: number
  totalHours: number
  targetHours: number
  balance: number
  trafficLight: 'GREEN' | 'YELLOW' | 'RED'
}

export interface BillingSummary {
  totalFamilies: number
  averageHours: number
  totalHoursAll: number
  greenCount: number
  yellowCount: number
  redCount: number
}

export interface BillingReportInfo {
  period: BillingPeriodInfo
  families: FamilyBillingEntry[]
  summary: BillingSummary
}

export interface CreateBillingPeriodRequest {
  name: string
  startDate: string
  endDate: string
}
