import { recordStepTotal } from '@/services/stepHistoryLocal.js'
import { getUserScopedStorageKey } from '@/utils/auth.js'

const BASE_STORAGE_KEY = 'health-app-step-counter-v3'

function storageKey() {
  return getUserScopedStorageKey(BASE_STORAGE_KEY)
}

const ALPHA = 0.82
const PEAK_THRESHOLD = 0.24
const MIN_INTERVAL_MS = 360
const MAX_INTERVAL_MS = 1400
const CONFIRM_COUNT = 4

const state = {
  started: false,
  startPromise: null,
  available: false,
  listenerBound: false,
  steps: 0,
  subscribers: new Set(),
  saveTimer: null,
  lastRaw: { x: 0, y: 0, z: 0 },
  filtered: { x: 0, y: 0, z: 0 },
  lastMag: 0,
  rising: false,
  lastPeakAt: 0,
  confirmCount: 0,
  confirmed: false
}

// 功能注释：读取本地步数缓存。
function readStorage() {
  try {
    return uni.getStorageSync(storageKey()) || {}
  } catch (error) {
    return {}
  }
}

// 功能注释：写入本地步数缓存。
function writeStorage() {
  try {
    uni.setStorageSync(storageKey(), {
      steps: state.steps
    })
    recordStepTotal(state.steps)
  } catch (error) {}
}

// 功能注释：延迟持久化步数。
function scheduleSave() {
  if (state.saveTimer) clearTimeout(state.saveTimer)
  state.saveTimer = setTimeout(() => {
    writeStorage()
    state.saveTimer = null
  }, 180)
}

// 功能注释：立即把当前步数写入本地缓存，避免切后台丢失最近数据。
export function flushStepCounterStorage() {
  if (state.saveTimer) {
    clearTimeout(state.saveTimer)
    state.saveTimer = null
  }
  writeStorage()
}


// 功能注释：账号切换或会话恢复后，重新读取当前用户自己的步数缓存，避免沿用上一个账号的内存步数。
export function reloadStepCounterForCurrentUser(baseSteps = 0) {
  if (state.saveTimer) {
    clearTimeout(state.saveTimer)
    state.saveTimer = null
  }
  const saved = readStorage()
  state.steps = Math.max(Number(saved.steps || 0), Number(baseSteps || 0))
  resetDetector()
  writeStorage()
  emit()
  return getSnapshot()
}

// 功能注释：通知所有步数订阅者。
function emit() {
  const snapshot = getSnapshot()
  state.subscribers.forEach((listener) => {
    try {
      listener(snapshot)
    } catch (error) {}
  })
}

// 功能注释：重置步数检测器的状态。
function resetDetector() {
  state.lastRaw = { x: 0, y: 0, z: 0 }
  state.filtered = { x: 0, y: 0, z: 0 }
  state.lastMag = 0
  state.rising = false
  state.lastPeakAt = 0
  state.confirmCount = 0
  state.confirmed = false
}

// 功能注释：计算高通滤波后的加速度幅值。
function highPassMagnitude(acc) {
  state.filtered.x = ALPHA * (state.filtered.x + acc.x - state.lastRaw.x)
  state.filtered.y = ALPHA * (state.filtered.y + acc.y - state.lastRaw.y)
  state.filtered.z = ALPHA * (state.filtered.z + acc.z - state.lastRaw.z)
  state.lastRaw = { ...acc }
  return Math.sqrt(
    state.filtered.x ** 2 +
    state.filtered.y ** 2 +
    state.filtered.z ** 2
  )
}

// 功能注释：登记一次已确认的步数。
function registerConfirmedStep(now) {
  if (!state.confirmed) {
    state.confirmCount += 1
    if (state.confirmCount >= CONFIRM_COUNT) {
      state.confirmed = true
      state.steps += CONFIRM_COUNT
      scheduleSave()
      emit()
    }
    return
  }

  state.steps += 1
  scheduleSave()
  emit()
}

// 功能注释：处理加速度传感器数据。
function handleAccelerometerChange(res) {
  const mag = highPassMagnitude({
    x: Number(res.x || 0),
    y: Number(res.y || 0),
    z: Number(res.z || 0)
  })

  const now = Date.now()
  const interval = now - state.lastPeakAt

  if (interval > MAX_INTERVAL_MS) {
    state.confirmCount = 0
    state.confirmed = false
  }

  if (mag > state.lastMag) {
    state.rising = true
  } else if (state.rising && state.lastMag > PEAK_THRESHOLD) {
    state.rising = false

    if (interval >= MIN_INTERVAL_MS && interval <= MAX_INTERVAL_MS) {
      registerConfirmedStep(now)
      state.lastPeakAt = now
    } else if (interval > MAX_INTERVAL_MS) {
      state.confirmCount = 0
      state.confirmed = false
      state.lastPeakAt = now
    }
  }

  state.lastMag = mag
}

// 功能注释：初始化步数基准和本地缓存。
function loadInitial(baseSteps = 0) {
  const saved = readStorage()
  state.steps = Math.max(Number(saved.steps || 0), Number(baseSteps || 0))
  resetDetector()
  writeStorage()
}

// 功能注释：获取当前步数快照。
export function getSnapshot() {
  return {
    available: state.available,
    started: state.started,
    steps: state.steps
  }
}

// 功能注释：设置传感器步数的初始基准。
export function setStepBase(baseSteps = 0) {
  const normalizedBase = Number(baseSteps || 0)
  // 初始基准值只作为首次启动参考，不能覆盖传感器已经累计出的更高步数。
  if (normalizedBase > state.steps) {
    state.steps = normalizedBase
    scheduleSave()
    emit()
  } else {
    recordStepTotal(state.steps)
  }
}

// 功能注释：订阅步数变化。
export function subscribeSteps(listener) {
  if (typeof listener !== 'function') return () => {}
  state.subscribers.add(listener)
  listener(getSnapshot())
  return () => {
    state.subscribers.delete(listener)
  }
}

// 功能注释：启动步数传感器监听。
export function startStepCounter(baseSteps = 0) {
  if (state.started) {
    setStepBase(baseSteps)
    return Promise.resolve(getSnapshot())
  }
  if (state.startPromise) {
    setStepBase(baseSteps)
    return state.startPromise
  }

  loadInitial(baseSteps)

  state.startPromise = new Promise((resolve) => {
    const finish = () => {
      emit()
      resolve(getSnapshot())
    }

    if (typeof uni.startAccelerometer !== 'function' || typeof uni.onAccelerometerChange !== 'function') {
      state.available = false
      state.started = false
      finish()
      return
    }

    uni.startAccelerometer({
      interval: 'game',
      success() {
        state.available = true
        state.started = true
        if (state.listenerBound && typeof uni.offAccelerometerChange === 'function') {
          uni.offAccelerometerChange(handleAccelerometerChange)
          state.listenerBound = false
        }
        uni.onAccelerometerChange(handleAccelerometerChange)
        state.listenerBound = true
        finish()
      },
      fail() {
        state.available = false
        state.started = false
        finish()
      }
    })
  }).finally(() => {
    state.startPromise = null
  })

  return state.startPromise
}
