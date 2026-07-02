<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div v-if="modelValue" class="dialog" @click.self="handleClose">
        <div class="dialog__panel" :style="{ maxWidth: width }" @click.stop>
          <div class="dialog__head">
            <div>
              <p v-if="eyebrow" class="dialog__eyebrow">{{ eyebrow }}</p>
              <h3>{{ title }}</h3>
            </div>
            <button v-if="closable" type="button" class="dialog__close" @click="handleClose">×</button>
          </div>

          <div class="dialog__body">
            <slot />
          </div>

          <div v-if="$slots.footer" class="dialog__footer">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: ''
  },
  eyebrow: {
    type: String,
    default: ''
  },
  width: {
    type: String,
    default: '640px'
  },
  closable: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['update:modelValue', 'close'])

function handleClose() {
  if (!props.closable) return
  emit('update:modelValue', false)
  emit('close')
}
</script>

<style scoped>
.dialog {
  position: fixed;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(15, 28, 24, 0.45);
  backdrop-filter: blur(10px);
  z-index: 9999;
  pointer-events: auto;
}

.dialog__panel {
  position: relative;
  z-index: 1;
  width: min(100%, 1000px);
  max-height: min(88vh, 900px);
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  overflow: hidden;
  border: 1px solid rgba(163, 189, 178, 0.45);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 26px 70px rgba(9, 28, 24, 0.25);
  pointer-events: auto;
}

.dialog__head,
.dialog__footer {
  padding: 18px 20px;
}

.dialog__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid rgba(163, 189, 178, 0.35);
}

.dialog__body {
  min-height: 0;
  overflow: auto;
  padding: 20px;
}

.dialog__footer {
  border-top: 1px solid rgba(163, 189, 178, 0.35);
}

.dialog__eyebrow {
  margin: 0 0 6px;
  color: #6f7f78;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.dialog__head h3 {
  margin: 0;
  font-size: 20px;
}

.dialog__close {
  border: 0;
  background: transparent;
  color: #5e6f68;
  font-size: 28px;
  line-height: 1;
}

.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.18s ease;
}

.dialog-fade-leave-active {
  pointer-events: none;
}

.dialog-fade-enter-active .dialog__panel,
.dialog-fade-leave-active .dialog__panel {
  transition: transform 0.18s ease, opacity 0.18s ease;
}

.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}

.dialog-fade-enter-from .dialog__panel,
.dialog-fade-leave-to .dialog__panel {
  transform: translateY(10px) scale(0.98);
  opacity: 0.98;
}
</style>
