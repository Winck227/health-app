const STORAGE_KEY = 'health-app-heart-monitor-v3'

const state = {
  currentBpm: null,
  history: [],
  initialized: false,
  subscribers: new Set()
}

function formatTime(date = new Date()) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

function validBpm(value) {
  const bpm = Number(value)
  return Number.isFinite(bpm) && bpm >= 30 && bpm <= 220 ? Math.round(bpm) : null
}

function cloneSnapshot() {
  return {
    currentBpm: state.currentBpm,
    history: state.history.map((item) => ({ ...item }))
  }
}

function save() {
  try {
    uni.setStorageSync(STORAGE_KEY, cloneSnapshot())
  } catch (error) {}
}

function emit() {
  const snapshot = cloneSnapshot()
  state.subscribers.forEach((listener) => {
    try {
      listener(snapshot)
    } catch (error) {}
  })
}

function normalize(payload = {}) {
  const history = Array.isArray(payload.history) ? payload.history : []
  const normalizedHistory = history
    .map((item, index) => {
      const bpm = validBpm(item.bpm ?? item.heartRate ?? item.heart_rate ?? payload.currentBpm)
      if (!bpm) return null
      return {
        id: item.id || `heart-${index}-${Date.now()}`,
        bpm,
        heartRate: bpm,
        time: item.time || item.recordTime || formatTime(),
        recordTime: item.recordTime || item.time || formatTime(),
        tag: item.tag || (item.measureType === 'manual' ? '手动录入' : '心率记录'),
        measureType: item.measureType || item.measure_type || ''
      }
    })
    .filter(Boolean)

  const currentBpm = validBpm(payload.currentBpm ?? payload.heartRate ?? payload.heart_rate)
    || normalizedHistory[0]?.bpm
    || null

  return {
    currentBpm,
    history: normalizedHistory
  }
}

export function initHeartMonitor(payload = {}) {
  const normalized = normalize(payload)
  state.currentBpm = normalized.currentBpm
  state.history = normalized.history
  state.initialized = true
  save()
  emit()
  return cloneSnapshot()
}

export function getHeartSnapshot() {
  if (!state.initialized) {
    return initHeartMonitor()
  }
  return cloneSnapshot()
}

export function subscribeHeart(listener) {
  if (typeof listener !== 'function') return () => {}
  state.subscribers.add(listener)
  listener(getHeartSnapshot())
  return () => {
    state.subscribers.delete(listener)
  }
}

export function recordHeartMeasurement(bpm, meta = {}) {
  const value = validBpm(bpm)
  if (!value) return cloneSnapshot()
  if (!state.initialized) initHeartMonitor()
  const time = formatTime()
  const record = {
    id: `heart-${Date.now()}`,
    bpm: value,
    heartRate: value,
    time,
    recordTime: time,
    tag: meta.tag || (value > 95 ? '运动后' : '静息'),
    source: meta.source || 'camera',
    measureType: meta.measureType || (meta.source === 'manual' ? 'manual' : 'camera_ppg')
  }
  state.currentBpm = value
  state.history.unshift(record)
  state.history = state.history.slice(0, 12)
  save()
  emit()
  return cloneSnapshot()
}
