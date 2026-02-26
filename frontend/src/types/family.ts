export interface FamilyInfo {
  id: string
  name: string
  avatarUrl: string | null
  hoursExempt: boolean
  active: boolean
  soleCustody: boolean
  soleCustodyApproved: boolean
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
  availableLanguages: string[]
  requireUserApproval: boolean
  privacyPolicyText?: string | null
  privacyPolicyVersion?: string | null
  termsText?: string | null
  termsVersion?: string | null
  dataRetentionDaysNotifications?: number | null
  dataRetentionDaysAudit?: number | null
  schoolFullName?: string | null
  schoolAddress?: string | null
  schoolPrincipal?: string | null
  techContactName?: string | null
  techContactEmail?: string | null
  twoFactorMode?: string
  twoFactorGraceDeadline?: string | null
  // LDAP/AD fields
  ldapUrl?: string | null
  ldapBaseDn?: string | null
  ldapBindDn?: string | null
  ldapUserSearchFilter?: string | null
  ldapAttrEmail?: string | null
  ldapAttrFirstName?: string | null
  ldapAttrLastName?: string | null
  ldapDefaultRole?: string | null
  ldapUseSsl?: boolean
  ldapConfigured?: boolean
  // directoryAdminOnly enabled via modules map
  // Maintenance (enabled via modules map)
  maintenanceMessage?: string | null
  // ClamAV virus scanner (enabled via modules map)
  clamavHost?: string
  clamavPort?: number
  // Jitsi video conferencing (enabled via modules map)
  jitsiServerUrl?: string
  // WOPI / ONLYOFFICE (enabled via modules map)
  wopiOfficeUrl?: string | null
  // Family settings
  soleCustodyEnabled?: boolean
  requireFamilySwitchApproval?: boolean
}
