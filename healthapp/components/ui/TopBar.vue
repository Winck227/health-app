<template>
  <view class="wrap" :style="{ paddingTop: `${safeTop}px` }">
    <view class="bar">
      <view class="left" @click="onBack">
        <image class="icon" src="/static/icons/back_grey.svg" mode="aspectFit" />
      </view>
      <text class="title">{{ title }}</text>
      <view class="right">
        <slot name="right" />
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { safeBack } from '@/utils/router.js'

const props = defineProps({
  title: { type: String, default: '' },
  back: { type: Boolean, default: true }
})

const safeTop = computed(() => {
  try {
    return Number(uni.getSystemInfoSync().statusBarHeight || 0)
  } catch (error) {
    return 0
  }
})

function onBack() {
  if (!props.back) return
  safeBack()
}
</script>

<style scoped>
.wrap {
  box-sizing: border-box;
}

.bar {
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10rpx;
}

.left,
.right {
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 9999rpx;
}

.left {
  background: rgba(148, 163, 184, 0.08);
}

.right {
  background: transparent;
}

.icon {
  width: 40rpx;
  height: 40rpx;
}

.title {
  flex: 1;
  text-align: center;
  font-size: 36rpx;
  font-weight: 900;
  letter-spacing: 1rpx;
  color: var(--c-title);
}
</style>
