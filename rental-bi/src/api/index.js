import request from './request'

// ---- 认证 ----
export const login = (data) => request.post('/auth/login', data)

// ---- BI 看板统计（需管理员登录） ----
export const getBiStats = () => request.get('/admin/stats/bi')
