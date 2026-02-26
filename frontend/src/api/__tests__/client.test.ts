import { describe, it, expect } from 'vitest'
import client from '@/api/client'

describe('API Client', () => {
  it('should have a base URL of /api/v1', () => {
    expect(client.defaults.baseURL).toBe('/api/v1')
  })

  it('should have a timeout configured', () => {
    expect(client.defaults.timeout).toBe(30000)
  })

  it('should have Content-Type header set to application/json', () => {
    expect(client.defaults.headers['Content-Type']).toBe('application/json')
  })
})
