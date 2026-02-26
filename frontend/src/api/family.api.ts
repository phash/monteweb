import client from './client'
import type { ApiResponse } from '@/types/api'
import type { CalendarEvent } from '@/types/calendar'
import type { FamilyInfo, FamilyInvitationInfo } from '@/types/family'

export const familyApi = {
  getMine() {
    return client.get<ApiResponse<FamilyInfo[]>>('/families/mine')
  },

  getAll() {
    return client.get<ApiResponse<FamilyInfo[]>>('/families')
  },

  create(name: string) {
    return client.post<ApiResponse<FamilyInfo>>('/families', { name })
  },

  generateInviteCode(familyId: string) {
    return client.post<ApiResponse<{ inviteCode: string }>>(`/families/${familyId}/invite`)
  },

  join(inviteCode: string) {
    return client.post<ApiResponse<FamilyInfo>>('/families/join', { inviteCode })
  },

  addChild(familyId: string, childUserId: string) {
    return client.post<ApiResponse<FamilyInfo>>(`/families/${familyId}/children`, {
      childUserId,
    })
  },

  removeMember(familyId: string, memberId: string) {
    return client.delete<ApiResponse<void>>(`/families/${familyId}/members/${memberId}`)
  },

  leaveFamily(familyId: string) {
    return client.post<ApiResponse<void>>(`/families/${familyId}/leave`)
  },

  uploadAvatar(familyId: string, file: File) {
    const form = new FormData()
    form.append('file', file)
    return client.post<ApiResponse<void>>(`/families/${familyId}/avatar`, form)
  },

  removeAvatar(familyId: string) {
    return client.delete<ApiResponse<void>>(`/families/${familyId}/avatar`)
  },

  updateName(familyId: string, name: string) {
    return client.put<ApiResponse<FamilyInfo>>(`/families/${familyId}`, { name })
  },

  deleteFamily(familyId: string) {
    return client.delete<ApiResponse<void>>(`/families/${familyId}`)
  },

  setHoursExempt(familyId: string, exempt: boolean) {
    return client.put<ApiResponse<FamilyInfo>>(`/families/${familyId}/hours-exempt`, { exempt })
  },

  setActive(familyId: string, active: boolean) {
    return client.put<ApiResponse<FamilyInfo>>(`/families/${familyId}/active`, { active })
  },

  adminAddMember(familyId: string, userId: string, role: string) {
    return client.post<ApiResponse<FamilyInfo>>(`/families/${familyId}/members`, { userId, role })
  },

  adminRemoveMember(familyId: string, memberId: string) {
    return client.delete<ApiResponse<void>>(`/families/${familyId}/members/${memberId}/admin`)
  },

  // Invitations
  inviteMember(familyId: string, inviteeId: string, role: string) {
    return client.post<ApiResponse<FamilyInvitationInfo>>(`/families/${familyId}/invitations`, { inviteeId, role })
  },

  getMyInvitations() {
    return client.get<ApiResponse<FamilyInvitationInfo[]>>('/families/my-invitations')
  },

  acceptInvitation(invitationId: string) {
    return client.post<ApiResponse<FamilyInvitationInfo>>(`/families/invitations/${invitationId}/accept`)
  },

  declineInvitation(invitationId: string) {
    return client.post<ApiResponse<FamilyInvitationInfo>>(`/families/invitations/${invitationId}/decline`)
  },

  getFamilyInvitations(familyId: string) {
    return client.get<ApiResponse<FamilyInvitationInfo[]>>(`/families/${familyId}/invitations`)
  },

  // Sole custody
  requestSoleCustody(familyId: string, soleCustody: boolean) {
    return client.put<ApiResponse<FamilyInfo>>(`/families/${familyId}/sole-custody`, { soleCustody })
  },

  approveSoleCustody(familyId: string) {
    return client.put<ApiResponse<FamilyInfo>>(`/families/${familyId}/sole-custody/approve`)
  },

  // Family Calendar
  getFamilyCalendar(familyId: string, from: string, to: string) {
    return client.get<ApiResponse<CalendarEvent[]>>(`/families/${familyId}/calendar`, {
      params: { from, to },
    })
  },

  downloadFamilyIcal(familyId: string) {
    return client.get(`/families/${familyId}/calendar/ical`, { responseType: 'blob' })
  },
}
