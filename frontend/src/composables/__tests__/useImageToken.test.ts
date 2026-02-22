import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
  },
}))

import client from '@/api/client'
import { useImageToken, authenticatedImageUrl } from '@/composables/useImageToken'

describe('useImageToken', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    const { clearImageToken } = useImageToken()
    clearImageToken()
  })

  it('should start with no token', () => {
    const { getImageToken } = useImageToken()
    expect(getImageToken()).toBeNull()
  })

  it('should fetch and store image token', async () => {
    vi.mocked(client.get).mockResolvedValue({
      data: { data: { token: 'test-image-token' } },
    } as any)

    const { fetchImageToken, getImageToken } = useImageToken()
    const token = await fetchImageToken()

    expect(token).toBe('test-image-token')
    expect(getImageToken()).toBe('test-image-token')
    expect(client.get).toHaveBeenCalledWith('/image-token')
  })

  it('should return null on fetch error', async () => {
    vi.mocked(client.get).mockRejectedValue(new Error('Unauthorized'))

    const { fetchImageToken, getImageToken } = useImageToken()
    const token = await fetchImageToken()

    expect(token).toBeNull()
    expect(getImageToken()).toBeNull()
  })

  it('should clear token', async () => {
    vi.mocked(client.get).mockResolvedValue({
      data: { data: { token: 'test-image-token' } },
    } as any)

    const { fetchImageToken, clearImageToken, getImageToken } = useImageToken()
    await fetchImageToken()
    expect(getImageToken()).toBe('test-image-token')

    clearImageToken()
    expect(getImageToken()).toBeNull()
  })
})

describe('authenticatedImageUrl', () => {
  beforeEach(() => {
    const { clearImageToken } = useImageToken()
    clearImageToken()
  })

  it('should return path without token when no image token', () => {
    const url = authenticatedImageUrl('/api/v1/fotobox/images/123')
    expect(url).toBe('/api/v1/fotobox/images/123')
  })

  it('should append token to path when image token exists', async () => {
    vi.mocked(client.get).mockResolvedValue({
      data: { data: { token: 'img-tok-123' } },
    } as any)

    const { fetchImageToken } = useImageToken()
    await fetchImageToken()

    const url = authenticatedImageUrl('/api/v1/fotobox/images/123')
    expect(url).toBe('/api/v1/fotobox/images/123?token=img-tok-123')
  })
})
