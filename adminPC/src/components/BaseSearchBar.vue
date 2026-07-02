<template>
  <!-- 组件说明：搜索区域组件，用于展示标题、说明、操作按钮和筛选条件。 -->
  <section class="base-search-bar surface">
    <div class="surface-head">
      <div>
        <p v-if="eyebrow" class="base-search-bar__eyebrow">{{ eyebrow }}</p>
        <h2 class="base-search-bar__title">{{ title }}</h2>
        <p v-if="description" class="base-search-bar__description">{{ description }}</p>
      </div>

      <div class="base-search-bar__actions">
        <slot name="actions" />
      </div>
    </div>

    <div class="surface-body">
      <slot />
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  },
  eyebrow: {
    type: String,
    default: ''
  },
  description: {
    type: String,
    default: ''
  }
})
</script>

<style scoped>
.base-search-bar__eyebrow {
  margin: 0;
  color: #7a8a83;
  font-size: 12px;
  letter-spacing: 0.14em;
  
}

.base-search-bar__title {
  margin: 6px 0 0;
  font-size: 18px;
}

.base-search-bar__description {
  margin: 6px 0 0;
  color: #667772;
}

.base-search-bar__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
