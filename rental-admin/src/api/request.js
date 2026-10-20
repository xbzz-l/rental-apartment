import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// axios 实例：统一携带 token，统一处理响应与错误
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (res) => {
    const { code, message, data } = res.data
    if (code === 200) {
      return data
    }
    // 未登录/无权限：清空登录态并跳转登录页
    if (code === 401 || code === 403) {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_user')
      ElMessage.error(message || '登录已失效')
      router.push('/login')
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
