const TOKEN_KEY = 'health-app-token'
const USER_KEY = 'health-app-user'

// 功能注释：读取当前登录 token。
export function getAuthToken() {
  try {
    return uni.getStorageSync(TOKEN_KEY) || ''
  } catch (error) {
    return ''
  }
}

// 功能注释：判断当前是否已有登录态。
export function hasAuthToken() {
  return Boolean(getAuthToken())
}

// 功能注释：保存登录态和用户信息。
export function setAuthSession(token, user) {
  uni.setStorageSync(TOKEN_KEY, token || '')
  uni.setStorageSync(USER_KEY, user || {})
}

// 功能注释：读取本地缓存的用户信息。
export function getCachedUser() {
  try {
    return uni.getStorageSync(USER_KEY) || {}
  } catch (error) {
    return {}
  }
}

// 功能注释：兼容旧调用名，返回当前缓存用户。
export function getCurrentUser() {
  return getCachedUser()
}

// 功能注释：清空登录态。
export function clearAuthSession() {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
}

// 功能注释：读取当前登录用户的稳定标识，用于隔离本地缓存。
function getCurrentUserStorageId() {
  const user = getCachedUser() || {}
  const raw = user.id || user.userId || user.uid || user.username || user.phone || 'guest'
  return String(raw || 'guest').replace(/[^a-zA-Z0-9_-]/g, '_')
}

// 功能注释：为本地辅助数据生成按用户隔离的缓存 key，避免多账号数据串用。
export function getUserScopedStorageKey(baseKey) {
  return `${baseKey}:user:${getCurrentUserStorageId()}`
}
