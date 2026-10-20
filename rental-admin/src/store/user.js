import { defineStore } from 'pinia'
import { login as loginApi } from '../api'

/**
 * 管理员登录态管理：token 与用户信息持久化到 localStorage
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('admin_token') || '',
    userInfo: JSON.parse(localStorage.getItem('admin_user') || 'null')
  }),
  actions: {
    // 管理员登录：校验角色为 admin 后保存登录态
    async login(payload) {
      const data = await loginApi(payload)
      if (data.user.role !== 'admin') {
        throw new Error('非管理员账号，无法登录管理后台')
      }
      this.token = data.token
      this.userInfo = data.user
      localStorage.setItem('admin_token', data.token)
      localStorage.setItem('admin_user', JSON.stringify(data.user))
    },
    // 退出登录：清空本地登录态
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_user')
    }
  }
})
