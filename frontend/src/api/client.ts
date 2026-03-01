import axios from 'axios'
import type { ApiResponse } from '@/types/api'

const client = axios.create({
  baseURL: '/api/v1',
  timeout: 30000, // 30 seconds default timeout
  withCredentials: true, // send httpOnly auth cookies automatically
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor: attach JWT from sessionStorage as Authorization header (fallback)
client.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor: handle token refresh
let isRefreshing = false
let failedQueue: Array<{
  resolve: (token: string) => void
  reject: (error: unknown) => void
}> = []

function processQueue(error: unknown, token: string | null) {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(token!)
    }
  })
  failedQueue = []
}

client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`
          return client(originalRequest)
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        // Refresh token is sent via httpOnly cookie automatically (withCredentials: true).
        // Also send body token as fallback for clients without cookie support.
        const refreshToken = sessionStorage.getItem('refreshToken')
        const response = await axios.post<ApiResponse<{ accessToken: string; refreshToken: string }>>(
          '/api/v1/auth/refresh',
          refreshToken ? { refreshToken } : {},
          { withCredentials: true }
        )
        const { accessToken, refreshToken: newRefreshToken } = response.data.data
        sessionStorage.setItem('accessToken', accessToken)
        sessionStorage.setItem('refreshToken', newRefreshToken)
        processQueue(null, accessToken)
        originalRequest.headers.Authorization = `Bearer ${accessToken}`
        return client(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError, null)
        sessionStorage.removeItem('accessToken')
        sessionStorage.removeItem('refreshToken')
        if (!window.location.pathname.startsWith('/login')) {
          window.location.href = '/login'
        }
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    // Handle maintenance mode (503)
    if (error.response?.status === 503 && error.response?.data?.maintenance) {
      window.dispatchEvent(new CustomEvent('monteweb:maintenance', {
        detail: { message: error.response.data.message },
      }))
      return Promise.reject(error)
    }

    // Report server errors
    if (error.response?.status >= 500) {
      window.dispatchEvent(new CustomEvent('monteweb:server-error', {
        detail: {
          url: error.config?.url,
          status: error.response.status,
          message: error.response?.data?.message || 'Server error',
        },
      }))
    }

    return Promise.reject(error)
  }
)

export default client
