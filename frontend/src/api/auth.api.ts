import client from './client'
import type { ApiResponse } from '@/types/api'
import type { LoginRequest, LoginResponse, RegisterRequest } from '@/types/user'

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
}
