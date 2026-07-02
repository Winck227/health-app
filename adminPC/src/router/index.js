import { createRouter, createWebHashHistory } from 'vue-router'
import { getAdminToken } from '../services/request.js'

const crudView = () => import('../views/AdminCrudPage.vue')

const routes = [
  {
    path: '/login',
    name: '登录页',
    meta: { title: '登录', public: true },
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: '管理概览', meta: { title: '管理概览', subtitle: '用户、饮食与健康数据总览' }, component: () => import('../views/Dashboard.vue') },
      { path: 'users', name: '用户管理', meta: { title: '用户管理', subtitle: '用户账号、状态与密码管理', resource: 'users' }, component: crudView },
      { path: 'diet-records', name: '饮食记录管理', meta: { title: '饮食记录管理', subtitle: '饮食记录全库管理', resource: 'diet-records' }, component: crudView },
      { path: 'health-records', name: '健康档案管理', meta: { title: '健康档案管理', subtitle: '身高、体重、BMI 与 BMR 记录管理', resource: 'health-records' }, component: crudView },
      { path: 'heart-records', name: '心率记录管理', meta: { title: '心率记录管理', subtitle: '手动与摄像头心率记录管理', resource: 'heart-records' }, component: crudView },
      { path: 'user-plans', name: '用户计划管理', meta: { title: '用户计划管理', subtitle: '用户健康计划与进度管理', resource: 'user-plans' }, component: crudView },
      { path: 'plan-checkins', name: '打卡记录管理', meta: { title: '打卡记录管理', subtitle: '健康计划打卡记录管理', resource: 'plan-checkins' }, component: crudView },
      { path: 'article-favorites', name: '收藏管理', meta: { title: '收藏管理', subtitle: '用户文章收藏管理', resource: 'article-favorites' }, component: crudView },
      { path: 'user-settings', name: '用户设置管理', meta: { title: '用户设置管理', subtitle: '用户目标、通知与隐私设置管理', resource: 'user-settings' }, component: crudView }
    ]
  }
]

export const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  document.title = `${String(to.meta?.title || '健康管理后台')} - 健康管理后台`

  if (to.meta?.public) {
    if (to.path === '/login' && getAdminToken()) {
      return '/dashboard'
    }
    return true
  }

  if (to.meta?.requiresAuth && !getAdminToken()) {
    return '/login'
  }

  return true
})
