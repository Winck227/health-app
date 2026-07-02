<template>
  <div class="empty-state">
    <div class="empty-state__icon">∅</div>
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
    <div v-if="$slots.actions" class="empty-state__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: '暂无数据'
  },
  description: {
    type: String,
    default: '当前条件下没有匹配的数据。'
  }
})
</script>

<style scoped>
.empty-state {
  min-height: 220px;
  display: grid;
  place-items: center;
  gap: 10px;
  padding: 28px 20px;
  text-align: center;
  color: #687871;
}

.empty-state__icon {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  border-radius: 18px;
  background: rgba(21, 134, 109, 0.08);
  color: #146454;
  font-size: 24px;
  font-weight: 800;
}

.empty-state h3,
.empty-state p {
  margin: 0;
}

.empty-state h3 {
  color: #234039;
}
</style>
