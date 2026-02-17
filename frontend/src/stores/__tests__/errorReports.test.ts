import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useErrorReportsStore } from '@/stores/errorReports'

vi.mock('@/api/errorReport.api', () => ({
  errorReportApi: {
    getAll: vi.fn(),
    getById: vi.fn(),
    updateStatus: vi.fn(),
    createGithubIssue: vi.fn(),
    deleteReport: vi.fn(),
    updateGithubConfig: vi.fn(),
    submitReport: vi.fn(),
  },
}))

import { errorReportApi } from '@/api/errorReport.api'

const mockReport = {
  id: 'report-1',
  source: 'FeedView',
  errorType: 'TypeError',
  message: 'Cannot read properties of null',
  stackTrace: 'Error at ...',
  status: 'NEW',
  fingerprint: 'fp-1',
  occurrenceCount: 3,
  githubIssueUrl: null,
  createdAt: '2025-01-01T10:00:00Z',
  lastSeenAt: '2025-01-02T10:00:00Z',
}

describe('ErrorReports Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  // ==================== Initial State ====================

  it('should start with empty state', () => {
    const store = useErrorReportsStore()
    expect(store.reports).toEqual([])
    expect(store.totalRecords).toBe(0)
    expect(store.loading).toBe(false)
    expect(store.githubRepo).toBe('')
    expect(store.githubPatConfigured).toBe(false)
  })

  // ==================== fetchReports ====================

  it('should fetch reports and update state', async () => {
    const store = useErrorReportsStore()
    vi.mocked(errorReportApi.getAll).mockResolvedValue({
      data: { data: { content: [mockReport], totalElements: 1 } },
    } as any)

    await store.fetchReports()

    expect(store.reports).toHaveLength(1)
    expect(store.reports[0]).toEqual(mockReport)
    expect(store.totalRecords).toBe(1)
    expect(store.loading).toBe(false)
  })

  it('should pass params to getAll', async () => {
    const store = useErrorReportsStore()
    vi.mocked(errorReportApi.getAll).mockResolvedValue({
      data: { data: { content: [], totalElements: 0 } },
    } as any)

    await store.fetchReports({ status: 'NEW', page: 0, size: 20 })

    expect(errorReportApi.getAll).toHaveBeenCalledWith({ status: 'NEW', page: 0, size: 20 })
  })

  it('should set loading to false after failed fetch', async () => {
    const store = useErrorReportsStore()
    vi.mocked(errorReportApi.getAll).mockRejectedValue(new Error('Network error'))

    await expect(store.fetchReports()).rejects.toThrow()
    expect(store.loading).toBe(false)
  })

  // ==================== updateStatus ====================

  it('should update report status in place', async () => {
    const store = useErrorReportsStore()
    store.reports = [{ ...mockReport }]

    const updated = { ...mockReport, status: 'RESOLVED' }
    vi.mocked(errorReportApi.updateStatus).mockResolvedValue({
      data: { data: updated },
    } as any)

    const result = await store.updateStatus('report-1', 'RESOLVED')

    expect(result).toEqual(updated)
    expect(store.reports[0]?.status).toBe('RESOLVED')
  })

  it('should not crash if report not found during status update', async () => {
    const store = useErrorReportsStore()
    store.reports = []

    vi.mocked(errorReportApi.updateStatus).mockResolvedValue({
      data: { data: { ...mockReport, status: 'IGNORED' } },
    } as any)

    await store.updateStatus('report-x', 'IGNORED')
    // Should not throw
  })

  // ==================== createGithubIssue ====================

  it('should update report with github URL after creating issue', async () => {
    const store = useErrorReportsStore()
    store.reports = [{ ...mockReport }]

    const updated = { ...mockReport, githubIssueUrl: 'https://github.com/org/repo/issues/42', status: 'REPORTED' }
    vi.mocked(errorReportApi.createGithubIssue).mockResolvedValue({
      data: { data: updated },
    } as any)

    const result = await store.createGithubIssue('report-1')

    expect(result).toEqual(updated)
    expect(store.reports[0]?.githubIssueUrl).toBe('https://github.com/org/repo/issues/42')
    expect(store.reports[0]?.status).toBe('REPORTED')
  })

  // ==================== deleteReport ====================

  it('should remove report from list and decrement total', async () => {
    const store = useErrorReportsStore()
    store.reports = [{ ...mockReport }]
    store.totalRecords = 1

    vi.mocked(errorReportApi.deleteReport).mockResolvedValue({ data: { data: null } } as any)

    await store.deleteReport('report-1')

    expect(store.reports).toHaveLength(0)
    expect(store.totalRecords).toBe(0)
  })

  // ==================== updateGithubConfig ====================

  it('should update github config', async () => {
    const store = useErrorReportsStore()
    vi.mocked(errorReportApi.updateGithubConfig).mockResolvedValue({ data: { data: null } } as any)

    await store.updateGithubConfig('org/repo', 'ghp_secret')

    expect(store.githubRepo).toBe('org/repo')
    expect(store.githubPatConfigured).toBe(true)
    expect(errorReportApi.updateGithubConfig).toHaveBeenCalledWith({
      githubRepo: 'org/repo',
      githubPat: 'ghp_secret',
    })
  })
})
