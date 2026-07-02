import { fetchPlanTemplates, fetchPlanTemplatesVersion } from '@/services/healthApi.js'

const STORAGE_TEMPLATE_KEY = 'health-app-plan-template-library-v1'
const STORAGE_VERSION_KEY = 'health-app-plan-template-version-v1'

function toText(value) {
  return String(value || '').trim()
}

function clone(data) {
  return JSON.parse(JSON.stringify(data || null))
}

function normalizeTemplate(item) {
  const source = item && typeof item === 'object' ? item : {}
  return {
    id: toText(source.id || source.templateId || source.name),
    name: toText(source.name || source.title || '未命名模板'),
    category: toText(source.category || '运动'),
    days: Math.max(1, Number(source.days || source.targetDays || 7)),
    goalText: toText(source.goalText || source.description || source.goal_text || ''),
    theme: toText(source.theme || 'green'),
    enabled: source.enabled === undefined ? true : Boolean(source.enabled)
  }
}

function normalizeTemplateLibrary(payload) {
  const source = payload && typeof payload === 'object' && !Array.isArray(payload) ? payload : {}
  const rawTemplates = Array.isArray(payload) ? payload : (Array.isArray(source.templates) ? source.templates : Array.isArray(source.items) ? source.items : [])
  const templates = rawTemplates.map(normalizeTemplate).filter((item) => item.id && item.enabled)
  return {
    version: toText(source.version || source.templateVersion || 'local-cache'),
    updatedAt: toText(source.updatedAt || source.updated_at || ''),
    source: toText(source.source || 'backend-json'),
    sourceLabel: '本地缓存',
    templates
  }
}

function readStoredLibrary() {
  try {
    const stored = uni.getStorageSync(STORAGE_TEMPLATE_KEY)
    const version = toText(uni.getStorageSync(STORAGE_VERSION_KEY))
    if (!stored) return null
    const normalized = normalizeTemplateLibrary(stored)
    if (version) normalized.version = version
    if (!normalized.templates.length) return null
    return normalized
  } catch (error) {
    return null
  }
}

function setStorageAsync(key, data) {
  return new Promise((resolve, reject) => {
    try {
      uni.setStorage({ key, data, success: resolve, fail: reject })
    } catch (error) {
      reject(error)
    }
  })
}

async function saveLibrary(library) {
  const payload = {
    version: library.version,
    updatedAt: library.updatedAt,
    source: library.source,
    templates: clone(library.templates)
  }
  await Promise.all([
    setStorageAsync(STORAGE_VERSION_KEY, payload.version),
    setStorageAsync(STORAGE_TEMPLATE_KEY, payload)
  ])
  return { ...payload, sourceLabel: '本地缓存' }
}

export function loadPlanTemplateLibrary() {
  return readStoredLibrary() || {
    version: '',
    updatedAt: '',
    source: 'none',
    sourceLabel: '未同步',
    templates: []
  }
}

export async function syncPlanTemplateLibrary() {
  const versionResult = await fetchPlanTemplatesVersion()
  const remoteVersion = toText(typeof versionResult === 'string' ? versionResult : versionResult?.version || versionResult?.templateVersion || '')
  const remoteUpdatedAt = toText(versionResult?.updatedAt || versionResult?.updated_at || '')
  if (!remoteVersion) throw new Error('计划模板版本无效')

  const stored = readStoredLibrary()
  if (stored && stored.version === remoteVersion && stored.templates.length) {
    return { ...stored, updatedAt: stored.updatedAt || remoteUpdatedAt, updated: false }
  }

  const list = await fetchPlanTemplates()
  const normalized = normalizeTemplateLibrary(Array.isArray(list) ? { version: remoteVersion, updatedAt: remoteUpdatedAt, templates: list } : list)
  normalized.version = remoteVersion
  if (remoteUpdatedAt) normalized.updatedAt = remoteUpdatedAt
  const saved = await saveLibrary(normalized)
  return { ...saved, updated: true, sourceLabel: '接口同步' }
}

export { normalizeTemplate }

export default {
  loadPlanTemplateLibrary,
  syncPlanTemplateLibrary,
  normalizeTemplate
}
