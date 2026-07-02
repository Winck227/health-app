<template>
  <!-- 页面说明：管理员登录页，只允许管理员账号进入后台。 -->
  <div class="login-page">
    <section class="login-panel surface">
      <div class="login-panel__head">
        <p>管理员登录</p>
        <h2>健康管理后台</h2>
      </div>

      <BaseForm :loading="loading" submit-text="登录" cancel-text="清空" @submit="handleSubmit" @cancel="handleReset">
        <div class="field">
          <label>管理员账号</label>
          <input v-model.trim="form.username" class="input" type="text" autocomplete="username" />
        </div>

        <div class="field">
          <label>密码</label>
          <input v-model.trim="form.password" class="input" type="password" autocomplete="current-password" />
        </div>

        <p v-if="errorText" class="login-panel__error">{{ errorText }}</p>
      </BaseForm>
    </section>
  </div>
</template>

<script setup>
// 登录页逻辑：校验账号密码，成功后保存管理员令牌并进入管理概览。
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import BaseForm from '../components/BaseForm.vue'
import { adminSession, login } from '../services/api.js'

const router = useRouter()
const loading = ref(false)
const errorText = ref('')
const form = reactive({
  username: '',
  password: ''
})

onMounted(() => {
  if (adminSession.getAdminToken()) {
    router.replace('/dashboard')
  }
})

async function handleSubmit() {
  errorText.value = ''
  if (!form.username || !form.password) {
    errorText.value = '请填写管理员账号和密码。'
    return
  }

  loading.value = true
  try {
    const result = await login({
      username: form.username,
      password: form.password
    })
    adminSession.setAdminSession(result.token, result.user)
    router.replace('/dashboard')
  } catch (error) {
    errorText.value = error?.message || '登录失败，请检查账号、密码和管理员权限。'
  } finally {
    loading.value = false
  }
}

function handleReset() {
  form.username = ''
  form.password = ''
  errorText.value = ''
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.login-panel {
  width: min(100%, 460px);
  padding: 26px;
  background: rgba(255, 255, 255, 0.94);
}

.login-panel__head p {
  margin: 0;
  color: #7a8a83;
  font-size: 12px;
  letter-spacing: 0.14em;
  
}

.login-panel__head h2 {
  margin: 8px 0 0;
  font-size: 24px;
}

.login-panel__error {
  margin: 0;
  color: #b54335;
  font-size: 13px;
}
</style>
