<template>
  <view class="page">
    <TopBar title="健康指数详情" />
    <text class="subhead">健康指数由数据库记录与本地步数综合计算，不单独建表</text>

    <view class="hero">
      <text class="hero-label">当前健康指数</text>
      <text class="hero-score">{{ score.totalScore ?? '--' }}</text>
      <text class="hero-state">{{ score.statusText || '数据不足' }}</text>
      <text class="hero-summary">{{ score.summary }}</text>
    </view>

    <text class="sec-title">评分拆解</text>
    <view class="u-card sec">
      <view v-for="item in score.dimensions" :key="item.key" class="dim">
        <view class="dim-top">
          <view>
            <text class="dim-name">{{ item.name }}</text>
            <text class="dim-desc">{{ item.desc }}</text>
          </view>
          <text class="dim-val">{{ item.score }}/{{ item.max }}</text>
        </view>
        <view class="track"><view class="fill" :class="item.status" :style="{ width: item.value + '%' }" /></view>
      </view>
    </view>

    <view class="u-card sec" v-if="score.suggestions.length">
      <text class="card-title">今日建议</text>
      <view v-for="item in score.suggestions" :key="item" class="advice">
        <text class="dot">•</text><text class="advice-text">{{ item }}</text>
      </view>
    </view>

    <view class="u-card sec" v-if="score.history.length">
      <text class="card-title">最近本地评分</text>
      <view class="history-row">
        <view v-for="item in score.history" :key="item.date" class="history-item">
          <text class="history-score">{{ item.score }}</text>
          <text class="history-date">{{ item.date.slice(5) }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import TopBar from '@/components/ui/TopBar.vue'
import { logUserAction } from '@/services/healthApi.js'
import { loadHealthScoreDetail } from '@/services/healthScoreComposer.js'

function emptyScore() {
  return { totalScore: null, statusText: '数据不足', summary: '正在读取可用数据', dimensions: [], suggestions: [], missing: [], history: [] }
}

const score = reactive(emptyScore())

async function loadData() {
  try {
    Object.assign(score, await loadHealthScoreDetail())
  } catch (error) {
    Object.assign(score, emptyScore(), { summary: error.message || '健康指数暂不可用' })
  }
}

onShow(() => {
  logUserAction('view_score_detail')
  loadData()
})
</script>

<style scoped>
.page { padding: 20rpx 36rpx 40rpx; box-sizing: border-box; background: radial-gradient(circle at 20% 0%, rgba(18, 183, 106, 0.08), transparent 28%), #F6F8FB; }
.subhead { display: block; margin-top: 6rpx; font-size: 24rpx; color: var(--c-muted); line-height: 1.6; }
.hero { margin-top: 18rpx; border-radius: 56rpx; padding: 46rpx 36rpx; background: linear-gradient(135deg, #0b1f2a, #0f3a44); box-shadow: 0 42rpx 90rpx rgba(15, 23, 42, 0.20); color: #fff; text-align: center; }
.hero-label { display: block; font-size: 24rpx; opacity: 0.85; font-weight: 800; }
.hero-score { display: block; font-size: 120rpx; font-weight: 1000; line-height: 1; margin-top: 10rpx; }
.hero-state { display: block; margin-top: 18rpx; font-size: 30rpx; font-weight: 900; color: #26e39d; }
.hero-summary { display: block; margin-top: 14rpx; font-size: 24rpx; line-height: 1.6; opacity: .88; }
.sec-title { display: block; margin-top: 30rpx; font-size: 34rpx; font-weight: 1000; color: var(--c-title); }
.sec { margin-top: 18rpx; border-radius: 52rpx; padding: 30rpx 26rpx; }
.card-title { display: block; font-size: 30rpx; font-weight: 1000; color: var(--c-title); margin-bottom: 18rpx; }
.dim { margin-bottom: 26rpx; }
.dim:last-child { margin-bottom: 0; }
.dim-top { display: flex; align-items: flex-start; justify-content: space-between; gap: 18rpx; margin-bottom: 12rpx; }
.dim-name, .dim-val { font-size: 28rpx; font-weight: 900; color: var(--c-title); }
.dim-desc { display: block; margin-top: 6rpx; font-size: 22rpx; color: var(--c-muted); font-weight: 700; }
.track { height: 18rpx; border-radius: 9999rpx; background: #EEF2F7; overflow: hidden; }
.fill { height: 100%; border-radius: 9999rpx; background: var(--c-primary); }
.fill.empty { background: #CBD5E1; }
.fill.warn { background: #FF3D3D; }
.fill.normal { background: var(--c-orange); }
.fill.good { background: var(--c-primary); }
.advice { display: flex; gap: 10rpx; margin-bottom: 14rpx; }
.dot { color: var(--c-primary); font-weight: 1000; }
.advice-text { flex: 1; font-size: 26rpx; color: var(--c-text); line-height: 1.6; font-weight: 700; }
.history-row { display: flex; gap: 14rpx; justify-content: space-between; }
.history-item { flex: 1; padding: 18rpx 8rpx; border-radius: 24rpx; background: #F8FAFC; text-align: center; }
.history-score { display: block; font-size: 30rpx; font-weight: 1000; color: var(--c-title); }
.history-date { display: block; margin-top: 4rpx; font-size: 20rpx; color: var(--c-muted); }
</style>
