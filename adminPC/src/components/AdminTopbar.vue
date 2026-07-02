<template>
  <header class="topbar">
    <div>
      <p class="topbar__eyebrow">健康管理后台</p>
      <h1>{{ title }}</h1>
      <p v-if="subtitle" class="topbar__subtitle">{{ subtitle }}</p>
    </div>

    <div class="topbar__actions">
      <div class="topbar__admin">
        <strong>{{ admin?.name || admin?.username || '管理员账号' }}</strong>
        <span>{{ formatRole(admin?.role) }}</span>
      </div>
      <button type="button" class="topbar__button" @click="$emit('refresh')">刷新</button>
      <button type="button" class="topbar__button topbar__button--ghost" @click="$emit('logout')">退出登录</button>
    </div>
  </header>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  },
  subtitle: {
    type: String,
    default: ''
  },
  admin: {
    type: Object,
    default: () => ({})
  }
})

defineEmits(['refresh', 'logout'])

function formatRole(role) {
  const value = String(role || '').toUpperCase()
  if (value === 'ADMIN') return '管理员'
  if (value === 'USER') return '普通用户'
  return '管理员'
}
</script>

<style scoped>
.topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 22px 0;
}

.topbar__eyebrow {
  margin: 0;
  color: #7a8a83;
  font-size: 12px;
  letter-spacing: 0.18em;
  
}

.topbar h1 {
  margin: 8px 0 0;
  font-size: clamp(26px, 2.5vw, 34px);
  line-height: 1.15;
}

.topbar__subtitle {
  margin: 8px 0 0;
  color: #67756f;
}

.topbar__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.topbar__admin {
  display: grid;
  gap: 2px;
  padding-right: 8px;
}

.topbar__admin strong {
  font-size: 14px;
}

.topbar__admin span {
  color: #7a8a83;
  font-size: 12px;
}

.topbar__button {
  border: 1px solid rgba(21, 134, 109, 0.2);
  border-radius: 12px;
  min-height: 40px;
  padding: 0 14px;
  background: #15866d;
  color: #fff;
}

.topbar__button--ghost {
  background: rgba(255, 255, 255, 0.86);
  color: #174238;
}

@media (max-width: 760px) {
  .topbar {
    flex-direction: column;
    padding: 16px 16px 0;
  }

  .topbar__actions {
    flex-wrap: wrap;
  }
}
</style>
