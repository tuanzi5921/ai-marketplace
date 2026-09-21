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
