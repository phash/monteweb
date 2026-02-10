import client from './client'
import type { ApiResponse } from '@/types/api'
import type { FamilyInfo } from '@/types/family'

export const familyApi = {
  getMine() {
    return client.get<ApiResponse<FamilyInfo[]>>('/families/mine')
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
}
