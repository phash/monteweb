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
import { familyApi } from '../family.api'

describe('familyApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getMine', () => {
    it('should GET /families/mine', async () => {
      await familyApi.getMine()
      expect(client.get).toHaveBeenCalledWith('/families/mine')
    })
  })

  describe('getAll', () => {
    it('should GET /families', async () => {
      await familyApi.getAll()
      expect(client.get).toHaveBeenCalledWith('/families')
    })
  })

  describe('create', () => {
    it('should POST /families with name', async () => {
      await familyApi.create('Mueller Family')
      expect(client.post).toHaveBeenCalledWith('/families', { name: 'Mueller Family' })
    })
  })

  describe('generateInviteCode', () => {
    it('should POST /families/{id}/invite', async () => {
      await familyApi.generateInviteCode('fam-1')
      expect(client.post).toHaveBeenCalledWith('/families/fam-1/invite')
    })
  })

  describe('join', () => {
    it('should POST /families/join with code', async () => {
      await familyApi.join('ABC123')
      expect(client.post).toHaveBeenCalledWith('/families/join', { inviteCode: 'ABC123' })
    })
  })

  describe('addChild', () => {
    it('should POST /families/{id}/children', async () => {
      await familyApi.addChild('fam-1', 'child-1')
      expect(client.post).toHaveBeenCalledWith('/families/fam-1/children', { childUserId: 'child-1' })
    })
  })

  describe('removeMember', () => {
    it('should DELETE /families/{id}/members/{memberId}', async () => {
      await familyApi.removeMember('fam-1', 'user-1')
      expect(client.delete).toHaveBeenCalledWith('/families/fam-1/members/user-1')
    })
  })

  describe('invitations', () => {
    it('should POST /families/{id}/invitations to invite member', async () => {
      await familyApi.inviteMember('fam-1', 'user-2', 'PARENT')
      expect(client.post).toHaveBeenCalledWith('/families/fam-1/invitations', {
        inviteeId: 'user-2',
        role: 'PARENT',
      })
    })

    it('should GET /families/my-invitations', async () => {
      await familyApi.getMyInvitations()
      expect(client.get).toHaveBeenCalledWith('/families/my-invitations')
    })

    it('should POST accept invitation', async () => {
      await familyApi.acceptInvitation('inv-1')
      expect(client.post).toHaveBeenCalledWith('/families/invitations/inv-1/accept')
    })

    it('should POST decline invitation', async () => {
      await familyApi.declineInvitation('inv-1')
      expect(client.post).toHaveBeenCalledWith('/families/invitations/inv-1/decline')
    })

    it('should GET family invitations', async () => {
      await familyApi.getFamilyInvitations('fam-1')
      expect(client.get).toHaveBeenCalledWith('/families/fam-1/invitations')
    })
  })

  describe('avatar', () => {
    it('should POST FormData to /families/{id}/avatar', async () => {
      const file = new File(['img'], 'avatar.jpg', { type: 'image/jpeg' })
      await familyApi.uploadAvatar('fam-1', file)
      const [url, formData] = vi.mocked(client.post).mock.calls[0]
      expect(url).toBe('/families/fam-1/avatar')
      expect(formData).toBeInstanceOf(FormData)
    })

    it('should DELETE /families/{id}/avatar', async () => {
      await familyApi.removeAvatar('fam-1')
      expect(client.delete).toHaveBeenCalledWith('/families/fam-1/avatar')
    })
  })
})
