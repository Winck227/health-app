<template>
  <view class="shell" :style="{ paddingTop: `${safeTop}px` }">
    <NavRail class="rail" :active="active" @navigate="onNavigate" />
    <scroll-view scroll-y class="main" :show-scrollbar="false" lower-threshold="160" @scrolltolower="emitReachBottom">
      <slot />
      <view class="bottom-spacer" />
    </scroll-view>
  </view>
</template>

<script setup>
import { computed, watchEffect } from 'vue'
import NavRail from './NavRail.vue'
import { openMainPage, rememberMainPage } from '@/utils/router.js'

const props = defineProps({
  active: { type: String, default: 'dashboard' }
})

const emit = defineEmits(['reach-bottom'])

const safeTop = computed(() => {
  try {
    return Number(uni.getSystemInfoSync().statusBarHeight || 0)
  } catch (error) {
    return 0
  }
})

watchEffect(() => {
  rememberMainPage(props.active)
})

function onNavigate(key) {
  if (key === props.active) return
  openMainPage(key)
}

function emitReachBottom() {
  emit('reach-bottom')
}
</script>

<style scoped>
.shell {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  background: var(--c-bg);
  box-sizing: border-box;
}

.rail {
  width: 120rpx;
  flex: 0 0 120rpx;
}

.main {
  flex: 1;
  padding: 48rpx 40rpx 40rpx;
  box-sizing: border-box;
}

.bottom-spacer {
  height: 40rpx;
}
</style>
