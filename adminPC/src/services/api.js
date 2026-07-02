import { clearAdminSession, getAdminToken, getCachedAdmin, request, setAdminSession } from './request.js'
import { buildQuery, toNumber } from './normalizers.js'

export const adminSession = {
  getAdminToken,
  setAdminSession,
  clearAdminSession,
  getCachedAdmin
}

export async function login(payload) {
  return request('/api/admin/login', {
    method: 'POST',
    body: payload
  })
}

export async function stats() {
  return request('/api/admin/stats')
}

export async function updateUserPassword(userId, newPassword) {
  return request(`/api/admin/user/${encodeURIComponent(userId)}/password`, {
    method: 'PUT',
    body: { newPassword }
  })
}

export async function adminList(resource, params = {}) {
  const payload = await request(`/api/admin/${resource}${buildQuery(params)}`)
  const items = Array.isArray(payload?.items) ? payload.items : []
  return {
    ...payload,
    items,
    total: toNumber(payload?.total, items.length),
    page: toNumber(payload?.page, params.page || 1),
    size: toNumber(payload?.size, params.size || items.length || 20)
  }
}

export async function adminCreate(resource, payload = {}) {
  return request(`/api/admin/${resource}`, {
    method: 'POST',
    body: payload
  })
}

export async function adminUpdate(resource, id, payload = {}) {
  return request(`/api/admin/${resource}/${encodeURIComponent(id)}`, {
    method: 'PUT',
    body: payload
  })
}

export async function adminDelete(resource, id) {
  return request(`/api/admin/${resource}/${encodeURIComponent(id)}`, {
    method: 'DELETE'
  })
}
