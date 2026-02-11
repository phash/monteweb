export interface FamilyInfo {
  id: string
  name: string
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

export interface TenantConfig {
  id: string
  schoolName: string
  logoUrl: string | null
  theme: Record<string, string>
  modules: Record<string, boolean>
  targetHoursPerFamily: number
  targetCleaningHours: number
}
