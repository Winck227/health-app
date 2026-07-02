<template>
	<AppShell active="diet">
		<view class="page">
			<view class="head">
				<text class="h1">饮食记录</text>
				<text class="sub">食物库是模板，也可以自定义输入食物</text>
			</view>

			<view class="u-card sync-card">
				<view class="sync-head">
					<view>
						<text class="section-title">食物库模板</text>
						
					</view>
					<view class="sync-btn" @click="refreshFoodLibrary">检查更新</view>
				</view>
				<text class="sync-note">模板只用于快速估算；自定义食物会作为本次饮食记录保存。</text>
			</view>

			<view class="u-card card">
				<text class="label mt">选择餐次的用餐时间</text>
				<picker mode="time" :value="form.time" @change="onMealTimeChange">
					<view class="field">
						<view class="field-ico">时</view>
						<text class="field-val">{{ form.time || '选择用餐时间' }}</text>
						<view class="field-ico">选</view>
					</view>
				</picker>

				<text class="label mt">记录方式</text>
				<view class="mode-row">
					<view class="mode-chip" :class="{ active: inputMode === 'template' }"
						@click="inputMode = 'template'">选模板</view>
					<view class="mode-chip" :class="{ active: inputMode === 'custom' }" @click="inputMode = 'custom'">
						自定义</view>
				</view>

				<template v-if="inputMode === 'template'">
					<text class="label mt">搜索食物模板</text>
					<view class="search-bar">
						<input v-model.trim="searchText" class="search-input" type="text" placeholder="输入食物名称、别名或分类" />
						<text class="search-count">{{ filteredFoods.length }} 项</text>
					</view>

					<scroll-view class="food-list" scroll-y>
						<view v-for="food in filteredFoods" :key="food.id" class="food-item"
							:class="{ active: food.id === form.foodId }" @click="selectFood(food)">
							<view>
								<text class="food-name">{{ food.name }}</text>
								<text class="food-meta">{{ food.category }} · {{ food.kcalPer100g }} 千卡 / 100g</text>
							</view>
							<text class="food-tag">{{ food.id === form.foodId ? '已选' : '选择' }}</text>
						</view>
						<view v-if="!filteredFoods.length" class="food-empty">
							<text class="food-empty-title">暂无匹配模板</text>
							<text class="food-empty-sub">可以切换到“自定义”手动输入食物</text>
						</view>
					</scroll-view>

					<view v-if="selectedFood" class="selected-card">
						<view class="selected-head">
							<text class="selected-title">当前模板</text>
							<text class="selected-kcal">{{ selectedFood.kcalPer100g }} 千卡 / 100g</text>
						</view>
						<text class="selected-name">{{ selectedFood.name }}</text>
						<text class="selected-meta">{{ selectedFood.category }} · 营养值来自当前食物库模板</text>
					</view>
				</template>

				<template v-else>
					<text class="label mt">自定义食物名称</text>
					<view class="field">
						<input v-model.trim="form.customFoodName" class="weight-input" maxlength="30"
							placeholder="例如 牛肉面 / 奶茶 / 自制沙拉" placeholder-class="placeholder-input" />
					</view>
					<text class="label mt">本次总热量（千卡）</text>
					<view class="field">
						<input v-model="form.customCalories" class="weight-input" type="number" maxlength="5"
							placeholder="例如 450" placeholder-class="placeholder-input" />
						<text class="unit">千卡</text>
					</view>
					<text class="custom-help">自定义食物只保存名称、重量和热量；蛋白质等营养项没有模板时显示为 --。</text>
				</template>

				<text class="label mt">摄入重量（克）</text>
				<view class="field">
					<input v-model="form.weight" class="weight-input" type="number" maxlength="5" placeholder="输入重量"
						placeholder-class="placeholder-input" />
					<text class="unit">克</text>
				</view>

				<text class="label mt">备注</text>
				<view class="remark-box">
					<textarea v-model.trim="form.remark" class="remark-input" maxlength="120"
						placeholder="例如 少油少盐 / 外卖 / 运动后加餐" placeholder-class="placeholder-input" />
				</view>

				<view class="btn" :class="{ disabled: !canSubmit }" @click="handleSubmit">记录数据</view>
			</view>

			<view class="u-card estimate-card">
				<view class="section-head">
					<text class="section-title">本次估算</text>
					<text class="section-sub">{{ estimateTitle }}</text>
				</view>
				<view class="estimate-main">
					<text class="estimate-kcal">{{ formatValue(nutritionEstimate?.calories, 0) }}</text>
					<text class="estimate-unit">千卡</text>
				</view>
				<view class="nutrition-grid">
					<view v-for="item in nutritionItems" :key="item.key" class="nutrition-item">
						<text class="nutrition-label">{{ item.label }}</text>
						<text
							class="nutrition-value">{{ formatValue(nutritionEstimate?.[item.key], item.digits) }}{{ item.unit }}</text>
					</view>
				</view>
			</view>

			<view class="u-card total-card">
				<view class="section-head">
					<text class="section-title">今日总热量</text>
					<text class="section-sub">{{ todayRecords.length }} 条记录</text>
				</view>
				<view class="total-row">
					<text class="total-value">{{ todayTotals.calories }}</text>
					<text class="total-unit">千卡</text>
				</view>
				<view class="nutrition-grid compact">
					<view v-for="item in nutritionItems" :key="`today-${item.key}`" class="nutrition-item">
						<text class="nutrition-label">{{ item.label }}</text>
						<text
							class="nutrition-value">{{ formatValue(todayTotals[item.key], item.digits) }}{{ item.unit }}</text>
					</view>
				</view>
			</view>

			<view class="history">
				<view class="section-head history-head">
					<text class="section-title">饮食记录</text>
					<text class="section-sub">模板食物和自定义食物都会进入记录</text>
				</view>

				<EmptyState v-if="historyError" title="加载失败" :message="historyError || '暂无数据'" />
				<EmptyState v-else-if="!records.length" title="暂无数据" message="还没有饮食记录" />
				<view v-else>
					<view v-for="item in recordItems" :key="item.id" class="record u-card">
						<view class="record-top">
							<view>
								<text class="record-meal">{{ mealTypeLabel(item.mealType) }}</text>
								<text class="record-name">{{ item.foodName }}</text>
							</view>
							<view class="record-right">
								<text class="record-kcal">{{ item.calories }} 千卡</text>
								<text class="record-del" @click="handleDelete(item)">删除</text>
							</view>
						</view>
						<text class="record-time">{{ item.recordTime }}</text>
						<text class="record-weight">重量 {{ item.weight }} g</text>
						<text v-if="item.remark" class="record-remark">备注：{{ item.remark }}</text>
						<view class="record-nutrition" v-if="item.nutrition">
							<text>蛋白质 {{ formatValue(item.nutrition?.protein) }}g</text>
							<text>脂肪 {{ formatValue(item.nutrition?.fat) }}g</text>
							<text>碳水 {{ formatValue(item.nutrition?.carb) }}g</text>
							<text>钠 {{ formatValue(item.nutrition?.sodium, 0) }}mg</text>
							<text>膳食纤维 {{ formatValue(item.nutrition?.fiber) }}g</text>
						</view>
						<text v-else class="record-custom-tip">自定义食物：仅统计热量</text>
					</view>
				</view>
			</view>
		</view>
	</AppShell>
</template>

<script setup>
	import {
		computed,
		reactive,
		ref
	} from 'vue'
	import {
		onShow
	} from '@dcloudio/uni-app'
	import AppShell from '@/components/app/AppShell.vue'
	import EmptyState from '@/components/ui/EmptyState.vue'
	import {
		aggregateNutrition,
		calculateFoodNutrition,
		estimateRecordNutrition,
		loadFoodNutritionLibrary,
		searchFoodNutrition,
		syncFoodNutritionLibrary
	} from '@/services/foodNutrition.js'
	import {
		deleteDietRecord,
		fetchDietPageData,
		logUserAction,
		submitDietRecord
	} from '@/services/healthApi.js'

	const mealTypeOptions = [{
			label: '早餐',
			value: 'breakfast'
		},
		{
			label: '午餐',
			value: 'lunch'
		},
		{
			label: '晚餐',
			value: 'dinner'
		},
		{
			label: '加餐',
			value: 'snack'
		}
	]

	const nutritionItems = [{
			key: 'protein',
			label: '蛋白质',
			unit: 'g',
			digits: 1
		},
		{
			key: 'fat',
			label: '脂肪',
			unit: 'g',
			digits: 1
		},
		{
			key: 'carb',
			label: '碳水',
			unit: 'g',
			digits: 1
		},
		{
			key: 'sodium',
			label: '钠',
			unit: 'mg',
			digits: 0
		},
		{
			key: 'fiber',
			label: '膳食纤维',
			unit: 'g',
			digits: 1
		}
	]

	const foodLibrary = reactive({
		version: '',
		updatedAt: '',
		sourceLabel: '默认食物库',
		foods: []
	})
	const records = ref([])
	const historyError = ref('')
	const searchText = ref('')
	const inputMode = ref('template')
	const form = reactive({
		mealType: inferMealTypeByTime(currentTimeText()),
		foodId: '',
		weight: '',
		customFoodName: '',
		customCalories: '',
		time: currentTimeText(),
		remark: ''
	})

	const selectedFood = computed(() => foodLibrary.foods.find((item) => item.id === form.foodId) || null)
	const filteredFoods = computed(() => searchFoodNutrition(searchText.value, foodLibrary.foods))
	const templateEstimate = computed(() => calculateFoodNutrition(selectedFood.value, form.weight))
	const customEstimate = computed(() => {
		const calories = Number(form.customCalories || 0)
		const weight = Number(form.weight || 0)
		if (!form.customFoodName.trim() || !Number.isFinite(calories) || calories <= 0 || !Number.isFinite(
			weight) || weight <= 0) return null
		return {
			calories: Math.round(calories),
			protein: null,
			fat: null,
			carb: null,
			sodium: null,
			fiber: null
		}
	})
	const nutritionEstimate = computed(() => inputMode.value === 'custom' ? customEstimate.value : templateEstimate.value)
	const estimateTitle = computed(() => {
		if (inputMode.value === 'custom') return form.customFoodName ?
			`${form.customFoodName} · ${form.weight || 0}g` : '请输入自定义食物'
		return selectedFood.value ? `${selectedFood.value.name} · ${form.weight || 0}g` : '请选择食物模板'
	})
	const recordItems = computed(() => records.value.map((item) => ({
		...item,
		nutrition: estimateRecordNutrition(item, foodLibrary.foods)
	})))
	const todayRecords = computed(() => recordItems.value.filter((item) => isToday(item.recordTime)))
	const todayTotals = computed(() => aggregateNutrition(todayRecords.value, foodLibrary.foods))
	const canSubmit = computed(() => {
		const weightOk = Number(form.weight) > 0
		if (inputMode.value === 'custom') {
			return Boolean(form.mealType) && Boolean(form.customFoodName.trim()) && weightOk && Number(form
				.customCalories) > 0
		}
		return Boolean(form.mealType) && Boolean(selectedFood.value) && weightOk && Boolean(nutritionEstimate
			.value)
	})

	async function loadPageData() {
		Object.assign(foodLibrary, loadFoodNutritionLibrary())
		ensureSelectedFood()
		await loadRecords()
	}

	async function loadRecords() {
		try {
			historyError.value = ''
			const data = await fetchDietPageData()
			records.value = Array.isArray(data?.logs) ? data.logs.map(normalizeRecord).filter(Boolean) : []
		} catch (error) {
			historyError.value = error.message || '加载失败'
			records.value = []
		}
	}

	async function refreshFoodLibrary() {
		try {
			const result = await syncFoodNutritionLibrary()
			Object.assign(foodLibrary, result)
			ensureSelectedFood()
			uni.showToast({
				title: result.updated ? '食物库已更新' : '已是最新',
				icon: 'none'
			})
		} catch (error) {
			uni.showToast({
				title: error.message || '获取失败',
				icon: 'none'
			})
		}
	}

	onShow(() => {
		logUserAction('view_diet_page')
		loadPageData()
	})

	function selectMealType(value) {
		form.mealType = mealTypeOptions.some((item) => item.value === value) ? value : 'snack'
	}

	function onMealTimeChange(event) {
		form.time = String(event.detail?.value || currentTimeText()).trim()
		form.mealType = inferMealTypeByTime(form.time)
	}

	function selectFood(food) {
		inputMode.value = 'template'
		form.foodId = food.id
		logUserAction('select_food', {
			foodId: food.id,
			foodName: food.name
		})
	}

	async function handleSubmit() {
		if (!canSubmit.value) return
		const foodName = inputMode.value === 'custom' ? form.customFoodName.trim() : selectedFood.value.name
		const calories = Number(nutritionEstimate.value?.calories || 0)
		try {
			await submitDietRecord({
				mealType: form.mealType,
				foodName,
				weight: Number(form.weight),
				calories,
				recordTime: getRecordTime(form.time),
				remark: form.remark.trim()
			})
			await loadRecords()
			logUserAction('submit_diet_record', {
				mealType: form.mealType,
				foodName,
				inputMode: inputMode.value
			})
			form.weight = ''
			form.customFoodName = ''
			form.customCalories = ''
			form.time = currentTimeText()
			form.mealType = inferMealTypeByTime(form.time)
			form.remark = ''
			uni.showToast({
				title: '记录成功',
				icon: 'success'
			})
		} catch (error) {
			uni.showToast({
				title: error.message || '记录失败',
				icon: 'none'
			})
		}
	}

	async function handleDelete(record) {
		const confirmed = await confirmAction('确认删除这条饮食记录吗？')
		if (!confirmed) return
		try {
			await deleteDietRecord(record.id)
			await loadRecords()
			uni.showToast({
				title: '已删除',
				icon: 'none'
			})
		} catch (error) {
			uni.showToast({
				title: error.message || '删除失败',
				icon: 'none'
			})
		}
	}

	function ensureSelectedFood() {
		if (foodLibrary.foods.some((item) => item.id === form.foodId)) return
		form.foodId = foodLibrary.foods[0]?.id || ''
	}

	function normalizeRecord(item) {
		const source = item && typeof item === 'object' ? item : {}
		const id = String(source.id || `diet-${Date.now()}`)
		const mealType = String(source.mealType || source.meal || 'breakfast').trim()
		const foodName = String(source.foodName || '').trim()
		const weight = Number(source.weight || 0)
		const calories = Number(source.calories ?? source.kcal ?? 0)
		const recordTime = String(source.recordTime || (source.date && source.time ? `${source.date} ${source.time}` : ''))
			.trim()
		const remark = String(source.remark || source.note || source.memo || '').trim()

		if (!foodName || !recordTime) return null

		return {
			id,
			mealType,
			foodName,
			weight,
			calories: Number.isFinite(calories) ? Math.round(calories) : 0,
			recordTime,
			remark
		}
	}

	function isToday(recordTime) {
		return String(recordTime || '').startsWith(todayText())
	}

	function getRecordTime(timeText) {
		return `${todayText()} ${timeText || currentTimeText()}`
	}

	function formatValue(value, digits = 1) {
		if (value === null || value === undefined || value === '') return '--'
		const num = Number(value)
		if (!Number.isFinite(num)) return '--'
		if (digits <= 0) return String(Math.round(num))
		return Number.isInteger(num) ? String(num) : num.toFixed(digits).replace(/\.0+$/, '')
	}

	function inferMealTypeByTime(timeText) {
		const [hourText, minuteText] = String(timeText || '').split(':')
		const hour = Number(hourText)
		const minute = Number(minuteText || 0)
		if (!Number.isFinite(hour) || hour < 0 || hour > 23) return 'snack'
		const minutes = hour * 60 + (Number.isFinite(minute) ? minute : 0)
		if (minutes >= 5 * 60 && minutes < 10 * 60) return 'breakfast'
		if (minutes >= 11 * 60 + 30 && minutes < 14 * 60) return 'lunch'
		if (minutes >= 17 * 60 + 30 && minutes < 21 * 60) return 'dinner'
		return 'snack'
	}

	function mealTypeLabel(value) {
		return mealTypeOptions.find((item) => item.value === value)?.label || '餐次'
	}

	function confirmAction(content) {
		return new Promise((resolve) => {
			uni.showModal({
				title: '提示',
				content,
				confirmText: '删除',
				success: (result) => resolve(Boolean(result.confirm)),
				fail: () => resolve(false)
			})
		})
	}

	function currentTimeText() {
		const date = new Date()
		return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
	}

	function todayText() {
		const date = new Date()
		return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
	}
</script>

<style scoped>
	.page {
		padding: 20rpx 36rpx 42rpx;
		box-sizing: border-box;
	}

	.head {
		margin-bottom: 20rpx;
	}

	.h1 {
		display: block;
		font-size: 46rpx;
		color: var(--c-title);
		font-weight: 1000;
	}

	.sub {
		display: block;
		margin-top: 10rpx;
		font-size: 25rpx;
		color: var(--c-muted);
		font-weight: 700;
	}

	.sync-card,
	.card,
	.estimate-card,
	.total-card {
		margin-top: 22rpx;
		border-radius: 48rpx;
		padding: 30rpx;
	}

	.sync-head,
	.section-head,
	.selected-head,
	.record-top {
		display: flex;
		justify-content: space-between;
		align-items: flex-start;
		gap: 18rpx;
	}

	.section-title {
		display: block;
		font-size: 30rpx;
		color: var(--c-title);
		font-weight: 1000;
	}

	.section-sub,
	.sync-sub,
	.sync-note,
	.custom-help {
		display: block;
		margin-top: 8rpx;
		font-size: 23rpx;
		color: var(--c-muted);
		line-height: 1.5;
		font-weight: 700;
	}

	.sync-btn,
	.btn {
		flex-shrink: 0;
		padding: 14rpx 22rpx;
		border-radius: 9999rpx;
		background: var(--c-primary);
		color: #fff;
		font-size: 24rpx;
		font-weight: 1000;
	}

	.btn {
		margin-top: 26rpx;
		text-align: center;
		padding: 24rpx;
		font-size: 30rpx;
	}

	.btn.disabled {
		opacity: 0.45;
	}

	.label {
		display: block;
		margin-top: 4rpx;
		margin-bottom: 12rpx;
		font-size: 24rpx;
		color: var(--c-muted);
		font-weight: 900;
	}

	.mt {
		margin-top: 24rpx;
	}

	.field,
	.search-bar {
		min-height: 88rpx;
		border-radius: 28rpx;
		background: #F8FAFC;
		display: flex;
		align-items: center;
		padding: 0 22rpx;
		box-sizing: border-box;
		gap: 14rpx;
	}

	.field-ico {
		width: 54rpx;
		height: 54rpx;
		border-radius: 18rpx;
		background: rgba(18, 183, 106, 0.12);
		color: var(--c-primary);
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 22rpx;
		font-weight: 1000;
	}

	.field-val {
		flex: 1;
		font-size: 28rpx;
		color: var(--c-title);
		font-weight: 900;
	}

	.search-input,
	.weight-input {
		flex: 1;
		font-size: 28rpx;
		color: var(--c-title);
		font-weight: 800;
	}

	.remark-box {
		min-height: 140rpx;
		border-radius: 28rpx;
		background: #F8FAFC;
		padding: 20rpx 22rpx;
		box-sizing: border-box;
	}

	.remark-input {
		width: 100%;
		height: 110rpx;
		font-size: 27rpx;
		line-height: 1.5;
		color: var(--c-title);
		font-weight: 800;
	}

	.search-count,
	.unit {
		font-size: 24rpx;
		color: var(--c-muted);
		font-weight: 900;
	}

	.mode-row {
		display: flex;
		gap: 14rpx;
	}

	.mode-chip {
		flex: 1;
		text-align: center;
		padding: 18rpx;
		border-radius: 9999rpx;
		background: #F8FAFC;
		color: var(--c-muted);
		font-size: 26rpx;
		font-weight: 1000;
	}

	.mode-chip.active {
		background: rgba(18, 183, 106, 0.14);
		color: var(--c-primary);
	}

	.food-list {
		margin-top: 16rpx;
		max-height: 360rpx;
	}

	.food-item {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 18rpx;
		padding: 20rpx;
		border-radius: 28rpx;
		background: #F8FAFC;
		margin-bottom: 14rpx;
	}

	.food-item.active {
		background: rgba(18, 183, 106, 0.12);
	}

	.food-name,
	.selected-name,
	.record-name {
		display: block;
		font-size: 29rpx;
		color: var(--c-title);
		font-weight: 1000;
	}

	.food-meta,
	.selected-meta,
	.record-time,
	.record-weight,
	.record-remark,
	.record-custom-tip {
		display: block;
		margin-top: 8rpx;
		font-size: 22rpx;
		color: var(--c-muted);
		line-height: 1.45;
		font-weight: 700;
	}

	.food-tag {
		flex-shrink: 0;
		font-size: 23rpx;
		color: var(--c-primary);
		font-weight: 1000;
	}

	.food-empty {
		padding: 30rpx;
		text-align: center;
		color: var(--c-muted);
	}

	.food-empty-title {
		display: block;
		font-size: 28rpx;
		font-weight: 1000;
		color: var(--c-title);
	}

	.food-empty-sub {
		display: block;
		margin-top: 8rpx;
		font-size: 23rpx;
		color: var(--c-muted);
	}

	.selected-card {
		margin-top: 16rpx;
		padding: 22rpx;
		border-radius: 30rpx;
		background: #FFF7ED;
	}

	.selected-title {
		font-size: 22rpx;
		color: var(--c-muted);
		font-weight: 900;
	}

	.selected-kcal {
		font-size: 22rpx;
		color: var(--c-orange);
		font-weight: 1000;
	}

	.estimate-main,
	.total-row {
		display: flex;
		align-items: baseline;
		justify-content: center;
		gap: 12rpx;
		margin-top: 18rpx;
	}

	.estimate-kcal,
	.total-value {
		font-size: 72rpx;
		line-height: 1;
		color: var(--c-title);
		font-weight: 1000;
	}

	.estimate-unit,
	.total-unit {
		font-size: 25rpx;
		color: var(--c-muted);
		font-weight: 900;
	}

	.nutrition-grid {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 14rpx;
		margin-top: 22rpx;
	}

	.nutrition-grid.compact {
		grid-template-columns: repeat(3, minmax(0, 1fr));
	}

	.nutrition-item {
		padding: 18rpx;
		border-radius: 26rpx;
		background: #F8FAFC;
	}

	.nutrition-label {
		display: block;
		font-size: 22rpx;
		color: var(--c-muted);
		font-weight: 800;
	}

	.nutrition-value {
		display: block;
		margin-top: 6rpx;
		font-size: 27rpx;
		color: var(--c-title);
		font-weight: 1000;
	}

	.history {
		margin-top: 28rpx;
	}

	.history-head {
		margin-bottom: 16rpx;
	}

	.record {
		margin-bottom: 16rpx;
		border-radius: 36rpx;
		padding: 24rpx;
	}

	.record-meal {
		display: inline-block;
		margin-bottom: 8rpx;
		padding: 6rpx 12rpx;
		border-radius: 9999rpx;
		background: rgba(18, 183, 106, 0.12);
		color: var(--c-primary);
		font-size: 21rpx;
		font-weight: 1000;
	}

	.record-right {
		text-align: right;
	}

	.record-kcal {
		display: block;
		font-size: 26rpx;
		color: var(--c-orange);
		font-weight: 1000;
	}

	.record-del {
		display: block;
		margin-top: 10rpx;
		font-size: 22rpx;
		color: #F04438;
		font-weight: 900;
	}

	.record-nutrition {
		margin-top: 12rpx;
		display: flex;
		flex-wrap: wrap;
		gap: 10rpx;
	}

	.record-nutrition text {
		padding: 8rpx 12rpx;
		border-radius: 9999rpx;
		background: #F8FAFC;
		color: var(--c-muted);
		font-size: 21rpx;
		font-weight: 800;
	}
</style>
