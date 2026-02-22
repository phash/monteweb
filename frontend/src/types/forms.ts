export type FormType = 'SURVEY' | 'CONSENT'

export type FormScope = 'ROOM' | 'SECTION' | 'SCHOOL'

export type FormStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'ARCHIVED'

export type QuestionType = 'TEXT' | 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'RATING' | 'YES_NO'

export interface FormInfo {
  id: string
  title: string
  description: string | null
  type: FormType
  scope: FormScope
  scopeId: string | null
  scopeName: string | null
  sectionIds: string[]
  sectionNames: string[]
  status: FormStatus
  anonymous: boolean
  deadline: string | null
  questionCount: number
  responseCount: number
  targetCount: number
  createdBy: string
  creatorName: string
  hasUserResponded: boolean
  createdAt: string
  updatedAt: string
  publishedAt: string | null
  closedAt: string | null
}

export interface QuestionInfo {
  id: string
  type: QuestionType
  label: string
  description: string | null
  required: boolean
  sortOrder: number
  options: string[] | null
  ratingConfig: Record<string, number> | null
}

export interface FormDetailInfo {
  form: FormInfo
  questions: QuestionInfo[]
}

export interface QuestionRequest {
  type: QuestionType
  label: string
  description?: string
  required: boolean
  options?: string[]
  ratingConfig?: Record<string, number>
}

export interface CreateFormRequest {
  title: string
  description?: string
  type: FormType
  scope: FormScope
  scopeId?: string
  sectionIds?: string[]
  anonymous: boolean
  deadline?: string
  questions: QuestionRequest[]
}

export interface UpdateFormRequest {
  title?: string
  description?: string
  deadline?: string
  questions?: QuestionRequest[]
}

export interface AnswerRequest {
  questionId: string
  text?: string
  selectedOptions?: string[]
  rating?: number
}

export interface SubmitResponseRequest {
  answers: AnswerRequest[]
}

export interface QuestionResult {
  questionId: string
  label: string
  type: QuestionType
  totalAnswers: number
  optionCounts: Record<string, number> | null
  averageRating: number | null
  ratingDistribution: Record<number, number> | null
  textAnswers: string[] | null
  yesCount: number
  noCount: number
}

export interface FormResultsSummary {
  form: FormInfo
  results: QuestionResult[]
}

export interface IndividualAnswer {
  questionId: string
  questionLabel: string
  type: QuestionType
  text: string | null
  selectedOptions: string[] | null
  rating: number | null
}

export interface IndividualResponse {
  responseId: string
  userId: string | null
  userName: string
  submittedAt: string
  answers: IndividualAnswer[]
}

export interface MyAnswerInfo {
  questionId: string
  text: string | null
  selectedOptions: string[] | null
  rating: number | null
}

export interface MyResponseInfo {
  responseId: string
  answers: MyAnswerInfo[]
}
