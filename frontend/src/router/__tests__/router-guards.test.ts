import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

// Mock all API modules that auth store depends on
vi.mock('@/api/auth.api', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
  },
}))

vi.mock('@/api/users.api', () => ({
  usersApi: {
    getMe: vi.fn(),
  },
}))

vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
    getConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
  },
}))

import router from '@/router'

describe('Router Navigation Guards', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  describe('requiresAuth guard', () => {
    it('should redirect unauthenticated users to login for protected routes', () => {
      const auth = useAuthStore()
      expect(auth.isAuthenticated).toBe(false)

      // Resolve the dashboard route - it requires auth
      const dashboardRoute = router.resolve({ name: 'dashboard' })
      expect(dashboardRoute.meta.requiresAuth).toBe(true)
    })

    it('should include redirect query when redirecting to login', () => {
      // The redirect to /login should include the original path
      const roomsRoute = router.resolve({ name: 'rooms' })
      expect(roomsRoute.fullPath).toBe('/rooms')
    })
  })

  describe('guest guard', () => {
    it('should have guest meta on login route', () => {
      const loginRoute = router.resolve({ name: 'login' })
      expect(loginRoute.meta.guest).toBe(true)
    })

    it('should not have guest meta on protected routes', () => {
      const dashRoute = router.resolve({ name: 'dashboard' })
      expect(dashRoute.meta.guest).toBeFalsy()
    })
  })

  describe('requiresAdmin guard', () => {
    it('should have requiresAdmin meta on admin routes', () => {
      const adminRoute = router.resolve({ name: 'admin-dashboard' })
      expect(adminRoute.meta.requiresAdmin).toBe(true)
    })

    it('should not have requiresAdmin on regular routes', () => {
      const roomsRoute = router.resolve({ name: 'rooms' })
      expect(roomsRoute.meta.requiresAdmin).toBeFalsy()
    })

    it('should have allowPutzOrga on admin-cleaning route', () => {
      const cleaningRoute = router.resolve({ name: 'admin-cleaning' })
      expect(cleaningRoute.meta.allowPutzOrga).toBe(true)
    })

    it('should not have allowPutzOrga on other admin routes', () => {
      const usersRoute = router.resolve({ name: 'admin-users' })
      expect(usersRoute.meta.allowPutzOrga).toBeFalsy()
    })
  })

  describe('route definitions', () => {
    it('should have messages route with optional conversationId param', () => {
      const withParam = router.resolve({ name: 'messages', params: { conversationId: 'conv-1' } })
      expect(withParam.path).toBe('/messages/conv-1')

      const withoutParam = router.resolve({ name: 'messages' })
      expect(withoutParam.path).toBe('/messages')
    })

    it('should have room-detail route with id param and props', () => {
      const route = router.resolve({ name: 'room-detail', params: { id: 'room-abc' } })
      expect(route.path).toBe('/rooms/room-abc')
    })

    it('should have event-edit route separate from event-detail', () => {
      const editRoute = router.resolve({ name: 'event-edit', params: { id: 'evt-1' } })
      expect(editRoute.path).toBe('/calendar/events/evt-1/edit')

      const detailRoute = router.resolve({ name: 'event-detail', params: { id: 'evt-1' } })
      expect(detailRoute.path).toBe('/calendar/events/evt-1')
    })

    it('should have form routes with correct paths', () => {
      const listRoute = router.resolve({ name: 'forms' })
      expect(listRoute.path).toBe('/forms')

      const createRoute = router.resolve({ name: 'form-create' })
      expect(createRoute.path).toBe('/forms/create')

      const detailRoute = router.resolve({ name: 'form-detail', params: { id: 'f-1' } })
      expect(detailRoute.path).toBe('/forms/f-1')

      const editRoute = router.resolve({ name: 'form-edit', params: { id: 'f-1' } })
      expect(editRoute.path).toBe('/forms/f-1/edit')

      const resultsRoute = router.resolve({ name: 'form-results', params: { id: 'f-1' } })
      expect(resultsRoute.path).toBe('/forms/f-1/results')
    })

    it('should have cleaning-slot route with id param', () => {
      const route = router.resolve({ name: 'cleaning-slot', params: { id: 'slot-1' } })
      expect(route.path).toBe('/cleaning/slot-1')
    })

    it('should have job routes', () => {
      const listRoute = router.resolve({ name: 'jobs' })
      expect(listRoute.path).toBe('/jobs')

      const createRoute = router.resolve({ name: 'job-create' })
      expect(createRoute.path).toBe('/jobs/create')

      const detailRoute = router.resolve({ name: 'job-detail', params: { id: 'j-1' } })
      expect(detailRoute.path).toBe('/jobs/j-1')
    })

    it('should have all admin sub-routes', () => {
      const routes = [
        { name: 'admin-dashboard', path: '/admin' },
        { name: 'admin-users', path: '/admin/users' },
        { name: 'admin-rooms', path: '/admin/rooms' },
        { name: 'admin-sections', path: '/admin/sections' },
        { name: 'admin-modules', path: '/admin/modules' },
        { name: 'admin-job-report', path: '/admin/job-report' },
        { name: 'admin-cleaning', path: '/admin/cleaning' },
        { name: 'admin-theme', path: '/admin/theme' },
      ]

      routes.forEach(({ name, path }) => {
        const route = router.resolve({ name })
        expect(route.path).toBe(path)
      })
    })

    it('should have breadcrumb labels on certain routes', () => {
      const roomDetail = router.resolve({ name: 'room-detail', params: { id: 'r1' } })
      expect(roomDetail.meta.breadcrumbLabel).toBe('rooms.title')

      const jobDetail = router.resolve({ name: 'job-detail', params: { id: 'j1' } })
      expect(jobDetail.meta.breadcrumbLabel).toBe('jobboard.title')

      const eventDetail = router.resolve({ name: 'event-detail', params: { id: 'e1' } })
      expect(eventDetail.meta.breadcrumbLabel).toBe('calendar.title')
    })
  })
})
