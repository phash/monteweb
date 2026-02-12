import { describe, it, expect } from 'vitest'
import type { RoomInfo, RoomPublicInfo, RoomSettings, RoomDetail, JoinPolicy, DiscussionMode } from '@/types/room'

describe('Room Types – Role Concept', () => {
  describe('JoinPolicy type', () => {
    it('should accept OPEN', () => {
      const policy: JoinPolicy = 'OPEN'
      expect(policy).toBe('OPEN')
    })

    it('should accept REQUEST', () => {
      const policy: JoinPolicy = 'REQUEST'
      expect(policy).toBe('REQUEST')
    })

    it('should accept INVITE_ONLY', () => {
      const policy: JoinPolicy = 'INVITE_ONLY'
      expect(policy).toBe('INVITE_ONLY')
    })
  })

  describe('DiscussionMode type', () => {
    it('should accept FULL', () => {
      const mode: DiscussionMode = 'FULL'
      expect(mode).toBe('FULL')
    })

    it('should accept ANNOUNCEMENTS_ONLY', () => {
      const mode: DiscussionMode = 'ANNOUNCEMENTS_ONLY'
      expect(mode).toBe('ANNOUNCEMENTS_ONLY')
    })

    it('should accept DISABLED', () => {
      const mode: DiscussionMode = 'DISABLED'
      expect(mode).toBe('DISABLED')
    })
  })

  describe('RoomInfo with joinPolicy', () => {
    it('should have joinPolicy field instead of discoverable', () => {
      const room: RoomInfo = {
        id: 'r1',
        name: 'Test Room',
        description: 'A test room',
        publicDescription: null,
        avatarUrl: null,
        type: 'KLASSE',
        sectionId: null,
        archived: false,
        memberCount: 10,
        joinPolicy: 'OPEN',
        expiresAt: null,
        tags: [],
      }

      expect(room.joinPolicy).toBe('OPEN')
      expect(room).not.toHaveProperty('discoverable')
    })

    it('should support all joinPolicy values', () => {
      const policies: JoinPolicy[] = ['OPEN', 'REQUEST', 'INVITE_ONLY']
      policies.forEach(policy => {
        const room: RoomInfo = {
          id: 'r1', name: 'Test', description: null, publicDescription: null,
          avatarUrl: null, type: 'GRUPPE', sectionId: null, archived: false,
          memberCount: 0, joinPolicy: policy, expiresAt: null, tags: [],
        }
        expect(room.joinPolicy).toBe(policy)
      })
    })
  })

  describe('RoomPublicInfo with joinPolicy', () => {
    it('should have joinPolicy field', () => {
      const room: RoomPublicInfo = {
        id: 'r1',
        name: 'Public Room',
        publicDescription: 'Visible to all',
        avatarUrl: null,
        type: 'PROJEKT',
        sectionId: null,
        memberCount: 15,
        joinPolicy: 'REQUEST',
        tags: ['project'],
      }

      expect(room.joinPolicy).toBe('REQUEST')
    })
  })

  describe('RoomSettings with new fields', () => {
    it('should have discussionMode field', () => {
      const settings: RoomSettings = {
        chatEnabled: true,
        filesEnabled: false,
        parentSpaceEnabled: false,
        visibility: 'MEMBERS_ONLY',
        discussionMode: 'FULL',
        allowMemberThreadCreation: false,
        childDiscussionEnabled: false,
      }

      expect(settings.discussionMode).toBe('FULL')
    })

    it('should have allowMemberThreadCreation field', () => {
      const settings: RoomSettings = {
        chatEnabled: false,
        filesEnabled: false,
        parentSpaceEnabled: false,
        visibility: 'MEMBERS_ONLY',
        discussionMode: 'ANNOUNCEMENTS_ONLY',
        allowMemberThreadCreation: true,
        childDiscussionEnabled: false,
      }

      expect(settings.allowMemberThreadCreation).toBe(true)
    })

    it('should have childDiscussionEnabled field', () => {
      const settings: RoomSettings = {
        chatEnabled: false,
        filesEnabled: false,
        parentSpaceEnabled: false,
        visibility: 'MEMBERS_ONLY',
        discussionMode: 'FULL',
        allowMemberThreadCreation: false,
        childDiscussionEnabled: true,
      }

      expect(settings.childDiscussionEnabled).toBe(true)
    })

    it('should support DISABLED discussion mode', () => {
      const settings: RoomSettings = {
        chatEnabled: false,
        filesEnabled: false,
        parentSpaceEnabled: false,
        visibility: 'MEMBERS_ONLY',
        discussionMode: 'DISABLED',
        allowMemberThreadCreation: false,
        childDiscussionEnabled: false,
      }

      expect(settings.discussionMode).toBe('DISABLED')
    })
  })

  describe('RoomDetail with extended settings', () => {
    it('should carry settings with all new fields', () => {
      const room: RoomDetail = {
        id: 'r1',
        name: 'Detail Room',
        description: 'Full detail',
        publicDescription: null,
        avatarUrl: null,
        type: 'KLASSE',
        sectionId: 'sec-1',
        archived: false,
        memberCount: 20,
        joinPolicy: 'INVITE_ONLY',
        expiresAt: null,
        tags: [],
        settings: {
          chatEnabled: true,
          filesEnabled: true,
          parentSpaceEnabled: true,
          visibility: 'MEMBERS_ONLY',
          discussionMode: 'FULL',
          allowMemberThreadCreation: true,
          childDiscussionEnabled: true,
        },
        createdBy: 'user-1',
        createdAt: '2025-01-01T00:00:00Z',
        members: [],
      }

      expect(room.joinPolicy).toBe('INVITE_ONLY')
      expect(room.settings.discussionMode).toBe('FULL')
      expect(room.settings.allowMemberThreadCreation).toBe(true)
      expect(room.settings.childDiscussionEnabled).toBe(true)
    })
  })
})
