export interface ApiResponse<T> {
  data: T
  message: string | null
  success: boolean
  timestamp: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface ErrorResponse {
  error: string
  message: string
  status: number
  details?: Record<string, string>
  timestamp: string
}
