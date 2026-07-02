<template>
  <article class="stat-card surface">
    <span class="stat-card__glow" aria-hidden="true"></span>
    <div class="stat-card__head">
      <div class="stat-card__label">{{ label }}</div>
      <div v-if="delta" class="stat-card__delta">{{ delta }}</div>
    </div>
    <div class="stat-card__value">{{ value }}</div>
  </article>
</template>

<script setup>
defineProps({
  label: {
    type: String,
    default: ''
  },
  value: {
    type: [String, Number],
    default: '--'
  },
  delta: {
    type: String,
    default: ''
  }
})
</script>

<style scoped>
.stat-card {
  position: relative;
  overflow: hidden;
  min-height: 136px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 18px 16px;
  border-radius: 26px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 251, 249, 0.98));
  box-shadow: 0 18px 38px rgba(15, 41, 34, 0.08);
}

.stat-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at top right, var(--stat-accent-soft, rgba(21, 104, 91, 0.12)), transparent 48%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.42), transparent 34%);
  pointer-events: none;
}

.stat-card::after {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 4px;
  background: linear-gradient(90deg, var(--stat-accent, #15685b), rgba(255, 255, 255, 0));
}

.stat-card__glow {
  position: absolute;
  right: -26px;
  top: -26px;
  width: 124px;
  height: 124px;
  border-radius: 50%;
  background: radial-gradient(circle, var(--stat-accent-soft, rgba(21, 104, 91, 0.12)), transparent 72%);
  pointer-events: none;
}

.stat-card__head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.stat-card__label {
  margin: 20px 0 0 20px;
  color: #6f8179;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.stat-card__value {
  position: relative;
  z-index: 1;
  font-size: clamp(28px, 4vw, 42px);
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.05em;
}

.stat-card__delta {
  flex: none;
  border-radius: 999px;
  padding: 7px 10px;
  border: 1px solid rgba(21, 104, 91, 0.12);
  background: var(--stat-accent-soft, rgba(21, 104, 91, 0.12));
  color: var(--stat-accent, #15685b);
  font-size: 12px;
  font-weight: 700;
}
</style>
