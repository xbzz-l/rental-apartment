import { defineStore } from 'pinia'
import { showToast } from 'vant'
import { login as loginApi, register as registerApi, getUserInfo, updateUserInfo } from '../api'

/**
 * 用户登录态管理：token 持久化到 localStorage，用户信息存内存
 * 注意：用户端只服务普通用户，管理员账号一律拒绝登录
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('rental_token') || '',
    userInfo: null
  }),
  getters: {
    isLogin: (state) => !!state.token
  },
  actions: {
    // 登录：保存 token 与用户信息
    async login(payload) {
      const data = await loginApi(payload)
      // C 端不面向管理员：管理员请走管理端 / BI 看板
      if (data.user?.role === 'admin') {
        showToast('管理员账号请从管理端登录')
        throw new Error('管理员账号请从管理端登录')
      }
      this.token = data.token
      this.userInfo = data.user
      localStorage.setItem('rental_token', data.token)
    },
    // 注册新账号
    async register(payload) {
      await registerApi(payload)
    },
    // 拉取当前用户信息
    async fetchInfo() {
      this.userInfo = await getUserInfo()
    },
    // 更新个人信息（真实姓名/年龄/电话/同住人信息）
    async updateInfo(payload) {
      this.userInfo = await updateUserInfo(payload)
    },
    // 退出登录：清空登录态
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('rental_token')
    }
  }
})
