import axios from 'axios'
import { ElMessage } from 'element-plus'
import { authState, logout } from '../store/auth'

// axios 实例：统一携带 token，统一处理响应与错误
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  if (authState.token) {
    config.headers.Authorization = `Bearer ${authState.token}`
  }
  return config
})

request.interceptors.response.use(
  (res) => {
    const { code, message, data } = res.data
    if (code === 200) {
      return data
    }
    // 登录失效：清空登录态（App 自动切回登录页）
    if (code === 401 || code === 403) {
      logout()
      ElMessage.error(message || '登录已失效，请重新登录')
      return Promise.reject(new Error(message))
    }
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  (err) => {
    ElMessage.error('网络异常，请稍后重试')
    return Promise.reject(err)
  }
)

export default request
