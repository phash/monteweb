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
import { calendarApi } from '../calendar.api'

describe('calendarApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getEvents', () => {
    it('should GET /calendar/events with date range', async () => {
      await calendarApi.getEvents('2026-01-01', '2026-01-31')
      expect(client.get).toHaveBeenCalledWith('/calendar/events', {
        params: { from: '2026-01-01', to: '2026-01-31', page: 0, size: 50 },
      })
    })

    it('should pass custom page and size', async () => {
      await calendarApi.getEvents('2026-01-01', '2026-01-31', 2, 10)
      expect(client.get).toHaveBeenCalledWith('/calendar/events', {
        params: { from: '2026-01-01', to: '2026-01-31', page: 2, size: 10 },
      })
    })
  })

  describe('getEvent', () => {
    it('should GET /calendar/events/{id}', async () => {
      await calendarApi.getEvent('evt-1')
      expect(client.get).toHaveBeenCalledWith('/calendar/events/evt-1')
    })
  })

  describe('createEvent', () => {
    it('should POST /calendar/events with data', async () => {
      const data = { title: 'School Fair', startDate: '2026-03-01', scope: 'SCHOOL' }
      await calendarApi.createEvent(data as any)
      expect(client.post).toHaveBeenCalledWith('/calendar/events', data)
    })
  })

  describe('updateEvent', () => {
    it('should PUT /calendar/events/{id}', async () => {
      const data = { title: 'Updated Title' }
      await calendarApi.updateEvent('evt-1', data as any)
      expect(client.put).toHaveBeenCalledWith('/calendar/events/evt-1', data)
    })
  })

  describe('deleteEvent', () => {
    it('should DELETE /calendar/events/{id}', async () => {
      await calendarApi.deleteEvent('evt-1')
      expect(client.delete).toHaveBeenCalledWith('/calendar/events/evt-1')
    })
  })

  describe('cancelEvent', () => {
    it('should POST /calendar/events/{id}/cancel', async () => {
      await calendarApi.cancelEvent('evt-1')
      expect(client.post).toHaveBeenCalledWith('/calendar/events/evt-1/cancel')
    })
  })

  describe('rsvp', () => {
    it('should POST /calendar/events/{id}/rsvp with status', async () => {
      await calendarApi.rsvp('evt-1', 'ATTENDING' as any)
      expect(client.post).toHaveBeenCalledWith('/calendar/events/evt-1/rsvp', { status: 'ATTENDING' })
    })
  })

  describe('getRoomEvents', () => {
    it('should GET /calendar/rooms/{roomId}/events', async () => {
      await calendarApi.getRoomEvents('room-1', '2026-01-01', '2026-01-31')
      expect(client.get).toHaveBeenCalledWith('/calendar/rooms/room-1/events', {
        params: { from: '2026-01-01', to: '2026-01-31', page: 0, size: 50 },
      })
    })
  })

  describe('getEventJobs', () => {
    it('should GET /calendar/events/{id}/jobs', async () => {
      await calendarApi.getEventJobs('evt-1')
      expect(client.get).toHaveBeenCalledWith('/calendar/events/evt-1/jobs')
    })
  })

  describe('getICalSubscriptions', () => {
    it('should GET /calendar/ical/subscriptions', async () => {
      await calendarApi.getICalSubscriptions()
      expect(client.get).toHaveBeenCalledWith('/calendar/ical/subscriptions')
    })
  })

  describe('createICalSubscription', () => {
    it('should POST /calendar/ical/subscriptions with data', async () => {
      const data = { name: 'School Holidays', url: 'https://example.com/cal.ics', color: '#ff0000' }
      await calendarApi.createICalSubscription(data)
      expect(client.post).toHaveBeenCalledWith('/calendar/ical/subscriptions', data)
    })
  })

  describe('deleteICalSubscription', () => {
    it('should DELETE /calendar/ical/subscriptions/{id}', async () => {
      await calendarApi.deleteICalSubscription('sub-1')
      expect(client.delete).toHaveBeenCalledWith('/calendar/ical/subscriptions/sub-1')
    })
  })

  describe('syncICalSubscription', () => {
    it('should POST /calendar/ical/subscriptions/{id}/sync', async () => {
      await calendarApi.syncICalSubscription('sub-1')
      expect(client.post).toHaveBeenCalledWith('/calendar/ical/subscriptions/sub-1/sync')
    })
  })

  describe('getICalEvents', () => {
    it('should GET /calendar/ical/events with date range', async () => {
      await calendarApi.getICalEvents('2026-01-01', '2026-12-31')
      expect(client.get).toHaveBeenCalledWith('/calendar/ical/events', {
        params: { from: '2026-01-01', to: '2026-12-31' },
      })
    })
  })

  describe('generateJitsiRoom', () => {
    it('should POST /calendar/events/{id}/jitsi', async () => {
      await calendarApi.generateJitsiRoom('evt-1')
      expect(client.post).toHaveBeenCalledWith('/calendar/events/evt-1/jitsi')
    })
  })

  describe('removeJitsiRoom', () => {
    it('should DELETE /calendar/events/{id}/jitsi', async () => {
      await calendarApi.removeJitsiRoom('evt-1')
      expect(client.delete).toHaveBeenCalledWith('/calendar/events/evt-1/jitsi')
    })
  })
})
