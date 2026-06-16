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
        ElMessage.error(res.message || '请求失败')
      }
      if (res.code === 401) {
        localStorage.removeItem('token')
        router.push('/login')
      }
      const error = new Error(res.message || '请求失败')
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
      ElMessage.error(error.response?.data?.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
