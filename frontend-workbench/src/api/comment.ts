import { request } from './request'

// 评论
export interface Comment {
  id: string | number
  submissionId: string | number
  parentId?: string | number | null
  content: string
  authorName?: string
  authorAvatarUrl?: string
  isOwn?: boolean
  createdAt?: string
  children?: Comment[]
}

// 发表评论 DTO
export interface CommentCreateDto {
  submissionId: string | number
  parentId?: string | number | null
  content: string
}

// 举报评论 DTO
export interface CommentReportDto {
  commentId: string | number
  reason: string
}

// 评论列表
export function listBySubmission(id: string | number) {
  return request.get<Comment[]>(`/comments/submissions/${id}`)
}

// 发表评论
export function create(dto: CommentCreateDto) {
  return request.post<Comment>('/comments', dto)
}

// 举报评论
export function report(dto: CommentReportDto) {
  return request.post<void>('/comments/report', dto)
}

// 删除自己的评论
export function deleteOwn(id: string | number) {
  return request.delete<void>(`/comments/${id}`)
}
