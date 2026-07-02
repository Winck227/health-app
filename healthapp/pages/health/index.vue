<template>
	<view class="page">
		<TopBar title="健康记录" />
		<EmptyState v-if="pageError" title="加载失败" :message="pageError || '暂无数据'" />
		<template v-else>
			<text class="subhead">记录身高和体重，自动计算 BMI 与基础代谢</text>

			<view v-if="record.current.height || record.current.weight" class="u-card latest-card">
				<view class="card-head">
					<text class="section-title">最近一次健康档案</text>
					<text class="section-sub">{{ record.current.recordTime || '--' }}</text>
				</view>
				<view class="metrics">
					<view class="metric">
						<text class="metric-label">身高</text>
						<text class="metric-value">{{ record.current.height ?? '--' }}</text>
					</view>
					<view class="metric">
						<text class="metric-label">体重</text>
						<text class="metric-value">{{ record.current.weight ?? '--' }}</text>
					</view>
					<view class="metric green">
						<text class="metric-label light">BMI</text>
						<text class="metric-value white">{{ record.current.bmi ?? '--' }}</text>
					</view>
					<view class="metric">
						<text class="metric-label">BMR</text>
						<text class="metric-value">{{ record.current.bmr ?? '--' }}</text>
					</view>
				</view>
			</view>

			<view v-else class="u-card empty-card">
				<EmptyState title="暂无数据" message="先保存一条健康档案" />
			</view>

			<view class="u-card form-card">
				<text class="section-title">新增 / 保存健康档案</text>
				<text class="section-sub">身高和体重仅写入 health_record，不写 user 表</text>
				<view class="field">
					<text class="label">身高（厘米）</text>
					<input v-model="height" class="input" type="number" placeholder="例如 176" />
				</view>
				<view class="field">
					<text class="label">体重（千克）</text>
					<input v-model="weight" class="input" type="digit" placeholder="例如 69.8" />
				</view>
				<view class="btn" :class="{ disabled: !canSave }" @click="saveRecord">保存档案</view>
			</view>

			<view class="u-card trend-card">
				<view class="card-head">
					<text class="section-title">最近趋势</text>
					<text class="section-sub">最近 7 条记录</text>
				</view>
				<view v-if="trendRecords.length" class="trend-list">
					<view v-for="item in trendRecords" :key="item.id" class="trend-item">
						<text class="trend-date">{{ item.date }}</text>
						<text class="trend-meta">{{ item.height }}cm · {{ item.weight }}kg</text>
						<text class="trend-meta">BMI {{ item.bmi }} · BMR {{ item.bmr }}</text>
					</view>
				</view>
			</view>

			<view class="list">
				<view v-for="item in record.history" :key="item.id" class="item u-card">
					<view class="item-head">
						<text class="item-date">{{ item.date }}</text>
						<text class="item-time">{{ item.recordTime || '--' }}</text>
					</view>
					<text class="item-meta">身高 {{ item.height }} cm｜体重 {{ item.weight }} kg</text>
					<text class="item-meta">BMI {{ item.bmi }}｜BMR {{ item.bmr }}</text>
				</view>
			</view>
		</template>
	</view>
</template>

<script setup>
	// 功能注释：页面数据来自接口和本地状态。
	import {
		computed,
		reactive,
		ref
	} from 'vue'
	import {
		onShow
	} from '@dcloudio/uni-app'
	import TopBar from '@/components/ui/TopBar.vue'
	import EmptyState from '@/components/ui/EmptyState.vue'
	import {
		fetchHealthRecord,
		logUserAction,
		saveHealthRecord
	} from '@/services/healthApi.js'

	const pageError = ref('')
	const record = reactive({
		current: {
			height: null,
			weight: null,
			bmi: null,
			bmr: null,
			recordTime: ''
		},
		history: []
	})
	const height = ref('')
	const weight = ref('')

	const canSave = computed(() => Number(height.value) > 0 && Number(weight.value) > 0)
	const trendRecords = computed(() => record.history.slice(0, 7))

	// 功能注释：加载健康档案详情。
	async function load() {
		try {
			pageError.value = ''
			const data = await fetchHealthRecord()
			record.current = {
				height: data?.current?.height ?? null,
				weight: data?.current?.weight ?? null,
				bmi: data?.current?.bmi ?? null,
				bmr: data?.current?.bmr ?? null,
				recordTime: data?.current?.recordTime || ''
			}
			record.history = Array.isArray(data?.history) ? data.history : []
		} catch (error) {
			pageError.value = error.message || '加载失败'
			record.current = {
				height: null,
				weight: null,
				bmi: null,
				bmr: null,
				recordTime: ''
			}
			record.history = []
		}
	}

	onShow(() => {
		logUserAction('view_health_record_page')
		load()
	})

	// 功能注释：保存健康档案。
	async function saveRecord() {
		if (!canSave.value) return
		try {
			const data = await saveHealthRecord({
				height: Number(height.value),
				weight: Number(weight.value)
			})
			record.current = {
				height: data?.current?.height ?? null,
				weight: data?.current?.weight ?? null,
				bmi: data?.current?.bmi ?? null,
				bmr: data?.current?.bmr ?? null,
				recordTime: data?.current?.recordTime || ''
			}
			record.history = Array.isArray(data?.history) ? data.history : []
			height.value = ''
			weight.value = ''
			uni.showToast({
				title: '已保存',
				icon: 'success'
			})
		} catch (error) {
			uni.showToast({
				title: error.message || '保存失败',
				icon: 'none'
			})
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
		margin: 8rpx 0 22rpx;
		font-size: 24rpx;
		color: var(--c-muted);
		line-height: 1.7;
	}

	.latest-card,
	.form-card,
	.trend-card {
		margin-top: 22rpx;
		border-radius: 52rpx;
		padding: 30rpx;
	}

	.card-head {
		display: flex;
		align-items: flex-end;
		justify-content: space-between;
		gap: 16rpx;
	}

	.section-title {
		font-size: 30rpx;
		font-weight: 1000;
		color: var(--c-title);
	}

	.section-sub {
		font-size: 22rpx;
		color: var(--c-muted);
		font-weight: 800;
		text-align: right;
	}

	.metrics {
		margin-top: 22rpx;
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 16rpx;
	}

	.metric {
		border-radius: 36rpx;
		padding: 22rpx;
		background: rgba(148, 163, 184, 0.10);
	}

	.metric-label {
		display: block;
		font-size: 22rpx;
		color: var(--c-muted);
		font-weight: 800;
	}

	.metric-value {
		display: block;
		margin-top: 10rpx;
		font-size: 42rpx;
		font-weight: 1000;
		color: var(--c-title);
	}

	.green {
		background: var(--c-primary);
	}

	.light {
		color: rgba(255, 255, 255, 0.82);
	}

	.white {
		color: #fff;
	}

	.empty-card {
		margin-top: 22rpx;
		border-radius: 52rpx;
		padding: 16rpx;
	}

	.field {
		margin-top: 22rpx;
	}

	.label {
		display: block;
		font-size: 22rpx;
		color: var(--c-muted);
		font-weight: 800;
		margin-bottom: 12rpx;
	}

	.input {
		height: 86rpx;
		border-radius: 9999rpx;
		background: #F8FAFC;
		padding: 0 26rpx;
		box-sizing: border-box;
		font-size: 30rpx;
		color: var(--c-title);
		font-weight: 900;
	}

	.btn {
		margin-top: 24rpx;
		height: 92rpx;
		border-radius: 9999rpx;
		background: linear-gradient(135deg, #0b1f2a, #0f3a44);
		color: #fff;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 30rpx;
		font-weight: 1000;
	}

	.btn.disabled {
		background: #8B919B;
	}

	.trend-list {
		margin-top: 22rpx;
		display: flex;
		gap: 14rpx;
		overflow-x: auto;
		padding-bottom: 4rpx;
	}

	.trend-item {
		min-width: 198rpx;
		border-radius: 32rpx;
		background: rgba(18, 183, 106, 0.08);
		padding: 18rpx;
		box-sizing: border-box;
	}

	.trend-date {
		display: block;
		font-size: 24rpx;
		color: var(--c-title);
		font-weight: 1000;
	}

	.trend-meta {
		display: block;
		margin-top: 8rpx;
		font-size: 22rpx;
		color: var(--c-muted);
		font-weight: 800;
	}

	.list {
		margin-top: 22rpx;
	}

	.item {
		margin-bottom: 14rpx;
		border-radius: 40rpx;
		padding: 22rpx 24rpx;
	}

	.item-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 16rpx;
	}

	.item-date {
		display: block;
		font-size: 22rpx;
		color: var(--c-muted);
		font-weight: 800;
	}

	.item-time {
		display: block;
		font-size: 22rpx;
		color: var(--c-muted);
		font-weight: 800;
	}

	.item-meta {
		display: block;
		margin-top: 8rpx;
		font-size: 28rpx;
		color: var(--c-title);
		font-weight: 900;
	}
</style>