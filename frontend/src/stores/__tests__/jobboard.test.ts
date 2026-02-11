import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useJobboardStore } from '@/stores/jobboard'

vi.mock('@/api/jobboard.api', () => ({
  jobboardApi: {
    listJobs: vi.fn(),
    getJob: vi.fn(),
    createJob: vi.fn(),
    applyForJob: vi.fn(),
    getCategories: vi.fn(),
    getReport: vi.fn(),
    getReportSummary: vi.fn(),
    getAssignments: vi.fn(),
    getMyAssignments: vi.fn(),
    startAssignment: vi.fn(),
    completeAssignment: vi.fn(),
    confirmAssignment: vi.fn(),
    getFamilyHours: vi.fn(),
    exportCsv: vi.fn(),
    exportPdf: vi.fn(),
  },
}))

import { jobboardApi } from '@/api/jobboard.api'

describe('Jobboard Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should start with empty state and hasMore=true', () => {
    const store = useJobboardStore()
    expect(store.jobs).toEqual([])
    expect(store.currentJob).toBeNull()
    expect(store.categories).toEqual([])
    expect(store.hasMore).toBe(true)
    expect(store.loading).toBe(false)
  })

  it('should fetch jobs with reset=true', async () => {
    const store = useJobboardStore()
    const mockJobs = [{ id: 'j1', title: 'Job 1' }, { id: 'j2', title: 'Job 2' }]

    vi.mocked(jobboardApi.listJobs).mockResolvedValue({
      data: { data: { content: mockJobs, last: false } },
    } as any)

    await store.fetchJobs(true)

    expect(store.jobs).toHaveLength(2)
    expect(store.hasMore).toBe(true)
  })

  it('should append jobs on pagination', async () => {
    const store = useJobboardStore()

    vi.mocked(jobboardApi.listJobs)
      .mockResolvedValueOnce({
        data: { data: { content: [{ id: 'j1' }], last: false } },
      } as any)
      .mockResolvedValueOnce({
        data: { data: { content: [{ id: 'j2' }], last: true } },
      } as any)

    await store.fetchJobs(true)
    await store.fetchJobs()

    expect(store.jobs).toHaveLength(2)
  })

  it('should set hasMore=false on last page', async () => {
    const store = useJobboardStore()

    vi.mocked(jobboardApi.listJobs).mockResolvedValue({
      data: { data: { content: [{ id: 'j1' }], last: true } },
    } as any)

    await store.fetchJobs(true)

    expect(store.hasMore).toBe(false)
  })

  it('should fetch single job', async () => {
    const store = useJobboardStore()
    const mockJob = { id: 'j1', title: 'Job 1', currentAssignees: 2 }

    vi.mocked(jobboardApi.getJob).mockResolvedValue({
      data: { data: mockJob },
    } as any)

    await store.fetchJob('j1')

    expect(store.currentJob).toEqual(mockJob)
  })

  it('should create job and prepend to list', async () => {
    const store = useJobboardStore()
    const newJob = { id: 'j-new', title: 'New Job' }

    vi.mocked(jobboardApi.createJob).mockResolvedValue({
      data: { data: newJob },
    } as any)

    const result = await store.createJob({ title: 'New Job' } as any)

    expect(result.id).toBe('j-new')
    expect(store.jobs[0].id).toBe('j-new')
  })

  it('should apply for job and increment currentAssignees', async () => {
    const store = useJobboardStore()
    store.jobs = [{ id: 'j1', title: 'Job 1', currentAssignees: 1 }] as any
    const assignment = { id: 'a1', jobId: 'j1', status: 'APPLIED' }

    vi.mocked(jobboardApi.applyForJob).mockResolvedValue({
      data: { data: assignment },
    } as any)

    const result = await store.applyForJob('j1')

    expect(result.id).toBe('a1')
    expect(store.myAssignments).toHaveLength(1)
    expect(store.jobs[0].currentAssignees).toBe(2)
  })

  it('should fetch categories', async () => {
    const store = useJobboardStore()

    vi.mocked(jobboardApi.getCategories).mockResolvedValue({
      data: { data: ['Garten', 'Küche', 'Büro'] },
    } as any)

    await store.fetchCategories()

    expect(store.categories).toEqual(['Garten', 'Küche', 'Büro'])
  })

  it('should fetch report and summary in parallel', async () => {
    const store = useJobboardStore()
    const mockReport = [{ familyId: 'f1', totalHours: 10 }]
    const mockSummary = { totalFamilies: 5, totalHours: 50 }

    vi.mocked(jobboardApi.getReport).mockResolvedValue({
      data: { data: mockReport },
    } as any)
    vi.mocked(jobboardApi.getReportSummary).mockResolvedValue({
      data: { data: mockSummary },
    } as any)

    await store.fetchReport()

    expect(store.report).toEqual(mockReport)
    expect(store.reportSummary).toEqual(mockSummary)
  })
})
