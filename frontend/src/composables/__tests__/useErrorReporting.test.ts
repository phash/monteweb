import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('axios', () => ({
  default: {
    post: vi.fn().mockResolvedValue({}),
  },
}))

import axios from 'axios'
import { reportError } from '@/composables/useErrorReporting'

describe('useErrorReporting - reportError', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Reset the module to clear the recentFingerprints set between tests
    vi.resetModules()
  })

  it('should post error data to the API', async () => {
    const { reportError: freshReportError } = await import('@/composables/useErrorReporting')

    freshReportError({
      source: 'TestView',
      errorType: 'TypeError',
      message: 'Something went wrong',
      stackTrace: 'Error at line 1',
      location: '/test',
    })

    // Fire-and-forget, so wait for microtask
    await new Promise((r) => setTimeout(r, 0))
    expect(axios.post).toHaveBeenCalledWith('/api/v1/error-reports', expect.objectContaining({
      source: 'TestView',
      errorType: 'TypeError',
      message: 'Something went wrong',
    }))
  })

  it('should include requestUrl (pathname only, no userAgent)', async () => {
    const { reportError: freshReportError } = await import('@/composables/useErrorReporting')

    freshReportError({ source: 'AnotherView', message: 'Another error' })

    await new Promise((r) => setTimeout(r, 0))
    expect(axios.post).toHaveBeenCalledWith('/api/v1/error-reports', expect.objectContaining({
      requestUrl: expect.any(String),
    }))
    expect(axios.post).not.toHaveBeenCalledWith('/api/v1/error-reports', expect.objectContaining({
      userAgent: expect.anything(),
    }))
  })

  it('should silently ignore when axios fails', async () => {
    vi.mocked(axios.post).mockRejectedValue(new Error('Network error'))
    const { reportError: freshReportError } = await import('@/composables/useErrorReporting')

    // Should not throw
    expect(() =>
      freshReportError({ source: 'FailView', message: 'Test error' }),
    ).not.toThrow()

    await new Promise((r) => setTimeout(r, 10))
  })

  it('should deduplicate the same error within 30 seconds', async () => {
    const { reportError: freshReportError } = await import('@/composables/useErrorReporting')

    freshReportError({ source: 'DedupeView', message: 'Duplicate error' })
    freshReportError({ source: 'DedupeView', message: 'Duplicate error' })

    await new Promise((r) => setTimeout(r, 0))
    expect(axios.post).toHaveBeenCalledTimes(1)
  })
})
