export interface ProfileFieldDefinition {
  id: string
  fieldKey: string
  labelDe: string
  labelEn: string
  fieldType: 'TEXT' | 'DATE' | 'SELECT' | 'BOOLEAN'
  options: string[] | null
  required: boolean
  position: number
}

export interface ProfileFieldValue {
  fieldId: string
  value: string | null
}

export interface CreateProfileFieldRequest {
  fieldKey: string
  labelDe: string
  labelEn: string
  fieldType: string
  options?: string[]
  required: boolean
  position: number
}

export interface UpdateProfileFieldRequest {
  labelDe?: string
  labelEn?: string
  options?: string[]
  required?: boolean
  position?: number
  active?: boolean
}
