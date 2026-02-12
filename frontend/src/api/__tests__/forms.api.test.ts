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
import { formsApi } from '../forms.api'

describe('formsApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getAvailableForms', () => {
    it('should GET /forms with default pagination', async () => {
      await formsApi.getAvailableForms()
      expect(client.get).toHaveBeenCalledWith('/forms', { params: { page: 0, size: 20 } })
    })
  })

  describe('getMyForms', () => {
    it('should GET /forms/mine', async () => {
      await formsApi.getMyForms(1, 10)
      expect(client.get).toHaveBeenCalledWith('/forms/mine', { params: { page: 1, size: 10 } })
    })
  })

  describe('getForm', () => {
    it('should GET /forms/{id}', async () => {
      await formsApi.getForm('form-1')
      expect(client.get).toHaveBeenCalledWith('/forms/form-1')
    })
  })

  describe('createForm', () => {
    it('should POST /forms', async () => {
      const data = { title: 'Survey', description: 'A survey' }
      await formsApi.createForm(data as any)
      expect(client.post).toHaveBeenCalledWith('/forms', data)
    })
  })

  describe('updateForm', () => {
    it('should PUT /forms/{id}', async () => {
      const data = { title: 'Updated Survey' }
      await formsApi.updateForm('form-1', data as any)
      expect(client.put).toHaveBeenCalledWith('/forms/form-1', data)
    })
  })

  describe('deleteForm', () => {
    it('should DELETE /forms/{id}', async () => {
      await formsApi.deleteForm('form-1')
      expect(client.delete).toHaveBeenCalledWith('/forms/form-1')
    })
  })

  describe('publishForm', () => {
    it('should POST /forms/{id}/publish', async () => {
      await formsApi.publishForm('form-1')
      expect(client.post).toHaveBeenCalledWith('/forms/form-1/publish')
    })
  })

  describe('closeForm', () => {
    it('should POST /forms/{id}/close', async () => {
      await formsApi.closeForm('form-1')
      expect(client.post).toHaveBeenCalledWith('/forms/form-1/close')
    })
  })

  describe('submitResponse', () => {
    it('should POST /forms/{id}/respond', async () => {
      const data = { answers: [{ questionId: 'q1', value: 'Yes' }] }
      await formsApi.submitResponse('form-1', data as any)
      expect(client.post).toHaveBeenCalledWith('/forms/form-1/respond', data)
    })
  })

  describe('getResults', () => {
    it('should GET /forms/{id}/results', async () => {
      await formsApi.getResults('form-1')
      expect(client.get).toHaveBeenCalledWith('/forms/form-1/results')
    })
  })

  describe('getIndividualResponses', () => {
    it('should GET /forms/{id}/responses', async () => {
      await formsApi.getIndividualResponses('form-1')
      expect(client.get).toHaveBeenCalledWith('/forms/form-1/responses')
    })
  })

  describe('exports', () => {
    it('should GET /forms/{id}/results/csv as blob', async () => {
      await formsApi.exportCsv('form-1')
      expect(client.get).toHaveBeenCalledWith('/forms/form-1/results/csv', { responseType: 'blob' })
    })

    it('should GET /forms/{id}/results/pdf as blob', async () => {
      await formsApi.exportPdf('form-1')
      expect(client.get).toHaveBeenCalledWith('/forms/form-1/results/pdf', { responseType: 'blob' })
    })
  })
})
