<template>
  <AppShell active="plan">
    <view class="head">
      <view class="head-row">
        <view>
          <text class="h1">健康计划</text>
          <text class="sub">自定义计划，今天是否已打卡一眼可见</text>
        </view>
        <view class="add-btn" @click="goAdd()">添加</view>
      </view>
    </view>

    <view v-if="pageError" class="u-card error-card">
      <text class="error-title">加载失败</text>
      <text class="error-sub">{{ pageError || '暂无数据' }}</text>
    </view>

    <template v-else>
      <view class="u-card section-card">
        <view class="section-head">
          <view>
            <text class="section-title">快速模板</text>
            <text class="section-sub">{{ templateLibrary.sourceLabel }} · 版本 {{ templateLibrary.version || '--' }}</text>
            <text class="section-sub">更新时间 {{ templateLibrary.updatedAt || '--' }}</text>
          </view>
          <view class="template-sync-btn" @click="refreshTemplates">检查更新</view>
        </view>

        <view v-if="templates.length" class="template-list">
          <view
            v-for="item in templates"
            :key="item.id"
            class="template-item"
            @click="goAdd(item.id)"
          >
            <view class="template-main">
              <text class="template-name">{{ item.name }}</text>
              <text class="template-meta">{{ item.days }} 天 · {{ item.category }}</text>
              <text class="template-goal">{{ item.goalText }}</text>
            </view>
            <view class="template-action">选用</view>
          </view>
        </view>

        <view v-else class="empty-text">暂无模板</view>
      </view>

      <view class="u-card section-card">
        <view class="section-head">
          <text class="section-title">我的计划</text>
          <text class="section-sub">今日已打卡 {{ todayCheckedInCount }}/{{ plans.length }}</text>
        </view>

        <view v-if="plans.length">
          <view
            v-for="item in plans"
            :key="item.id"
            class="plan-card"
            @click="openPlan(item.id)"
          >
            <view class="plan-top">
              <view>
                <text class="plan-name">{{ item.name }}</text>
                <text class="plan-meta">{{ item.days }} 天计划 · {{ item.goalText }}</text>
              </view>
              <view class="plan-status" :class="item.todayCheckedIn ? 'status-done' : 'status-warn'">
                {{ item.todayCheckedIn ? '今日已打卡' : '今日待打卡' }}
              </view>
            </view>

            <view class="bar">
              <view
                class="bar-fill"
                :class="item.theme === 'red' ? 'fill-red' : item.theme === 'blue' ? 'fill-blue' : 'fill-green'"
                :style="{ width: item.percent + '%' }"
              />
            </view>

            <view class="plan-bottom">
              <text class="plan-tip">已完成 {{ item.completedDays }}/{{ item.days }} 天</text>
              <view
                class="plan-action"
                :class="{ done: item.todayCheckedIn }"
                @click.stop="handleCheckin(item)"
              >
                {{ item.todayCheckedIn ? '今日已打卡' : '今日打卡' }}
              </view>
            </view>
          </view>
        </view>

        <view v-else class="empty-text">暂无计划</view>
      </view>
    </template>
  </AppShell>
</template>

<script setup>
// 功能注释：计划页只保留模板建计划和今日打卡流程。
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppShell from '@/components/app/AppShell.vue'
import {
  checkinPlan,
  fetchPlanList,
  logUserAction
} from '@/services/healthApi.js'
import { loadPlanTemplateLibrary, syncPlanTemplateLibrary } from '@/services/planTemplateLocal.js'
import { openDetailPage } from '@/utils/router.js'

const pageError = ref('')
const templates = ref([])
const plans = ref([])
const templateLibrary = ref(loadPlanTemplateLibrary())

const todayCheckedInCount = computed(() => plans.value.filter((item) => item.todayCheckedIn).length)

function todayText() {
  const date = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function normalizePlan(item) {
  const today = todayText()
  return {
    ...item,
    completedDays: Number(item.completedDays || 0),
    percent: Number(item.percent || 0),
    todayCheckedIn: Boolean(
      item.todayCheckedIn ||
      item.lastCheckinDate === today ||
      (Array.isArray(item.checkinDates) && item.checkinDates.includes(today))
    )
  }
}

async function loadPlans() {
  try {
    pageError.value = ''
    templateLibrary.value = loadPlanTemplateLibrary()
    templates.value = templateLibrary.value.templates || []
    const planList = await fetchPlanList()
    plans.value = Array.isArray(planList) ? planList.map(normalizePlan) : []
    if (!templates.value.length) {
      await refreshTemplates(false)
    }
  } catch (error) {
    pageError.value = error.message || '加载失败'
    templates.value = []
    plans.value = []
  }
}

async function refreshTemplates(showToast = true) {
  try {
    const result = await syncPlanTemplateLibrary()
    templateLibrary.value = result
    templates.value = result.templates || []
    if (showToast) {
      uni.showToast({ title: result.updated ? '模板已更新' : '已是最新', icon: 'none' })
    }
  } catch (error) {
    if (showToast) uni.showToast({ title: error.message || '模板更新失败', icon: 'none' })
  }
}

onShow(() => {
  logUserAction('view_plan_page')
  loadPlans()
})

async function handleCheckin(plan) {
  if (!plan?.id) return
  if (plan.todayCheckedIn) {
    uni.showToast({ title: '今日已打卡', icon: 'none' })
    return
  }
  try {
    const result = await checkinPlan(plan.id)
    const nextPlan = normalizePlan(result?.plan || result || plan)
    plans.value = plans.value.map((item) => (item.id === nextPlan.id ? nextPlan : item))
    uni.showToast({ title: result?.alreadyCheckedIn ? '今日已打卡' : '打卡成功', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error.message || '打卡失败', icon: 'none' })
  }
}

function openPlan(planId) {
  logUserAction('open_plan_detail', { planId })
  openDetailPage(`/pages/plan/detail?id=${encodeURIComponent(planId)}`)
}

function goAdd(templateId = '') {
  logUserAction('open_add_plan', { templateId })
  const url = templateId
    ? `/pages/plan/add?templateId=${encodeURIComponent(templateId)}`
    : '/pages/plan/add'
  openDetailPage(url)
}
</script>

<style scoped>
.head {
  margin-bottom: 22rpx;
  padding-top: 6rpx;
}
.head-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}
.h1 {
  display: block;
  font-size: 46rpx;
  font-weight: 900;
  color: var(--c-title);
}
.sub {
  display: block;
  margin-top: 10rpx;
  font-size: 26rpx;
  color: var(--c-muted);
}
.add-btn {
  margin-top: 6rpx;
  padding: 14rpx 22rpx;
  border-radius: 9999rpx;
  background: rgba(18, 183, 106, 0.14);
  color: var(--c-primary);
  font-size: 26rpx;
  font-weight: 1000;
}
.error-card,
.section-card {
  margin-top: 22rpx;
  border-radius: 52rpx;
  padding: 28rpx;
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
.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
  margin-bottom: 16rpx;
}
.section-title {
  font-size: 28rpx;
  font-weight: 1000;
  color: var(--c-title);
}
.section-sub {
  font-size: 22rpx;
  color: var(--c-muted);
  font-weight: 800;
}
.template-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}
.template-item,
.plan-card {
  border-radius: 36rpx;
  background: #fff;
  padding: 24rpx;
  box-shadow: 0 16rpx 44rpx rgba(15, 23, 42, 0.06);
}
.template-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}
.template-name,
.plan-name {
  display: block;
  font-size: 32rpx;
  font-weight: 1000;
  color: var(--c-title);
}
.template-meta,
.plan-meta {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: var(--c-muted);
  font-weight: 800;
}
.template-goal {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: var(--c-text);
  font-weight: 700;
}
.template-sync-btn {
  flex-shrink: 0;
  padding: 10rpx 16rpx;
  border-radius: 9999rpx;
  background: rgba(18, 183, 106, 0.12);
  color: var(--c-primary);
  font-size: 22rpx;
  font-weight: 1000;
}
.template-action,
.plan-action {
  flex-shrink: 0;
  padding: 10rpx 18rpx;
  border-radius: 9999rpx;
  background: rgba(18, 183, 106, 0.12);
  color: var(--c-primary);
  font-size: 22rpx;
  font-weight: 1000;
}
.plan-card {
  margin-top: 16rpx;
}
.plan-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}
.plan-status {
  padding: 10rpx 18rpx;
  border-radius: 9999rpx;
  font-size: 22rpx;
  font-weight: 1000;
}
.status-done {
  background: rgba(18, 183, 106, 0.12);
  color: var(--c-primary);
}
.status-warn {
  background: rgba(255, 122, 47, 0.12);
  color: #ff7a2f;
}
.bar {
  margin-top: 22rpx;
  height: 18rpx;
  border-radius: 9999rpx;
  background: #EEF2F7;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  border-radius: 9999rpx;
}
.fill-red {
  background: linear-gradient(90deg, #FF2D55, #FF5D7B);
}
.fill-blue {
  background: linear-gradient(90deg, #5B6CFF, #7B86FF);
}
.fill-green {
  background: linear-gradient(90deg, #12B76A, #36D399);
}
.plan-bottom {
  margin-top: 16rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}
.plan-tip {
  font-size: 22rpx;
  color: var(--c-muted);
  font-weight: 700;
}
.plan-action.done {
  background: rgba(148, 163, 184, 0.12);
  color: #64748B;
}
</style>
