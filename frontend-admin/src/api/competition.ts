import { http } from './request'

/** 大赛阶段 */
export type CompetitionPhase = 'SUBMIT' | 'REVIEW' | 'AWARD'

/** 赛道配置 */
export interface TrackConfig {
  key: string
  name: string
  description?: string
}

/** 奖项配置 */
export interface AwardItem {
  name: string
  count?: number
  reward?: string
}

/** 大赛创建/更新 DTO */
export interface CompetitionDTO {
  id?: string
  name: string
  submitStartAt: string
  submitEndAt: string
  reviewAt: string
  shortlistThreshold: number
  topNPerTrack: number
  awardsConfig: AwardItem[]
  tracksConfig: TrackConfig[]
}

/** 大赛实体 */
export interface Competition extends CompetitionDTO {
  id: string
  phase: CompetitionPhase
  active: boolean
  createdAt?: string
  updatedAt?: string
}

/**
 * 创建大赛
 * POST /api/competitions
 */
export function create(dto: CompetitionDTO): Promise<Competition> {
  return http<Competition>({ url: '/competitions', method: 'post', data: dto })
}

/**
 * 更新大赛
 * PUT /api/competitions
 */
export function update(dto: CompetitionDTO): Promise<Competition> {
  return http<Competition>({ url: '/competitions', method: 'put', data: dto })
}

/**
 * 激活大赛
 * POST /api/competitions/{id}/activate
 */
export function activate(id: string): Promise<void> {
  return http<void>({ url: `/competitions/${id}/activate`, method: 'post' })
}

/**
 * 切换大赛阶段
 * POST /api/competitions/{id}/phase?phase=xxx
 */
export function switchPhase(id: string, phase: CompetitionPhase): Promise<void> {
  return http<void>({
    url: `/competitions/${id}/phase`,
    method: 'post',
    params: { phase }
  })
}

/**
 * 当前活跃大赛
 * GET /api/competitions/current
 */
export function current(): Promise<Competition | null> {
  return http<Competition | null>({ url: '/competitions/current', method: 'get' })
}

/**
 * 大赛列表
 * GET /api/competitions
 */
export function list(): Promise<Competition[]> {
  return http<Competition[]>({ url: '/competitions', method: 'get' })
}
