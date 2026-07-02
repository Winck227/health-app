<template>
  <div class="loading-state">
    <span class="loading-state__spinner"></span>
    <p>{{ text }}</p>
  </div>
</template>

<script setup>
defineProps({
  text: {
    type: String,
    default: '加载中...'
  }
})
</script>

<style scoped>
.loading-state {
  min-height: 180px;
  display: grid;
  place-items: center;
  gap: 12px;
  color: #60706a;
}

.loading-state__spinner {
  width: 34px;
  height: 34px;
  border: 3px solid rgba(21, 134, 109, 0.15);
  border-top-color: #15685b;
  border-radius: 50%;
  animation: spin 0.75s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
