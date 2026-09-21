import { http, type PageResult } from './request'

/** 被举报评论 */
export interface ReportedComment {
  id: string
  content: string
  authorName: string
  authorId?: string
  reportCount: number
  reportedAt: string
  targetTitle?: string
  hidden?: boolean
}

/**
 * 被举报评论列表
 * GET /api/comments/reported?page=&size=
 */
export function listReported(page = 1, size = 10): Promise<PageResult<ReportedComment>> {
  return http<PageResult<ReportedComment>>({
    url: '/comments/reported',
    method: 'get',
    params: { page, size }
  })
}

/**
 * 隐藏评论
 * POST /api/comments/{id}/hide
 */
export function hide(id: string): Promise<void> {
  return http<void>({ url: `/comments/${id}/hide`, method: 'post' })
}

/**
 * 恢复评论
 * POST /api/comments/{id}/restore
 */
export function restore(id: string): Promise<void> {
  return http<void>({ url: `/comments/${id}/restore`, method: 'post' })
}

/**
 * 删除评论
 * DELETE /api/comments/{id}
 */
export function deleteComment(id: string): Promise<void> {
  return http<void>({ url: `/comments/${id}`, method: 'delete' })
}
