import { API_CONFIG } from '../config/apiConfig.js'

const TOKEN_KEY = 'health-admin-token'
const ADMIN_KEY = 'health-admin-user'

export function getAdminToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setAdminSession(token, admin) {
  localStorage.setItem(TOKEN_KEY, token || '')
  localStorage.setItem(ADMIN_KEY, JSON.stringify(admin || {}))
}

export function clearAdminSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(ADMIN_KEY)
}

export function getCachedAdmin() {
  try {
    return JSON.parse(localStorage.getItem(ADMIN_KEY) || '{}')
  } catch {
    return {}
  }
}

export async function request(path, opt = {}) {
  const ctl = new AbortController()
  const tm = setTimeout(() => ctl.abort(), opt.timeout || API_CONFIG.TIMEOUT)
  const hdr = {
    ...(opt.headers || {})
  }

  const mtd = String(opt.method || 'GET').toUpperCase()
  const hasBody = opt.body !== undefined && mtd !== 'GET'

  if (hasBody) {
    hdr['Content-Type'] = 'application/json'
  }

  const tok = getAdminToken()
  if (tok) {
    hdr.Authorization = `Bearer ${tok}`
  }

  // 管理端统一注入 token 和超时控制，CRUD 页只关心业务数据。
  const reqCfg = {
    method: mtd,
    headers: hdr,
    signal: ctl.signal
  }

  if (hasBody) {
    reqCfg.body = typeof opt.body === 'string' ? opt.body : JSON.stringify(opt.body)
  }

  try {
    const resp = await fetch(`${API_CONFIG.BASE_URL}${path}`, reqCfg)
    const data = await resp.json().catch(() => ({}))
    if (!resp.ok || (data && typeof data === 'object' && data.code !== 200)) {
      throw new Error(data.message || `请求失败 (${resp.status})`)
    }
    if (data && typeof data === 'object' && Object.prototype.hasOwnProperty.call(data, 'data')) {
      return data.data
    }
    return data
  } catch (err) {
    if (err && err.name === 'AbortError') {
      throw new Error('请求超时')
    }
    throw err instanceof Error ? err : new Error('请求失败')
  } finally {
    clearTimeout(tm)
  }
}
