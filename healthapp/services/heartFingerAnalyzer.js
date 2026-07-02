// 功能注释：根据 CameraX 连续帧统计值判断手指是否正确覆盖摄像头和闪光灯。
export function analyzeFingerStatus(samples) {
  const recent = Array.isArray(samples) ? samples.slice(-60) : []
  if (recent.length < 20) {
    return {
      status: 'waiting_finger',
      ready: false,
      quality: 0,
      message: '请将手指轻轻覆盖后置摄像头和闪光灯'
    }
  }

  const redValues = recent.map((item) => Number(item.red || 0))
  const brightnessValues = recent.map((item) => Number(item.brightness || 0))
  const redRatioValues = recent.map((item) => Number(item.redRatio || 0))

  const redMean = average(redValues)
  const brightnessMean = average(brightnessValues)
  const redRatioMean = average(redRatioValues)
  const redStd = standardDeviation(redValues)
  const metrics = { redMean, brightnessMean, redRatioMean, redStd }

  if (brightnessMean < 35 || redMean < 60) {
    return {
      status: 'finger_too_dark',
      ready: false,
      quality: 0.15,
      message: '画面过暗，请确认闪光灯已开启，并稍微放松手指',
      metrics
    }
  }

  if (redMean > 245 || brightnessMean > 240) {
    return {
      status: 'finger_too_bright',
      ready: false,
      quality: 0.2,
      message: '画面过亮，请稍微移动手指，避免完全贴死镜头',
      metrics
    }
  }

  if (redRatioMean < 0.42) {
    return {
      status: 'waiting_finger',
      ready: false,
      quality: 0.25,
      message: '未检测到手指，请让手指同时覆盖摄像头和闪光灯',
      metrics
    }
  }

  if (redStd > 18) {
    return {
      status: 'finger_unstable',
      ready: false,
      quality: 0.35,
      message: '信号不稳定，请保持手指不动',
      metrics
    }
  }

  if (redStd < 0.3) {
    return {
      status: 'finger_unstable',
      ready: false,
      quality: 0.4,
      message: '波动过弱，请稍微调整手指位置',
      metrics
    }
  }

  const quality = Math.max(0.6, Math.min(1, 0.62 + Math.min(redStd, 8) / 25 + Math.min(redRatioMean - 0.42, 0.22)))
  return {
    status: 'measuring',
    ready: true,
    quality: Number(quality.toFixed(2)),
    message: '信号良好，请保持不动，正在测量',
    metrics
  }
}

export function getFingerStatusLabel(status) {
  const labels = {
    idle: '准备中',
    waiting_finger: '等待手指覆盖',
    finger_too_dark: '画面过暗',
    finger_too_bright: '画面过亮',
    finger_unstable: '信号不稳定',
    measuring: '信号良好',
    done: '测量完成',
    error: '测量失败'
  }
  return labels[status] || labels.idle
}

function average(list) {
  if (!list.length) return 0
  return list.reduce((sum, value) => sum + value, 0) / list.length
}

function standardDeviation(list) {
  if (!list.length) return 0
  const mean = average(list)
  const variance = average(list.map((value) => (value - mean) ** 2))
  return Math.sqrt(variance)
}
