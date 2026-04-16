export interface ArticleResponse {
  id: string
  title: string
  slug: string
  content: string
  coverImage: string | null
  category: 'FASHION' | 'CULTURE'
  status: 'DRAFT' | 'PUBLISHED'
  authorName: string
  authorId: string
  publishedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface CommentResponse {
  id: string
  content: string
  authorName: string
  authorId: string
  createdAt: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface LikeStatus {
  count: number
  liked: boolean
}

export interface User {
  id: string
  email: string
  username: string
  profileImage: string
  role: 'OWNER' | 'CONTRIBUTOR' | 'READER'
}
