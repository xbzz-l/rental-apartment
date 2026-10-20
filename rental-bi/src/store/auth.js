import { reactive } from 'vue'

/**
 * BI 看板登录态管理（与管理端共用 admin 账号体系）
 * token 持久化到 localStorage；authState 为响应式，token 变化时 App 自动切换登录页/看板页
 */
export const authState = reactive({
  token: localStorage.getItem('bi_token') || ''
})

// 保存登录态
export function setToken(token) {
  authState.token = token
  localStorage.setItem('bi_token', token)
}

// 退出登录：清空登录态
export function logout() {
  authState.token = ''
  localStorage.removeItem('bi_token')
}
