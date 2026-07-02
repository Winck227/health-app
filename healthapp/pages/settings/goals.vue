<template>
  <view class="page">
    <TopBar title="目标设定" />
    <EmptyState v-if="pageError" title="加载失败" :message="pageError || '暂无数据'" />
    <template v-else>
      <view class="u-card card">
        <text class="section-title">入库目标</text>
        <text class="label">每日步数目标</text>
        <view class="field"><input v-model="form.dailySteps" class="input" type="number" placeholder="9000" /></view>
        <text class="hint">该字段写入 user_settings.step_goal</text>
      </view>

      <view class="u-card card">
        <text class="section-title">本机扩展目标</text>
        <text class="label">目标体重（千克）</text>
        <view class="field"><input v-model="localForm.targetWeight" class="input" type="digit" placeholder="68" /></view>
        <text class="label mt">目标睡眠（小时）</text>
        <view class="field"><input v-model="localForm.sleepHours" class="input" type="digit" placeholder="8" /></view>
        <text class="label mt">每周运动天数</text>
        <view class="field"><input v-model="localForm.weeklyExerciseDays" class="input" type="number" placeholder="5" /></view>
        <text class="hint">这些目标仅保存在当前设备，不进入数据库</text>
      </view>

      <view class="btn" :class="{ disabled: !canSave }" @click="save">保存设置</view>
    </template>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import TopBar from '@/components/ui/TopBar.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { fetchGoals, logUserAction, updateGoals } from '@/services/healthApi.js'

const LOCAL_KEY = 'health-app-local-goals-v1'
const pageError = ref('')
const form = reactive({ dailySteps: '' })
const localForm = reactive({ targetWeight: '', sleepHours: '', weeklyExerciseDays: '' })
const canSave = computed(() => Number(form.dailySteps) > 0)

function readLocal() { try { return uni.getStorageSync(LOCAL_KEY) || {} } catch (error) { return {} } }
function saveLocal() { try { uni.setStorageSync(LOCAL_KEY, { ...localForm }) } catch (error) {} }

async function load() {
  try {
    pageError.value = ''
    const data = await fetchGoals()
    form.dailySteps = String(data?.dailySteps || data?.stepGoal || 9000)
    Object.assign(localForm, readLocal())
  } catch (error) { pageError.value = error.message || '加载失败' }
}

onShow(() => { logUserAction('view_goals_page'); load() })

async function save() {
  if (!canSave.value) return
  try {
    await updateGoals({ dailySteps: Number(form.dailySteps) })
    saveLocal()
    uni.showToast({ title: '已保存', icon: 'success' })
  } catch (error) { uni.showToast({ title: error.message || '保存失败', icon: 'none' }) }
}
</script>

<style scoped>
.page { padding: 20rpx 36rpx 40rpx; box-sizing: border-box; }
.card { margin-top: 22rpx; padding: 30rpx; border-radius: 52rpx; }
.section-title { display:block; font-size: 32rpx; font-weight: 1000; color: var(--c-title); margin-bottom: 20rpx; }
.label { display:block; font-size: 24rpx; color: var(--c-muted); font-weight: 900; }
.mt { margin-top: 20rpx; }
.field { margin-top: 10rpx; }
.input { height: 78rpx; border-radius: 24rpx; background: #F8FAFC; padding: 0 20rpx; font-size: 28rpx; font-weight: 800; }
.hint { display:block; margin-top: 14rpx; font-size: 22rpx; color: #CBD5E1; font-weight: 800; line-height:1.6; }
.btn { margin-top: 28rpx; height: 84rpx; border-radius: 9999rpx; background: var(--c-primary); color:#fff; display:flex; align-items:center; justify-content:center; font-size: 28rpx; font-weight:1000; }
.btn.disabled { opacity: .45; }
</style>
