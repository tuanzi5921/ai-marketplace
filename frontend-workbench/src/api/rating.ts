import { request } from './request'

// 评分 DTO：四维度 1-5 + 评论
export interface RatingDto {
  submissionId: string | number
  easeOfUse: number
  businessValue: number
  techQuality: number
  innovation: number
  commentText?: string
}

// 提交评分
export function rate(dto: RatingDto) {
  return request.post<void>('/ratings', dto)
}

// 我对某作品的评分 + 该作品的评分汇总
export interface MyRating {
  submissionId: string | number
  rated: boolean
  easeOfUse?: number | null
  businessValue?: number | null
  techQuality?: number | null
  innovation?: number | null
  commentText?: string | null
  /** USER / JUDGE，决定这条评分计入哪一侧权重 */
  ratedByRole?: string | null
  ratedAt?: string | null
  avgScore?: number | null
  raterCount?: number | null
}

export function myRating(submissionId: string | number) {
  return request.get<MyRating>(`/ratings/submissions/${submissionId}/mine`)
}
