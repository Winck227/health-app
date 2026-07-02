<template>
  <div class="admin-layout">
    <AdminSidebar :items="navItems" :admin="admin" @logout="handleLogout" />

    <div class="admin-layout__main">
      <AdminTopbar
        :title="pageTitle"
        :subtitle="pageSubtitle"
        :admin="admin"
        @refresh="handleRefresh"
        @logout="handleLogout"
      />

      <main class="admin-layout__content">
        <router-view :key="viewKey" />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminSidebar from '../components/AdminSidebar.vue'
import AdminTopbar from '../components/AdminTopbar.vue'
import { adminSession } from '../services/api.js'

const route = useRoute()
const router = useRouter()
const viewKey = ref(0)
const admin = computed(() => adminSession.getCachedAdmin() || {})

const navItems = [
  { path: '/dashboard', label: '管理概览', icon: '概览' },
  { path: '/users', label: '用户管理', icon: '用户' },
  { path: '/diet-records', label: '饮食记录管理', icon: '饮食' },
  { path: '/health-records', label: '健康档案管理', icon: '健康' },
  { path: '/heart-records', label: '心率记录管理', icon: '心率' },
  { path: '/user-plans', label: '用户计划管理', icon: '计划' },
  { path: '/plan-checkins', label: '打卡记录管理', icon: '打卡' },
  { path: '/article-favorites', label: '收藏管理', icon: '收藏' },
  { path: '/user-settings', label: '用户设置管理', icon: '设置' }
]

const pageTitle = computed(() => String(route.meta?.title || '管理概览'))
const pageSubtitle = computed(() => String(route.meta?.subtitle || ''))

function handleRefresh() {
  viewKey.value += 1
}

function handleLogout() {
  adminSession.clearAdminSession()
  router.replace('/login')
}
</script>

<style scoped>
.admin-layout {
  display: grid;
  min-height: 100vh;
  grid-template-columns: 272px minmax(0, 1fr);
  background:
    radial-gradient(circle at top left, rgba(23, 134, 109, 0.12), transparent 30%),
    linear-gradient(180deg, #f3f7f4 0%, #eef4ef 100%);
}

.admin-layout__main {
  min-width: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
}

.admin-layout__content {
  min-width: 0;
  padding: 22px;
}

@media (max-width: 1080px) {
  .admin-layout {
    grid-template-columns: 1fr;
  }

  .admin-layout__content {
    padding: 16px;
  }
}
</style>
