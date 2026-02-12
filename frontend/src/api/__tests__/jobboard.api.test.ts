import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('../client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: { data: {} } }),
    post: vi.fn().mockResolvedValue({ data: { data: {} } }),
    put: vi.fn().mockResolvedValue({ data: { data: {} } }),
    delete: vi.fn().mockResolvedValue({ data: { data: null } }),
  },
}))

import client from '../client'
import { jobboardApi } from '../jobboard.api'

describe('jobboardApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('listJobs', () => {
    it('should GET /jobs with default pagination', async () => {
      await jobboardApi.listJobs()
      expect(client.get).toHaveBeenCalledWith('/jobs', {
        params: { page: 0, size: 20, category: undefined, status: undefined, eventId: undefined },
      })
    })

    it('should pass category and status filters', async () => {
      await jobboardApi.listJobs(0, 10, 'cleaning', ['OPEN', 'IN_PROGRESS'] as any)
      expect(client.get).toHaveBeenCalledWith('/jobs', {
        params: { page: 0, size: 10, category: 'cleaning', status: 'OPEN,IN_PROGRESS', eventId: undefined },
      })
    })
  })

  describe('getJob', () => {
    it('should GET /jobs/{id}', async () => {
      await jobboardApi.getJob('job-1')
      expect(client.get).toHaveBeenCalledWith('/jobs/job-1')
    })
  })

  describe('createJob', () => {
    it('should POST /jobs', async () => {
      const data = { title: 'Help needed', description: 'desc', estimatedHours: 2 }
      await jobboardApi.createJob(data as any)
      expect(client.post).toHaveBeenCalledWith('/jobs', data)
    })
  })

  describe('updateJob', () => {
    it('should PUT /jobs/{id}', async () => {
      await jobboardApi.updateJob('job-1', { title: 'Updated' })
      expect(client.put).toHaveBeenCalledWith('/jobs/job-1', { title: 'Updated' })
    })
  })

  describe('cancelJob', () => {
    it('should DELETE /jobs/{id}', async () => {
      await jobboardApi.cancelJob('job-1')
      expect(client.delete).toHaveBeenCalledWith('/jobs/job-1')
    })
  })

  describe('assignments', () => {
    it('should GET /jobs/{id}/assignments', async () => {
      await jobboardApi.getAssignments('job-1')
      expect(client.get).toHaveBeenCalledWith('/jobs/job-1/assignments')
    })

    it('should POST /jobs/{id}/apply', async () => {
      await jobboardApi.applyForJob('job-1')
      expect(client.post).toHaveBeenCalledWith('/jobs/job-1/apply')
    })

    it('should PUT /jobs/assignments/{id}/start', async () => {
      await jobboardApi.startAssignment('assign-1')
      expect(client.put).toHaveBeenCalledWith('/jobs/assignments/assign-1/start')
    })

    it('should PUT /jobs/assignments/{id}/complete', async () => {
      await jobboardApi.completeAssignment('assign-1', 3.5, 'Done well')
      expect(client.put).toHaveBeenCalledWith('/jobs/assignments/assign-1/complete', {
        actualHours: 3.5,
        notes: 'Done well',
      })
    })

    it('should PUT /jobs/assignments/{id}/confirm', async () => {
      await jobboardApi.confirmAssignment('assign-1')
      expect(client.put).toHaveBeenCalledWith('/jobs/assignments/assign-1/confirm')
    })

    it('should DELETE /jobs/assignments/{id}', async () => {
      await jobboardApi.cancelAssignment('assign-1')
      expect(client.delete).toHaveBeenCalledWith('/jobs/assignments/assign-1')
    })
  })

  describe('hours and reports', () => {
    it('should GET /jobs/family/{id}/hours', async () => {
      await jobboardApi.getFamilyHours('fam-1')
      expect(client.get).toHaveBeenCalledWith('/jobs/family/fam-1/hours')
    })

    it('should GET /jobs/report', async () => {
      await jobboardApi.getReport()
      expect(client.get).toHaveBeenCalledWith('/jobs/report')
    })

    it('should GET /jobs/report/summary', async () => {
      await jobboardApi.getReportSummary()
      expect(client.get).toHaveBeenCalledWith('/jobs/report/summary')
    })

    it('should GET /jobs/report/export as blob', async () => {
      await jobboardApi.exportCsv()
      expect(client.get).toHaveBeenCalledWith('/jobs/report/export', { responseType: 'blob' })
    })

    it('should GET /jobs/report/pdf as blob', async () => {
      await jobboardApi.exportPdf()
      expect(client.get).toHaveBeenCalledWith('/jobs/report/pdf', { responseType: 'blob' })
    })
  })

  describe('linkEvent', () => {
    it('should PUT /jobs/{id}/link-event', async () => {
      await jobboardApi.linkEvent('job-1', 'evt-1')
      expect(client.put).toHaveBeenCalledWith('/jobs/job-1/link-event', { eventId: 'evt-1' })
    })
  })
})
