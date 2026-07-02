<template>
  <AppShell active="profile">
    <view class="profile">
      <view class="avatar">
        <image v-if="avatarUrl" class="avatar-img" :src="avatarUrl" mode="aspectFill" @error="onAvatarError" />
        <text v-else>{{ user.avatarLetter }}</text>
      </view>
      <text class="name">{{ user.name || '未登录' }}</text>
      <text class="id">账号：{{ user.id || '--' }}</text>

      <view class="stats">
        <view class="stat"><text class="num">{{ stats.bmi ?? '--' }}</text><text class="lab">最近 BMI</text></view>
        <view class="stat"><text class="num">{{ stats.weight ?? '--' }}</text><text class="lab">最近体重</text></view>
        <view class="stat"><text class="num">{{ stats.heartRate ?? '--' }}</text><text class="lab">最近心率</text></view>
      </view>

      <view class="u-card menu">
        <view v-for="item in menuItems" :key="item.key" class="row" @click="openPage(item.url, item.logKey)">
          <view class="row-ico" :class="item.iconBg">
            <image class="row-icon-img" :src="item.icon" mode="aspectFit" />
          </view>
          <view class="row-main">
            <text class="row-txt">{{ item.label }}</text>
            <text v-if="item.desc" class="row-desc">{{ item.desc }}</text>
          </view>
          <text class="chev">></text>
        </view>
      </view>
    </view>
  </AppShell>
</template>

<script setup>
import { computed, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppShell from '@/components/app/AppShell.vue'
import { fetchHeartData, fetchHealthRecord, fetchProfileData, logUserAction } from '@/services/healthApi.js'
import { openDetailPage } from '@/utils/router.js'
import { resolveMediaUrl } from '@/utils/media.js'

const user = reactive({ avatar: '', avatarLetter: '用', name: '', id: '' })
const avatarUrl = computed(() => resolveMediaUrl(user.avatar))
const stats = reactive({ bmi: null, weight: null, heartRate: null })
const menuItems = [
  { key: 'edit', label: '编辑资料', desc: '昵称、手机号、性别、生日', url: '/pages/profile/edit', logKey: 'open_profile_edit_from_profile', icon: '/static/icons/user_slate.svg', iconBg: 'soft-slate' },
  { key: 'favorites', label: '我的收藏', desc: '查看收藏文章', url: '/pages/profile/favorites', logKey: 'open_favorites_from_profile', icon: '/static/icons/book_slate.svg', iconBg: 'soft-pink' },
  { key: 'health', label: '健康档案', desc: '身高、体重、BMI、BMR', url: '/pages/health/index', logKey: 'open_health_record_from_profile', icon: '/static/icons/target_slate.svg', iconBg: 'soft-green' }
]

function normalizeUser(data = {}) {
  const name = data.nickname || data.name || data.username || data.phone || '用户'
  user.name = name
  user.id = data.username || data.phone || data.id || ''
  user.avatar = data.avatar || ''
  user.avatarLetter = String(name || '用').slice(0, 1)
}

function onAvatarError() {
  user.avatar = ''
}

function latestFrom(data) {
  if (data?.latest) return data.latest
  if (data?.current) return data.current
  if (Array.isArray(data?.history)) return data.history[0]
  if (Array.isArray(data?.items)) return data.items[0]
  if (Array.isArray(data)) return data[0]
  return null
}

async function load() {
  try {
    normalizeUser(await fetchProfileData())
  } catch (error) {
    normalizeUser({})
  }
  try {
    const latest = latestFrom(await fetchHealthRecord())
    stats.bmi = latest?.bmi ?? null
    stats.weight = latest?.weight ?? null
  } catch (error) {
    stats.bmi = null
    stats.weight = null
  }
  try {
    const latest = latestFrom(await fetchHeartData())
    stats.heartRate = latest?.heartRate || latest?.heart_rate || latest?.bpm || null
  } catch (error) {
    stats.heartRate = null
  }
}

onShow(() => {
  logUserAction('view_profile_page')
  load()
})

function openPage(url, logKey) {
  logUserAction(logKey)
  openDetailPage(url)
}
</script>

<style scoped>
.profile { display: flex; flex-direction: column; align-items: center; padding-top: 14rpx; }
.avatar { width: 164rpx; height: 164rpx; border-radius: 52rpx; background: var(--c-primary); box-shadow: 0 30rpx 70rpx rgba(18, 183, 106, 0.24); display: flex; align-items: center; justify-content: center; font-size: 72rpx; font-weight: 900; color: #fff; margin-top: 16rpx; overflow: hidden; }
.avatar-img { width: 100%; height: 100%; display: block; }
.name { margin-top: 28rpx; font-size: 44rpx; font-weight: 900; color: var(--c-title); }
.id { margin-top: 10rpx; font-size: 26rpx; color: var(--c-muted); font-weight: 700; }
.stats { width: 100%; margin-top: 38rpx; display: flex; justify-content: space-between; padding: 0 10rpx; box-sizing: border-box; }
.stat { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 10rpx; }
.num { font-size: 44rpx; font-weight: 900; color: var(--c-title); }
.lab { font-size: 22rpx; color: var(--c-muted); font-weight: 800; }
.menu { width: 100%; margin-top: 34rpx; border-radius: 52rpx; overflow: hidden; box-shadow: var(--shadow-card); }
.row { padding: 26rpx 28rpx; display: flex; align-items: center; gap: 16rpx; border-bottom: 1px solid var(--c-divider); }
.row:last-child { border-bottom: none; }
.row-ico { width: 72rpx; height: 72rpx; border-radius: 28rpx; display: flex; align-items: center; justify-content: center; }
.row-icon-img { width: 34rpx; height: 34rpx; }
.row-main { flex: 1; }
.row-txt { display: block; font-size: 30rpx; color: var(--c-title); font-weight: 900; }
.row-desc { display: block; margin-top: 6rpx; font-size: 22rpx; color: var(--c-muted); font-weight: 800; }
.chev { color: #CBD5E1; font-size: 30rpx; font-weight: 900; }
.soft-slate { background: #F1F5F9; }
.soft-pink { background: rgba(255, 59, 107, 0.10); }
.soft-green { background: rgba(18, 183, 106, 0.10); }
</style>
