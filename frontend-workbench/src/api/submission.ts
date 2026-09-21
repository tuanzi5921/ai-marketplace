import { request } from './request'

// 作品类型
export type SubmissionType = 'CODE' | 'EXECUTABLE' | 'SAAS' | 'DOC'

// 作品分页查询参数
export interface SubmissionQuery {
  page?: number
  size?: number
  type?: SubmissionType | ''
  domain?: string
}

// 分页结果
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

// 作者信息
export interface AuthorInfo {
  id?: string | number
  username?: string
  displayName?: string
  avatarUrl?: string
  department?: string
}

// 作品列表项
export interface SubmissionSummary {
  id: string | number
  title: string
  shortDesc?: string
  type: SubmissionType
  track?: string
  businessDomain?: string
  techStack?: string[]
  coverUrl?: string
  version?: string
  downloadCount?: number
  avgScore?: number
  raterCount?: number
  author?: AuthorInfo
  createdAt?: string
}

// 作品详情
export interface SubmissionDetail extends SubmissionSummary {
  detailDesc?: string
  usageScenario?: string
  tags?: string[]
  saasUrl?: string
  saasCredentials?: string
  fileUrl?: string
  fileName?: string
  fileSize?: number
}

// 上传作品表单
export interface SubmissionForm {
  title: string
  shortDesc: string
  detailDesc?: string
  type: SubmissionType
  businessDomain?: string
  usageScenario?: string
  techStack?: string
  tags?: string
  version?: string
  coverUrl?: string
  saasUrl?: string
  saasCredentials?: string
  file?: File | null
}

// 分页查询已发布作品
export function listPublished(params: SubmissionQuery) {
  return request.get<PageResult<SubmissionSummary>>('/submissions', { params })
}

// 作品详情
export function detail(id: string | number) {
  return request.get<SubmissionDetail>(`/submissions/${id}`)
}

// 上传作品（multipart/form-data）
export function submit(form: SubmissionForm) {
  const fd = new FormData()
  fd.append('title', form.title)
  fd.append('shortDesc', form.shortDesc)
  fd.append('type', form.type)
  if (form.detailDesc) fd.append('detailDesc', form.detailDesc)
  if (form.businessDomain) fd.append('businessDomain', form.businessDomain)
  if (form.usageScenario) fd.append('usageScenario', form.usageScenario)
  if (form.techStack) fd.append('techStack', form.techStack)
  if (form.tags) fd.append('tags', form.tags)
  if (form.version) fd.append('version', form.version)
  if (form.coverUrl) fd.append('coverUrl', form.coverUrl)
  if (form.type === 'SAAS') {
    if (form.saasUrl) fd.append('saasUrl', form.saasUrl)
    if (form.saasCredentials) fd.append('saasCredentials', form.saasCredentials)
  }
  if (form.file) fd.append('file', form.file)
  return request.post<SubmissionDetail>('/submissions', fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 下载作品
export function download(id: string | number) {
  return request.post<Blob>(`/submissions/${id}/download`, undefined, {
    responseType: 'blob'
  })
}
