<template>
  <!-- 页面说明：通用数据维护页，8 张数据库表和 2 个配置文件都复用此页面。 -->
  <div class="page-shell admin-crud-page">
    <BaseSearchBar :title="config.title" :eyebrow="config.eyebrow" :description="config.description">
      <template #actions>
        <button v-if="canCreate" class="button button--primary" type="button" @click="openCreate">新增</button>
        <button class="button button--ghost" type="button" @click="loadData">刷新</button>
      </template>

      <div class="toolbar">
        <input
          v-model.trim="keyword"
          class="input"
          type="text"
          @keyup.enter="loadData"
        />
        <button class="button button--primary" type="button" @click="loadData">查询</button>
        <button class="button button--ghost" type="button" @click="clearKeyword">清空</button>
      </div>
      <p class="panel-note" v-if="config.note">{{ config.note }}</p>
    </BaseSearchBar>

    <BaseTable :columns="tableColumns" :rows="displayRows" :loading="loading" :empty-text="`暂无${config.title}`">
      <template #cell-password="{ row }">
        <span class="hash-text">{{ row.password || '--' }}</span>
      </template>
      <template #cell-enabled="{ row }">
        <span class="status-pill" :class="row.enabled === false ? 'is-off' : 'is-on'">{{ row.enabled === false ? '停用' : '启用' }}</span>
      </template>
      <template #cell-status="{ row }">
        <span class="status-pill" :class="String(row.status).includes('deleted') || String(row.status).includes('disabled') || row.status === 0 ? 'is-off' : 'is-on'">{{ formatValue(row.status) }}</span>
      </template>
      <template #cell-role="{ row }">
        {{ formatValue(row.role) }}
      </template>
      <template #cell-gender="{ row }">
        {{ formatValue(row.gender) }}
      </template>
      <template #cell-measure_type="{ row }">
        {{ formatValue(row.measure_type) }}
      </template>
      <template #cell-privacy_level="{ row }">
        {{ formatValue(row.privacy_level) }}
      </template>
      <template #cell-actions="{ row }">
        <div class="table-actions">
          <button v-if="canEdit" class="button button--ghost button--sm" type="button" @click="openEdit(row)">编辑</button>
          <button v-if="config.key === 'users'" class="button button--ghost button--sm" type="button" @click="openReset(row)">重置密码</button>
          <button class="button button--danger button--sm" type="button" @click="deleteRow(row)">删除</button>
        </div>
      </template>
    </BaseTable>

    <div class="pager surface">
      <button class="button button--ghost button--sm" type="button" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
      <span>第 {{ page }} 页 / 共 {{ displayTotal }} 条</span>
      <button class="button button--ghost button--sm" type="button" :disabled="page * size >= displayTotal" @click="changePage(page + 1)">下一页</button>
    </div>

    <BaseDialog v-model="dialogVisible" :title="editingId ? `编辑${config.title}` : `新增${config.title}`" :eyebrow="config.eyebrow" width="820px">
      <BaseForm :loading="saving" submit-text="保存" cancel-text="取消" @submit="saveRow" @cancel="dialogVisible = false">
        <div class="form-grid">
          <template v-for="field in visibleFields" :key="field.key">
            <div class="field" :class="{ 'field--full': field.full }">
              <label>{{ field.label }}</label>
              <select v-if="field.type === 'select'" v-model="form[field.key]" class="select">
                <option v-for="option in field.options" :key="option.value" :value="option.value">{{ option.label }}</option>
              </select>
              <textarea v-else-if="field.type === 'textarea'" v-model="form[field.key]" class="textarea" />
              <label v-else-if="field.type === 'checkbox'" class="check-field">
                <input v-model="form[field.key]" type="checkbox" />
                <span>{{ form[field.key] ? '启用' : '停用' }}</span>
              </label>
              <input
                v-else
                v-model="form[field.key]"
                class="input"
                :type="field.type === 'number' ? 'number' : 'text'"
                :step="field.step || (field.type === 'number' ? '1' : undefined)"
              />
            </div>
          </template>
        </div>
        <p v-if="errorText" class="form-error">{{ errorText }}</p>
      </BaseForm>
    </BaseDialog>

    <BaseDialog v-model="resetVisible" title="重置用户密码" eyebrow="用户密码" width="520px">
      <BaseForm :loading="saving" submit-text="确认重置" cancel-text="取消" @submit="submitReset" @cancel="resetVisible = false">
        <p class="muted">用户：{{ resetTarget?.username }}（编号：{{ resetTarget?.id }}）</p>
        <div class="field">
          <label>新密码</label>
          <input v-model.trim="newPassword" class="input" type="password" />
        </div>
        <p v-if="errorText" class="form-error">{{ errorText }}</p>
      </BaseForm>
    </BaseDialog>
  </div>
</template>

<script setup>
// 页面配置说明：根据路由中的 resource 决定当前要维护的数据表或配置文件。
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import BaseDialog from '../components/BaseDialog.vue'
import BaseForm from '../components/BaseForm.vue'
import BaseSearchBar from '../components/BaseSearchBar.vue'
import BaseTable from '../components/BaseTable.vue'
import { adminCreate, adminDelete, adminList, adminUpdate, updateUserPassword } from '../services/api.js'
import { adminResourceConfigs } from '../config/adminResources.js'

const route = useRoute()
const keyword = ref('')
const rows = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const resetVisible = ref(false)
const resetTarget = ref(null)
const newPassword = ref('')
const errorText = ref('')
const editingId = ref('')
const form = reactive({})
const page = ref(1)
const size = ref(5)
const total = ref(0)

const configs = adminResourceConfigs
const config = computed(() => configs[route.meta?.resource] || configs.users)
const canCreate = computed(() => config.value.allowCreate !== false)
const canEdit = computed(() => config.value.allowEdit !== false)
const tableColumns = computed(() => {
  const width = config.value.key === 'users' ? '260px' : canEdit.value ? '190px' : '110px'
  return [...config.value.columns, { key: 'actions', label: '操作', width }]
})
const visibleFields = computed(() => config.value.fields.filter((item) => !(editingId.value && item.createOnly)))
const displayTotal = computed(() => total.value)
const displayRows = computed(() => rows.value)

watch(() => route.meta?.resource, () => {
  keyword.value = ''
  page.value = 1
  loadData()
})

watch(displayTotal, (value) => {
  const maxPage = Math.max(1, Math.ceil(Number(value || 0) / size.value))
  if (page.value > maxPage) page.value = maxPage
})

onMounted(loadData)


// 数据加载：数据库表走分页接口，食物库和模板配置走完整列表接口。
async function loadData() {
  loading.value = true
  errorText.value = ''
  try {
    const payload = await adminList(config.value.resource, { keyword: keyword.value, page: page.value, size: size.value })
    rows.value = payload.items || []
    total.value = Number(payload.total || rows.value.length || 0)
  } catch (error) {
    rows.value = []
    total.value = 0
    window.alert(error?.message || '数据加载失败')
  } finally {
    loading.value = false
  }
}

// 清空查询条件后重新加载第一页数据。
function clearKeyword() {
  keyword.value = ''
  page.value = 1
  loadData()
}

function changePage(next) {
  page.value = Math.max(1, next)
  loadData()
}

// 表单重置：根据字段配置生成新增或编辑时的默认值。
function resetForm() {
  Object.keys(form).forEach((key) => delete form[key])
  config.value.fields.forEach((item) => {
    if (item.type === 'checkbox') form[item.key] = true
    else if (item.type === 'select' && item.options?.length) form[item.key] = item.options[0].value
    else form[item.key] = ''
  })
}

// 打开新增弹窗。
function openCreate() {
  editingId.value = ''
  errorText.value = ''
  resetForm()
  dialogVisible.value = true
}

// 打开编辑弹窗，并把当前行数据回填到表单。
function openEdit(row) {
  editingId.value = row.id
  errorText.value = ''
  resetForm()
  config.value.fields.forEach((item) => {
    const value = row[item.key]
    form[item.key] = value ?? form[item.key] ?? ''
  })
  dialogVisible.value = true
}

// 提交前整理字段格式，尤其是数字和复选框。
function normalizePayload() {
  const payload = {}
  visibleFields.value.forEach((item) => {
    let value = form[item.key]
    if (item.type === 'number') value = value === '' ? null : Number(value)
    if (item.type === 'checkbox') value = Boolean(value)
    payload[item.key] = value
  })
  return payload
}

// 保存：有编号时执行修改，没有编号时执行新增。
async function saveRow() {
  errorText.value = ''
  saving.value = true
  try {
    const payload = normalizePayload()
    if (editingId.value) await adminUpdate(config.value.resource, editingId.value, payload)
    else await adminCreate(config.value.resource, payload)
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    errorText.value = error?.message || '保存失败'
  } finally {
    saving.value = false
  }
}

// 删除：删除前二次确认，避免误删数据库记录或配置文件项目。
async function deleteRow(row) {
  const ok = window.confirm(`确认删除编号为「${row.id}」的记录吗？`)
  if (!ok) return
  loading.value = true
  try {
    await adminDelete(config.value.resource, row.id)
    await loadData()
  } catch (error) {
    window.alert(error?.message || '删除失败')
  } finally {
    loading.value = false
  }
}

// 打开重置密码弹窗。
function openReset(row) {
  resetTarget.value = row
  newPassword.value = ''
  errorText.value = ''
  resetVisible.value = true
}

// 提交密码重置：后端会保存加密后的密码，不保存明文。
async function submitReset() {
  errorText.value = ''
  if (!newPassword.value || newPassword.value.length < 6) {
    errorText.value = '新密码至少 6 位'
    return
  }
  saving.value = true
  try {
    await updateUserPassword(resetTarget.value.id, newPassword.value)
    resetVisible.value = false
    window.alert('密码已重置')
    await loadData()
  } catch (error) {
    errorText.value = error?.message || '重置失败'
  } finally {
    saving.value = false
  }
}

function formatValue(value) {
  if (value === null || value === undefined || value === '') return '--'
  const text = String(value)
  const upper = text.toUpperCase()
  const lower = text.toLowerCase()
  if (value === 1 || text === '1' || value === true || lower === 'true' || lower === 'enabled') return '启用'
  if (value === 0 || text === '0' || value === false || lower === 'false' || lower === 'disabled') return '停用'
  if (upper === 'ADMIN') return '管理员'
  if (upper === 'USER') return '普通用户'
  if (lower === 'unknown') return '未知'
  if (lower === 'male') return '男'
  if (lower === 'female') return '女'
  if (lower === 'manual') return '手动录入'
  if (lower === 'camera') return '摄像头测量'
  if (lower === 'active') return '进行中'
  if (lower === 'finished') return '已完成'
  if (lower === 'deleted') return '已删除'
  if (lower === 'public') return '公开'
  if (lower === 'partial') return '部分可见'
  if (lower === 'private') return '私密'
  return value
}
</script>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.hash-text {
  display: inline-block;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 12px;
  color: #52635c;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  border-radius: 999px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 800;
}

.status-pill.is-on {
  color: #0b6d5a;
  background: rgba(31, 158, 132, 0.12);
}

.status-pill.is-off {
  color: #b14638;
  background: rgba(194, 68, 54, 0.12);
}

.check-field {
  min-height: 42px;
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid rgba(95, 117, 109, 0.2);
  border-radius: 14px;
  padding: 0 14px;
  background: rgba(255, 255, 255, 0.92);
}

.form-error {
  margin: 0;
  color: #b54335;
  font-size: 13px;
}

.pager {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 12px 16px;
}

@media (max-width: 760px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
