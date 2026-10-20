import request from './request'

// ---- 认证 ----
export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)

// ---- 用户 ----
export const getUserInfo = () => request.get('/user/info')
export const updateUserInfo = (data) => request.put('/user/info', data)

// ---- 房源 ----
export const getApartments = (params) => request.get('/apartments', { params })
export const getApartment = (id) => request.get(`/apartments/${id}`)

// ---- 租房预约 ----
export const createBooking = (apartmentId) => request.post('/bookings', { apartmentId })
export const myBookings = () => request.get('/bookings/my')
export const cancelBooking = (id) => request.put(`/bookings/${id}/cancel`)
