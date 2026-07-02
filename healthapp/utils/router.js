import { logUserAction } from '@/services/healthApi.js'
import { hasAuthToken } from '@/utils/auth.js'

const MAIN_ROUTE_MAP = {
  dashboard: '/pages/dashboard/index',
  diet: '/pages/diet/index',
  plan: '/pages/plan/index',
  knowledge: '/pages/knowledge/index',
  profile: '/pages/profile/index',
  settings: '/pages/settings/index'
}

const LAST_MAIN_KEY = 'health-app-last-main-page'
const LOGIN_ROUTE = '/pages/auth/login'

export function getMainRoute(key = 'dashboard') {
  return MAIN_ROUTE_MAP[key] || MAIN_ROUTE_MAP.dashboard
}

export function getLoginRoute() {
  return LOGIN_ROUTE
}

export function rememberMainPage(key) {
  if (!MAIN_ROUTE_MAP[key]) return
  uni.setStorageSync(LAST_MAIN_KEY, key)
}

function getLastMainPageKey() {
  const key = uni.getStorageSync(LAST_MAIN_KEY)
  return MAIN_ROUTE_MAP[key] ? key : 'dashboard'
}

export function getLastMainPageRoute() {
  return getMainRoute(getLastMainPageKey())
}

export function openMainPage(key) {
  if (!hasAuthToken()) {
    openLoginPage()
    return
  }
  const url = getMainRoute(key)
  rememberMainPage(key)
  logUserAction('open_main_page', { key, url })
  uni.reLaunch({ url })
}

export function openDetailPage(url) {
  if (!url) return
  if (!hasAuthToken()) {
    openLoginPage()
    return
  }
  logUserAction('open_detail_page', { url })
  uni.navigateTo({ url })
}

export function safeBack() {
  if (!hasAuthToken()) {
    openLoginPage()
    return
  }
  logUserAction('navigate_back')
  uni.navigateBack({
    fail() {
      const url = getLastMainPageRoute()
      uni.redirectTo({
        url,
        fail() {
          uni.reLaunch({ url })
        }
      })
    }
  })
}

export function openLoginPage() {
  uni.reLaunch({
    url: LOGIN_ROUTE
  })
}
