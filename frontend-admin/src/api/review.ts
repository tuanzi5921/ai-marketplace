import { http, type PageResult } from './request'

/** 待审作品项 */
export interface ReviewItem {
  id: string
  title: string
  authorName: string
  authorDepartment?: string
  type: string
  track?: string
  submittedAt: string
  description?: string
  coverUrl?: string
  demoUrl?: string
  status?: string
}

/** 后端 /review/pending 直接返回 MpSubmission 实体，字段名与 ReviewItem 不一致 */
interface PendingSubmission {
  id: number
  title: string
  authorId: number
  shortDesc?: string
  type: string
  businessDomain?: string
  coverUrl?: string
  status?: string
  createdAt: string
}

// 后端未 join sys_user，拿不到作者姓名与部门，先退化为展示用户 ID
function toItem(s: PendingSubmission): ReviewItem {
  return {
    id: String(s.id),
    title: s.title,
    authorName: `用户 #${s.authorId}`,
    type: s.type,
    track: s.businessDomain,
    submittedAt: s.createdAt,
    description: s.shortDesc,
    coverUrl: s.coverUrl,
    status: s.status
  }
}

/**
 * 待审作品列表
 * GET /api/review/pending?page=&size=
 */
export async function listPending(page = 1, size = 10): Promise<PageResult<ReviewItem>> {
  const res = await http<PageResult<PendingSubmission>>({
    url: '/review/pending',
    method: 'get',
    params: { page, size }
  })
  return { ...res, list: (res.list || []).map(toItem) }
}

/**
 * 审核通过
 * POST /api/review/action  body: {submissionId, decision:'APPROVED', rejectReason}
 */
export function approve(id: string, reason: string): Promise<void> {
  return action(id, 'APPROVED', reason)
}

/**
 * 审核退回
 * POST /api/review/action  body: {submissionId, decision:'REJECTED', rejectReason}
 */
export function reject(id: string, reason: string): Promise<void> {
  return action(id, 'REJECTED', reason)
}

function action(id: string, decision: 'APPROVED' | 'REJECTED', reason: string): Promise<void> {
  return http<void>({
    url: '/review/action',
    method: 'post',
    data: { submissionId: Number(id), decision, rejectReason: reason }
  })
}
