function toText(value) {
  return value === null || value === undefined ? '' : String(value).trim()
}

// 表单输入先转数字，非法值回退，避免表格编辑写入 NaN。
export function toNumber(value, fallback = 0) {
  const num = Number(value)
  return Number.isFinite(num) ? num : fallback
}

export function buildQuery(params = {}) {
  const qs = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null) return
    const txt = toText(value)
    if (!txt) return
    qs.set(key, txt)
  })
  const str = qs.toString()
  return str ? `?${str}` : ''
}
