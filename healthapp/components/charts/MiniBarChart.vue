<template>
  <view class="wrap" @tap.stop="handleChartTap" @click.stop="handleChartTap" @longpress.stop>
    <view v-if="normalizedBars.length" class="bars">
      <view
        v-for="(b, idx) in normalizedBars"
        :key="idx"
        class="bar-col"
        :class="{ selected: isSelected(idx, b) }"
        @tap.stop="emitSelect(b, idx)"
        @click.stop="emitSelect(b, idx)"
        @longpress.stop="emitSelect(b, idx)"
      >
        <view class="bar-hit" @tap.stop="emitSelect(b, idx)" @click.stop="emitSelect(b, idx)" />
        <view class="bar" :style="{ height: b.h + '%', background: b.c }" />
        <text class="label">{{ b.label }}</text>
      </view>
    </view>
    <view v-else class="empty-chart">
      <text class="empty-text">暂无可展示数据</text>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  bars: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['select'])
const selectedIndex = ref(-1)
let lastEmitAt = 0

const normalizedBars = computed(() => (Array.isArray(props.bars) ? props.bars : [])
  .map((item) => ({
    ...item,
    label: item?.label || '',
    value: Number(item?.value || 0),
    h: Math.max(0, Math.min(100, Number(item?.h || 0))),
    c: item?.c || 'rgba(18,183,106,0.36)',
    unit: item?.unit || '',
    time: item?.time || '',
    timeLabel: item?.timeLabel || item?.label || '',
    active: Boolean(item?.active)
  }))
  .filter((item) => item.label || item.value > 0 || item.h > 0))

function isSelected(index, item) {
  if (selectedIndex.value >= 0) return selectedIndex.value === index
  return Boolean(item?.active)
}

function emitSelect(item, index) {
  const now = Date.now()
  if (now - lastEmitAt < 80 && selectedIndex.value === index) return
  lastEmitAt = now
  selectedIndex.value = index
  emit('select', { ...item, index })
}

function handleChartTap() {
  // App 端有时只触发外层 tap，不触发柱子 click。这里兜底选中当前高亮项或最后一项。
  if (!normalizedBars.value.length) return
  const activeIndex = normalizedBars.value.findIndex((item) => item?.active)
  const fallbackIndex = selectedIndex.value >= 0 ? selectedIndex.value : (activeIndex >= 0 ? activeIndex : normalizedBars.value.length - 1)
  emitSelect(normalizedBars.value[fallbackIndex], fallbackIndex)
}
</script>

<style scoped>
.wrap {
  height: 220rpx;
}

.bars {
  height: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 14rpx;
  padding: 20rpx 8rpx 0;
  box-sizing: border-box;
}

.bar-col {
  flex: 1;
  position: relative;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  gap: 14rpx;
}

.bar-hit {
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 100%;
  min-width: 72rpx;
  height: 100%;
  transform: translateX(-50%);
  z-index: 3;
  background: transparent;
}

.bar {
  width: 100%;
  max-width: 52rpx;
  min-height: 4rpx;
  border-radius: 16rpx;
  box-shadow: 0 10rpx 18rpx rgba(15,23,42,0.08);
  position: relative;
  z-index: 1;
}

.bar-col.selected .bar {
  box-shadow: 0 0 0 8rpx rgba(255,122,47,0.10), 0 12rpx 24rpx rgba(255,122,47,0.18);
}

.label {
  font-size: 22rpx;
  color: var(--c-muted);
  position: relative;
  z-index: 1;
}

.bar-col.selected .label {
  color: var(--c-orange);
  font-weight: 900;
}

.empty-chart {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 28rpx;
  background: #F8FAFC;
}

.empty-text {
  font-size: 24rpx;
  color: var(--c-muted);
  font-weight: 800;
}
</style>
