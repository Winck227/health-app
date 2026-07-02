import { BASE_URL, request } from '@/services/request.js'
import { clearAuthSession, getAuthToken, setAuthSession } from '@/utils/auth.js'
import { reloadStepCounterForCurrentUser } from '@/services/stepCounter.js'
import categoryConfig from '@/config/categories.json'
import { fetchArticleDetailByUrl, fetchSearchPages } from '@/services/healthCrawler.js'
import { hashText, safeText, uniqueValidArticles } from '@/utils/textSafe.js'

function keepSession(data) {
  if (data && typeof data === 'object' && data.token) {
    setAuthSession(data.token, data.user || {})
    try {
      reloadStepCounterForCurrentUser(0)
    } catch (error) {}
  }
  return data
}

function requestBody(method, url, data) {
  return request(url, { method, data })
}

export async function login(payload) {
  const data = await requestBody('POST', '/api/auth/login', payload)
  return keepSession(data)
}

export async function register(payload) {
  const data = await requestBody('POST', '/api/auth/register', payload)
  return keepSession(data)
}

export async function fetchSession() {
  const data = await request('/api/auth/session')
  return keepSession(data)
}

export async function logout() {
  await request('/api/auth/logout', { method: 'POST' }).catch(() => {})
  clearAuthSession()
}

export function logUserAction() {
  return Promise.resolve()
}

export async function fetchDashboardData() {
  return request('/api/dashboard')
}

export async function fetchDietPageData() {
  return request('/api/diet')
}

export async function submitDietRecord(payload) {
  return requestBody('POST', '/api/diet', payload)
}

export async function deleteDietRecord(recordId) {
  return request(`/api/diet/${encodeURIComponent(recordId || '')}`, { method: 'DELETE' })
}

export async function fetchPlanTemplatesVersion() {
  return request('/api/plan-templates/version')
}

const KNOWLEDGE_CATEGORY_MAP = {
  all: '生活方式',
  sport: '运动',
  diet: '饮食',
  sleep: '睡眠',
  mental: '心理'
}

const DEFAULT_KEYWORDS = {
  all: ['健康生活', '养生', '久坐', '喝水', '长寿'],
  sport: ['跑步', '瑜伽', '力量训练', '有氧', '减脂'],
  diet: ['健康饮食', '均衡饮食', '低糖低脂', '高蛋白', '膳食纤维'],
  sleep: ['睡眠质量', '早睡早起', '深度睡眠', '失眠', '作息规律'],
  mental: ['心理健康', '减压', '情绪管理', '正念冥想', '焦虑']
}

function readKeywordsByFilter(filterKey = 'all') {
  const categoryName = KNOWLEDGE_CATEGORY_MAP[filterKey] || KNOWLEDGE_CATEGORY_MAP.all
  const categories = Array.isArray(categoryConfig?.categories) ? categoryConfig.categories : []
  const matched = categories.find((item) => item.name === categoryName)
  const keywords = Array.isArray(matched?.keywords) ? matched.keywords.filter(Boolean) : []
  return keywords.length ? keywords : (DEFAULT_KEYWORDS[filterKey] || DEFAULT_KEYWORDS.all)
}

function toKnowledgeArticle(item, filterKey, keyword, index = 0) {
  const title = safeText(item?.title)
  const summary = safeText(item?.summary || item?.content || item?.analysis?.summary || '')
  const url = safeText(item?.url)
  return {
    id: `crawler-${filterKey}-${hashText(url || title)}-${index}`,
    category: keyword || '健康资讯',
    filterKey,
    keyword,
    title,
    summary,
    source: safeText(item?.source || item?.siteName || '39健康网'),
    publishTime: safeText(item?.publishTime || item?.publishedAt || ''),
    readMinutes: Math.max(1, Math.ceil(String(summary || title).length / 500)),
    themeClass: filterKey === 'diet' ? 'amber' : filterKey === 'sleep' ? 'indigo' : filterKey === 'sport' ? 'green' : 'blue',
    icon: '文',
    url
  }
}

export async function fetchKnowledgeList(options = {}) {
  const filterKey = typeof options === 'string' ? options : (options.filterKey || 'all')
  const keywords = readKeywordsByFilter(filterKey)
  const keyword = options.keyword || keywords[0] || '健康生活'
  const startPage = Math.max(1, Number(options.startPage || 1))
  const pageCount = Math.max(1, Number(options.pageCount || 2))
  const result = await fetchSearchPages(keyword, startPage, pageCount)
  return uniqueValidArticles(result.items).map((item, index) => toKnowledgeArticle(item, filterKey, keyword, index))
}

export function getKnowledgeKeywords(filterKey = 'all') {
  return readKeywordsByFilter(filterKey)
}

export async function fetchRandomKnowledgeTips(limit = 3) {
  const safeLimit = Math.max(1, Number(limit) || 3)
  const filterKeys = ['all', 'sport', 'diet', 'sleep', 'mental']
  const pickedFilter = filterKeys[Math.floor(Math.random() * filterKeys.length)] || 'all'
  const keywords = readKeywordsByFilter(pickedFilter)
  const keyword = keywords[Math.floor(Math.random() * keywords.length)] || '健康生活'
  const startPage = Math.max(1, Math.floor(Math.random() * 4) + 1)
  const list = await fetchKnowledgeList({ filterKey: pickedFilter, keyword, startPage, pageCount: 2 })
  return list.slice(0, safeLimit)
}

export async function fetchExternalKnowledgeDetail(url, fallback = {}) {
  return fetchArticleDetailByUrl(safeText(url), fallback)
}

export async function fetchHealthRecord() {
  return request('/api/health/record')
}

export async function saveHealthRecord(payload) {
  return requestBody('POST', '/api/health/record', payload)
}

export async function fetchPlanList() {
  return request('/api/plans')
}

export async function createPlan(payload) {
  return requestBody('POST', '/api/plans', payload)
}

export async function fetchPlanDetail(planId) {
  return request(`/api/plans/${encodeURIComponent(planId)}`)
}

export async function deletePlan(planId) {
  return request(`/api/plans/${encodeURIComponent(planId)}`, { method: 'DELETE' })
}

export async function fetchProfileData() {
  return request('/api/profile')
}

export async function updateProfile(payload) {
  return requestBody('PUT', '/api/profile', payload)
}

export function uploadAvatar(filePath) {
  const token = getAuthToken()
  return new Promise((resolve, reject) => {
    if (!filePath) {
      reject(new Error('请选择头像文件'))
      return
    }
    uni.uploadFile({
      url: `${BASE_URL}/api/profile/avatar`,
      filePath,
      name: 'file',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success(response) {
        try {
          const body = typeof response.data === 'string' ? JSON.parse(response.data || '{}') : (response.data || {})
          if (response.statusCode >= 200 && response.statusCode < 300 && body.code === 200) {
            resolve(body.data || {})
            return
          }
          reject(new Error(body.message || `头像上传失败：HTTP ${response.statusCode}`))
        } catch (error) {
          reject(new Error('头像上传响应解析失败'))
        }
      },
      fail(error) {
        reject(new Error(error?.errMsg || '头像上传失败'))
      }
    })
  })
}

export async function clearAvatar() {
  return request('/api/profile/avatar', { method: 'DELETE' })
}

export async function fetchSettingsData() {
  return request('/api/settings')
}

export async function updateSettings(payload) {
  return requestBody('PUT', '/api/settings', payload)
}

export async function fetchGoals() {
  const settings = await fetchSettingsData()
  return {
    dailySteps: settings?.stepGoal ?? settings?.step_goal ?? settings?.dailySteps ?? 9000,
    stepGoal: settings?.stepGoal ?? settings?.step_goal ?? settings?.dailySteps ?? 9000
  }
}

export async function updateGoals(payload) {
  return updateSettings({
    stepGoal: payload?.stepGoal ?? payload?.dailySteps ?? payload?.step_goal,
    step_goal: payload?.stepGoal ?? payload?.dailySteps ?? payload?.step_goal
  })
}

export async function fetchPrivacy() {
  const settings = await fetchSettingsData()
  return {
    privacyLevel: settings?.privacyLevel || settings?.privacy_level || 'partial'
  }
}

export async function updatePrivacy(payload) {
  return updateSettings({
    privacyLevel: payload?.privacyLevel || payload?.privacy_level || 'partial',
    privacy_level: payload?.privacyLevel || payload?.privacy_level || 'partial'
  })
}

export async function updatePassword(payload) {
  return requestBody('PUT', '/api/user/password', payload)
}

export async function fetchHeartData() {
  return request('/api/heart')
}

export async function saveHeartRecord(payload) {
  return requestBody('POST', '/api/heart', payload)
}

export async function fetchPlanTemplates() {
  return request('/api/plan-templates')
}

export async function checkinPlan(planId) {
  return requestBody('POST', `/api/plans/${encodeURIComponent(planId)}/checkin`, {})
}

export async function fetchArticleFavorites() {
  return request('/api/article-favorite')
}

export async function addFavoriteArticle(article) {
  const payload = typeof article === 'object' ? article : { articleId: article }
  return requestBody('POST', '/api/article-favorite', payload)
}

export async function removeFavoriteArticle(favoriteId) {
  return request(`/api/article-favorite/${encodeURIComponent(favoriteId || '')}`, { method: 'DELETE' })
}
