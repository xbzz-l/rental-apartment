import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  // 先登录后看房：首页与详情均需登录
  { path: '/', name: 'Home', component: () => import('../views/Home.vue'), meta: { requiresAuth: true } },
  { path: '/detail/:id', name: 'Detail', component: () => import('../views/Detail.vue'), meta: { requiresAuth: true } },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/profile', name: 'Profile', component: () => import('../views/Profile.vue'), meta: { requiresAuth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：需要登录的页面未登录时跳转登录页
router.beforeEach((to) => {
  const token = localStorage.getItem('rental_token')
  if (to.meta.requiresAuth && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
