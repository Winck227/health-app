import { API_CONFIG } from '@/config/api.config.js'
import { clearAuthSession, getAuthToken } from '@/utils/auth.js'

export const BASE_URL = API_CONFIG.BASE_URL

// 统一拆后端 Result 包，页面层只拿业务 data。
function normResp(resp) {
  const body = resp?.data
  if (body && typeof body === 'object' && Object.prototype.hasOwnProperty.call(body, 'code')) {
    if (body.code === 200) return body.data
    throw new Error(body.message || '请求失败')
  }
  return body
}

export function request(url, opt = {}) {
  const tok = opt.skipAuth ? '' : getAuthToken()
  const reqUrl = /^https?:\/\//.test(url) ? url : `${opt.baseUrl || API_CONFIG.BASE_URL}${url}`
  const ct = opt.contentType === undefined ? 'application/json' : opt.contentType
  const mtd = opt.method || 'GET'

  return new Promise((resolve, reject) => {
    const reqCfg = {
      url: reqUrl,
      method: mtd,
      data: opt.data || {},
      timeout: opt.timeout || API_CONFIG.TIMEOUT,
      withCredentials: Boolean(opt.withCredentials),
      header: {
        ...(ct ? { 'Content-Type': ct } : {}),
        ...(tok ? { Authorization: `Bearer ${tok}` } : {}),
        ...(opt.header || {})
      },
      success(resp) {
        try {
          if (resp.statusCode >= 200 && resp.statusCode < 300) {
            resolve(opt.rawResponse ? resp : normResp(resp))
            return
          }
          // 401 统一回登录页，避免各页面重复写会话失效逻辑。
          if (resp.statusCode === 401 && !opt.skipAuth) {
            clearAuthSession()
            uni.reLaunch({ url: '/pages/auth/login' })
            reject(new Error('登录已过期'))
            return
          }
          reject(new Error(resp.data?.message || `HTTP ${resp.statusCode}`))
        } catch (err) {
          reject(err)
        }
      },
      fail(err) {
        reject(new Error(err?.errMsg || '网络请求失败'))
      }
    }

    if (opt.responseType) {
      reqCfg.responseType = opt.responseType
    }

    uni.request(reqCfg)
  })
}
