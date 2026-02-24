export type UserRole = 'SUPERADMIN' | 'SECTION_ADMIN' | 'TEACHER' | 'PARENT' | 'STUDENT'

export interface UserInfo {
  id: string
  email: string
  firstName: string
  lastName: string
  displayName: string
  phone: string | null
  avatarUrl: string | null
  role: UserRole
  specialRoles: string[]
  assignedRoles: string[]
  active: boolean
  darkMode: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  firstName: string
  lastName: string
  phone?: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  userId: string
  email: string
  role: string
  requires2fa?: boolean
  tempToken?: string
  requires2faSetup?: boolean
}

export interface DeletionStatus {
  deletionRequested: boolean
  deletionRequestedAt: string | null
  scheduledDeletionAt: string | null
}

export interface ConsentRecord {
  id: string
  consentType: string
  granted: boolean
  grantedAt: string
  notes: string | null
}

export interface TermsStatus {
  currentVersion: string | null
  accepted: boolean
}

export interface PrivacyPolicy {
  text: string | null
  version: string | null
}

// CSV Import types
export interface CsvImportResult {
  totalRows: number
  usersCreated: number
  familiesCreated: number
  errorsCount: number
  errors: CsvRowError[]
  preview: CsvRowPreview[]
}

export interface CsvRowError {
  row: number
  field: string
  message: string
}

export interface CsvRowPreview {
  row: number
  email: string
  name: string
  role: string
  familyName: string
  familyRole: string
  sectionSlug: string
  valid: boolean
  error: string | null
}
