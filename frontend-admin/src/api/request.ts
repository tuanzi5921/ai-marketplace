import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig
} from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

/** 后端统一响应包络（约定：code===0 为成功，其余为业务错误） */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

/** 分页结果（约定字段，后端如不一致可在 normalizer 处适配） */
export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

/** 分页查询参数 */
export interface PageQuery {
  page?: number
  size?: number
}

const service: AxiosInstance = axios.create({
  // 所有接口统一前缀 /api，dev 环境由 Vite 代理转发到后端
  baseURL: '/api',
  timeout: 20000
})

// —— 请求拦截器：注入 Authorization ——
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const auth = useAuthStore()
    if (auth.token) {
      config.headers.Authorization = `Bearer ${auth.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 防止 401 时短时间内重复弹窗 / 跳转
let unauthorizedLock = false

// —— 响应拦截器：解包业务包络 + 401 跳登录 + 业务错误 ElMessage ——
service.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse
    // 非标准包络（如二进制流），原样返回
    if (body == null || typeof body !== 'object' || !('code' in body)) {
      return response.data
    }
    if (body.code === 0) {
      return body.data
    }
    // 业务错误
    ElMessage.error(body.message || '请求失败')
    return Promise.reject(new Error(body.message || '请求失败'))
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      handleUnauthorized()
    } else {
      const msg = error?.response?.data?.message || error?.message || '网络异常'
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

function handleUnauthorized() {
  if (unauthorizedLock) return
  unauthorizedLock = true
  const auth = useAuthStore()
  auth.logout()
  ElMessage.error('登录已过期，请重新登录')
  router
    .push({ name: 'Login' })
    .finally(() => {
      unauthorizedLock = false
    })
}

/**
 * 统一请求方法：拦截器已解包业务包络，返回值即为 data。
 * 用法：http<User>({ url: '/auth/me' })
 */
export function http<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  return service.request<unknown, T>(config)
}

export default service
export type { AxiosRequestConfig }
