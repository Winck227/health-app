import defaultFoodNutrition from '@/static/data/food-nutrition.default.json'
import { request } from '@/services/request.js'

const STORAGE_VERSION_KEY = 'food_nutrition_version'
const STORAGE_LIBRARY_KEY = 'food_nutrition_library'

// 功能注释：把任意值转成可显示文本。
function toText(value) {
  return value === null || value === undefined ? '' : String(value).trim()
}

// 功能注释：把任意值转成数值。
function toNumber(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num : NaN
}

// 功能注释：统一保留一位小数，便于营养估算展示。
function roundOne(value) {
  return Math.round(Number(value) * 10) / 10
}

// 功能注释：克隆对象，避免引用被页面直接改写。
function clone(value) {
  return JSON.parse(JSON.stringify(value))
}

// 功能注释：把数字格式化成页面可读值。
function formatEstimateValue(value, digits = 1) {
  if (!Number.isFinite(Number(value))) return null
  const next = Number(value)
  const rounded = Number.isInteger(next) ? next : Number(next.toFixed(digits))
  return rounded
}

// 功能注释：把单条食物数据规范成统一格式。
function normalizeFoodItem(item, index) {
  const source = item && typeof item === 'object' ? item : {}
  const id = toText(source.id || `food-${index + 1}`)
  const name = toText(source.name)
  const category = toText(source.category || '其他')
  const aliases = Array.isArray(source.aliases) ? source.aliases.map(toText).filter(Boolean) : []
  const kcalPer100g = toNumber(source.kcalPer100g)
  const proteinPer100g = toNumber(source.proteinPer100g)
  const fatPer100g = toNumber(source.fatPer100g)
  const carbPer100g = toNumber(source.carbPer100g)
  const sodiumMgPer100g = toNumber(source.sodiumMgPer100g)
  const fiberPer100g = toNumber(source.fiberPer100g)

  if (!id || !name) return null
  if ([kcalPer100g, proteinPer100g, fatPer100g, carbPer100g, sodiumMgPer100g, fiberPer100g].some((value) => !Number.isFinite(value) || value < 0)) {
    throw new Error(`食物库条目格式错误：${name}`)
  }

  return {
    id,
    name,
    category,
    aliases,
    kcalPer100g: formatEstimateValue(kcalPer100g, 0),
    proteinPer100g: formatEstimateValue(proteinPer100g),
    fatPer100g: formatEstimateValue(fatPer100g),
    carbPer100g: formatEstimateValue(carbPer100g),
    sodiumMgPer100g: formatEstimateValue(sodiumMgPer100g, 0),
    fiberPer100g: formatEstimateValue(fiberPer100g)
  }
}

// 功能注释：把接口或静态 JSON 统一成食物库对象。
function normalizeFoodLibrary(payload) {
  const source = Array.isArray(payload)
    ? { foods: payload }
    : (payload && typeof payload === 'object' ? payload : {})
  const rawFoods = Array.isArray(source.foods) ? source.foods : Array.isArray(source.items) ? source.items : []
  const foods = rawFoods.map(normalizeFoodItem)

  if (!foods.length) {
    throw new Error('食物库为空')
  }

  const version = toText(source.version || source.food_nutrition_version || defaultFoodNutrition.version)
  return {
    version: version || toText(defaultFoodNutrition.version) || 'local-default',
    updatedAt: toText(source.updatedAt || source.updated_at || defaultFoodNutrition.updatedAt || ''),
    source: toText(source.source || defaultFoodNutrition.source || 'local'),
    foods
  }
}

// 功能注释：读取本地缓存的食物库。
function readStoredFoodLibrary() {
  try {
    const storedLibrary = uni.getStorageSync(STORAGE_LIBRARY_KEY)
    const storedVersion = toText(uni.getStorageSync(STORAGE_VERSION_KEY))
    const normalized = normalizeFoodLibrary(storedLibrary)
    if (storedVersion && normalized.version !== storedVersion) {
      normalized.version = storedVersion
    }
    return {
      ...normalized,
      sourceLabel: '本地缓存'
    }
  } catch (error) {
    return null
  }
}

// 功能注释：读取内置默认食物库。
function getDefaultFoodLibrary() {
  const normalized = normalizeFoodLibrary(defaultFoodNutrition)
  return {
    ...normalized,
    sourceLabel: '默认食物库'
  }
}

// 功能注释：把食物库写入本地缓存。
function setStorageAsync(key, data) {
  return new Promise((resolve, reject) => {
    try {
      uni.setStorage({
        key,
        data,
        success: resolve,
        fail: reject
      })
    } catch (error) {
      reject(error)
    }
  })
}

// 功能注释：保存同步后的食物库版本和数据。
async function saveFoodLibrary(library) {
  const payload = {
    version: library.version,
    updatedAt: library.updatedAt,
    source: library.source,
    foods: clone(library.foods)
  }
  await Promise.all([
    setStorageAsync(STORAGE_VERSION_KEY, library.version),
    setStorageAsync(STORAGE_LIBRARY_KEY, payload)
  ])
  return {
    ...payload,
    sourceLabel: '本地缓存'
  }
}

// 功能注释：读取当前可用的食物库，优先本地缓存，其次用默认库兜底。
export function loadFoodNutritionLibrary() {
  return readStoredFoodLibrary() || getDefaultFoodLibrary()
}

// 功能注释：对比版本并同步最新食物库。
export async function syncFoodNutritionLibrary() {
  const versionResult = await request('/api/foods/version')
  const remoteVersion = toText(
    typeof versionResult === 'string'
      ? versionResult
      : versionResult?.version || versionResult?.food_nutrition_version || ''
  )
  if (!remoteVersion) {
    throw new Error('食物库版本无效')
  }

  const storedLibrary = readStoredFoodLibrary()
  if (storedLibrary && storedLibrary.version === remoteVersion && storedLibrary.foods.length) {
    return {
      ...storedLibrary,
      sourceLabel: '本地缓存',
      updated: false
    }
  }

  const libraryResult = await request('/api/foods')
  const normalized = normalizeFoodLibrary(libraryResult)
  if (normalized.version !== remoteVersion) {
    normalized.version = remoteVersion
  }
  const saved = await saveFoodLibrary(normalized)
  return {
    ...saved,
    updated: true,
    sourceLabel: '接口同步'
  }
}

// 功能注释：按搜索词过滤食物。
export function searchFoodNutrition(keyword, foods = []) {
  const qry = toText(keyword).toLowerCase()
  if (!qry) return foods.slice()
  return foods.filter((item) => {
    const pool = [item.name, item.category, ...(Array.isArray(item.aliases) ? item.aliases : [])]
      .map((value) => toText(value).toLowerCase())
    return pool.some((text) => text.includes(qry))
  })
}

// 功能注释：按重量计算当前食物的热量与营养估算。
export function calculateFoodNutrition(food, weight) {
  const source = food && typeof food === 'object' ? food : null
  const grams = toNumber(weight)
  if (!source || !Number.isFinite(grams) || grams <= 0) return null

  const factor = grams / 100
  return {
    calories: Math.round((source.kcalPer100g || 0) * factor),
    protein: roundOne((source.proteinPer100g || 0) * factor),
    fat: roundOne((source.fatPer100g || 0) * factor),
    carb: roundOne((source.carbPer100g || 0) * factor),
    sodium: Math.round((source.sodiumMgPer100g || 0) * factor),
    fiber: roundOne((source.fiberPer100g || 0) * factor)
  }
}

// 功能注释：根据记录反查对应食物，再估算营养值。
export function estimateRecordNutrition(record, foods = []) {
  const source = record && typeof record === 'object' ? record : {}
  if (source.nutrition && typeof source.nutrition === 'object') {
    return source.nutrition
  }
  const key = toText(source.foodId || source.foodName)
  const food = foods.find((item) => {
    const aliases = Array.isArray(item.aliases) ? item.aliases : []
    return item.id === key || item.name === key || aliases.includes(key)
  })
  if (!food) {
    return null
  }
  return calculateFoodNutrition(food, source.weight)
}

// 功能注释：汇总当天全部饮食记录的营养数据。
export function aggregateNutrition(records = [], foods = []) {
  const totals = {
    calories: 0,
    protein: 0,
    fat: 0,
    carb: 0,
    sodium: 0,
    fiber: 0
  }

  records.forEach((record) => {
    const nutrition = record && typeof record === 'object' && record.nutrition && typeof record.nutrition === 'object'
      ? record.nutrition
      : estimateRecordNutrition(record, foods)
    if (nutrition) {
      totals.calories += Number(nutrition.calories || 0)
      totals.protein += Number(nutrition.protein || 0)
      totals.fat += Number(nutrition.fat || 0)
      totals.carb += Number(nutrition.carb || 0)
      totals.sodium += Number(nutrition.sodium || 0)
      totals.fiber += Number(nutrition.fiber || 0)
      return
    }

    totals.calories += Number(record.calories || 0)
  })

  return {
    calories: Math.round(totals.calories),
    protein: roundOne(totals.protein),
    fat: roundOne(totals.fat),
    carb: roundOne(totals.carb),
    sodium: Math.round(totals.sodium),
    fiber: roundOne(totals.fiber)
  }
}

export default {
  loadFoodNutritionLibrary,
  syncFoodNutritionLibrary,
  searchFoodNutrition,
  calculateFoodNutrition,
  estimateRecordNutrition,
  aggregateNutrition
}
