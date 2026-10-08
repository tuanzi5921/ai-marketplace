import { request } from './request'

export type CompetitionPhase = 'SUBMIT' | 'REVIEW' | 'AWARD'

export interface CompetitionDTO {
  id?: string
  name: string
  submitStartAt: string
  submitEndAt: string
  reviewAt: string
  shortlistThreshold: number
  topNPerTrack: number
  awardsConfig: unknown
  tracksConfig: unknown
}

export interface Competition extends CompetitionDTO {
  id: string
  phase: CompetitionPhase
  status: string
  createdAt?: string
  updatedAt?: string
}

export function createCompetition(dto: CompetitionDTO) {
  return request.post<Competition>('/competitions', dto)
}

export function updateCompetition(dto: CompetitionDTO) {
  return request.put<Competition>('/competitions', dto)
}

export function activateCompetition(id: string) {
  return request.post<void>(`/competitions/${id}/activate`)
}

export function switchPhase(id: string, phase: CompetitionPhase) {
  return request.post<void>(`/competitions/${id}/phase`, undefined, { params: { phase } })
}

export function currentCompetition() {
  return request.get<Competition | null>('/competitions/current')
}

export function listCompetitions() {
  return request.get<Competition[]>('/competitions')
}
