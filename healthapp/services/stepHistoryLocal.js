import { getUserScopedStorageKey } from '@/utils/auth.js'

const BASE_STORAGE_KEY = 'health-app-step-history-v1'

function storageKey() {
  return getUserScopedStorageKey(BASE_STORAGE_KEY)
}

function pad(value) {
  return String(value).padStart(2, '0')
}

function todayText(date = new Date()) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function clamp(value, min, max) {
  const num = Number(value)
  if (!Number.isFinite(num)) return min
  return Math.max(min, Math.min(max, num))
}

function readState() {
  try {
    const data = uni.getStorageSync(storageKey())
    if (data && typeof data === 'object') return data
  } catch (error) {}
  return { days: {}, lastDate: '', lastTotal: 0 }
}

function writeState(state) {
  try {
    uni.setStorageSync(storageKey(), state)
  } catch (error) {}
}

function createDay() {
  return { total: 0, hourly: Array.from({ length: 24 }, () => 0) }
}

function normalizeDay(day) {
  const source = day && typeof day === 'object' ? day : {}
  const hourly = Array.isArray(source.hourly) ? source.hourly.slice(0, 24).map((item) => clamp(item, 0, 999999)) : []
  while (hourly.length < 24) hourly.push(0)
  return {
    total: clamp(source.total, 0, 999999),
    hourly
  }
}

function trimOldDays(days) {
  const keys = Object.keys(days || {}).sort().slice(-90)
  const next = {}
  keys.forEach((key) => {
    next[key] = normalizeDay(days[key])
  })
  return next
}

export function recordStepTotal(totalSteps, date = new Date()) {
  const currentDate = todayText(date)
  const hour = date.getHours()
  const state = readState()
  state.days = trimOldDays(state.days || {})
  if (!state.days[currentDate]) state.days[currentDate] = createDay()

  if (state.lastDate && state.lastDate !== currentDate) {
    state.lastTotal = 0
  }

  const day = normalizeDay(state.days[currentDate])
  const total = clamp(totalSteps, 0, 999999)
  const previous = state.lastDate === currentDate ? clamp(state.lastTotal, 0, 999999) : 0
  const delta = Math.max(0, total - previous)
  day.total = Math.max(day.total, total)
  day.hourly[hour] = clamp((day.hourly[hour] || 0) + delta, 0, 999999)

  state.days[currentDate] = day
  state.lastDate = currentDate
  state.lastTotal = total
  writeState(state)
  return getStepHistorySnapshot()
}

// 显式标记当天数据，调用方不需要再通过颜色推断。
function makeBar(label, value, max, active = false) {
  const h = value > 0 ? Math.max(8, Math.round((value / Math.max(1, max)) * 100)) : 4
  return {
    label,
    value,
    h,
    active,
    c: active ? 'rgba(255,122,47,1)' : 'rgba(255,122,47,0.36)'
  }
}

function buildLinePoints(hourly = []) {
  const hours = [0, 4, 8, 12, 16, 20, 23]
  const samples = hours.map((hour) => ({ hour, value: Number(hourly[hour] || 0) }))
  const positiveCount = samples.filter((item) => item.value > 0).length
  if (positiveCount < 2) {
    return { points: '', samples, hasTrend: false }
  }
  const max = Math.max(...samples.map((item) => item.value), 1)
  return {
    points: samples.map((item, index) => {
      const x = 5 + Math.round((90 * index) / (samples.length - 1))
      const y = 84 - Math.round((item.value / max) * 60)
      return `${x},${y}`
    }).join(' '),
    samples,
    hasTrend: true
  }
}

function dayLabel(date) {
  return `${pad(date.getMonth() + 1)}/${pad(date.getDate())}`
}

export function getStepHistorySnapshot(range = 'week') {
  const state = readState()
  const days = trimOldDays(state.days || {})
  const today = new Date()
  const todayKey = todayText(today)
  const todayDay = normalizeDay(days[todayKey])
  const count = range === 'month' ? 30 : range === 'season' ? 90 : 7
  const barItems = []
  for (let offset = count - 1; offset >= 0; offset -= 1) {
    const date = new Date()
    date.setDate(today.getDate() - offset)
    const key = todayText(date)
    const day = normalizeDay(days[key])
    barItems.push({ key, date, total: day.total })
  }
  const hasHistory = barItems.some((item) => item.total > 0)
  const max = Math.max(...barItems.map((item) => item.total), 1)
  const step = count > 14 ? Math.ceil(count / 7) : 1
  const bars = hasHistory
    ? barItems
      .filter((_, index) => index % step === 0 || index === barItems.length - 1)
      .map((item) => makeBar(dayLabel(item.date), item.total, max, item.key === todayKey))
    : []
  const line = buildLinePoints(todayDay.hourly)

  return {
    total: todayDay.total,
    today: todayDay,
    bars,
    barItems,
    hasHistory,
    hourlySamples: line.samples,
    hasTodayTrend: line.hasTrend,
    linePoints: line.points,
    range,
    days
  }
}

export default {
  recordStepTotal,
  getStepHistorySnapshot
}
