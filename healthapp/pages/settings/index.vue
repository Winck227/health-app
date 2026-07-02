<template>
  <AppShell active="settings">
    <view class="head">
      <text class="h1">设置</text>
      <text class="sub">管理你的账号与应用偏好</text>
    </view>

    <text class="group-title">通用设置</text>
    <view class="u-card group">
      <view class="row" @click="openPage('/pages/profile/edit', 'open_profile_edit_from_settings')">
        <view class="ico bg-soft">
          <image class="ico-img" src="/static/icons/user_slate.svg" mode="aspectFit" />
        </view>
        <text class="txt">个人资料</text>
        <text class="hint">{{ profileName }}</text>
        <text class="chev">></text>
      </view>
      <view class="divider" />
      <view class="row" @click="openPage('/pages/settings/goals', 'open_goal_settings')">
        <view class="ico bg-soft">
          <image class="ico-img" src="/static/icons/target_slate.svg" mode="aspectFit" />
        </view>
        <text class="txt">目标设定</text>
        <text class="hint">{{ settings.dailyGoal == null ? '--' : `${settings.dailyGoal} 步 / 天` }}</text>
        <text class="chev">></text>
      </view>
    </view>

    <text class="group-title">应用偏好</text>
    <view class="u-card group">
      <view class="row" @click="openPage('/pages/settings/privacy', 'open_privacy_settings')">
        <view class="ico bg-soft">
          <image class="ico-img" src="/static/icons/info_slate.svg" mode="aspectFit" />
        </view>
        <text class="txt">隐私设置</text>
        <text class="hint">{{ settings.privacyLevel || '--' }}</text>
        <text class="chev">></text>
      </view>
      <view class="divider" />
      <view class="row" @click="openPage('/pages/settings/password', 'open_password_settings')">
        <view class="ico bg-soft">
          <image class="ico-img" src="/static/icons/settings_grey.svg" mode="aspectFit" />
        </view>
        <text class="txt">修改密码</text>
        <text class="hint">更新登录密码</text>
        <text class="chev">></text>
      </view>
    </view>

    <text class="group-title">其他</text>
    <view class="u-card group">
      <view class="row" @click="handleLogout">
        <view class="ico logout-ico">
          <image class="ico-img" src="/static/icons/logout_red.svg" mode="aspectFit" />
        </view>
        <text class="txt logout">退出登录</text>
        <view class="spacer" />
        <text class="chev">></text>
      </view>
    </view>
  </AppShell>
</template>

<script setup>
// 功能注释：页面数据来自接口和本地状态。
import { computed, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppShell from '@/components/app/AppShell.vue'
import { fetchProfileData, fetchSettingsData, logUserAction, logout } from '@/services/healthApi.js'
import { openDetailPage } from '@/utils/router.js'

const settings = reactive({
  dailyGoal: null,
  privacyLevel: ''
})

const profile = reactive({ name: '' })
const profileName = computed(() => profile.name || '未登录')

// 功能注释：加载设置页所需数据。
async function loadData() {
  try {
    const data = await fetchSettingsData()
    settings.dailyGoal = data?.stepGoal ?? data?.step_goal ?? data?.dailyGoal ?? null
    settings.privacyLevel = data?.privacyLevel || data?.privacy_level || ''
  } catch (error) {
    settings.dailyGoal = null
    settings.privacyLevel = ''
  }
  try {
    const data = await fetchProfileData()
    profile.name = data?.nickname || data?.name || data?.username || data?.phone || ''
  } catch (error) {
    profile.name = ''
  }
}

onShow(() => {
  logUserAction('view_settings_page')
  loadData()
})

// 功能注释：打开设置子页面。
function openPage(url, logKey) {
  logUserAction(logKey)
  openDetailPage(url)
}

// 功能注释：退出登录并回到登录页。
async function handleLogout() {
  logUserAction('logout_click')
  await logout()
  uni.reLaunch({ url: '/pages/auth/login' })
}
</script>

<style scoped>
.head {
  margin-bottom: 18rpx;
  padding-top: 14rpx;
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

.group-title {
  display: block;
  margin: 22rpx 6rpx 14rpx;
  font-size: 22rpx;
  color: var(--c-muted);
  font-weight: 900;
}

.group {
  border-radius: 52rpx;
  overflow: hidden;
}

.row {
  padding: 24rpx 28rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.ico {
  width: 72rpx;
  height: 72rpx;
  border-radius: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 72rpx;
  background: rgba(148, 163, 184, 0.10);
}

.ico-img {
  width: 34rpx;
  height: 34rpx;
}

.txt {
  font-size: 30rpx;
  font-weight: 900;
  color: var(--c-title);
}

.hint {
  margin-left: auto;
  font-size: 24rpx;
  color: #94A3B8;
  font-weight: 800;
}

.chev {
  margin-left: 10rpx;
  font-size: 44rpx;
  color: #CBD5E1;
  font-weight: 900;
  line-height: 1;
}

.divider {
  height: 1px;
  background: var(--c-divider);
  margin: 0 28rpx;
}

.spacer {
  flex: 1;
}

.logout {
  color: #FF3D3D;
}

.logout-ico {
  background: rgba(255, 61, 61, 0.10);
}
</style>

