<template>
  <div class="page-shell dashboard-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">管理概览</h2>
        <p class="page-subtitle">用户、饮食与健康数据总览。</p>
      </div>
    </div>

    <div class="grid-4 dashboard-kpis">
      <StatCard
        v-for="card in cards"
        :key="card.key"
        :label="card.label"
        :value="card.value"
        :delta="card.delta"
        :style="cardTone(card.key)"
      />
    </div>

    <div class="grid-2 dashboard-panels">
      <section class="surface dashboard-panel">
        <div class="surface-head">
          <div>
            <h3>用户趋势</h3>
            <p class="muted">近一周样本趋势</p>
          </div>
        </div>
        <div class="surface-body">
          <div class="mini-chart">
            <div v-for="item in userTrend" :key="item.label" class="mini-chart__bar">
              <span class="mini-chart__fill" :style="{ height: `${Math.max(18, item.value * 5)}px` }"></span>
              <span class="mini-chart__label">{{ item.label }}</span>
            </div>
          </div>
        </div>
      </section>

      <section class="surface dashboard-panel">
        <div class="surface-head">
          <div>
            <h3>核心指标</h3>
            <p class="muted">后端最新指标</p>
          </div>
        </div>
        <div class="surface-body">
          <div class="overview-grid">
            <div class="overview-card">
              <span>步数</span>
              <strong>{{ info.steps || 0 }}</strong>
            </div>
            <div class="overview-card">
              <span>心率</span>
              <strong>{{ info.heartRate || '--' }}</strong>
            </div>
            <div class="overview-card">
              <span>日期</span>
              <strong>{{ info.heartDate || '--' }}</strong>
            </div>
            <div class="overview-card">
              <span>得分</span>
              <strong>{{ info.score || '--' }}</strong>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import StatCard from '../components/StatCard.vue'
import { stats } from '../services/api.js'

const statsData = ref({ cards: [], userTrend: [] })
const cards = computed(() => statsData.value.cards || [])
const userTrend = computed(() => statsData.value.userTrend || [])
const info = computed(() => {
  const latest = statsData.value.latest || {}
  return {
    steps: latest.steps || 0,
    heartRate: latest.heart || '--',
    heartDate: latest.date || '--',
    score: latest.score || '--'
  }
})

onMounted(loadData)

async function loadData() {
  statsData.value = (await stats()) || {}
}

function cardTone(key) {
  const tones = {
    users: {
      '--stat-accent': '#15685b',
      '--stat-accent-soft': 'rgba(21, 104, 91, 0.12)'
    },
    diet: {
      '--stat-accent': '#0f88a6',
      '--stat-accent-soft': 'rgba(15, 136, 166, 0.12)'
    },
    health: {
      '--stat-accent': '#d97706',
      '--stat-accent-soft': 'rgba(217, 119, 6, 0.12)'
    },
    plans: {
      '--stat-accent': '#1f9e84',
      '--stat-accent-soft': 'rgba(31, 158, 132, 0.12)'
    }
  }
  return tones[key] || tones.users
}
</script>

<style scoped>
.dashboard-page {
  gap: 22px;
}

.dashboard-kpis {
  align-items: stretch;
}

.dashboard-panel {
  min-height: 320px;
}

.dashboard-panel :deep(.surface-body) {
  padding-top: 10px;
}

.overview-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.overview-card {
  min-height: 112px;
  display: grid;
  gap: 8px;
  padding: 18px;
  border: 1px solid rgba(163, 189, 178, 0.3);
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(21, 104, 91, 0.06), rgba(21, 104, 91, 0.02));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.overview-card span {
  color: #67817a;
  font-size: 13px;
}

.overview-card strong {
  font-size: clamp(24px, 3vw, 34px);
  line-height: 1;
  letter-spacing: -0.04em;
}

@media (max-width: 760px) {
  .dashboard-panel {
    min-height: auto;
  }
}
</style>
