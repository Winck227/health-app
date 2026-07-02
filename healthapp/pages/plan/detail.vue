<template>
  <view class="page">
    <TopBar title="计划详情" />

    <view v-if="pageError" class="u-card error-card">
      <text class="error-title">加载失败</text>
      <text class="error-sub">{{ pageError || '暂无数据' }}</text>
    </view>

    <template v-else>
      <view class="hero u-card" :class="plan.theme === 'red' ? 'hero-red' : plan.theme === 'blue' ? 'hero-blue' : 'hero-green'">
        <text class="hero-name">{{ plan.name }}</text>
        <text class="hero-goal">{{ plan.goalText }}</text>
        <view class="hero-progress">
          <text class="hero-percent">{{ plan.percent }}%</text>
          <text class="hero-days">{{ plan.completedDays }}/{{ plan.days }} 天</text>
        </view>
        <view class="hero-bar">
          <view class="hero-fill" :style="{ width: plan.percent + '%' }" />
        </view>
        <view class="hero-badge" :class="plan.todayCheckedIn ? 'badge-done' : 'badge-warn'">
          {{ plan.todayCheckedIn ? '今日已打卡' : '今日待打卡' }}
        </view>
      </view>

      <view class="u-card card">
        <text class="label">计划说明</text>
        <text class="value">{{ plan.goalText }}</text>

        <text class="label mt">计划周期</text>
        <text class="value">{{ plan.days }} 天</text>

        <text class="label mt">最近打卡</text>
        <text class="value">{{ plan.lastCheckinDate || '暂无记录' }}</text>

        <text class="label mt">今日建议</text>
        <text class="value">{{ suggestion }}</text>
      </view>

      <view class="u-card card">
        <view class="section-head">
          <text class="label-title">打卡记录</text>
          <text class="section-sub">{{ plan.checkinDates?.length || 0 }} 次</text>
        </view>
        <view v-if="recordList.length" class="record-list">
          <view v-for="item in recordList" :key="item" class="record-item">
            <text class="record-date">{{ item }}</text>
          </view>
        </view>
        <text v-else class="empty-text">暂无打卡记录</text>
      </view>

      <view class="actions">
        <view class="btn primary" :class="{ disabled: plan.todayCheckedIn }" @click="handleCheckin">
          {{ plan.todayCheckedIn ? '今日已打卡' : '今日打卡' }}
        </view>
        <view class="btn danger" @click="handleDelete">删除计划</view>
      </view>
    </template>
  </view>
</template>

<script setup>
// 功能注释：详情页只保留模板计划的打卡和删除。
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import TopBar from '@/components/ui/TopBar.vue'
import { checkinPlan, deletePlan, fetchPlanDetail, logUserAction } from '@/services/healthApi.js'
import { safeBack } from '@/utils/router.js'

const pageError = ref('')
const plan = reactive({
  id: '',
  name: '',
  days: 21,
  completedDays: 0,
  percent: 0,
  theme: 'red',
  goalText: '',
  todayCheckedIn: false,
  lastCheckinDate: '',
  checkinDates: []
})

const suggestion = computed(() => {
  if (plan.theme === 'blue') return '今天完成一次力量训练，并记录训练后的身体反馈。'
  if (plan.theme === 'green') return '今天保持稳定步数，晚餐控制到八分饱。'
  return '今天控制饮食节奏，保持步数与基础运动量达标。'
})

const recordList = computed(() => Array.isArray(plan.checkinDates) ? plan.checkinDates.slice(0, 12) : [])

function todayText() {
  const date = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function normalizePlan(data) {
  const today = todayText()
  return {
    ...data,
    completedDays: Number(data?.completedDays || 0),
    percent: Number(data?.percent || 0),
    checkinDates: Array.isArray(data?.checkinDates) ? data.checkinDates : [],
    todayCheckedIn: Boolean(
      data?.todayCheckedIn ||
      data?.lastCheckinDate === today ||
      (Array.isArray(data?.checkinDates) && data.checkinDates.includes(today))
    )
  }
}

async function load(id) {
  try {
    pageError.value = ''
    const data = await fetchPlanDetail(id)
    if (!data) {
      pageError.value = '计划不存在'
      return
    }
    Object.assign(plan, normalizePlan(data))
  } catch (error) {
    pageError.value = error.message || '加载失败'
  }
}

onLoad((options) => {
  const id = options?.id || ''
  logUserAction('view_plan_detail', { id })
  load(id)
})

async function handleCheckin() {
  if (!plan.id) return
  if (plan.todayCheckedIn) {
    uni.showToast({ title: '今日已打卡', icon: 'none' })
    return
  }
  try {
    logUserAction('checkin_plan', { id: plan.id })
    const result = await checkinPlan(plan.id)
    Object.assign(plan, normalizePlan(result?.plan || result || plan))
    uni.showToast({ title: result?.alreadyCheckedIn ? '今日已打卡' : '打卡成功', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error.message || '打卡失败', icon: 'none' })
  }
}

async function handleDelete() {
  if (!plan.id) return
  logUserAction('delete_plan', { id: plan.id })
  await deletePlan(plan.id)
  uni.showToast({ title: '已删除', icon: 'none' })
  setTimeout(() => safeBack(), 250)
}
</script>

<style scoped>
.page {
  padding: 20rpx 36rpx 40rpx;
  box-sizing: border-box;
}
.error-card,
.hero,
.card {
  margin-top: 22rpx;
  border-radius: 52rpx;
}
.error-card {
  padding: 34rpx;
}
.error-title {
  display: block;
  font-size: 34rpx;
  font-weight: 1000;
  color: var(--c-title);
}
.error-sub,
.empty-text {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: var(--c-muted);
  font-weight: 700;
  line-height: 1.7;
}
.hero {
  padding: 32rpx;
  color: #fff;
}
.hero-red {
  background: linear-gradient(135deg, #ff2d55, #ff6b81);
}
.hero-blue {
  background: linear-gradient(135deg, #5b6cff, #7b86ff);
}
.hero-green {
  background: linear-gradient(135deg, #12B76A, #36D399);
}
.hero-name {
  display: block;
  font-size: 42rpx;
  font-weight: 1000;
}
.hero-goal {
  display: block;
  margin-top: 14rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.86);
}
.hero-progress {
  margin-top: 24rpx;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.hero-percent {
  font-size: 54rpx;
  font-weight: 1000;
}
.hero-days {
  font-size: 24rpx;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.86);
}
.hero-bar {
  margin-top: 18rpx;
  height: 18rpx;
  border-radius: 9999rpx;
  background: rgba(255, 255, 255, 0.2);
  overflow: hidden;
}
.hero-fill {
  height: 100%;
  border-radius: 9999rpx;
  background: rgba(255, 255, 255, 0.92);
}
.hero-badge {
  display: inline-flex;
  margin-top: 18rpx;
  padding: 10rpx 18rpx;
  border-radius: 9999rpx;
  font-size: 22rpx;
  font-weight: 1000;
}
.badge-done {
  background: rgba(255, 255, 255, 0.18);
}
.badge-warn {
  background: rgba(255, 255, 255, 0.12);
}
.card {
  padding: 28rpx;
}
.label,
.label-title {
  display: block;
  font-size: 22rpx;
  color: var(--c-muted);
  font-weight: 900;
}
.value {
  display: block;
  margin-top: 10rpx;
  font-size: 28rpx;
  color: var(--c-title);
  font-weight: 800;
  line-height: 1.8;
}
.mt {
  margin-top: 24rpx;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  margin-bottom: 12rpx;
}
.section-sub {
  font-size: 22rpx;
  color: var(--c-muted);
  font-weight: 800;
}
.record-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}
.record-item {
  padding: 10rpx 14rpx;
  border-radius: 9999rpx;
  background: #F8FAFC;
}
.record-date {
  font-size: 22rpx;
  color: var(--c-title);
  font-weight: 800;
}
.actions {
  margin-top: 24rpx;
  display: flex;
  gap: 16rpx;
}
.btn {
  flex: 1;
  height: 96rpx;
  border-radius: 9999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  font-weight: 1000;
}
.primary {
  background: linear-gradient(135deg, #0b1f2a, #0f3a44);
  color: #fff;
}
.danger {
  background: rgba(255, 61, 61, 0.12);
  color: #ff3d3d;
}
.primary.disabled {
  background: #8B919B;
}
</style>
