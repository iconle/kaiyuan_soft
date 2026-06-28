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
    // blob 响应（文件下载）：正常应是 pdf/excel 等二进制流；
    // 若后端返回 application/json，说明是错误（被全局异常处理器包成 JSON），
    // 需解析出错误信息并拒绝，避免把错误 JSON 当成文件下载下来打不开。
    if (response.config.responseType === 'blob') {
      const contentType = response.headers['content-type'] || ''
      if (contentType.includes('application/json')) {
        return response.data.text().then(text => {
          let message = '文件下载失败'
          try {
            const parsed = JSON.parse(text)
            message = parsed.message || message
          } catch (e) { /* 非 JSON 文本，保留默认提示 */ }
          if (!response.config.skipErrorMessage) {
            ElMessage.error(message)
          }
          const error = new Error(message)
          error.response = response
          return Promise.reject(error)
        })
      }
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
