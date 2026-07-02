<template>
  <view class="nav">
    <view class="items">
      <view
        v-for="item in mainItems"
        :key="item.key"
        class="item"
        :class="{ active: active === item.key }"
        @click="emit('navigate', item.key)"
      >
        <view class="icon-wrap" :class="{ active: active === item.key }">
          <image class="icon" :src="iconFor(item)" mode="aspectFit" />
        </view>
      </view>
    </view>

    <view class="bottom">
      <view class="item" :class="{ active: active === 'settings' }" @click="emit('navigate', 'settings')">
        <view class="icon-wrap" :class="{ active: active === 'settings' }">
          <image
            class="icon"
            :src="active === 'settings' ? '/static/icons/settings_green.svg' : '/static/icons/settings_grey.svg'"
            mode="aspectFit"
          />
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
const props = defineProps({
  active: { type: String, default: 'dashboard' }
})

const emit = defineEmits(['navigate'])

const mainItems = [
  { key: 'dashboard', grey: '/static/icons/home_grey.svg', green: '/static/icons/home_green.svg' },
  { key: 'diet', grey: '/static/icons/diet_grey.svg', green: '/static/icons/diet_green.svg' },
  { key: 'plan', grey: '/static/icons/plan_grey.svg', green: '/static/icons/plan_green.svg' },
  { key: 'knowledge', grey: '/static/icons/knowledge_grey.svg', green: '/static/icons/knowledge_green.svg' },
  { key: 'profile', grey: '/static/icons/profile_grey.svg', green: '/static/icons/profile_green.svg' }
]

function iconFor(item) {
  return props.active === item.key ? item.green : item.grey
}
</script>

<style scoped>
.nav {
  height: 100%;
  padding: 28rpx 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
}


.items,
.bottom {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 22rpx;
}

.items {
  flex: 1;
  justify-content: center;
  padding: 18rpx 0;
  box-sizing: border-box;
}

.bottom {
  padding-bottom: 8rpx;
  box-sizing: border-box;
}

.item {
  width: 100%;
  display: flex;
  justify-content: center;
}

.icon-wrap {
  width: 72rpx;
  height: 72rpx;
  border-radius: 9999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-wrap.active {
  background: var(--c-primary-weak);
}

.icon {
  width: 40rpx;
  height: 40rpx;
}
</style>
