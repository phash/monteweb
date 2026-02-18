export interface FamilyInfo {
  id: string
  name: string
  avatarUrl: string | null
  hoursExempt: boolean
  active: boolean
  members: FamilyMemberInfo[]
}

export interface FamilyMemberInfo {
  userId: string
  displayName: string
  role: 'PARENT' | 'CHILD'
}

export interface SchoolSectionInfo {
  id: string
  name: string
  slug: string
  description: string | null
  sortOrder: number
  active: boolean
}

export interface FamilyInvitationInfo {
  id: string
  familyId: string
  familyName: string
  inviterId: string
  inviterName: string
  inviteeId: string
  inviteeName: string
  role: 'PARENT' | 'CHILD'
  status: 'PENDING' | 'ACCEPTED' | 'DECLINED'
  createdAt: string
  resolvedAt: string | null
}

export interface TenantConfig {
  id: string
  schoolName: string
  logoUrl: string | null
  theme: Record<string, string>
  modules: Record<string, boolean>
  targetHoursPerFamily: number
  targetCleaningHours: number
  bundesland: string
  schoolVacations: { name: string; from: string; to: string }[]
  githubRepo?: string
  githubPatConfigured?: boolean
  requireAssignmentConfirmation: boolean
  multilanguageEnabled: boolean
  defaultLanguage: string
  requireUserApproval: boolean
}
