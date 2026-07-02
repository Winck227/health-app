<template>
  <view class="page">
    <TopBar title="隐私设置" />
    <EmptyState v-if="pageError" title="加载失败" :message="pageError || '暂无数据'" />
    <template v-else>
      <text class="subhead">隐私等级写入 user_settings.privacy_level</text>
      <view class="u-card card">
        <view v-for="item in options" :key="item.key" class="option" :class="{ active: form.privacyLevel === item.key }" @click="form.privacyLevel = item.key">
          <view><text class="name">{{ item.name }}</text><text class="desc">{{ item.desc }}</text></view>
          <text class="check">{{ form.privacyLevel === item.key ? '已选' : '' }}</text>
        </view>
      </view>
      <view class="btn" @click="save">保存隐私等级</view>
    </template>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import TopBar from '@/components/ui/TopBar.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { fetchPrivacy, logUserAction, updatePrivacy } from '@/services/healthApi.js'

const pageError = ref('')
const form = reactive({ privacyLevel: 'partial' })
const options = [
  { key: 'public', name: '公开', desc: '允许展示基础健康数据' },
  { key: 'partial', name: '部分公开', desc: '仅公开昵称等基础资料' },
  { key: 'private', name: '仅自己可见', desc: '健康数据不对外展示' }
]

async function load() { try { pageError.value = ''; Object.assign(form, await fetchPrivacy()) } catch (error) { pageError.value = error.message || '加载失败' } }
onShow(() => { logUserAction('view_privacy_page'); load() })
async function save() { try { await updatePrivacy({ privacyLevel: form.privacyLevel }); uni.showToast({ title: '已保存', icon: 'success' }) } catch (error) { uni.showToast({ title: error.message || '保存失败', icon: 'none' }) } }
</script>

<style scoped>
.page { padding: 20rpx 36rpx 40rpx; box-sizing: border-box; }
.subhead { display:block; margin-top: 6rpx; font-size:24rpx; color:var(--c-muted); line-height:1.6; }
.card { margin-top:22rpx; padding: 18rpx; border-radius:52rpx; }
.option { padding: 26rpx; border-radius: 34rpx; display:flex; justify-content:space-between; gap:18rpx; }
.option.active { background: rgba(18,183,106,.12); }
.name { display:block; font-size:30rpx; font-weight:1000; color:var(--c-title); }
.desc { display:block; margin-top:8rpx; font-size:24rpx; color:var(--c-muted); line-height:1.6; }
.check { color:var(--c-primary); font-weight:1000; font-size:24rpx; }
.btn { margin-top:28rpx; height:84rpx; border-radius:9999rpx; background:var(--c-primary); color:#fff; display:flex; align-items:center; justify-content:center; font-size:28rpx; font-weight:1000; }
</style>
