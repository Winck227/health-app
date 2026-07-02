<template>
  <view class="page">
    <view class="panel u-card">
      <view class="logo-wrap">
        <image class="logo" src="/static/icons/pulse_white.svg" mode="aspectFit" />
      </view>
      <text class="title">健康管理</text>
      <text class="sub">先登录，或先注册后进入主页</text>

      <view class="tabs">
        <view class="tab" :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</view>
        <view class="tab" :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</view>
      </view>

      <template v-if="mode === 'login'">
        <view class="field">
          <text class="label">账号</text>
          <input v-model="loginForm.username" class="input" placeholder="输入学号或用户名" />
        </view>
        <view class="field">
          <text class="label">密码</text>
          <input v-model="loginForm.password" class="input" password placeholder="输入密码" />
        </view>
        <view class="btn" :class="{ disabled: loading || !canLogin }" @click="submitLogin">
          {{ loading ? '登录中...' : '登录进入主页' }}
        </view>
      </template>

      <template v-else>
        <view class="field">
          <text class="label">用户名</text>
          <input v-model="registerForm.username" class="input" placeholder="建议输入学号" />
        </view>
        <view class="field">
          <text class="label">姓名</text>
          <input v-model="registerForm.name" class="input" placeholder="你的姓名" />
        </view>
        <view class="field">
          <text class="label">密码</text>
          <input v-model="registerForm.password" class="input" password placeholder="设置密码" />
        </view>
        <view class="field">
          <text class="label">确认密码</text>
          <input v-model="registerForm.confirmPassword" class="input" password placeholder="再次输入密码" />
        </view>
        <view class="btn" :class="{ disabled: loading || !canRegister }" @click="submitRegister">
          {{ loading ? '注册中...' : '注册并进入主页' }}
        </view>
      </template>
    </view>
  </view>
</template>

<script setup>
// 功能注释：页面数据来自接口和本地状态。
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchSession, logUserAction, login, register } from '@/services/healthApi.js'
import { hasAuthToken } from '@/utils/auth.js'
import { getLastMainPageRoute } from '@/utils/router.js'

const mode = ref('login')
const loading = ref(false)
const loginForm = reactive({
  username: '',
  password: ''
})
const registerForm = reactive({
  username: '',
  name: '',
  password: '',
  confirmPassword: ''
})

const canLogin = computed(() => Boolean(loginForm.username.trim()) && Boolean(loginForm.password.trim()))
const canRegister = computed(() => Boolean(registerForm.username.trim()) &&
  Boolean(registerForm.name.trim()) &&
  Boolean(registerForm.password.trim()) &&
  registerForm.password === registerForm.confirmPassword)

// 功能注释：进入页面时自动恢复已有会话。
onShow(async () => {
  if (!hasAuthToken()) return
  try {
    await fetchSession()
    uni.reLaunch({ url: getLastMainPageRoute() })
  } catch (error) {}
})

// 功能注释：提交登录表单。
async function submitLogin() {
  if (!canLogin.value || loading.value) return
  loading.value = true
  try {
    await login({
      username: loginForm.username.trim(),
      password: loginForm.password.trim()
    })
    logUserAction('login_success')
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      uni.reLaunch({ url: getLastMainPageRoute() })
    }, 300)
  } catch (error) {
    uni.showToast({ title: error.message || '登录失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}	

// 功能注释：提交注册表单。
async function submitRegister() {
  if (!canRegister.value || loading.value) return
  loading.value = true
  try {
    await register({
      username: registerForm.username.trim(),
      name: registerForm.name.trim(),
      password: registerForm.password.trim()
    })
    logUserAction('register_success')
    uni.showToast({ title: '注册成功', icon: 'success' })
    setTimeout(() => {
      uni.reLaunch({ url: getLastMainPageRoute() })
    }, 300)
  } catch (error) {
    uni.showToast({ title: error.message || '注册失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 60rpx 36rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at 20% 15%, rgba(18, 183, 106, 0.12), transparent 30%),
    radial-gradient(circle at 82% 18%, rgba(79, 103, 255, 0.08), transparent 26%),
    #F6F8FB;
}
.panel {
  width: 100%;
  border-radius: 56rpx;
  padding: 44rpx 34rpx;
  box-sizing: border-box;
}
.logo-wrap {
  width: 108rpx;
  height: 108rpx;
  margin: 0 auto;
  border-radius: 9999rpx;
  background: var(--c-primary);
  box-shadow: 0 28rpx 70rpx rgba(18, 183, 106, 0.24);
  display: flex;
  align-items: center;
  justify-content: center;
}
.logo {
  width: 50rpx;
  height: 50rpx;
}
.title {
  display: block;
  margin-top: 28rpx;
  text-align: center;
  font-size: 48rpx;
  font-weight: 900;
  color: var(--c-title);
}
.sub {
  display: block;
  margin-top: 12rpx;
  text-align: center;
  font-size: 26rpx;
  color: var(--c-muted);
}
.tabs {
  margin-top: 28rpx;
  display: flex;
  gap: 16rpx;
}
.tab {
  flex: 1;
  height: 82rpx;
  border-radius: 9999rpx;
  background: #F8FAFC;
  color: var(--c-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  font-weight: 900;
}
.tab.active {
  background: var(--c-primary);
  color: #fff;
}
.field {
  margin-top: 24rpx;
}
.label {
  display: block;
  margin-bottom: 12rpx;
  font-size: 24rpx;
  color: var(--c-muted);
  font-weight: 800;
}
.input {
  height: 92rpx;
  border-radius: 28rpx;
  background: #F8FAFC;
  padding: 0 24rpx;
  box-sizing: border-box;
  font-size: 30rpx;
  color: var(--c-title);
  font-weight: 800;
}
.tip {
  margin-top: 22rpx;
  padding: 20rpx 24rpx;
  border-radius: 28rpx;
  background: rgba(148, 163, 184, 0.08);
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  font-size: 22rpx;
  color: var(--c-muted);
  font-weight: 700;
}
.btn {
  margin-top: 34rpx;
  height: 100rpx;
  border-radius: 9999rpx;
  background: linear-gradient(135deg, #0b1f2a, #0f3a44);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  font-weight: 900;
  box-shadow: var(--shadow-btn);
}
.btn.disabled {
  opacity: 0.7;
}
</style>

