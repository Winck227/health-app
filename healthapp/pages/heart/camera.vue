<template>
  <view class="page">
    <TopBar title="摄像头测心率" />

    <view class="preview-card u-card" :class="statusClass">
      <view class="preview-window">
        <view class="preview-glow"></view>
        <image class="heart-icon" src="/static/icons/heart_pink.svg" mode="aspectFit" />
        <text class="preview-main">{{ previewTitle }}</text>
        <text class="preview-sub">{{ previewSubtitle }}</text>
      </view>
      <view class="status-pill">{{ statusLabel }}</view>
    </view>

    <view class="measure-card u-card">
      <text class="status-title">{{ statusTitle }}</text>
      <text class="status-desc">{{ statusText }}</text>

      <view class="progress-bar">
        <view class="progress-inner" :style="{ width: `${progressPercent}%` }"></view>
      </view>

      <view class="number-row">
        <text class="number">{{ numberText }}</text>
        <text class="unit">{{ numberUnit }}</text>
      </view>

      <view class="sample-grid">
        <view class="sample-item">
          <text class="sample-value">{{ sampleCount }}</text>
          <text class="sample-label">采样帧</text>
        </view>
        <view class="sample-item">
          <text class="sample-value">{{ Math.round(signalQuality * 100) }}%</text>
          <text class="sample-label">信号质量</text>
        </view>
        <view class="sample-item">
          <text class="sample-value">{{ progressBpm || '--' }}</text>
          <text class="sample-label">估算 BPM</text>
        </view>
      </view>
    </view>

    <view class="action-row">
      <view class="action-btn ghost" @click="safeBack">返回</view>
      <view class="action-btn primary" @click="finishOrStart">
        {{ primaryButtonText }}
      </view>
    </view>

    <view v-if="showManual" class="manual-panel u-card">
      <text class="manual-title">手动录入心率</text>
      <text class="manual-desc">
        当前安装包没有可用的原生摄像头心率插件时，可以录入手环或其他设备测得的心率。
      </text>
      <view class="manual-row">
        <input v-model="manualBpm" class="manual-input" type="number" placeholder="输入心率，例如 72" />
        <view class="manual-btn" @click="saveManual">保存</view>
      </view>
    </view>

    <view v-if="lastError" class="error-card">{{ lastError }}</view>

    <view class="tip-card u-card">
      <text class="tip-title">测量说明</text>
      <text class="tip-line">请使用原生 App 安装包进行摄像头测心率，浏览器摄像头采样入口已移除。</text>
      <text class="tip-line">测量时用指腹轻轻覆盖后置摄像头和闪光灯，并保持手机稳定。</text>
      <text class="tip-line">心率结果仅用于日常健康管理参考，不作为医学诊断依据。</text>
    </view>
  </view>
</template>

<script setup>
import { computed, onUnmounted, ref } from 'vue'
import { onHide, onLoad, onShow } from '@dcloudio/uni-app'
import TopBar from '@/components/ui/TopBar.vue'
import { recordHeartMeasurement } from '@/services/heartMonitor.js'
import { logUserAction, saveHeartRecord } from '@/services/healthApi.js'
import { isAppRuntime } from '@/services/deviceSensors.js'
import { estimateHeartRate } from '@/services/ppgHeartRate.js'
import { analyzeFingerStatus, getFingerStatusLabel } from '@/services/heartFingerAnalyzer.js'
import {
  hasNativeHeartCameraPlugin,
  startNativeHeartCamera,
  stopNativeHeartCamera
} from '@/services/nativeHeartCamera.js'
import { safeBack } from '@/utils/router.js'

const SAMPLE_DURATION = 20
const NATIVE_CAPTURE_SECONDS = 90
const READY_REQUIRED_MS = 2000

const autoStart = ref(false)
const autoStarted = ref(false)
const measuring = ref(false)
const nativeRunning = ref(false)
const showManual = ref(false)
const countdown = ref(SAMPLE_DURATION)
const sampleCount = ref(0)
const progressBpm = ref(0)
const signalQuality = ref(0)
const lastBpm = ref(0)
const lastError = ref('')
const manualBpm = ref('')
const status = ref('idle')
const statusText = ref('请将手指轻轻覆盖后置摄像头和闪光灯')
const nativeSamples = ref([])
const measureSamples = ref([])

let readySince = 0
let measureStartAt = 0
let progressEstimateAt = 0

const canUseNative = computed(() => isAppRuntime() && hasNativeHeartCameraPlugin())

const progressPercent = computed(() => {
  const used = SAMPLE_DURATION - countdown.value
  return Math.min(100, Math.max(0, Math.round((used / SAMPLE_DURATION) * 100)))
})

const statusClass = computed(() => ({
  waiting: status.value === 'waiting_finger',
  warning: ['finger_too_dark', 'finger_too_bright', 'finger_unstable'].includes(status.value),
  active: status.value === 'measuring',
  done: status.value === 'done',
  error: status.value === 'error'
}))

const statusLabel = computed(() => getFingerStatusLabel(status.value))

const statusTitle = computed(() => {
  if (status.value === 'done') return '测量完成'
  if (status.value === 'error') return '测量不可用'
  if (measuring.value) return '正在测量心率'
  if (nativeRunning.value) return '正在判断手指状态'
  if (showManual.value) return '请手动录入结果'
  return '准备测量'
})

const previewTitle = computed(() => (canUseNative.value ? '指尖心率采样' : '等待原生测量环境'))
const previewSubtitle = computed(() => (
  canUseNative.value
    ? '请覆盖后置摄像头和闪光灯，保持稳定完成测量'
    : '当前环境未检测到原生 HeartRateCamera 插件，可手动录入心率'
))

const primaryButtonText = computed(() => {
  if (nativeRunning.value || measuring.value) return '停止测量'
  if (lastBpm.value) return '再次测量'
  if (canUseNative.value) return '开始摄像头测量'
  return '手动录入'
})

const numberText = computed(() => {
  if (measuring.value) return countdown.value
  if (lastBpm.value) return lastBpm.value
  return '--'
})

const numberUnit = computed(() => (measuring.value ? '秒' : '次/分钟'))

onLoad((options = {}) => {
  autoStart.value = String(options.auto || '') === '1'
})

onShow(() => {
  if (autoStart.value && !autoStarted.value) {
    autoStarted.value = true
    setTimeout(() => startMeasure(), 350)
  }
})

onHide(() => {
  if (nativeRunning.value) {
    return
  }
  stopMeasureRuntime()
})
onUnmounted(() => stopMeasureRuntime())

function nowText() {
  const date = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function resetRuntimeState() {
  stopNativeHeartCamera()
  nativeRunning.value = false
  measuring.value = false
  readySince = 0
  measureStartAt = 0
}

function stopMeasureRuntime() {
  resetRuntimeState()
}

function resetMeasureState() {
  lastError.value = ''
  sampleCount.value = 0
  progressBpm.value = 0
  signalQuality.value = 0
  countdown.value = SAMPLE_DURATION
  nativeSamples.value = []
  measureSamples.value = []
  readySince = 0
  measureStartAt = 0
  status.value = 'idle'
  statusText.value = '请将手指轻轻覆盖后置摄像头和闪光灯'
}

function finishOrStart() {
  if (nativeRunning.value || measuring.value) {
    stopMeasureRuntime()
    status.value = 'idle'
    statusText.value = '已停止测量'
    return
  }
  startMeasure()
}

async function startMeasure() {
  resetMeasureState()
  logUserAction('start_camera_heart_measure')

  if (canUseNative.value) {
    startNativeMeasure()
    return
  }

  showManual.value = true
  status.value = 'error'
  lastError.value = isAppRuntime()
    ? '当前安装包未集成 HeartRateCamera 原生插件，请使用自定义基座或手动录入。'
    : '浏览器摄像头测心率已移除，请使用原生 App 或手动录入。'
  statusText.value = lastError.value
}

function startNativeMeasure() {
  try {
    showManual.value = false
    nativeRunning.value = true
    status.value = 'waiting_finger'
    statusText.value = '正在准备测量，请把手指覆盖后置摄像头和闪光灯'
    startNativeHeartCamera({
      durationSeconds: NATIVE_CAPTURE_SECONDS,
      torch: true,
      fps: 30
    }, handleNativeCameraEvent)
  } catch (error) {
    nativeRunning.value = false
    showManual.value = true
    status.value = 'error'
    lastError.value = error?.message || '自动测量启动失败'
    statusText.value = lastError.value
  }
}

function handleNativeCameraEvent(event = {}) {
  if (event.type === 'started') {
    status.value = 'waiting_finger'
    statusText.value = '请将手指轻轻覆盖后置摄像头和闪光灯'
    return
  }

  if (event.type === 'sample') {
    handleNativeSample(event)
    return
  }

  if (event.type === 'done') {
    if (!lastBpm.value && nativeRunning.value) {
      status.value = 'error'
      statusText.value = '采样已结束，但未获得稳定心率，请重新测量'
      lastError.value = statusText.value
    }
    nativeRunning.value = false
    measuring.value = false
    return
  }

  if (event.type === 'error') {
    nativeRunning.value = false
    measuring.value = false
    status.value = 'error'
    lastError.value = event.message || '原生摄像头测量失败'
    statusText.value = lastError.value
    showManual.value = true
    uni.showToast({ title: lastError.value, icon: 'none' })
  }
}

function handleNativeSample(event) {
  const sample = {
    time: Number(event.time || 0),
    red: Number(event.red || 0),
    green: Number(event.green || 0),
    blue: Number(event.blue || 0),
    brightness: Number(event.brightness || 0),
    redRatio: Number(event.redRatio || 0)
  }

  if (!Number.isFinite(sample.red) || sample.red <= 0) return

  nativeSamples.value.push(sample)
  if (nativeSamples.value.length > 300) nativeSamples.value.shift()
  sampleCount.value = Number(event.sampleCount || sampleCount.value + 1)

  const finger = analyzeFingerStatus(nativeSamples.value)
  status.value = finger.status
  statusText.value = finger.message
  signalQuality.value = finger.quality || 0

  if (!measuring.value) {
    if (finger.ready) {
      if (!readySince) readySince = Date.now()
      if (Date.now() - readySince >= READY_REQUIRED_MS) startFormalMeasure()
    } else {
      readySince = 0
    }
    return
  }

  if (!finger.ready) {
    resetMeasureBecauseFingerLost()
    return
  }

  measureSamples.value.push(sample)
  countdown.value = Math.max(0, SAMPLE_DURATION - Math.floor((Date.now() - measureStartAt) / 1000))
  updateProgressBpm()

  if (Date.now() - measureStartAt >= SAMPLE_DURATION * 1000) {
    finishFormalMeasure()
  }
}

function startFormalMeasure() {
  measuring.value = true
  measureSamples.value = []
  measureStartAt = Date.now()
  countdown.value = SAMPLE_DURATION
  status.value = 'measuring'
  statusText.value = '信号良好，正在测量，请保持 20 秒不动'
}

function resetMeasureBecauseFingerLost() {
  measuring.value = false
  measureSamples.value = []
  countdown.value = SAMPLE_DURATION
  readySince = 0
  progressBpm.value = 0
  status.value = 'waiting_finger'
  statusText.value = '检测到手指移开，请重新覆盖摄像头和闪光灯'
}

function updateProgressBpm() {
  const now = Date.now()
  if (now - progressEstimateAt < 1200 || measureSamples.value.length < 90) return
  progressEstimateAt = now
  const result = estimateHeartRate(measureSamples.value, { minDurationSeconds: 4 })
  if (result?.bpm) progressBpm.value = result.bpm
}

async function finishFormalMeasure() {
  if (!measuring.value) return
  measuring.value = false
  statusText.value = '正在计算心率...'

  const result = estimateHeartRate(measureSamples.value, { minDurationSeconds: 10 })
  if (!result || !result.bpm) {
    status.value = 'error'
    lastError.value = result?.reason || '信号不足，请重新测量'
    statusText.value = lastError.value
    return
  }

  await finishMeasure({ bpm: result.bpm, quality: result.quality, source: 'native-camerax' })
}

async function persistHeartRecord(bpm, measureType) {
  const payload = {
    heartRate: bpm,
    measureType,
    recordTime: nowText()
  }
  const response = await saveHeartRecord(payload)
  recordHeartMeasurement(bpm, {
    source: measureType === 'manual' ? 'manual' : 'camera-ppg',
    tag: measureType === 'manual' ? '手动录入' : '摄像头测量',
    measureType
  })
  return response
}

async function finishMeasure(result) {
  const bpm = Math.round(Number(result.bpm || 0))
  if (!bpm) throw new Error(result.reason || '没有检测到稳定心率信号')
  lastBpm.value = bpm
  progressBpm.value = bpm
  signalQuality.value = result.quality || signalQuality.value || 0
  await persistHeartRecord(bpm, 'camera_ppg')
  resetRuntimeState()
  status.value = 'done'
  statusText.value = `测量完成，${bpm} 次/分钟，结果已保存`
  countdown.value = SAMPLE_DURATION
  uni.showToast({ title: `平均 ${bpm} 次/分钟`, icon: 'none' })
  setTimeout(() => safeBack(), 700)
}

async function saveManual() {
  const bpm = Number(manualBpm.value)
  if (!Number.isFinite(bpm) || bpm < 30 || bpm > 220) {
    uni.showToast({ title: '请输入 30-220 的心率', icon: 'none' })
    return
  }
  try {
    const value = Math.round(bpm)
    await persistHeartRecord(value, 'manual')
    lastBpm.value = value
    manualBpm.value = ''
    status.value = 'done'
    statusText.value = '手动心率已保存'
    uni.showToast({ title: '已保存', icon: 'success' })
    setTimeout(() => safeBack(), 400)
  } catch (error) {
    uni.showToast({ title: error.message || '保存失败', icon: 'none' })
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20rpx 36rpx 48rpx;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 18% 0%, rgba(255, 59, 107, 0.08), transparent 28%),
    #F6F8FB;
}
.preview-card {
  margin-top: 24rpx;
  border-radius: 48rpx;
  padding: 22rpx;
  position: relative;
}
.preview-window {
  height: 360rpx;
  border-radius: 36rpx;
  background: linear-gradient(145deg, #111827, #1f2937);
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #fff;
}
.preview-glow {
  position: absolute;
  width: 360rpx;
  height: 360rpx;
  border-radius: 9999rpx;
  background: rgba(255, 59, 107, 0.22);
  filter: blur(20rpx);
  opacity: 0.6;
}
.preview-card.active .preview-glow { animation: pulseGlow 1.1s ease-in-out infinite; }
@keyframes pulseGlow {
  0% { transform: scale(0.82); opacity: 0.45; }
  50% { transform: scale(1.04); opacity: 0.86; }
  100% { transform: scale(0.82); opacity: 0.45; }
}
.heart-icon {
  width: 96rpx;
  height: 96rpx;
  position: relative;
  z-index: 1;
}
.preview-main {
  display: block;
  margin-top: 18rpx;
  font-size: 34rpx;
  font-weight: 1000;
  position: relative;
  z-index: 1;
}
.preview-sub {
  display: block;
  max-width: 520rpx;
  margin-top: 10rpx;
  font-size: 22rpx;
  line-height: 1.6;
  font-weight: 800;
  color: rgba(255,255,255,0.72);
  position: relative;
  z-index: 1;
}
.status-pill {
  position: absolute;
  right: 42rpx;
  top: 42rpx;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(255,255,255,0.9);
  color: #0f172a;
  font-size: 22rpx;
  font-weight: 1000;
}
.measure-card {
  margin-top: 22rpx;
  border-radius: 48rpx;
  padding: 34rpx 30rpx;
  text-align: center;
}
.status-title {
  display: block;
  font-size: 38rpx;
  font-weight: 1000;
  color: var(--c-title);
}
.status-desc {
  display: block;
  margin-top: 14rpx;
  font-size: 24rpx;
  color: var(--c-muted);
  line-height: 1.7;
  font-weight: 800;
}
.progress-bar {
  margin-top: 28rpx;
  width: 100%;
  height: 14rpx;
  border-radius: 999rpx;
  background: rgba(148, 163, 184, 0.18);
  overflow: hidden;
}
.progress-inner {
  height: 100%;
  border-radius: 999rpx;
  background: #12b76a;
  transition: width 0.25s ease;
}
.number-row {
  margin-top: 20rpx;
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 10rpx;
}
.number {
  font-size: 76rpx;
  font-weight: 1000;
  color: var(--c-title);
  line-height: 1;
}
.unit {
  font-size: 24rpx;
  font-weight: 900;
  color: var(--c-muted);
}
.sample-grid {
  margin-top: 20rpx;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
}
.sample-item {
  border-radius: 26rpx;
  background: #F8FAFC;
  padding: 14rpx 8rpx;
}
.sample-value {
  display: block;
  font-size: 28rpx;
  font-weight: 1000;
  color: var(--c-title);
}
.sample-label {
  display: block;
  margin-top: 4rpx;
  font-size: 20rpx;
  font-weight: 800;
  color: var(--c-muted);
}
.action-row {
  margin-top: 24rpx;
  display: flex;
  gap: 16rpx;
}
.action-btn {
  flex: 1;
  height: 92rpx;
  border-radius: 9999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 1000;
}
.action-btn.primary { background: linear-gradient(135deg, #0b1f2a, #0f3a44); color: #fff; }
.action-btn.ghost { background: #fff; color: var(--c-title); }
.manual-panel,
.tip-card {
  margin-top: 22rpx;
  border-radius: 44rpx;
  padding: 28rpx;
}
.manual-title,
.tip-title {
  display: block;
  font-size: 28rpx;
  font-weight: 1000;
  color: var(--c-title);
}
.manual-desc,
.tip-line {
  display: block;
  margin-top: 10rpx;
  font-size: 23rpx;
  color: var(--c-muted);
  line-height: 1.65;
  font-weight: 700;
}
.manual-row {
  margin-top: 18rpx;
  display: flex;
  gap: 14rpx;
}
.manual-input {
  flex: 1;
  height: 78rpx;
  border-radius: 24rpx;
  background: #F8FAFC;
  padding: 0 20rpx;
  font-size: 28rpx;
  font-weight: 900;
}
.manual-btn {
  width: 150rpx;
  height: 78rpx;
  border-radius: 9999rpx;
  background: var(--c-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-weight: 1000;
}
.error-card {
  margin-top: 18rpx;
  padding: 18rpx 24rpx;
  border-radius: 28rpx;
  background: rgba(245, 158, 11, 0.12);
  color: #b45309;
  font-size: 24rpx;
  font-weight: 800;
  line-height: 1.6;
}
</style>
