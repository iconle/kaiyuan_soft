import request from '../utils/request'

export function login(data) {
  return request.post('/api/auth/login', data)
}

export function changePassword(data) {
  return request.put('/api/auth/password', data)
}
