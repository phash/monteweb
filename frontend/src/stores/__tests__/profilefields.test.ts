import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/profilefields.api', () => ({
  profileFieldsApi: {
    getDefinitions: vi.fn().mockResolvedValue({
      data: {
        data: [
          { id: 'f1', fieldKey: 'hobby', labelDe: 'Hobby', labelEn: 'Hobby', fieldType: 'TEXT', options: null, required: false, position: 0 },
          { id: 'f2', fieldKey: 'birthday', labelDe: 'Geburtstag', labelEn: 'Birthday', fieldType: 'DATE', options: null, required: true, position: 1 },
        ],
      },
    }),
    getMyValues: vi.fn().mockResolvedValue({
      data: { data: { f1: 'Reading', f2: '2000-01-15' } },
    }),
    updateMyValues: vi.fn().mockResolvedValue({
      data: { data: { f1: 'Coding', f2: '2000-01-15' } },
    }),
    listAllDefinitions: vi.fn().mockResolvedValue({
      data: {
        data: [
          { id: 'f1', fieldKey: 'hobby', labelDe: 'Hobby', labelEn: 'Hobby', fieldType: 'TEXT', options: null, required: false, position: 0 },
        ],
      },
    }),
    createDefinition: vi.fn().mockResolvedValue({
      data: {
        data: { id: 'f3', fieldKey: 'color', labelDe: 'Lieblingsfarbe', labelEn: 'Favorite Color', fieldType: 'SELECT', options: ['Rot', 'Blau'], required: false, position: 2 },
      },
    }),
    updateDefinition: vi.fn().mockResolvedValue({
      data: {
        data: { id: 'f1', fieldKey: 'hobby', labelDe: 'Hobbys', labelEn: 'Hobbies', fieldType: 'TEXT', options: null, required: true, position: 0 },
      },
    }),
    deleteDefinition: vi.fn().mockResolvedValue({ data: { data: null } }),
  },
}))

import { useProfileFieldsStore } from '@/stores/profilefields'
import { profileFieldsApi } from '@/api/profilefields.api'

describe('profilefields store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should start with empty state', () => {
    const store = useProfileFieldsStore()
    expect(store.definitions).toEqual([])
    expect(store.values).toEqual({})
    expect(store.loading).toBe(false)
  })

  describe('fetchDefinitions', () => {
    it('should load active field definitions', async () => {
      const store = useProfileFieldsStore()
      await store.fetchDefinitions()
      expect(profileFieldsApi.getDefinitions).toHaveBeenCalled()
      expect(store.definitions).toHaveLength(2)
      expect(store.definitions[0].fieldKey).toBe('hobby')
    })

    it('should set loading during fetch', async () => {
      const store = useProfileFieldsStore()
      const promise = store.fetchDefinitions()
      expect(store.loading).toBe(true)
      await promise
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchMyValues', () => {
    it('should load user values', async () => {
      const store = useProfileFieldsStore()
      await store.fetchMyValues()
      expect(profileFieldsApi.getMyValues).toHaveBeenCalled()
      expect(store.values).toEqual({ f1: 'Reading', f2: '2000-01-15' })
    })
  })

  describe('updateMyValues', () => {
    it('should update and refresh values', async () => {
      const store = useProfileFieldsStore()
      await store.updateMyValues({ f1: 'Coding', f2: '2000-01-15' })
      expect(profileFieldsApi.updateMyValues).toHaveBeenCalledWith({ f1: 'Coding', f2: '2000-01-15' })
      expect(store.values).toEqual({ f1: 'Coding', f2: '2000-01-15' })
    })
  })

  describe('admin operations', () => {
    it('should fetch all definitions', async () => {
      const store = useProfileFieldsStore()
      await store.fetchAllDefinitions()
      expect(profileFieldsApi.listAllDefinitions).toHaveBeenCalled()
      expect(store.allDefinitions).toHaveLength(1)
    })

    it('should create a definition', async () => {
      const store = useProfileFieldsStore()
      store.allDefinitions = []
      const result = await store.createDefinition({
        fieldKey: 'color',
        labelDe: 'Lieblingsfarbe',
        labelEn: 'Favorite Color',
        fieldType: 'SELECT',
        options: ['Rot', 'Blau'],
        required: false,
        position: 2,
      })
      expect(profileFieldsApi.createDefinition).toHaveBeenCalled()
      expect(result.fieldKey).toBe('color')
      expect(store.allDefinitions).toHaveLength(1)
    })

    it('should update a definition', async () => {
      const store = useProfileFieldsStore()
      store.allDefinitions = [
        { id: 'f1', fieldKey: 'hobby', labelDe: 'Hobby', labelEn: 'Hobby', fieldType: 'TEXT' as const, options: null, required: false, position: 0 },
      ]
      const result = await store.updateDefinition('f1', { labelDe: 'Hobbys', required: true })
      expect(profileFieldsApi.updateDefinition).toHaveBeenCalledWith('f1', { labelDe: 'Hobbys', required: true })
      expect(result.labelDe).toBe('Hobbys')
      expect(store.allDefinitions[0].labelDe).toBe('Hobbys')
    })

    it('should delete a definition', async () => {
      const store = useProfileFieldsStore()
      store.allDefinitions = [
        { id: 'f1', fieldKey: 'hobby', labelDe: 'Hobby', labelEn: 'Hobby', fieldType: 'TEXT' as const, options: null, required: false, position: 0 },
      ]
      await store.deleteDefinition('f1')
      expect(profileFieldsApi.deleteDefinition).toHaveBeenCalledWith('f1')
      expect(store.allDefinitions).toHaveLength(0)
    })
  })
})
