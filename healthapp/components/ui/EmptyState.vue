<template>
  <view class="empty u-card">
    <text class="title">{{ title }}</text>
    <text class="sub">{{ message }}</text>
  </view>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: '暂无数据'
  },
  message: {
    type: String,
    default: '当前内容暂时无法显示'
  }
})
</script>

<style scoped>
.empty {
  border-radius: 52rpx;
  padding: 40rpx 30rpx;
  text-align: center;
}

.title {
  display: block;
  font-size: 34rpx;
  font-weight: 1000;
  color: var(--c-title);
}

.sub {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: var(--c-muted);
  line-height: 1.7;
}
</style>
