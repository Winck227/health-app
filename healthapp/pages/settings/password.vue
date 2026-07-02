<template>
  <view class="page">
    <TopBar title="修改密码" />
    <view class="u-card card">
      <text class="label">旧密码</text>
      <view class="field"><input v-model="form.oldPassword" class="input" password placeholder="输入旧密码" /></view>

      <text class="label mt">新密码</text>
      <view class="field"><input v-model="form.newPassword" class="input" password placeholder="请输入新密码" /></view>

      <text class="label mt">确认新密码</text>
      <view class="field"><input v-model="form.confirmPassword" class="input" password placeholder="再次输入新密码" /></view>

      <view class="btn" :class="{ disabled: !canSave }" @click="savePassword">确认修改</view>
    </view>
  </view>
</template>

<script setup>
// 功能注释：页面数据来自接口和本地状态。
import { computed, reactive } from 'vue'
import TopBar from '@/components/ui/TopBar.vue'
import { logUserAction, logout, updatePassword } from '@/services/healthApi.js'

const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const canSave = computed(() => Boolean(form.oldPassword.trim()) && Boolean(form.newPassword.trim()) && form.newPassword === form.confirmPassword)

// 功能注释：提交密码修改。
async function savePassword() {
  if (!canSave.value) return
  try {
    logUserAction('update_password_submit')
    await updatePassword({ oldPassword: form.oldPassword.trim(), newPassword: form.newPassword.trim() })
    uni.showToast({ title: '密码已更新，请重新登录', icon: 'none' })
    setTimeout(async () => {
      await logout()
      uni.reLaunch({ url: '/pages/auth/login' })
    }, 500)
  } catch (error) {
    uni.showToast({ title: error.message || '修改失败', icon: 'none' })
  }
}
</script>

<style scoped>
.page { padding:20rpx 36rpx 40rpx; box-sizing:border-box; }
.card { margin-top:12rpx; border-radius:52rpx; padding:30rpx; }
.label { display:block; font-size:22rpx; color:var(--c-muted); font-weight:800; margin-bottom:12rpx; }
.mt { margin-top:22rpx; }
.field { height:86rpx; border-radius:9999rpx; background:#F8FAFC; display:flex; align-items:center; padding:0 24rpx; }
.input { flex:1; height:86rpx; font-size:30rpx; color:var(--c-title); font-weight:900; }
.btn { margin-top:30rpx; height:96rpx; border-radius:9999rpx; background:linear-gradient(135deg,#0b1f2a,#0f3a44); display:flex; align-items:center; justify-content:center; color:#fff; font-size:30rpx; font-weight:1000; }
.btn.disabled { background:#8B919B; }
</style>

