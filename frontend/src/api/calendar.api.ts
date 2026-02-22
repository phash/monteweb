import client from './client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { CalendarEvent, CreateEventRequest, UpdateEventRequest, RsvpStatus } from '@/types/calendar'
import type { JobInfo } from '@/types/jobboard'

export const calendarApi = {
  getEvents(from: string, to: string, page = 0, size = 50) {
    return client.get<ApiResponse<PageResponse<CalendarEvent>>>('/calendar/events', {
      params: { from, to, page, size },
    })
  },

  getEvent(id: string) {
    return client.get<ApiResponse<CalendarEvent>>(`/calendar/events/${id}`)
  },

  createEvent(data: CreateEventRequest) {
    return client.post<ApiResponse<CalendarEvent>>('/calendar/events', data)
  },

  updateEvent(id: string, data: UpdateEventRequest) {
    return client.put<ApiResponse<CalendarEvent>>(`/calendar/events/${id}`, data)
  },

  deleteEvent(id: string) {
    return client.delete<ApiResponse<void>>(`/calendar/events/${id}`)
  },

  cancelEvent(id: string) {
    return client.post<ApiResponse<CalendarEvent>>(`/calendar/events/${id}/cancel`)
  },

  rsvp(id: string, status: RsvpStatus) {
    return client.post<ApiResponse<CalendarEvent>>(`/calendar/events/${id}/rsvp`, { status })
  },

  getRoomEvents(roomId: string, from?: string, to?: string, page = 0, size = 50) {
    return client.get<ApiResponse<PageResponse<CalendarEvent>>>(`/calendar/rooms/${roomId}/events`, {
      params: { from, to, page, size },
    })
  },

  getEventJobs(eventId: string) {
    return client.get<ApiResponse<JobInfo[]>>(`/calendar/events/${eventId}/jobs`)
  },

  exportEvent(id: string) {
    return client.get(`/calendar/events/${id}/export`, { responseType: 'blob' })
  },

  exportCalendar(from: string, to: string) {
    return client.get('/calendar/export', { params: { from, to }, responseType: 'blob' })
  },
}
