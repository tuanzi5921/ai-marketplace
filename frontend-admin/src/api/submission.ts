import { http, type PageResult } from './request'

/** 作品状态 */
export type SubmissionStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'ONLINE'
  | 'OFFLINE'

/** 作品类型 */
export type SubmissionType = 'APP' | 'AGENT' | 'PLUGIN' | 'SOLUTION'

/** 作品项（管理列表 / 详情） */
export interface Submission {
  id: string
  title: string
  authorName: string
  authorDepartment?: string
  type: SubmissionType
  track?: string
  status: SubmissionStatus
  submittedAt: string
  approvedAt?: string
  downloads?: number
  views?: number
  description?: string
  coverUrl?: string
  demoUrl?: string
  rejectReason?: string
}

/** 列表查询参数 */
export interface SubmissionQuery {
  status?: SubmissionStatus
  type?: SubmissionType
}

/**
 * 全部作品（管理端）
 * GET /api/submissions/admin?page=&size=&status=&type=
 */
export function listAll(
  page = 1,
  size = 10,
  params: SubmissionQuery = {}
): Promise<PageResult<Submission>> {
  return http<PageResult<Submission>>({
    url: '/submissions/admin',
    method: 'get',
    params: { page, size, ...params }
  })
}

/**
 * 作品详情
 * GET /api/submissions/{id}
 */
export function detail(id: string): Promise<Submission> {
  return http<Submission>({ url: `/submissions/${id}`, method: 'get' })
}

/**
 * 下架作品
 * POST /api/submissions/{id}/offline
 */
export function offline(id: string): Promise<void> {
  return http<void>({ url: `/submissions/${id}/offline`, method: 'post' })
}

/**
 * 删除作品
 * DELETE /api/submissions/{id}
 */
export function deleteSubmission(id: string): Promise<void> {
  return http<void>({ url: `/submissions/${id}`, method: 'delete' })
}
