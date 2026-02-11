import { describe, it, expect } from 'vitest'
import router from '@/router'

describe('Router', () => {
  it('should have login route', () => {
    const route = router.resolve({ name: 'login' })
    expect(route.path).toBe('/login')
    expect(route.meta.guest).toBe(true)
  })

  it('should have dashboard route at root', () => {
    const route = router.resolve({ name: 'dashboard' })
    expect(route.path).toBe('/')
  })

  it('should have rooms route', () => {
    const route = router.resolve({ name: 'rooms' })
    expect(route.path).toBe('/rooms')
  })

  it('should have family route', () => {
    const route = router.resolve({ name: 'family' })
    expect(route.path).toBe('/family')
  })

  it('should have calendar route', () => {
    const route = router.resolve({ name: 'calendar' })
    expect(route.path).toBe('/calendar')
  })

  it('should have admin routes', () => {
    const route = router.resolve({ name: 'admin-dashboard' })
    expect(route.path).toBe('/admin')
  })

  it('should have catch-all 404 route', () => {
    const route = router.resolve('/nonexistent')
    expect(route.name).toBe('not-found')
  })
})
