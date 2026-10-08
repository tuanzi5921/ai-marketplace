import axios, { type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

const TOKEN_KEY = 'ai_market_token'

function getToken(): string {
  let raw: string | null = null
  try {
    raw = localStorage.getItem(TOKEN_KEY)
    if (!raw) return ''
    return JSON.parse(raw) || ''
  } catch {
    return raw || ''
  }
}

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000
})

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

export interface PageResult<T = unknown> {
  list: T[]
  total: number
  page: number
  size: number
}

service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && typeof res === 'object' && 'code' in res && 'data' in res) {
      if (res.code !== 0 && res.code !== 200) {
        ElMessage.error(res.message || '请求失败')
        return Promise.reject(new Error(res.message || 'Error'))
      }
      return res.data
    }
    return res
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem('ai_market_user')
      ElMessage.error('登录已失效，请重新登录')
      const redirect = window.location.pathname + window.location.search
      window.location.href = `/login?redirect=${encodeURIComponent(redirect)}`
      return Promise.reject(error)
    }
    const msg = error?.response?.data?.message || error.message || '网络异常'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export const request = {
  get<T = unknown>(url: string, config?: AxiosRequestConfig) {
    return service.get<unknown, T>(url, config)
  },
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return service.post<unknown, T>(url, data, config)
  },
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return service.put<unknown, T>(url, data, config)
  },
  delete<T = unknown>(url: string, config?: AxiosRequestConfig) {
    return service.delete<unknown, T>(url, config)
  }
}

export default service
