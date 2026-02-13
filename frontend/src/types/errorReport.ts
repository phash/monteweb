export interface ErrorReportInfo {
  id: string
  fingerprint: string
  source: 'BACKEND' | 'FRONTEND'
  errorType: string | null
  message: string
  stackTrace: string | null
  location: string | null
  userId: string | null
  userAgent: string | null
  requestUrl: string | null
  occurrenceCount: number
  firstSeenAt: string
  lastSeenAt: string
  status: 'NEW' | 'REPORTED' | 'RESOLVED' | 'IGNORED'
  githubIssueUrl: string | null
  createdAt: string
}

export interface SubmitErrorReportRequest {
  source: string
  errorType?: string
  message: string
  stackTrace?: string
  location?: string
  userAgent?: string
  requestUrl?: string
}

export interface UpdateErrorStatusRequest {
  status: string
}

export interface UpdateGithubConfigRequest {
  githubRepo: string
  githubPat: string
}
