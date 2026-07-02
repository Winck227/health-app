import { API_CONFIG } from '@/config/api.config.js'

// 功能注释：把后端返回的相对头像路径转为可直接给 <image> 使用的完整地址。
export function resolveMediaUrl(value = '') {
  const text = String(value || '').trim()
  if (!text) return ''
  if (/^https?:\/\//i.test(text)) return text
  const base = String(API_CONFIG.BASE_URL || '').replace(/\/$/, '')
  const path = text.startsWith('/') ? text : `/${text}`
  return `${base}${path}`
}
