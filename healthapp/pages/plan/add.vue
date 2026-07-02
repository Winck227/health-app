<template>
  <view class="page">
    <TopBar title="添加计划" />

    <text class="subhead">自定义计划名称、周期和每日目标，提交后写入 user_plan</text>

    <view v-if="pageError" class="u-card error-card">
      <text class="error-title">加载模板失败</text>
      <text class="error-sub">{{ pageError || '不影响自定义创建' }}</text>
    </view>

    <view class="u-card card">
      <view class="section-head">
        <text class="section-title">计划内容</text>
        <text class="section-sub">必填</text>
      </view>

      <view class="field">
        <text class="label">计划名称</text>
        <input
          v-model="form.name"
          class="input"
          maxlength="30"
          placeholder="例如 7 天晨跑计划"
          placeholder-class="placeholder"
        />
      </view>

      <view class="field">
        <text class="label">计划分类</text>
        <view class="chip-row">
          <view
            v-for="item in categories"
            :key="item"
            class="chip"
            :class="{ active: form.category === item }"
            @click="form.category = item"
          >
            {{ item }}
          </view>
        </view>
      </view>

      <view class="field">
        <text class="label">目标天数</text>
        <input
          v-model="form.days"
          class="input"
          type="number"
          maxlength="3"
          placeholder="例如 21"
          placeholder-class="placeholder"
        />
      </view>

      <view class="field">
        <text class="label">每日目标 / 计划说明</text>
        <textarea
          v-model="form.goalText"
          class="textarea"
          maxlength="120"
          auto-height
          placeholder="例如 每天完成 30 分钟快走，并记录身体状态"
          placeholder-class="placeholder"
        />
      </view>

      <view class="field">
        <text class="label">主题颜色</text>
        <view class="chip-row">
          <view
            v-for="item in themes"
            :key="item.value"
            class="chip"
            :class="[{ active: form.theme === item.value }, `theme-${item.value}`]"
            @click="form.theme = item.value"
          >
            {{ item.label }}
          </view>
        </view>
      </view>
    </view>

    <view class="u-card card" v-if="templates.length">
      <view class="section-head">
        <text class="section-title">快速套用模板</text>
        <text class="section-sub">{{ templateLibrary.sourceLabel }} · 版本 {{ templateLibrary.version || '--' }}</text>
      </view>
      <text class="hint">点击模板只会把内容填入上方表单，提交前仍可自由修改。</text>
      <scroll-view scroll-x class="template-scroll" :show-scrollbar="false">
        <view class="template-inner">
          <view
            v-for="item in templates"
            :key="item.id"
            class="template-chip"
            @click="applyTemplate(item)"
          >
            <text class="template-name">{{ item.name }}</text>
            <text class="template-meta">{{ item.category }} · {{ item.days }} 天</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <view class="u-card preview-card">
      <text class="preview-title">预览</text>
      <text class="preview-name">{{ previewName }}</text>
      <text class="preview-meta">{{ form.category || '未分类' }} · {{ safeDays }} 天</text>
      <text class="preview-goal">{{ form.goalText || '填写每日目标后会显示在计划详情页。' }}</text>
    </view>

    <view class="btn" :class="{ disabled: !canSubmit || loading }" @click="submit">
      {{ loading ? '提交中...' : '创建自定义计划' }}
    </view>
  </view>
</template>

<script setup>
// 功能注释：计划创建页改为自定义内容，模板只作为快速填充，不再强制选择模板。
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import TopBar from '@/components/ui/TopBar.vue'
import { createPlan, logUserAction } from '@/services/healthApi.js'
import { loadPlanTemplateLibrary, normalizeTemplate, syncPlanTemplateLibrary } from '@/services/planTemplateLocal.js'
import { safeBack } from '@/utils/router.js'

const pageError = ref('')
const loading = ref(false)
const templates = ref([])
const templateLibrary = ref(loadPlanTemplateLibrary())

const categories = ['运动', '饮食', '睡眠', '习惯', '康复', '其他']
const themes = [
  { value: 'green', label: '绿色' },
  { value: 'red', label: '红色' },
  { value: 'blue', label: '蓝色' }
]

const form = reactive({
  name: '',
  category: '运动',
  days: '7',
  goalText: '',
  theme: 'green'
})

const safeDays = computed(() => {
  const value = Number(form.days)
  if (!Number.isFinite(value) || value < 1) return 7
  return Math.min(365, Math.floor(value))
})

const previewName = computed(() => form.name.trim() || '未命名计划')

const canSubmit = computed(() => {
  return Boolean(form.name.trim()) && Boolean(form.goalText.trim()) && safeDays.value >= 1 && !loading.value
})

function applyTemplate(item) {
  const template = normalizeTemplate(item)
  form.name = template.name
  form.category = categories.includes(template.category) ? template.category : '运动'
  form.days = String(template.days || 7)
  form.goalText = template.goalText
  form.theme = themes.some((theme) => theme.value === template.theme) ? template.theme : 'green'
  logUserAction('apply_plan_template_to_custom_form', { templateId: template.id })
}

async function loadTemplates(defaultTemplateId = '') {
  try {
    pageError.value = ''
    templateLibrary.value = loadPlanTemplateLibrary()
    templates.value = templateLibrary.value.templates || []
    if (!templates.value.length) {
      templateLibrary.value = await syncPlanTemplateLibrary()
      templates.value = templateLibrary.value.templates || []
    }
    if (defaultTemplateId) {
      const matched = templates.value.find((item) => item.id === defaultTemplateId)
      if (matched) applyTemplate(matched)
    }
  } catch (error) {
    pageError.value = error.message || '加载模板失败'
    templates.value = []
  }
}

onLoad((options) => {
  loadTemplates(options?.templateId || '')
})

async function submit() {
  if (!canSubmit.value) {
    uni.showToast({ title: '请填写计划名称和目标', icon: 'none' })
    return
  }
  loading.value = true
  const payload = {
    name: form.name.trim(),
    category: form.category || '运动',
    days: safeDays.value,
    goalText: form.goalText.trim(),
    theme: form.theme || 'green'
  }
  try {
    logUserAction('create_custom_plan_submit', payload)
    const result = await createPlan(payload)
    uni.showToast({ title: '创建成功', icon: 'success' })
    setTimeout(() => safeBack(), 250)
  } catch (error) {
    uni.showToast({ title: error.message || '创建失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page {
  padding: 20rpx 36rpx 40rpx;
  box-sizing: border-box;
}
.subhead {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: var(--c-muted);
  line-height: 1.6;
}
.error-card,
.card,
.preview-card {
  margin-top: 22rpx;
  border-radius: 52rpx;
  padding: 34rpx;
}
.error-title {
  display: block;
  font-size: 34rpx;
  font-weight: 1000;
  color: var(--c-title);
}
.error-sub {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: var(--c-muted);
  font-weight: 700;
  line-height: 1.7;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  margin-bottom: 18rpx;
}
.section-title {
  font-size: 28rpx;
  font-weight: 1000;
  color: var(--c-title);
}
.section-sub {
  font-size: 22rpx;
  color: var(--c-muted);
  font-weight: 800;
}
.field {
  margin-top: 24rpx;
}
.label {
  display: block;
  margin-bottom: 12rpx;
  font-size: 24rpx;
  color: var(--c-muted);
  font-weight: 900;
}
.input,
.textarea {
  width: 100%;
  box-sizing: border-box;
  border-radius: 28rpx;
  background: #F8FAFC;
  color: var(--c-title);
  font-size: 28rpx;
  font-weight: 900;
}
.input {
  height: 86rpx;
  padding: 0 24rpx;
}
.textarea {
  min-height: 150rpx;
  padding: 22rpx 24rpx;
  line-height: 1.55;
}
.placeholder {
  color: #9AA4B2;
  font-weight: 800;
}
.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
}
.chip {
  padding: 14rpx 24rpx;
  border-radius: 9999rpx;
  background: #F8FAFC;
  color: var(--c-muted);
  font-size: 24rpx;
  font-weight: 1000;
}
.chip.active {
  background: rgba(18, 183, 106, 0.14);
  color: var(--c-primary);
}
.chip.theme-red.active {
  background: rgba(255, 45, 85, 0.14);
  color: #ff2d55;
}
.chip.theme-blue.active {
  background: rgba(91, 108, 255, 0.14);
  color: #5b6cff;
}
.hint {
  display: block;
  color: var(--c-muted);
  font-size: 23rpx;
  font-weight: 700;
  line-height: 1.6;
  margin-bottom: 18rpx;
}
.template-scroll {
  white-space: nowrap;
}
.template-inner {
  display: flex;
  gap: 16rpx;
}
.template-chip {
  min-width: 260rpx;
  padding: 22rpx;
  border-radius: 30rpx;
  background: #F8FAFC;
  box-sizing: border-box;
}
.template-name {
  display: block;
  font-size: 26rpx;
  font-weight: 1000;
  color: var(--c-title);
}
.template-meta {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: var(--c-muted);
  font-weight: 800;
}
.preview-card {
  background: linear-gradient(135deg, #0b1f2a, #0f3a44);
  color: #fff;
}
.preview-title {
  display: block;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.72);
  font-weight: 900;
}
.preview-name {
  display: block;
  margin-top: 12rpx;
  font-size: 38rpx;
  font-weight: 1000;
}
.preview-meta,
.preview-goal {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.82);
  font-weight: 800;
  line-height: 1.6;
}
.btn {
  margin-top: 34rpx;
  height: 96rpx;
  border-radius: 9999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  font-weight: 1000;
  color: #fff;
  background: linear-gradient(135deg, #0b1f2a, #0f3a44);
  box-shadow: var(--shadow-btn);
}
.btn.disabled {
  background: #8B919B;
  box-shadow: none;
}
</style>
