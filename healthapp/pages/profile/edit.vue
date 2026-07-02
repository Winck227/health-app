<template>
  <view class="page">
    <TopBar title="编辑资料" />
    <EmptyState v-if="pageError" title="加载失败" :message="pageError || '暂无数据'" />
    <template v-else>
      <text class="subhead">个人资料只写入 user 表；身高、体重请到健康档案页维护</text>
      <view class="u-card card avatar-card">
        <text class="label">头像</text>
        <view class="avatar-line">
          <view class="avatar-preview">
            <image v-if="avatarUrl" class="avatar-img" :src="avatarUrl" mode="aspectFill" @error="form.avatar = ''" />
            <text v-else>{{ avatarLetter }}</text>
          </view>
          <view class="avatar-actions">
            <view class="mini-btn" @click="chooseAvatarFile">从相册上传</view>
            <view v-if="form.avatar" class="mini-btn ghost" @click="removeAvatar">清除头像</view>
          </view>
        </view>
        <text class="tip">仅从本机相册选择图片，不调用拍照功能</text>
      </view>

      <view class="u-card card">
        <text class="label">昵称</text>
        <input v-model="form.nickname" class="input" placeholder="请输入昵称" />
        <text class="label mt">手机号</text>
        <input v-model="form.phone" class="input" type="number" placeholder="请输入手机号" />
        <text class="label mt">性别</text>
        <picker :range="genderOptions" range-key="label" :value="genderIndex" @change="onGenderChange"><view class="input picker">{{ genderLabel }}</view></picker>
        <text class="label mt">生日</text>
        <picker mode="date" :value="form.birthday" @change="onBirthdayChange"><view class="input picker">{{ form.birthday || '请选择生日' }}</view></picker>
		
      </view>
      <view class="btn" :class="{ disabled: !canSave }" @click="save">保存资料</view>
    </template>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import TopBar from '@/components/ui/TopBar.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { clearAvatar, fetchProfileData, logUserAction, updateProfile, uploadAvatar } from '@/services/healthApi.js'
import { showError, showSuccess } from '@/utils/feedback.js'
import { resolveMediaUrl } from '@/utils/media.js'

const pageError = ref('')
const form = reactive({ nickname: '', phone: '', gender: 'unknown', birthday: '', avatar: '' })
const genderOptions = [
  { label: '未知', value: 'unknown' },
  { label: '男', value: 'male' },
  { label: '女', value: 'female' }
]
const genderIndex = computed(() => Math.max(0, genderOptions.findIndex((item) => item.value === form.gender)))
const genderLabel = computed(() => genderOptions[genderIndex.value]?.label || '未知')
const canSave = computed(() => Boolean(form.nickname || form.phone || form.avatar))
const avatarUrl = computed(() => resolveMediaUrl(form.avatar))
const avatarLetter = computed(() => String(form.nickname || form.phone || '用').slice(0, 1))

async function load() {
  try {
    pageError.value = ''
    const data = await fetchProfileData()
    form.nickname = data?.nickname || data?.name || data?.username || ''
    form.phone = data?.phone || ''
    form.gender = data?.gender || 'unknown'
    form.birthday = data?.birthday || ''
    form.avatar = data?.avatar || ''
  } catch (error) { pageError.value = error.message || '加载失败' }
}

onShow(() => { logUserAction('view_profile_edit'); load() })
function onGenderChange(event) { form.gender = genderOptions[Number(event.detail.value)]?.value || 'unknown' }
function onBirthdayChange(event) { form.birthday = event.detail.value }

function chooseAvatarFile() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album'],
    async success(res) {
      const filePath = Array.isArray(res?.tempFilePaths) ? res.tempFilePaths[0] : ''
      if (!filePath) {
        showError(new Error('未获取到图片文件'), '上传失败')
        return
      }
      uni.showLoading({ title: '上传中' })
      try {
        const data = await uploadAvatar(filePath)
        form.avatar = data?.avatar || data?.avatarUrl || ''
        showSuccess('头像已上传')
      } catch (error) {
        showError(error, '上传失败')
      } finally {
        uni.hideLoading()
      }
    },
    fail(error) {
      if (!String(error?.errMsg || '').includes('cancel')) showError(error, '选择图片失败')
    }
  })
}

async function removeAvatar() {
  try {
    await clearAvatar()
    form.avatar = ''
    showSuccess('头像已清除')
  } catch (error) {
    showError(error, '清除失败')
  }
}

async function save() {
  if (!canSave.value) return
  try { await updateProfile({ ...form }); showSuccess('已保存'); setTimeout(() => uni.navigateBack(), 500) } catch (error) { showError(error, '保存失败') }
}
</script>

<style scoped>
.page { padding: 20rpx 36rpx 40rpx; box-sizing: border-box; }
.subhead { display:block; margin-top:6rpx; font-size:24rpx; color:var(--c-muted); line-height:1.6; }
.card { margin-top:22rpx; padding:30rpx; border-radius:52rpx; }
.label { display:block; font-size:24rpx; color:var(--c-muted); font-weight:900; }
.mt { margin-top:22rpx; }
.input { margin-top:10rpx; height:78rpx; border-radius:24rpx; background:#F8FAFC; padding:0 20rpx; font-size:28rpx; font-weight:800; color:var(--c-title); }
.picker { display:flex; align-items:center; }
.btn { margin-top:28rpx; height:84rpx; border-radius:9999rpx; background:var(--c-primary); color:#fff; display:flex; align-items:center; justify-content:center; font-size:28rpx; font-weight:1000; }
.btn.disabled { opacity:.45; }

.avatar-card { display: block; }
.avatar-line { margin-top: 18rpx; display: flex; align-items: center; gap: 24rpx; }
.avatar-preview { width: 132rpx; height: 132rpx; border-radius: 44rpx; overflow: hidden; background: var(--c-primary); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 56rpx; font-weight: 900; }
.avatar-img { width: 100%; height: 100%; display: block; }
.avatar-actions { flex: 1; display: flex; flex-direction: column; gap: 14rpx; }
.mini-btn { height: 58rpx; border-radius: 9999rpx; background: rgba(18, 183, 106, 0.12); color: var(--c-primary); display: flex; align-items: center; justify-content: center; font-size: 24rpx; font-weight: 900; }
.mini-btn.ghost { background: #F1F5F9; color: #64748B; }
.tip { display: block; margin-top: 16rpx; font-size: 22rpx; color: var(--c-muted); font-weight: 700; }
</style>
