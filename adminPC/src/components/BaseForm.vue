<template>
  <form class="base-form" @submit.prevent="$emit('submit')">
    <div class="base-form__fields">
      <slot />
    </div>

    <div v-if="$slots.actions || !hideDefaultActions" class="base-form__actions">
      <slot name="actions">
        <button type="submit" class="button button--primary" :disabled="loading">
          {{ loading ? '保存中...' : submitText }}
        </button>
        <button type="button" class="button button--ghost" @click="$emit('cancel')">
          {{ cancelText }}
        </button>
      </slot>
    </div>
  </form>
</template>

<script setup>
defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  submitText: {
    type: String,
    default: '保存'
  },
  cancelText: {
    type: String,
    default: '取消'
  },
  hideDefaultActions: {
    type: Boolean,
    default: false
  }
})

defineEmits(['submit', 'cancel'])
</script>

<style scoped>
.base-form {
  display: grid;
  gap: 18px;
}

.base-form__fields {
  display: grid;
  gap: 16px;
}

.base-form__actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  flex-wrap: wrap;
}
</style>
