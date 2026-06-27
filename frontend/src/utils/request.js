import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '',
  timeout: 15000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  response => {
    // Skip JSON unwrapping for blob responses (file downloads)
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const res = response.data
    if (res.code !== 200) {
      // 调用方可通过 config.skipErrorMessage 自行处理错误展示（如多行校验报告）
      if (!response.config.skipErrorMessage) {
        ElMessage.error(res.message || getRequestErrorMessage(response.status))
      }
      if (res.code === 401) {
        localStorage.removeItem('token')
        router.push('/login')
      }
      const error = new Error(res.message || getRequestErrorMessage(response.status))
      error.response = response
      return Promise.reject(error)
    }
    return res
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
    }
    if (!error.config?.skipErrorMessage) {
      ElMessage.error(error.response?.data?.message || getRequestErrorMessage(error.response?.status))
    }
    return Promise.reject(error)
  }
)

function getRequestErrorMessage(status) {
  if (status === 401) return '登录已失效，请重新登录'
  if (status === 403) return '无权限执行该操作，请联系管理员'
  if (status === 404) return '接口不存在，请联系管理员检查服务配置'
  if (status >= 500) return '后端服务异常，请稍后重试'
  return '后端服务不可用，请确认服务是否启动'
}

export default request
