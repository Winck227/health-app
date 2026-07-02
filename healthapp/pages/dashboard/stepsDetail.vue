<template>
  <view class="page">
    <TopBar title="步数详情" />
    <text class="subhead">步数来自本地传感器和本地缓存，不写入数据库</text>

    <view class="hero-num">
      <text class="big">{{ formattedTotal }}</text>
      <text class="small">今日步数</text>
      <text class="tip">{{ availabilityText }}</text>
    </view>

    <view class="u-card card goal-card">
      <view>
        <text class="card-title">每日目标</text>
        <text class="goal-text">{{ goalText }}</text>
      </view>
      <view class="percent-pill">{{ percent }}%</view>
    </view>

    <view class="u-card card">
      <view class="card-head">
        <text class="card-title">本地步数统计</text>
        <view class="seg">
          <text
            v-for="item in ranges"
            :key="item.key"
            class="seg-item"
            :class="{ active: range === item.key }"
            @click="changeRange(item.key)"
          >
            {{ item.label }}
          </text>
        </view>
      </view>
      <MiniBarChart v-if="detail.hasHistory" :bars="detail.bars" @select="showBarValue" />
      <view v-else class="empty-box">
        <text class="empty-title">暂无步数历史</text>
        <text class="empty-sub">开始行走后，系统会把本地计步结果写入最近记录。</text>
      </view>
      <text v-if="selectedBarText" class="value-tip">{{ selectedBarText }}</text>
    </view>

  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onHide, onShow } from '@dcloudio/uni-app'
import TopBar from '@/components/ui/TopBar.vue'
import MiniBarChart from '@/components/charts/MiniBarChart.vue'
import { fetchSettingsData, logUserAction } from '@/services/healthApi.js'
import { getStepHistorySnapshot, recordStepTotal } from '@/services/stepHistoryLocal.js'
import { getSnapshot, startStepCounter, subscribeSteps } from '@/services/stepCounter.js'

const ranges = [
  { key: 'week', label: '周' },
  { key: 'month', label: '月' },
  { key: 'season', label: '季' }
]

const detail = reactive({
  total: 0,
  bars: [],
  hasHistory: false,
  available: false
})
const range = ref('week')
const stepGoal = ref(9000)
const selectedBarText = ref('')
let unsubscribeSteps = null

const formattedTotal = computed(() => Number(detail.total || 0).toLocaleString())
const percent = computed(() => Math.min(100, Math.round((Number(detail.total || 0) / Math.max(1, Number(stepGoal.value || 9000))) * 100)))
const goalText = computed(() => `${Number(stepGoal.value || 9000).toLocaleString()} 步`)
const availabilityText = computed(() => detail.available ? '计步传感器运行中' : '未检测到计步传感器，显示本地缓存')

function applySnapshot(snapshot) {
  detail.total = Number(snapshot?.total || 0)
  detail.bars = Array.isArray(snapshot?.bars) ? snapshot.bars : []
  detail.hasHistory = Boolean(snapshot?.hasHistory)
  if (detail.bars.length) {
    // 优先使用显式 active 标记，不再通过颜色反推当前项。
    const active = detail.bars.find((item) => item?.active) || detail.bars[detail.bars.length - 1]
    selectedBarText.value = `${active?.label || '日期'}：${Number(active?.value || 0).toLocaleString()} 步`
  } else {
    selectedBarText.value = ''
  }
}

async function loadSettings() {
  try {
    const settings = await fetchSettingsData()
    stepGoal.value = Number(settings?.stepGoal || settings?.step_goal || settings?.dailySteps || 9000)
  } catch (error) {
    stepGoal.value = 9000
  }
}

async function loadData() {
  await loadSettings()
  const sensor = await startStepCounter(0)
  detail.available = Boolean(sensor?.available)
  recordStepTotal(Number(getSnapshot().steps || 0))
  applySnapshot(getStepHistorySnapshot(range.value))
  bindSteps()
}

function bindSteps() {
  if (unsubscribeSteps) return
  unsubscribeSteps = subscribeSteps((snapshot) => {
    detail.available = Boolean(snapshot.available)
    recordStepTotal(Number(snapshot.steps || 0))
    applySnapshot(getStepHistorySnapshot(range.value))
  })
}

onShow(() => {
  logUserAction('view_steps_detail', { range: range.value })
  loadData()
})

onHide(() => {
  if (unsubscribeSteps) {
    unsubscribeSteps()
    unsubscribeSteps = null
  }
})

function changeRange(nextRange) {
  if (range.value === nextRange) return
  range.value = nextRange
  logUserAction('change_steps_range', { range: nextRange })
  applySnapshot(getStepHistorySnapshot(range.value))
}

function showBarValue(item) {
  if (!item) return
  const label = item.timeLabel || item.label || item.time || '日期'
  const value = Number(item.value || 0).toLocaleString()
  selectedBarText.value = `${label}：${value} 步`
}
</script>

<style scoped>
.page { padding: 20rpx 36rpx 40rpx; box-sizing: border-box; background: radial-gradient(circle at 80% 0%, rgba(255, 122, 47, 0.08), transparent 28%), #F6F8FB; }
.subhead { display: block; margin-top: 6rpx; font-size: 24rpx; color: var(--c-muted); line-height: 1.6; }
.hero-num { margin-top: 18rpx; text-align: center; padding: 12rpx 0 8rpx; }
.big { display: block; font-size: 92rpx; font-weight: 1000; color: var(--c-title); line-height: 1; }
.small { display: block; margin-top: 10rpx; font-size: 26rpx; color: var(--c-muted); font-weight: 800; }
.tip { display: block; margin-top: 8rpx; font-size: 22rpx; color: #CBD5E1; font-weight: 800; }
.card { margin-top: 22rpx; border-radius: 52rpx; padding: 26rpx; }
.goal-card { display: flex; align-items: center; justify-content: space-between; }
.goal-text { display: block; margin-top: 10rpx; font-size: 34rpx; font-weight: 1000; color: var(--c-title); }
.percent-pill { padding: 12rpx 20rpx; border-radius: 9999rpx; background: rgba(255,122,47,0.12); color: var(--c-orange); font-size: 26rpx; font-weight: 1000; }
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10rpx; }
.card-title { display: block; font-size: 22rpx; color: var(--c-muted); font-weight: 900; }
.seg { background: #fff; border-radius: 9999rpx; padding: 8rpx; display: flex; gap: 8rpx; box-shadow: 0 16rpx 44rpx rgba(15, 23, 42, 0.06); }
.seg-item { min-width: 52rpx; padding: 10rpx 18rpx; border-radius: 9999rpx; font-size: 24rpx; font-weight: 900; color: var(--c-muted); text-align: center; }
.seg-item.active { background: #F1F5F9; color: var(--c-primary); }
.empty-box { min-height: 220rpx; border-radius: 34rpx; background: #F8FAFC; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 30rpx; box-sizing: border-box; }
.empty-title { font-size: 28rpx; font-weight: 1000; color: var(--c-title); }
.empty-sub { margin-top: 8rpx; font-size: 23rpx; line-height: 1.6; text-align: center; color: var(--c-muted); font-weight: 700; }
.value-tip { display: block; margin-top: 14rpx; padding: 14rpx 18rpx; border-radius: 22rpx; background: rgba(255, 122, 47, 0.1); color: var(--c-orange); font-size: 24rpx; font-weight: 900; }
</style>
