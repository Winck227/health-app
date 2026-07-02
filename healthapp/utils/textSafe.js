// 功能注释：统一处理外部抓取文本，避免乱码、空标题进入页面。
export function safeText(value) {
  return String(value || '')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/gi, ' ')
    .replace(/&amp;/gi, '&')
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/\s+/g, ' ')
    .trim()
}

// 功能注释：判断是否为明显乱码。
function hasLuanMa(value) {
  const txt = safeText(value)
  if (!txt) return true
  if (txt.includes('锟')) return true
  if (/\uFFFD/.test(txt)) return true
  if (/[脙脗氓忙莽猫茅锚矛铆卯茂冒帽貌贸么玫枚]/.test(txt)) return true
  if (/[鐢俊缁旈柅缂佺挧鍡曠换閹礭]{2,}/.test(txt)) return true
  return false
}

// 功能注释：标题必须可展示，且不能是乱码。
function isOkTitle(value) {
  const txt = safeText(value)
  if (!txt) return false
  if (hasLuanMa(txt)) return false
  return /[\u4e00-\u9fa5A-Za-z0-9]/.test(txt)
}

// 功能注释：生成稳定 ID。
export function hashText(value) {
  const txt = safeText(value)
  let hash = 0
  for (let i = 0; i < txt.length; i += 1) {
    hash = ((hash << 5) - hash) + txt.charCodeAt(i)
    hash |= 0
  }
  return Math.abs(hash).toString(36)
}

// 功能注释：按 URL 去重并过滤乱码标题。
export function uniqueValidArticles(list) {
  const seenUrl = new Set()
  return (Array.isArray(list) ? list : []).filter((item) => {
    const url = safeText(item?.url)
    const title = safeText(item?.title)
    if (!url || !isOkTitle(title)) return false
    if (seenUrl.has(url)) return false
    seenUrl.add(url)
    return true
  })
}
