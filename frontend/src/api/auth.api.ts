import client from './client'
import type { ApiResponse } from '@/types/api'
import type { LoginRequest, LoginResponse, RegisterRequest } from '@/types/user'

export interface TwoFactorSetupResponse {
  secret: string
  qrUri: string
}

export interface TwoFactorConfirmResponse {
  recoveryCodes: string[]
}

export interface TwoFactorStatusResponse {
  enabled: boolean
}

export const authApi = {
  login(data: LoginRequest) {
    return client.post<ApiResponse<LoginResponse>>('/auth/login', data)
  },

  register(data: RegisterRequest) {
    return client.post<ApiResponse<LoginResponse>>('/auth/register', data)
  },

  refresh(refreshToken: string) {
    return client.post<ApiResponse<LoginResponse>>('/auth/refresh', { refreshToken })
  },

  logout(refreshToken: string | null) {
    return client.post<ApiResponse<void>>('/auth/logout', refreshToken ? { refreshToken } : {})
  },

  // 2FA endpoints
  setup2fa() {
    return client.post<ApiResponse<TwoFactorSetupResponse>>('/auth/2fa/setup')
  },

  confirm2fa(code: string) {
    return client.post<ApiResponse<TwoFactorConfirmResponse>>('/auth/2fa/confirm', { code })
  },

  disable2fa(password: string) {
    return client.post<ApiResponse<void>>('/auth/2fa/disable', { password })
  },

  verify2fa(tempToken: string, code: string) {
    return client.post<ApiResponse<LoginResponse>>('/auth/2fa/verify', { tempToken, code })
  },

  get2faStatus() {
    return client.get<ApiResponse<TwoFactorStatusResponse>>('/auth/2fa/status')
  },
}
