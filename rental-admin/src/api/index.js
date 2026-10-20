import request from './request'

// ---- 认证 ----
export const login = (data) => request.post('/auth/login', data)

// ---- 房源管理 ----
export const getApartments = (params) => request.get('/apartments', { params })
export const createApartment = (data) => request.post('/admin/apartments', data)
export const updateApartment = (id, data) => request.put(`/admin/apartments/${id}`, data)
export const deleteApartment = (id) => request.delete(`/admin/apartments/${id}`)

// ---- 用户管理 ----
export const getUsers = (params) => request.get('/admin/users', { params })
export const getUserCount = () => request.get('/admin/users/count')
export const getUserBookings = (userId, params) => request.get(`/admin/users/${userId}/bookings`, { params })
export const deleteUser = (id) => request.delete(`/admin/users/${id}`)

// ---- 业务统计 ----
export const getStatsOverview = () => request.get('/admin/stats/overview')
export const getStatsBookings = (params) => request.get('/admin/stats/bookings', { params })
