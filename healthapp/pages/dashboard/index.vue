<template>
  <AppShell active="dashboard">
    <EmptyState v-if="pageError" title="加载失败" :message="pageError || '暂无数据'" />
    <template v-else>
      <view class="head">
        <view>
          <text class="h1">早安，{{ user.name || '加载中' }}</text>
          <text class="sub">{{ user.id ? `${user.id} · ${user.status}` : '正在加载账户信息' }}</text>
        </view>
      </view>

      <view class="score-card" @click="goScoreDetail">
        <text class="score-title">今日健康指数</text>
        <view class="score-row">
          <text class="score">{{ dashboard.score ?? '--' }}</text>
          <text class="score-max">/ 100</text>
        </view>
        <view class="delta-row">
          <text class="delta">较昨日提升 {{ dashboard.scoreDelta ?? '--' }}%</text>
        </view>
      </view>

      <view class="metrics">
        <view class="metric u-card metric-steps" @click="goStepsDetail">
          <view class="metric-top">
            <view class="metric-ico bg-orange">步</view>
            <text class="metric-label">步数</text>
          </view>
          <text class="metric-val">{{ formatNumber(dashboard.steps) }}</text>
        </view>

        <view class="metric u-card metric-heart" @click="goHeart">
          <view class="metric-top">
            <view class="metric-ico bg-pink">
              <image class="heart-icon" src="/static/icons/heart_pink.svg" mode="aspectFit" />
            </view>
            <text class="metric-label">心率</text>
            <text class="metric-date">{{ dashboard.heartDate || '--' }}</text>
          </view>
          <text class="metric-val">{{ formatNumber(dashboard.heartRate) }}</text>
        </view>
      </view>

      <view class="section">
        <view class="section-head">
          <text class="section-title">健康小贴士</text>
          <text class="section-more" @click.stop="goKnowledge">查看全部 ></text>
        </view>

        <view v-if="tipsLoading" class="tip-empty u-card">
          正在获取健康小贴士...
        </view>
        <view v-else-if="tipsError" class="tip-empty u-card">
          健康小贴士暂不可用，稍后再试
        </view>
        <view v-else-if="!tips.length" class="tip-empty u-card">
          暂无推荐内容，稍后再试
        </view>

        <view v-for="item in tips" :key="item.id" class="tip u-card" @click="goArticle(item)">
          <view class="tip-ico" :class="tipClass(item.themeClass)">阅</view>
          <view class="tip-body">
            <text class="tip-cat">{{ item.category }}</text>
            <text class="tip-title">{{ item.title }}</text>
          </view>
        </view>
      </view>
    </template>
  </AppShell>
</template>

<script setup>
import { onHide, onShow } from '@dcloudio/uni-app'
import { reactive, ref } from 'vue'
import AppShell from '@/components/app/AppShell.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { fetchDashboardData, fetchRandomKnowledgeTips, logUserAction } from '@/services/healthApi.js'
import { loadHealthScoreDetail } from '@/services/healthScoreComposer.js'
import { initHeartMonitor, subscribeHeart } from '@/services/heartMonitor.js'
import { startStepCounter, subscribeSteps } from '@/services/stepCounter.js'
import { openDetailPage, openMainPage } from '@/utils/router.js'
import { getCurrentUser } from '@/utils/auth.js'

const pageError = ref('')
const user = reactive({ name: '', id: '', status: '' })
const dashboard = reactive({ score: null, scoreDelta: null, steps: null, heartRate: null, heartDate: '' })
const tips = reactive([])
const tipsLoading = ref(false)
const tipsError = ref('')
let unsubscribeSteps = null
let unsubscribeHeart = null

function syncTips(items) {
  tips.splice(0, tips.length, ...items)
}

async function loadRandomTips() {
  tipsLoading.value = true
  tipsError.value = ''
  try {
    const list = await fetchRandomKnowledgeTips(3)
    syncTips(list)
  } catch (error) {
    tipsError.value = error?.message || '健康小贴士暂不可用'
    syncTips([])
  } finally {
    tipsLoading.value = false
  }
}

async function loadData() {
  try {
    pageError.value = ''
    const data = await fetchDashboardData().catch((error) => {
      return {}
    })

    const cachedUser = getCurrentUser() || {}
    const serverUser = data.user || {}
    const finalUser = { ...cachedUser, ...serverUser }
    user.name = finalUser.nickname || finalUser.name || '用户'
    user.id = finalUser.username ? `账号 ${finalUser.username}` : (finalUser.phone || '')
    user.status = finalUser.statusText || (finalUser.status === 1 ? '正常' : finalUser.status) || '正常'

    const scoreDetail = await loadHealthScoreDetail().catch(() => null)
    dashboard.score = scoreDetail?.totalScore ?? data.dashboard?.score ?? null
    dashboard.scoreDelta = data.dashboard?.scoreDelta ?? null
    dashboard.steps = null
    const latestHeartRate = Number(data.dashboard?.heartRate)
    const hasHeart = Number.isFinite(latestHeartRate) && latestHeartRate >= 30 && latestHeartRate <= 220
    dashboard.heartRate = hasHeart ? latestHeartRate : null
    dashboard.heartDate = hasHeart ? (data.dashboard?.heartDate || '') : ''

    await startStepCounter(0)
    const heart = initHeartMonitor({
      currentBpm: hasHeart ? latestHeartRate : null,
      history: hasHeart
        ? [{ bpm: latestHeartRate, heartRate: latestHeartRate, time: data.dashboard?.heartDate ? `2026-${data.dashboard.heartDate.replace('/', '-')} 09:30` : '', recordTime: data.dashboard?.heartDate || '' }]
        : []
    })
    dashboard.heartRate = heart.currentBpm ?? null
    dashboard.heartDate = heart.currentBpm ? (heart.history[0]?.time?.slice(5, 10).replace('-', '/') || dashboard.heartDate) : ''
    bindSteps()
    bindHeart()
    await loadRandomTips()
  } catch (error) {
    pageError.value = error.message || '加载失败'
    syncTips([])
    user.name = ''
    user.id = ''
    user.status = ''
    dashboard.score = null
    dashboard.scoreDelta = null
    dashboard.steps = null
    dashboard.heartRate = null
    dashboard.heartDate = ''
    if (unsubscribeSteps) {
      unsubscribeSteps()
      unsubscribeSteps = null
    }
    if (unsubscribeHeart) {
      unsubscribeHeart()
      unsubscribeHeart = null
    }
  }
}

function bindSteps() {
  if (unsubscribeSteps) return
  unsubscribeSteps = subscribeSteps((snapshot) => {
    dashboard.steps = snapshot.steps
  })
}

function bindHeart() {
  if (unsubscribeHeart) return
  unsubscribeHeart = subscribeHeart((snapshot) => {
    const bpm = Number(snapshot.currentBpm)
    const hasHeart = Number.isFinite(bpm) && bpm >= 30 && bpm <= 220
    dashboard.heartRate = hasHeart ? bpm : null
    dashboard.heartDate = hasHeart ? (snapshot.history[0]?.time?.slice(5, 10).replace('-', '/') || '') : ''
  })
}

onShow(() => {
  logUserAction('view_dashboard')
  loadData()
})

onHide(() => {
  if (unsubscribeSteps) {
    unsubscribeSteps()
    unsubscribeSteps = null
  }
  if (unsubscribeHeart) {
    unsubscribeHeart()
    unsubscribeHeart = null
  }
})

function formatNumber(value) {
  if (value === null || value === undefined || value === '') return '--'
  return Number(value).toLocaleString()
}

function tipClass(theme) {
  return {
    pink: 'bg-soft-pink',
    blue: 'bg-soft-blue',
    indigo: 'bg-soft-indigo',
    green: 'bg-soft-green',
    amber: 'bg-soft-amber'
  }[theme] || 'bg-soft-pink'
}

function goScoreDetail() {
  logUserAction('open_score_detail')
  openDetailPage('/pages/dashboard/healthScoreDetail')
}

function goStepsDetail() {
  logUserAction('open_steps_detail')
  openDetailPage('/pages/dashboard/stepsDetail')
}

function goHeart() {
  logUserAction('open_heart_page')
  openDetailPage('/pages/heart/index')
}

function goKnowledge() {
  logUserAction('open_knowledge_from_dashboard')
  openMainPage('knowledge')
}

function goArticle(item) {
  if (!item?.url) return
  logUserAction('open_article_from_dashboard', { id: item.id, url: item.url, title: item.title })
  const params = [
    `externalUrl=${encodeURIComponent(item.url)}`,
    `title=${encodeURIComponent(item.title || '')}`,
    `summary=${encodeURIComponent(item.summary || '')}`,
    `source=${encodeURIComponent(item.source || '')}`,
    `publishTime=${encodeURIComponent(item.publishTime || '')}`
  ].join('&')
  openDetailPage(`/pages/knowledge/detail?${params}`)
}
</script>

<style scoped>
.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 28rpx;
}
.h1 {
  display: block;
  font-size: 46rpx;
  font-weight: 800;
  color: var(--c-title);
}
.sub {
  display: block;
  margin-top: 10rpx;
  font-size: 26rpx;
  color: var(--c-muted);
}
.score-card {
  border-radius: 52rpx;
  padding: 34rpx;
  color: #fff;
  background: linear-gradient(135deg, #0b1f2a, #0f3a44);
  box-shadow: 0 36rpx 80rpx rgba(15, 23, 42, 0.18);
}
.score-title {
  display: block;
  font-size: 26rpx;
  opacity: 0.9;
}
.score-row {
  display: flex;
  align-items: flex-end;
  gap: 16rpx;
  margin-top: 10rpx;
}
.score {
  font-size: 92rpx;
  font-weight: 900;
  line-height: 1;
}
.score-max {
  font-size: 28rpx;
  opacity: 0.55;
  padding-bottom: 10rpx;
}
.delta-row {
  margin-top: 16rpx;
}
.delta {
  font-size: 26rpx;
  color: #26e39d;
  font-weight: 600;
}
.metrics {
  margin-top: 34rpx;
  display: flex;
  gap: 22rpx;
}
.metric {
  flex: 1;
  padding: 26rpx 26rpx 28rpx;
  border-radius: 44rpx;
}
.metric-steps {
  background: rgba(255, 122, 47, 0.06);
  box-shadow: none;
}
.metric-heart {
  background: rgba(255, 59, 107, 0.06);
  box-shadow: none;
}
.metric-top {
  display: flex;
  align-items: center;
  gap: 14rpx;
}
.metric-ico {
  width: 64rpx;
  height: 64rpx;
  border-radius: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
}
.heart-icon {
  width: 36rpx;
  height: 36rpx;
}
.metric-label {
  font-size: 26rpx;
  color: var(--c-text);
  font-weight: 700;
}
.metric-date {
  margin-left: auto;
  font-size: 22rpx;
  color: var(--c-muted);
  font-weight: 600;
}
.metric-val {
  display: block;
  margin-top: 18rpx;
  font-size: 52rpx;
  font-weight: 900;
  color: var(--c-title);
}
.bg-orange {
  background: rgba(255, 122, 47, 0.14);
}
.bg-pink {
  background: rgba(255, 59, 107, 0.14);
}
.section {
  margin-top: 34rpx;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
}
.section-title {
  font-size: 40rpx;
  font-weight: 900;
  color: var(--c-title);
}
.section-more {
  font-size: 26rpx;
  color: var(--c-primary);
  font-weight: 800;
}
.tip-empty {
  border-radius: 36rpx;
  padding: 26rpx 28rpx;
  color: var(--c-muted);
  font-size: 26rpx;
  line-height: 1.65;
  font-weight: 800;
}
.tip {
  border-radius: 44rpx;
  padding: 24rpx;
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-bottom: 18rpx;
}
.tip-ico {
  width: 78rpx;
  height: 78rpx;
  border-radius: 30rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34rpx;
}
.tip-body {
  flex: 1;
}
.tip-cat {
  display: block;
  font-size: 22rpx;
  color: var(--c-muted);
  margin-bottom: 6rpx;
}
.tip-title {
  display: block;
  font-size: 30rpx;
  font-weight: 900;
  color: var(--c-title);
}
.bg-soft-pink { background: rgba(255, 59, 107, 0.12); }
.bg-soft-blue { background: rgba(79, 103, 255, 0.12); }
.bg-soft-indigo { background: rgba(92, 107, 192, 0.12); }
.bg-soft-green { background: rgba(18, 183, 106, 0.12); }
.bg-soft-amber { background: rgba(255, 122, 47, 0.12); }
</style>
