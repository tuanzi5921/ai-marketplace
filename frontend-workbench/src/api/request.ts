import axios, { type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

// axios 实例：统一 baseURL、请求/响应拦截
const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器：注入 Bearer token
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('jwt')
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 后端统一响应结构（按需使用）
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

// 响应拦截器：统一处理 401 与业务错误
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 兼容后端两种风格：纯数据 或 { code, message, data }
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
      // token 失效：清理并跳登录
      localStorage.removeItem('jwt')
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

// 便捷封装
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
