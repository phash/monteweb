export interface FundgrubeImageInfo {
  id: string
  itemId: string
  originalFilename: string
  imageUrl: string
  thumbnailUrl: string
  fileSize: number
  contentType: string
}

export interface FundgrubeItemInfo {
  id: string
  title: string
  description: string | null
  sectionId: string | null
  sectionName: string | null
  createdBy: string
  createdByName: string
  createdAt: string
  updatedAt: string
  claimedBy: string | null
  claimedByName: string | null
  claimedAt: string | null
  expiresAt: string | null
  claimed: boolean
  images: FundgrubeImageInfo[]
}

export interface CreateFundgrubeItemRequest {
  title: string
  description?: string
  sectionId?: string
}

export interface UpdateFundgrubeItemRequest {
  title?: string
  description?: string
  sectionId?: string
}

export interface ClaimItemRequest {
  comment?: string
}
