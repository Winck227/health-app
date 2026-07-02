<template>
  <view class="page">
    <TopBar title="心率监测" />
    <EmptyState v-if="pageError" title="加载失败" :message="pageError || '暂无数据'" />
    <template v-else>
      <text class="subhead">查看当前心率和最近一次测量记录</text>

      <view class="center">
        <image class="heart" src="/static/icons/heart_pink.svg" mode="aspectFit" />
        <text class="bpm">{{ heart.currentBpm ?? '--' }}</text>
        <text class="bpm-sub">次 / 分钟</text>
        <text class="bpm-time">{{ heart.latest?.recordTime || '暂无最近记录' }}</text>
      </view>

      <view class="btn" :class="{ measuring: isOpeningCamera }" @click="startMeasure">
        {{ isOpeningCamera ? '正在打开摄像头...' : '打开摄像头测量' }}
      </view>

      <view class="u-card manual-card">
        <text class="manual-title">手动录入</text>
        <view class="manual-row">
          <input v-model="manualBpm" class="manual-input" type="number" placeholder="例如 72" />
          <view class="manual-btn" @click="saveManual">保存</view>
        </view>
        <text class="manual-tip">当设备无法连续取帧时，可手动录入测量结果，保存到 heart_record。</text>
      </view>

      <view v-if="!heart.history.length" class="empty-card">
        <EmptyState title="暂无数据" message="还没有心率记录" />
      </view>

      <template v-else>
        <text class="his-title">历史记录</text>
        <view v-for="item in heart.history" :key="item.id" class="item u-card">
          <view class="item-left">
            <view class="dot">
              <image class="dot-ico" src="/static/icons/heart_pink.svg" mode="aspectFit" />
            </view>
            <view>
              <text class="item-bpm">{{ item.heartRate ?? item.bpm ?? '--' }} 次/分钟</text>
              <text class="item-time">{{ item.recordTime || item.time || '--' }}</text>
            </view>
          </view>
          <text class="tag">{{ formatMeasureType(item.measureType) }}</text>
        </view>
      </template>
    </template>
  </view>
</template>

<script setup>
// 功能注释：页面数据来自接口和本地状态。
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import TopBar from '@/components/ui/TopBar.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { fetchHeartData, logUserAction, saveHeartRecord } from '@/services/healthApi.js'
import { openDetailPage } from '@/utils/router.js'
import { recordHeartMeasurement } from '@/services/heartMonitor.js'

const pageError = ref('')
const heart = reactive({
  currentBpm: null,
  latest: null,
  history: []
})
const isOpeningCamera = ref(false)
const manualBpm = ref('')

// 功能注释：加载心率历史数据。
async function loadData() {
  try {
    pageError.value = ''
    const data = await fetchHeartData()
    const history = Array.isArray(data?.history) ? data.history : []
    heart.currentBpm = data?.currentBpm ?? data?.latest?.heartRate ?? null
    heart.latest = data?.latest || history[0] || null
    heart.history = history
  } catch (error) {
    pageError.value = error.message || '加载失败'
    heart.currentBpm = null
    heart.latest = null
    heart.history = []
  }
}

onShow(() => {
  logUserAction('view_heart_page')
  loadData()
})

function formatMeasureType(value) {
  if (value === 'camera_ppg') return '摄像头测量'
  if (value === 'manual') return '手动录入'
  if (value) return String(value)
  return '静息'
}

// 功能注释：打开摄像头正式测量页。
function nowText() {
  const date = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

async function saveManual() {
  const bpm = Number(manualBpm.value)
  if (!Number.isFinite(bpm) || bpm < 30 || bpm > 220) {
    uni.showToast({ title: '请输入 30-220 的心率', icon: 'none' })
    return
  }
  try {
    const value = Math.round(bpm)
    await saveHeartRecord({ heartRate: value, measureType: 'manual', recordTime: nowText() })
    recordHeartMeasurement(value, { source: 'manual', tag: '手动录入', measureType: 'manual' })
    manualBpm.value = ''
    uni.showToast({ title: '已保存', icon: 'success' })
    loadData()
  } catch (error) {
    uni.showToast({ title: error.message || '保存失败', icon: 'none' })
  }
}

function startMeasure() {
  if (isOpeningCamera.value) return
  isOpeningCamera.value = true
  logUserAction('open_heart_camera')
  setTimeout(() => {
    isOpeningCamera.value = false
    openDetailPage('/pages/heart/camera?auto=1')
  }, 180)
}
</script>

<style scoped>
.page {
  padding: 20rpx 36rpx 40rpx;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 18% 0%, rgba(255, 59, 107, 0.08), transparent 28%),
    #F6F8FB;
}
.subhead { display:block; margin-top: 6rpx; font-size: 24rpx; color: var(--c-muted); line-height: 1.6; }
.center { margin-top: 34rpx; display: flex; flex-direction: column; align-items: center; }
.heart { width: 140rpx; height: 140rpx; }
.bpm { margin-top: 22rpx; font-size: 110rpx; font-weight: 1000; color: var(--c-title); line-height: 1; }
.bpm-sub { margin-top: 10rpx; font-size: 26rpx; color: var(--c-muted); font-weight: 900; }
.bpm-time { margin-top: 10rpx; font-size: 22rpx; color: var(--c-muted); font-weight: 800; }
.btn { margin-top: 34rpx; height: 110rpx; border-radius: 9999rpx; background: linear-gradient(135deg, #0b1f2a, #0f3a44); box-shadow: 0 40rpx 90rpx rgba(15, 23, 42, 0.22); display: flex; align-items: center; justify-content: center; color: #fff; font-size: 32rpx; font-weight: 1000; }
.btn.measuring { opacity: 0.86; }
.manual-card { margin-top: 22rpx; border-radius: 44rpx; padding: 24rpx; }
.manual-title { display:block; font-size: 28rpx; font-weight:1000; color: var(--c-title); }
.manual-row { margin-top: 16rpx; display:flex; gap: 14rpx; }
.manual-input { flex:1; height: 74rpx; border-radius: 24rpx; background:#F8FAFC; padding:0 20rpx; font-size:28rpx; font-weight:800; }
.manual-btn { width: 150rpx; height: 74rpx; border-radius: 9999rpx; background: var(--c-primary); color:#fff; display:flex; align-items:center; justify-content:center; font-size:26rpx; font-weight:1000; }
.manual-tip { display:block; margin-top: 12rpx; font-size: 22rpx; color: var(--c-muted); line-height:1.6; font-weight:700; }
.empty-card { margin-top: 22rpx; border-radius: 44rpx; padding: 16rpx; }
.his-title { display: block; margin-top: 26rpx; font-size: 22rpx; color: var(--c-muted); font-weight: 900; }
.item { margin-top: 16rpx; border-radius: 44rpx; padding: 20rpx 22rpx; display: flex; align-items: center; justify-content: space-between; }
.item-left { display: flex; align-items: center; gap: 14rpx; }
.dot { width: 70rpx; height: 70rpx; border-radius: 30rpx; background: rgba(255, 59, 107, 0.12); display: flex; align-items: center; justify-content: center; }
.dot-ico { width: 34rpx; height: 34rpx; }
.item-bpm { display: block; font-size: 30rpx; font-weight: 1000; color: var(--c-title); }
.item-time { display: block; margin-top: 6rpx; font-size: 22rpx; color: var(--c-muted); font-weight: 800; }
.tag { padding: 10rpx 20rpx; border-radius: 9999rpx; background: rgba(148, 163, 184, 0.14); color: var(--c-muted); font-size: 22rpx; font-weight: 900; }
</style>
