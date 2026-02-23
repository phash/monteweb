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
import { wikiApi } from '../wiki.api'

describe('wiki.api', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getPageTree should GET /rooms/{roomId}/wiki', async () => {
    await wikiApi.getPageTree('room-1')
    expect(client.get).toHaveBeenCalledWith('/rooms/room-1/wiki')
  })

  it('getPage should GET /rooms/{roomId}/wiki/pages/{slug}', async () => {
    await wikiApi.getPage('room-1', 'test-page')
    expect(client.get).toHaveBeenCalledWith('/rooms/room-1/wiki/pages/test-page')
  })

  it('createPage should POST /rooms/{roomId}/wiki/pages', async () => {
    const data = { title: 'Test', content: 'Hello' }
    await wikiApi.createPage('room-1', data)
    expect(client.post).toHaveBeenCalledWith('/rooms/room-1/wiki/pages', data)
  })

  it('updatePage should PUT /rooms/{roomId}/wiki/pages/{pageId}', async () => {
    const data = { title: 'Updated', content: 'World' }
    await wikiApi.updatePage('room-1', 'page-1', data)
    expect(client.put).toHaveBeenCalledWith('/rooms/room-1/wiki/pages/page-1', data)
  })

  it('deletePage should DELETE /rooms/{roomId}/wiki/pages/{pageId}', async () => {
    await wikiApi.deletePage('room-1', 'page-1')
    expect(client.delete).toHaveBeenCalledWith('/rooms/room-1/wiki/pages/page-1')
  })

  it('getVersions should GET /rooms/{roomId}/wiki/pages/{pageId}/versions', async () => {
    await wikiApi.getVersions('room-1', 'page-1')
    expect(client.get).toHaveBeenCalledWith('/rooms/room-1/wiki/pages/page-1/versions')
  })

  it('getVersion should GET /rooms/{roomId}/wiki/versions/{versionId}', async () => {
    await wikiApi.getVersion('room-1', 'ver-1')
    expect(client.get).toHaveBeenCalledWith('/rooms/room-1/wiki/versions/ver-1')
  })

  it('searchPages should GET /rooms/{roomId}/wiki/search with query param', async () => {
    await wikiApi.searchPages('room-1', 'hello')
    expect(client.get).toHaveBeenCalledWith('/rooms/room-1/wiki/search', { params: { q: 'hello' } })
  })
})
