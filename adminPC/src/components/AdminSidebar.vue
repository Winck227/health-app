<template>
  <aside class="sidebar">
    <div class="sidebar__brand">
      <div class="sidebar__mark">健</div>
      <div>
        <strong>健康管理后台</strong>
        <span>管理控制台</span>
      </div>
    </div>

    <nav class="sidebar__nav">
      <RouterLink
        v-for="item in items"
        :key="item.path"
        :to="item.path"
        class="sidebar__link"
        active-class="is-active"
      >
        <span class="sidebar__icon">{{ item.icon }}</span>
        <span>{{ item.label }}</span>
      </RouterLink>
    </nav>

    <div class="sidebar__card">
      <div>
        <strong>{{ admin?.name || admin?.username || '管理员账号' }}</strong>
        <p>{{ formatRole(admin?.role) }}</p>
      </div>
      <button type="button" class="sidebar__logout" @click="$emit('logout')">退出登录</button>
    </div>
  </aside>
</template>

<script setup>
defineProps({
  items: {
    type: Array,
    default: () => []
  },
  admin: {
    type: Object,
    default: () => ({})
  }
})

defineEmits(['logout'])

// 将后端角色值转换成中文显示，避免页面模板找不到 formatRole 方法。
function formatRole(role) {
  const value = String(role || '').toUpperCase()
  if (value === 'ADMIN') return '管理员'
  if (value === 'USER') return '普通用户'
  return '管理员'
}
</script>

<style scoped>
.sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 18px;
  padding: 20px;
  border-right: 1px solid rgba(163, 189, 178, 0.65);
  background:
    radial-gradient(circle at top left, rgba(21, 134, 109, 0.14), transparent 32%),
    rgba(248, 252, 249, 0.96);
  backdrop-filter: blur(18px);
}

.sidebar__brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sidebar__mark {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: linear-gradient(135deg, #15685b, #1d9a7c);
  color: #fff;
  font-weight: 800;
  box-shadow: 0 14px 30px rgba(21, 104, 91, 0.24);
}

.sidebar__brand strong,
.sidebar__brand span {
  display: block;
}

.sidebar__brand strong {
  font-size: 18px;
}

.sidebar__brand span {
  margin-top: 2px;
  color: #6a7a73;
  font-size: 12px;
}

.sidebar__nav {
  display: grid;
  gap: 8px;
  overflow: auto;
  padding-right: 4px;
}

.sidebar__link {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 44px;
  border-radius: 14px;
  padding: 0 14px;
  color: #18312a;
  text-decoration: none;
  transition: transform 0.18s ease, background-color 0.18s ease, color 0.18s ease;
}

.sidebar__link:hover {
  transform: translateX(2px);
  background: rgba(21, 134, 109, 0.08);
}

.sidebar__link.is-active {
  background: linear-gradient(135deg, rgba(21, 134, 109, 0.16), rgba(233, 139, 66, 0.14));
  color: #0d5d4d;
}

.sidebar__icon {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 10px;
  background: rgba(21, 134, 109, 0.08);
  font-size: 10px;
  font-weight: 700;
}

.sidebar__card {
  display: grid;
  gap: 10px;
  border: 1px solid rgba(163, 189, 178, 0.7);
  border-radius: 18px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 10px 24px rgba(14, 44, 35, 0.06);
}

.sidebar__card strong,
.sidebar__card p {
  margin: 0;
}

.sidebar__card p {
  color: #6a7a73;
  font-size: 12px;
}

.sidebar__logout {
  border: 0;
  border-radius: 12px;
  min-height: 40px;
  background: #173630;
  color: #fff;
}

@media (max-width: 1080px) {
  .sidebar {
    position: static;
    height: auto;
  }

  .sidebar__nav {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .sidebar__nav {
    grid-template-columns: 1fr;
  }
}
</style>
