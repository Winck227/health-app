<template>
	<AppShell active="knowledge" @reach-bottom="loadMoreArticles">
		<view class="head">
			<text class="h1">健康知识库</text>
			<text class="sub">探索科学的健康生活方式</text>
		</view>

		<scroll-view scroll-x class="tabs" :show-scrollbar="false">
			<view class="tabs-inner">
				<view v-for="tab in tabs" :key="tab.key" class="tab" :class="{ active: activeTab === tab.key }"
					@click="changeTab(tab.key)">
					{{ tab.label }}
				</view>
			</view>
		</scroll-view>

		<view class="keyword-bar">
			<scroll-view scroll-x class="keyword-scroll" :show-scrollbar="false">
				<view class="keyword-inner">
					<view v-for="keyword in keywordTabs" :key="keyword" class="keyword-chip"
						:class="{ active: activeKeyword === keyword }" @click="changeKeyword(keyword)">
						{{ keyword }}
					</view>
				</view>
			</scroll-view>
			<view class="refresh-btn" @click="refreshNextBatch">换一批</view>
		</view>

		<view class="list">
			<view v-if="loading" class="state-text">正在加载健康资讯...</view>
			<view v-else-if="loadError && !articles.length" class="state-text">{{ loadError }}</view>
			<view v-else-if="!articles.length" class="state-text">暂无相关文章，换个分类再看看</view>

			<view v-for="item in articles" :key="item.id" class="item u-card" @click="goExternalDetail(item)">
				<view class="ico" :class="iconClass(item.themeClass)">{{ item.icon || '文' }}</view>
				<view class="body">
					<text class="cat">{{ item.category }}</text>
					<text class="title">{{ item.title }}</text>
					<text v-if="item.summary" class="summary">{{ item.summary }}</text>
				</view>
				<text class="min">{{ item.readMinutes }} 分钟</text>
			</view>

			<view v-if="articles.length" class="load-more" @click="loadMoreArticles">
				{{ finished ? '没有更多了' : (loadingMore ? '正在加载更多...' : '加载更多') }}
			</view>
		</view>
	</AppShell>
</template>

<script setup>
	import {
		computed,
		ref
	} from 'vue'
	import {
		onShow
	} from '@dcloudio/uni-app'
	import AppShell from '@/components/app/AppShell.vue'
	import {
		fetchKnowledgeList,
		getKnowledgeKeywords,
		logUserAction
	} from '@/services/healthApi.js'
	import {
		openDetailPage
	} from '@/utils/router.js'

	const tabs = [{
			key: 'all',
			label: '全部'
		},
		{
			key: 'sport',
			label: '运动'
		},
		{
			key: 'diet',
			label: '饮食'
		},
		{
			key: 'sleep',
			label: '睡眠'
		},
		{
			key: 'mental',
			label: '心理'
		}
	]

	const activeTab = ref('all')
	const activeKeyword = ref('')
	const articles = ref([])
	const loading = ref(false)
	const loadingMore = ref(false)
	const finished = ref(false)
	const loadError = ref('')
	const nextPage = ref(1)

	const keywordTabs = computed(() => getKnowledgeKeywords(activeTab.value))

	function ensureKeyword() {
		const keywords = keywordTabs.value
		if (!activeKeyword.value || !keywords.includes(activeKeyword.value)) {
			activeKeyword.value = keywords[0] || '健康生活'
		}
	}

	async function loadArticles({
		reset = false,
		replace = false
	} = {}) {
		if (loading.value || loadingMore.value) return
		ensureKeyword()

		if (reset) {
			nextPage.value = 1
			finished.value = false
			loadError.value = ''
		}

		const isFirstLoad = reset || replace || !articles.value.length
		loading.value = isFirstLoad
		loadingMore.value = !isFirstLoad

		try {
			const list = await fetchKnowledgeList({
				filterKey: activeTab.value,
				keyword: activeKeyword.value,
				startPage: nextPage.value,
				pageCount: 2
			})

			if (replace) {
				articles.value = list
			} else {
				const merged = [...articles.value, ...list]
				const seen = new Set()
				articles.value = merged.filter((item) => {
					if (!item?.url || seen.has(item.url)) return false
					seen.add(item.url)
					return true
				})
			}

			nextPage.value += 2
			if (!list.length) finished.value = true
		} catch (error) {
			loadError.value = error?.message || '外部健康资讯暂时不可用'
			if (reset || replace) articles.value = []
		} finally {
			loading.value = false
			loadingMore.value = false
		}
	}

	function loadMoreArticles() {
		if (finished.value || loading.value || loadingMore.value) return
		loadArticles({
			reset: false,
			replace: false
		})
	}

	function refreshNextBatch() {
		if (loading.value || loadingMore.value) return
		loadArticles({
			reset: false,
			replace: true
		})
	}

	onShow(() => {
		logUserAction('view_knowledge_page', {
			filter: activeTab.value
		})
		if (!articles.value.length) {
			loadArticles({
				reset: true,
				replace: true
			})
		}
	})


	function changeTab(key) {
		if (activeTab.value === key) return
		activeTab.value = key
		activeKeyword.value = ''
		articles.value = []
		nextPage.value = 1
		finished.value = false
		loadError.value = ''
		logUserAction('change_knowledge_tab', {
			filter: key
		})
		loadArticles({
			reset: true,
			replace: true
		})
	}

	function changeKeyword(keyword) {
		if (activeKeyword.value === keyword) return
		activeKeyword.value = keyword
		articles.value = []
		nextPage.value = 1
		finished.value = false
		loadError.value = ''
		logUserAction('change_knowledge_keyword', {
			filter: activeTab.value,
			keyword
		})
		loadArticles({
			reset: true,
			replace: true
		})
	}

	function iconClass(theme) {
		return {
			pink: 'bg-pink',
			blue: 'bg-blue',
			indigo: 'bg-indigo',
			green: 'bg-green',
			amber: 'bg-amber'
		} [theme] || 'bg-pink'
	}

	function goExternalDetail(item) {
		const url = item?.url || ''
		if (!url) return
		logUserAction('open_external_knowledge_detail', {
			url,
			title: item?.title
		})
		const params = [
			`externalUrl=${encodeURIComponent(url)}`,
			`title=${encodeURIComponent(item?.title || '')}`,
			`summary=${encodeURIComponent(item?.summary || '')}`,
			`source=${encodeURIComponent(item?.source || '')}`,
			`publishTime=${encodeURIComponent(item?.publishTime || '')}`
		].join('&')
		openDetailPage(`/pages/knowledge/detail?${params}`)
	}
</script>

<style scoped>
	.head {
		margin-bottom: 20rpx;
	}

	.h1 {
		display: block;
		font-size: 46rpx;
		font-weight: 900;
		color: var(--c-title);
	}

	.sub {
		display: block;
		margin-top: 10rpx;
		font-size: 24rpx;
		color: var(--c-muted);
	}

	.tabs {
		margin-top: 18rpx;
		margin-bottom: 18rpx;
	}

	.tabs-inner {
		display: flex;
		gap: 12rpx;
		width: 550rpx;
		padding: 0 4rpx;
	}

	.tab {
		padding: 13rpx 24rpx;
		border-radius: 9999rpx;
		background: #fff;
		color: var(--c-muted);
		font-size: 26rpx;
		font-weight: 900;
		box-shadow: 0 16rpx 44rpx rgba(15, 23, 42, 0.06);
		white-space: nowrap;
	}

	.tab.active {
		background: var(--c-primary);
		color: #fff;
		box-shadow: 0 20rpx 50rpx rgba(18, 183, 106, 0.24);
	}

	.keyword-bar {
		display: flex;
		align-items: center;
		gap: 16rpx;
		width: 550rpx;
		margin-bottom: 18rpx;
	}

	.keyword-scroll {
		flex: 1;
		white-space: nowrap;
	}

	.keyword-inner {
		display: flex;
		gap: 10rpx;
		padding: 0 4rpx;
	}

	.keyword-chip {
		padding: 10rpx 20rpx;
		border-radius: 9999rpx;
		background: #fff;
		color: var(--c-muted);
		font-size: 24rpx;
		font-weight: 900;
		box-shadow: 0 12rpx 34rpx rgba(15, 23, 42, 0.05);
		white-space: nowrap;
	}

	.keyword-chip.active {
		background: rgba(18, 183, 106, 0.12);
		color: var(--c-primary);
	}

	.refresh-btn {
		flex-shrink: 0;
		padding: 12rpx 22rpx;
		border-radius: 9999rpx;
		background: var(--c-primary);
		color: #fff;
		font-size: 24rpx;
		font-weight: 900;
		box-shadow: 0 18rpx 42rpx rgba(18, 183, 106, 0.2);
	}

	.list {
		margin-top: 8rpx;
	}

	.item {
		border-radius: 36rpx;
		padding: 22rpx;
		display: flex;
		align-items: flex-start;
		gap: 16rpx;
		width: 500rpx;
		margin-bottom: 16rpx;
	}

	.ico {
		width: 68rpx;
		height: 68rpx;
		border-radius: 24rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 26rpx;
	}

	.body {
		flex: 1;
	}

	.cat {
		display: block;
		font-size: 22rpx;
		color: var(--c-muted);
		font-weight: 800;
		margin-bottom: 6rpx;
	}

	.title {
		display: block;
		font-size: 29rpx;
		font-weight: 900;
		color: var(--c-title);
		line-height: 1.35;
		overflow: hidden;
		display: -webkit-box;
		-webkit-line-clamp: 2;
		-webkit-box-orient: vertical;
	}

	.summary {
		display: block;
		margin-top: 8rpx;
		font-size: 23rpx;
		line-height: 1.55;
		color: var(--c-muted);
		font-weight: 700;
		overflow: hidden;
		display: -webkit-box;
		-webkit-line-clamp: 3;
		-webkit-box-orient: vertical;
	}

	.min {
		align-self: flex-start;
		font-size: 22rpx;
		color: #CBD5E1;
		font-weight: 900;
	}

	.state-text,
	.load-more {
		padding: 26rpx 20rpx;
		text-align: center;
		color: var(--c-muted);
		font-size: 24rpx;
		font-weight: 800;
	}

	.bg-pink {
		background: rgba(255, 59, 107, 0.12);
	}

	.bg-blue {
		background: rgba(79, 103, 255, 0.12);
	}

	.bg-indigo {
		background: rgba(92, 107, 192, 0.12);
	}

	.bg-green {
		background: rgba(18, 183, 106, 0.12);
	}

	.bg-amber {
		background: rgba(255, 122, 47, 0.12);
	}
</style>