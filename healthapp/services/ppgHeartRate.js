const DEFAULT_MIN_BPM = 45
const DEFAULT_MAX_BPM = 180

// 功能注释：判断数值是否为有限数字。
function isFiniteNumber(value) {
  return Number.isFinite(Number(value))
}

// 功能注释：计算数值平均值。
function mean(values) {
  if (!values.length) return 0
  return values.reduce((sum, value) => sum + value, 0) / values.length
}

// 功能注释：计算标准差。
function standardDeviation(values, avg = mean(values)) {
  if (!values.length) return 0
  const variance = values.reduce((sum, value) => sum + ((value - avg) ** 2), 0) / values.length
  return Math.sqrt(variance)
}

// 功能注释：对序列做滑动平均。
export function movingAverage(values, windowSize = 3) {
  const size = Math.max(1, Math.floor(windowSize))
  const half = Math.floor(size / 2)
  return values.map((value, index) => {
    const start = Math.max(0, index - half)
    const end = Math.min(values.length, index + half + 1)
    return mean(values.slice(start, end))
  })
}

// 功能注释：从图像数据中提取红色平均值。
export function extractRedMeanFromImageData(imageData, options = {}) {
  const width = Number(imageData?.width || 0)
  const height = Number(imageData?.height || 0)
  const data = imageData?.data
  if (!width || !height || !data || data.length < width * height * 4) return 0

  const cropRatio = Math.max(0.1, Math.min(1, Number(options.cropRatio || 0.35)))
  const cropWidth = Math.max(1, Math.floor(width * cropRatio))
  const cropHeight = Math.max(1, Math.floor(height * cropRatio))
  const startX = Math.floor((width - cropWidth) / 2)
  const startY = Math.floor((height - cropHeight) / 2)
  const endX = startX + cropWidth
  const endY = startY + cropHeight

  let total = 0
  let count = 0
  for (let y = startY; y < endY; y += 1) {
    for (let x = startX; x < endX; x += 1) {
      total += data[((y * width + x) * 4)]
      count += 1
    }
  }
  return count ? total / count : 0
}

// 功能注释：规范化采样点数组。
function normalizeSamples(samples) {
  const normalized = samples
    .map((item, index) => {
      if (typeof item === 'number') {
        return { time: index, red: item }
      }
      return {
        time: Number(item?.time ?? item?.timestamp ?? index),
        red: Number(item?.red ?? item?.value ?? item?.brightness ?? 0)
      }
    })
    .filter((item) => isFiniteNumber(item.time) && isFiniteNumber(item.red))
    .sort((a, b) => a.time - b.time)

  if (normalized.length < 2) return normalized

  const first = normalized[0].time
  const last = normalized[normalized.length - 1].time
  const looksLikeMilliseconds = last - first > 1000
  return normalized.map((item) => ({
    time: looksLikeMilliseconds ? (item.time - first) / 1000 : item.time - first,
    red: item.red
  }))
}

// 功能注释：准备去趋势后的信号序列。
function prepareSignal(samples, sampleRate) {
  const raw = samples.map((item) => item.red)
  const shortWindow = Math.max(3, Math.round(sampleRate * 0.18))
  const longWindow = Math.max(shortWindow + 2, Math.round(sampleRate * 1.4))
  const smoothed = movingAverage(raw, shortWindow)
  const trend = movingAverage(smoothed, longWindow)
  const detrended = smoothed.map((value, index) => value - trend[index])
  const avg = mean(detrended)
  const std = standardDeviation(detrended, avg)
  if (!std) return detrended.map(() => 0)
  return detrended.map((value) => (value - avg) / std)
}

// 功能注释：在信号中提取峰值点。
function collectPeaks(signal, times, options) {
  const minBpm = Number(options.minBpm || DEFAULT_MIN_BPM)
  const maxBpm = Number(options.maxBpm || DEFAULT_MAX_BPM)
  const minInterval = 60 / maxBpm
  const threshold = Number(options.threshold || 0.35)
  const peaks = []

  for (let i = 1; i < signal.length - 1; i += 1) {
    if (signal[i] <= threshold) continue
    if (signal[i] <= signal[i - 1] || signal[i] < signal[i + 1]) continue

    const current = { index: i, time: times[i], value: signal[i] }
    const previous = peaks[peaks.length - 1]
    if (previous && current.time - previous.time < minInterval) {
      if (current.value > previous.value) {
        peaks[peaks.length - 1] = current
      }
      continue
    }
    peaks.push(current)
  }

  const maxInterval = 60 / minBpm
  return peaks.filter((peak, index) => {
    if (index === 0 || index === peaks.length - 1) return true
    const before = peak.time - peaks[index - 1].time
    const after = peaks[index + 1].time - peak.time
    return before <= maxInterval || after <= maxInterval
  })
}

// 功能注释：把峰值间隔换算成 BPM。
function bpmFromPeaks(peaks, options) {
  if (peaks.length < 2) return 0
  const minBpm = Number(options.minBpm || DEFAULT_MIN_BPM)
  const maxBpm = Number(options.maxBpm || DEFAULT_MAX_BPM)
  const intervals = []
  for (let i = 1; i < peaks.length; i += 1) {
    const interval = peaks[i].time - peaks[i - 1].time
    const bpm = 60 / interval
    if (bpm >= minBpm && bpm <= maxBpm) intervals.push(interval)
  }
  if (!intervals.length) return 0
  return Math.round(60 / mean(intervals))
}

// 功能注释：根据采样点估算心率。
export function estimateHeartRate(samples, options = {}) {
  const points = normalizeSamples(Array.isArray(samples) ? samples : [])
  const minDurationSeconds = Number(options.minDurationSeconds || 6)
  if (points.length < 30) {
    return { bpm: 0, reliable: false, quality: 0, reason: '采样点不足', peaks: [] }
  }

  const duration = points[points.length - 1].time - points[0].time
  if (duration < minDurationSeconds) {
    return { bpm: 0, reliable: false, quality: 0, reason: '采样时间太短', peaks: [] }
  }

  const sampleRate = (points.length - 1) / duration
  const redValues = points.map((item) => item.red)
  const redMean = mean(redValues)
  const redStd = standardDeviation(redValues, redMean)
  if (redMean < 25) {
    return { bpm: 0, reliable: false, quality: 0, reason: '画面太暗，请盖住摄像头并开启补光', peaks: [] }
  }
  if (redMean > 248 && redStd < 0.6) {
    return { bpm: 0, reliable: false, quality: 0, reason: '画面过曝，请轻一点按压手指', peaks: [] }
  }
  if (redStd < 0.15) {
    return { bpm: 0, reliable: false, quality: 0, reason: '脉搏波动太弱，请保持手指稳定', peaks: [] }
  }

  const times = points.map((item) => item.time)
  const signal = prepareSignal(points, sampleRate)
  const positivePeaks = collectPeaks(signal, times, options)
  const negativePeaks = collectPeaks(signal.map((value) => -value), times, options)
  const selectedPeaks = negativePeaks.length > positivePeaks.length ? negativePeaks : positivePeaks
  const bpm = bpmFromPeaks(selectedPeaks, options)
  if (!bpm) {
    return { bpm: 0, reliable: false, quality: 0.25, reason: '没有检测到稳定波峰', peaks: selectedPeaks }
  }

  const expectedPeaks = duration * bpm / 60
  const peakScore = Math.min(1, selectedPeaks.length / Math.max(1, expectedPeaks))
  const amplitudeScore = Math.min(1, redStd / 3)
  const quality = Number((0.45 + peakScore * 0.35 + amplitudeScore * 0.2).toFixed(2))

  return {
    bpm,
    reliable: quality >= 0.6,
    quality,
    reason: quality >= 0.6 ? '检测成功' : '信号偏弱，结果仅供参考',
    duration,
    sampleRate,
    peaks: selectedPeaks
  }
}

// 功能注释：生成模拟 PPG 采样数据。
export function buildSyntheticPpgSamples(options = {}) {
  const bpm = Number(options.bpm || 78)
  const durationSeconds = Number(options.durationSeconds || 12)
  const sampleRate = Number(options.sampleRate || 30)
  const baseline = Number(options.baseline || 175)
  const amplitude = Number(options.amplitude || 8)
  const noise = Number(options.noise || 0)
  const samples = []
  const total = Math.floor(durationSeconds * sampleRate)
  for (let i = 0; i <= total; i += 1) {
    const time = i / sampleRate
    const wave = Math.sin(2 * Math.PI * (bpm / 60) * time)
    const harmonic = 0.22 * Math.sin(4 * Math.PI * (bpm / 60) * time + 0.4)
    const noiseValue = noise ? (Math.sin(i * 12.9898) * 43758.5453 % 1) * noise : 0
    samples.push({
      time,
      red: baseline + amplitude * (wave + harmonic) + noiseValue
    })
  }
  return samples
}
