import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/apartments',
    children: [
      { path: 'apartments', name: 'Apartment', component: () => import('../views/Apartment.vue'), meta: { title: '房源管理' } },
      { path: 'users', name: 'Users', component: () => import('../views/Users.vue'), meta: { title: '用户管理' } },
      { path: 'stats', name: 'Stats', component: () => import('../views/Stats.vue'), meta: { title: '业务统计' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录跳转登录页
router.beforeEach((to) => {
  const token = localStorage.getItem('admin_token')
  if (to.path !== '/login' && !token) {
    return { path: '/login' }
  }
  return true
})

export default router
