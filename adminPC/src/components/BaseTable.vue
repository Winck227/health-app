<template>
  <!-- 组件说明：通用表格组件，支持加载、空状态和自定义单元格。 -->
  <div class="base-table surface">
    <div v-if="loading" class="surface-body">
      <LoadingState text="数据加载中..." />
    </div>

    <div v-else-if="!rows.length" class="surface-body">
      <EmptyState :title="emptyText" :description="emptyDescription" />
    </div>

    <div v-else class="base-table__wrap">
      <table>
        <thead>
          <tr>
            <th v-for="column in columns" :key="column.key" :style="headerStyle(column)">
              {{ column.label }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, index) in rows" :key="resolveRowKey(row, index)">
            <td v-for="column in columns" :key="column.key" :style="cellStyle(column)">
              <slot :name="`cell-${column.key}`" :row="row" :index="index">
                {{ formatCell(row, column) }}
              </slot>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
// 表格逻辑：根据传入列配置和数据行渲染，空数据时显示统一提示。
import EmptyState from './EmptyState.vue'
import LoadingState from './LoadingState.vue'

const props = defineProps({
  columns: {
    type: Array,
    default: () => []
  },
  rows: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  rowKey: {
    type: [String, Function],
    default: 'id'
  },
  emptyText: {
    type: String,
    default: '暂无数据'
  },
  emptyDescription: {
    type: String,
    default: '当前条件下没有匹配的数据。'
  }
})

function resolveRowKey(row, index) {
  if (typeof props.rowKey === 'function') {
    return props.rowKey(row, index)
  }
  return row?.[props.rowKey] ?? index
}

function formatCell(row, column) {
  const value = row?.[column.key]
  if (value === null || value === undefined || value === '') return '--'
  if (Array.isArray(value)) return value.join(', ')
  if (typeof value === 'boolean') return value ? '是' : '否'
  if (typeof value === 'object') return JSON.stringify(value)
  return value
}

function headerStyle(column) {
  const style = {}
  if (column.width) style.width = column.width
  if (column.minWidth) style.minWidth = column.minWidth
  return Object.keys(style).length ? style : undefined
}

function cellStyle(column) {
  const style = {}
  if (column.align) style.textAlign = column.align
  if (column.wrap) style.whiteSpace = 'normal'
  return Object.keys(style).length ? style : undefined
}
</script>

<style scoped>
.base-table__wrap {
  overflow: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  border-bottom: 1px solid rgba(163, 189, 178, 0.35);
  padding: 14px 16px;
  text-align: left;
  vertical-align: middle;
  white-space: nowrap;
}

th {
  background: rgba(245, 249, 246, 0.92);
  color: #6e7f78;
  font-size: 13px;
  font-weight: 700;
}

td {
  color: #1f302b;
}
</style>
