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
  active: boolean
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
}
