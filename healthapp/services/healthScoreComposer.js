import {
  fetchDietPageData,
  fetchHealthRecord,
  fetchHeartData,
  fetchPlanList,
  fetchSettingsData
} from '@/services/healthApi.js'
import { getStepHistorySnapshot } from '@/services/stepHistoryLocal.js'

const HISTORY_KEY = 'health-app-score-history-v1'

function pad(value) {
  return String(value).padStart(2, '0')
}

function todayText(date = new Date()) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function toNumber(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, Number(value) || 0))
}

function isToday(value) {
  return String(value || '').slice(0, 10) === todayText()
}

function round(value, digits = 1) {
  const factor = 10 ** digits
  return Math.round(toNumber(value) * factor) / factor
}

async function safeLoad(loader, fallback) {
  try {
    return await loader()
  } catch (error) {
    return fallback
  }
}

function asList(value) {
  if (Array.isArray(value)) return value
  if (Array.isArray(value?.logs)) return value.logs
  if (Array.isArray(value?.items)) return value.items
  if (Array.isArray(value?.records)) return value.records
  if (Array.isArray(value?.list)) return value.list
  if (Array.isArray(value?.history)) return value.history
  return []
}

function latestHealth(data) {
  return data?.current || data?.latest || asList(data)[0] || null
}

function latestHeart(data) {
  return data?.latest || data?.current || asList(data)[0] || null
}

function heartBpm(data) {
  const latest = latestHeart(data)
  return toNumber(latest?.heartRate || latest?.heart_rate || latest?.bpm)
}

function dimension(key, name, score, max, desc, status = 'normal', advice = '') {
  const safeScore = clamp(score, 0, max)
  return {
    key,
    name,
    score: safeScore,
    max,
    value: Math.round((safeScore / max) * 100),
    desc,
    status,
    advice: advice || desc
  }
}

function buildHealthDimension(data) {
  const latest = latestHealth(data)
  if (!latest) {
    return dimension('health', '健康档案', 0, 20, '暂无身高、体重、BMI、BMR 记录', 'empty', '健康档案：建议先保存一次档案，生成 BMI 和 BMR。')
  }
  const bmi = toNumber(latest.bmi)
  const count = ['height', 'weight', 'bmi', 'bmr'].filter((key) => latest[key] !== null && latest[key] !== undefined && latest[key] !== '').length
  const score = count >= 4 && bmi >= 18.5 && bmi < 24 ? 20 : count >= 4 ? 16 : count >= 2 ? 10 : 5
  let advice = `健康档案：已记录 ${count}/4 项核心档案。`
  if (bmi) {
    if (bmi < 18.5) advice = `健康档案：BMI ${round(bmi, 1)}，体重偏低，建议关注营养摄入。`
    else if (bmi < 24) advice = `健康档案：BMI ${round(bmi, 1)}，处于常见参考范围。`
    else if (bmi < 28) advice = `健康档案：BMI ${round(bmi, 1)}，体重偏重，建议控制总热量并增加活动。`
    else advice = `健康档案：BMI ${round(bmi, 1)}，肥胖风险较高，建议持续记录并调整生活方式。`
  }
  return dimension('health', '健康档案', score, 20, `已记录 ${count}/4 项健康档案`, score >= 16 ? 'good' : 'normal', advice)
}

function buildDietDimension(data) {
  const today = asList(data).filter((item) => isToday(item.recordTime || item.record_time || item.createdAt))
  const mealTypes = new Set(today.map((item) => item.mealType || item.meal_type).filter(Boolean))
  const count = Math.max(mealTypes.size, today.length)
  const calories = today.reduce((sum, item) => sum + toNumber(item.calories), 0)
  const score = count >= 3 ? 20 : count === 2 ? 14 : count === 1 ? 8 : 0
  let advice = '饮食记录：今日暂无饮食记录，建议至少记录一餐。'
  if (count === 1) advice = '饮食记录：今日仅记录 1 餐，建议补齐其他餐次。'
  else if (count === 2) advice = '饮食记录：今日已记录 2 餐，饮食记录习惯正在建立。'
  else if (count >= 3) advice = `饮食记录：今日已记录 ${count} 条，记录较完整。`
  if (calories > 2600) advice = `饮食记录：今日约 ${Math.round(calories)} 千卡，热量偏高，建议关注晚餐和加餐。`
  return dimension('diet', '饮食记录', score, 20, count ? `今日已记录 ${count} 条` : '今日暂无饮食记录', score >= 14 ? 'good' : count ? 'normal' : 'empty', advice)
}

function buildHeartDimension(data) {
  const bpm = heartBpm(data)
  if (!bpm) {
    return dimension('heart', '心率状态', 0, 20, '暂无心率记录', 'empty', '心率状态：建议完成一次手动或摄像头测量。')
  }
  const score = bpm >= 60 && bpm <= 100 ? 20 : bpm <= 120 ? 13 : 8
  let advice = `心率状态：最近 ${bpm} BPM，处于常见静息参考范围。`
  if (bpm < 60) advice = `心率状态：最近 ${bpm} BPM，偏低，仅作日常参考。`
  else if (bpm > 120) advice = `心率状态：最近 ${bpm} BPM，明显偏高，建议休息后复测。`
  else if (bpm > 100) advice = `心率状态：最近 ${bpm} BPM，偏高，建议休息后复测。`
  return dimension('heart', '心率状态', score, 20, `最近心率 ${bpm} BPM`, score === 20 ? 'good' : 'warn', advice)
}

function buildStepDimension(stepSnapshot, settings) {
  const steps = toNumber(stepSnapshot?.total)
  const goal = Math.max(1, toNumber(settings?.stepGoal || settings?.step_goal || settings?.dailySteps || 9000))
  const ratio = steps / goal
  const score = ratio >= 1 ? 20 : ratio >= 0.7 ? 14 : ratio >= 0.4 ? 8 : steps > 0 ? 4 : 0
  const remaining = Math.max(0, goal - steps)
  let advice = '运动步数：暂无今日步数，建议先开启步数记录或补充活动。'
  if (ratio >= 1) advice = '运动步数：今日步数已达标，继续保持当前活动量。'
  else if (steps) advice = `运动步数：距离目标还差 ${remaining} 步，可以安排一段轻量步行。`
  return dimension('steps', '运动步数', score, 20, ratio >= 1 ? '今日步数已达标' : `距离目标还差 ${remaining} 步`, score >= 14 ? 'good' : steps ? 'normal' : 'empty', advice)
}

function buildPlanDimension(data) {
  const plans = asList(data)
  if (!plans.length) {
    return dimension('plans', '计划执行', 0, 20, '暂无健康计划', 'empty', '计划执行：建议创建一个可执行的小目标。')
  }
  const checked = plans.filter((item) => item.todayCheckedIn || item.today_checked_in || item.lastCheckinDate === todayText() || (Array.isArray(item.checkinDates) && item.checkinDates.includes(todayText()))).length
  const totalDays = plans.reduce((sum, item) => sum + toNumber(item.days), 0)
  const completedDays = plans.reduce((sum, item) => sum + toNumber(item.completedDays), 0)
  const percent = totalDays ? Math.round((completedDays / totalDays) * 100) : 0
  const score = checked > 0 ? (percent >= 90 ? 20 : percent >= 60 ? 16 : 12) : 10
  let advice = checked ? `计划执行：今日已打卡 ${checked} 个计划。` : '计划执行：今天还没有打卡，建议先完成一个计划。'
  if (checked && percent < 60) advice = `计划执行：整体完成度 ${percent}%，先保持连续打卡。`
  else if (checked && percent >= 90) advice = `计划执行：整体完成度 ${percent}%，计划执行优秀。`
  return dimension('plans', '计划执行', score, 20, checked ? `今日已打卡 ${checked} 个计划` : '有计划但今日未打卡', checked ? 'good' : 'normal', advice)
}

function buildLevel(score, hasData) {
  if (!hasData) {
    return {
      level: '数据不足',
      summary: '完善健康档案、饮食、心率、步数和计划记录后可生成健康指数'
    }
  }
  if (score >= 85) {
    return { level: '优秀', summary: '整体记录较完整，继续保持当前节奏' }
  }
  if (score >= 70) {
    return { level: '良好', summary: '整体状态良好，仍有部分维度可以继续完善' }
  }
  if (score >= 50) {
    return { level: '一般', summary: '建议补齐缺失记录，并优先完成运动和计划目标' }
  }
  return { level: '待改善', summary: '当前数据较少或多项未达标，请先从记录和打卡开始' }
}

function buildSuggestions(dimensions) {
  const order = ['steps', 'heart', 'diet', 'plans', 'health']
  const map = new Map(dimensions.map((item) => [item.key, item]))
  const list = order.map((key) => map.get(key)?.advice).filter(Boolean)
  list.push('健康指数仅用于日常管理，不作为医学诊断。')
  return list.slice(0, 6)
}

function readHistory() {
  try {
    return uni.getStorageSync(HISTORY_KEY) || []
  } catch (error) {
    return []
  }
}

function saveHistory(score) {
  if (!score) return readHistory()
  const today = todayText()
  const next = readHistory().filter((item) => item.date !== today)
  next.push({ date: today, score })
  const trimmed = next.sort((a, b) => a.date.localeCompare(b.date)).slice(-7)
  try {
    uni.setStorageSync(HISTORY_KEY, trimmed)
  } catch (error) {}
  return trimmed
}

export async function loadHealthScoreDetail() {
  const [health, diet, heart, plans, settings] = await Promise.all([
    safeLoad(fetchHealthRecord, {}),
    safeLoad(fetchDietPageData, []),
    safeLoad(fetchHeartData, {}),
    safeLoad(fetchPlanList, []),
    safeLoad(fetchSettingsData, {})
  ])
  const steps = getStepHistorySnapshot('week')
  const dimensions = [
    buildHealthDimension(health),
    buildDietDimension(diet),
    buildHeartDimension(heart),
    buildStepDimension(steps, settings),
    buildPlanDimension(plans)
  ]
  const totalScore = dimensions.reduce((sum, item) => sum + item.score, 0)
  const validDimensionCount = dimensions.filter((item) => item.status !== 'empty').length
  const hasData = validDimensionCount >= 2
  const score = hasData ? Math.round(totalScore) : null
  const level = buildLevel(score || 0, hasData)
  const history = score ? saveHistory(score) : readHistory()
  return {
    score,
    totalScore: score,
    level: level.level,
    statusText: level.level,
    summary: level.summary,
    dimensions,
    suggestions: buildSuggestions(dimensions),
    missing: dimensions.filter((item) => item.status === 'empty').map((item) => item.name),
    history,
    hasData,
    validDimensionCount
  }
}

export default {
  loadHealthScoreDetail
}
