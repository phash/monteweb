import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('../client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: { data: {} } }),
    post: vi.fn().mockResolvedValue({ data: { data: { bookmarked: true } } }),
  },
}))

import client from '../client'
import { bookmarksApi } from '../bookmarks.api'

describe('bookmarksApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('toggle', () => {
    it('should POST /bookmarks with contentType and contentId', async () => {
      await bookmarksApi.toggle('POST', 'abc-123')
      expect(client.post).toHaveBeenCalledWith('/bookmarks', {
        contentType: 'POST',
        contentId: 'abc-123',
      })
    })
  })

  describe('list', () => {
    it('should GET /bookmarks with params', async () => {
      await bookmarksApi.list({ type: 'EVENT', page: 0, size: 10 })
      expect(client.get).toHaveBeenCalledWith('/bookmarks', {
        params: { type: 'EVENT', page: 0, size: 10 },
      })
    })

    it('should GET /bookmarks without params', async () => {
      await bookmarksApi.list()
      expect(client.get).toHaveBeenCalledWith('/bookmarks', { params: undefined })
    })
  })

  describe('check', () => {
    it('should GET /bookmarks/check with contentType and contentId', async () => {
      await bookmarksApi.check('JOB', 'job-123')
      expect(client.get).toHaveBeenCalledWith('/bookmarks/check', {
        params: { contentType: 'JOB', contentId: 'job-123' },
      })
    })
  })

  describe('getBookmarkedIds', () => {
    it('should GET /bookmarks/ids with contentType', async () => {
      await bookmarksApi.getBookmarkedIds('WIKI_PAGE')
      expect(client.get).toHaveBeenCalledWith('/bookmarks/ids', {
        params: { contentType: 'WIKI_PAGE' },
      })
    })
  })
})
