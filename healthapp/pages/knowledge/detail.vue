<template>
  <view class="page">
    <TopBar title="">
      <template #right>
        <view v-if="canFavorite" class="top-actions">
          <view class="action-btn" :class="{ active: article.isFavorite, loading: favoriteLoading }" @click="toggleFavorite">
            <image class="action-icon" src="/static/icons/heart_pink.svg" mode="aspectFit" />
          </view>
        </view>
      </template>
    </TopBar>

    <view class="tag u-pill">{{ article.category || '健康资讯' }}</view>
    <text class="title">{{ article.title || '健康资讯' }}</text>
    <view v-if="article.source || article.publishTime" class="source-row">
      <text class="source-text">来源：{{ article.source || '外部来源' }}</text>
      <text v-if="article.publishTime" class="source-text">{{ article.publishTime }}</text>
    </view>
    <view v-if="article.quote" class="quote"><view class="quote-line" /><text class="quote-text">“{{ article.quote }}”</text></view>
    <view class="content">
      <template v-for="(block, index) in article.paragraphs" :key="index">
        <image v-if="block.type === 'image'" class="article-image" :src="block.text" mode="widthFix" />
        <text v-else :class="block.type === 'h2' ? 'h2' : 'p'">{{ block.text }}</text>
      </template>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import TopBar from '@/components/ui/TopBar.vue'
import { addFavoriteArticle, fetchExternalKnowledgeDetail, logUserAction, removeFavoriteArticle } from '@/services/healthApi.js'
import { hashText, safeText } from '@/utils/textSafe.js'

const article = ref({ id: '', favoriteId: '', category: '', title: '', quote: '', source: '', sourceUrl: '', publishTime: '', paragraphs: [], images: [], isFavorite: false })
const favoriteLoading = ref(false)
const canFavorite = computed(() => Boolean(article.value.sourceUrl || article.value.title))

function normalizeParagraphs(data) {
  const list = Array.isArray(data?.paragraphs) ? data.paragraphs : []
  const normalized = []
  const seen = new Set()

  list.forEach((item) => {
    const type = item?.type === 'h2' ? 'h2' : (item?.type === 'image' || item?.type === 'img' ? 'image' : 'p')
    const text = safeText(item?.text || item?.url)
    if (!text) return
    const key = `${type}:${text}`
    if (seen.has(key)) return
    seen.add(key)
    normalized.push({ type, text })
  })

  const hasImageBlock = normalized.some((item) => item.type === 'image')
  if (!hasImageBlock && Array.isArray(data?.images)) {
    data.images.map((item) => safeText(item)).filter(Boolean).forEach((src) => {
      const key = `image:${src}`
      if (seen.has(key)) return
      seen.add(key)
      normalized.push({ type: 'image', text: src })
    })
  }

  return normalized.length ? normalized : [{ type: 'p', text: safeText(data?.summary || data?.quote || '源文章正文暂不可用，请稍后重试。') }]
}

function normalizeArticle(data = {}, url = '') {
  const sourceUrl = safeText(data.sourceUrl || data.url || url)
  const title = safeText(data.title || '健康资讯')
  return {
    id: data.id || `external-${hashText(sourceUrl || title)}`,
    favoriteId: data.favoriteId || data.favorite_id || data.id || '',
    category: safeText(data.category || '健康资讯'),
    title,
    quote: safeText(data.quote || data.summary || ''),
    source: safeText(data.source || data.siteName || '外部来源'),
    sourceUrl,
    publishTime: safeText(data.publishTime || data.publishedAt || ''),
    paragraphs: normalizeParagraphs(data),
    images: Array.isArray(data.images) ? data.images.map((item) => safeText(item)).filter(Boolean) : [],
    isFavorite: Boolean(data.isFavorite || data.is_favorite)
  }
}

function buildFallback(url = '') {
  return normalizeArticle({ title: '健康资讯', source: '外部来源', url, paragraphs: [{ type: 'p', text: '源文章正文暂不可用，请稍后重试。' }], images: [] }, url)
}

onLoad(async (options) => {
  const externalUrl = options?.externalUrl ? decodeURIComponent(options.externalUrl) : ''
  const fallback = {
    title: options?.title ? decodeURIComponent(options.title) : '',
    summary: options?.summary ? decodeURIComponent(options.summary) : '',
    quote: options?.summary ? decodeURIComponent(options.summary) : '',
    source: options?.source ? decodeURIComponent(options.source) : '',
    publishTime: options?.publishTime ? decodeURIComponent(options.publishTime) : '',
    url: externalUrl,
    paragraphs: options?.summary ? [{ type: 'p', text: decodeURIComponent(options.summary) }] : [],
    images: []
  }
  logUserAction('view_article_detail', { externalUrl, fallback })
  try {
    if (externalUrl) {
      article.value = normalizeArticle(await fetchExternalKnowledgeDetail(externalUrl, fallback), externalUrl)
      return
    }
    article.value = normalizeArticle(fallback, externalUrl)
  } catch (error) {
    article.value = normalizeArticle(fallback, externalUrl)
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  }
})

async function toggleFavorite() {
  if (!canFavorite.value || favoriteLoading.value) return
  favoriteLoading.value = true
  try {
    if (article.value.isFavorite && article.value.favoriteId) {
      await removeFavoriteArticle(article.value.favoriteId)
      article.value.isFavorite = false
      article.value.favoriteId = ''
      uni.showToast({ title: '已取消收藏', icon: 'none' })
    } else {
      const result = await addFavoriteArticle({
        articleTitle: article.value.title,
        article_title: article.value.title,
        articleUrl: article.value.sourceUrl,
        article_url: article.value.sourceUrl,
        source: article.value.source,
        summary: article.value.quote
      })
      article.value.favoriteId = result?.id || result?.favoriteId || result?.favorite_id || article.value.favoriteId
      article.value.isFavorite = true
      uni.showToast({ title: '已收藏', icon: 'success' })
    }
  } catch (error) {
    uni.showToast({ title: error.message || '操作失败', icon: 'none' })
  } finally {
    favoriteLoading.value = false
  }
}
</script>

<style scoped>
.page { max-width: 920px; margin: 0 auto; padding: 24rpx 40rpx 70rpx; box-sizing: border-box; }
.top-actions { display: flex; align-items: center; gap: 14rpx; }
.action-btn { width: 72rpx; height: 72rpx; border-radius: 9999rpx; background: rgba(148, 163, 184, 0.12); display: flex; align-items: center; justify-content: center; }
.action-btn.active { background: rgba(255, 59, 107, 0.12); }
.action-btn.loading { opacity: 0.6; }
.action-icon { width: 34rpx; height: 34rpx; }
.tag { margin-top: 12rpx; display: inline-flex; align-items: center; padding: 10rpx 18rpx; background: rgba(18, 183, 106, 0.12); color: var(--c-primary); font-size: 22rpx; font-weight: 900; }
.title { display: block; margin-top: 22rpx; font-size: 46rpx; font-weight: 1000; color: var(--c-title); line-height: 1.28; letter-spacing: 1rpx; }
.source-row { margin-top: 14rpx; display: flex; flex-wrap: wrap; gap: 12rpx; }
.source-text { font-size: 23rpx; color: var(--c-muted); font-weight: 800; }
.quote { margin-top: 28rpx; display: flex; gap: 18rpx; padding: 24rpx; border-radius: 30rpx; background: rgba(18, 183, 106, 0.08); }
.quote-line { width: 8rpx; border-radius: 9999rpx; background: var(--c-primary); }
.quote-text { flex: 1; font-size: 28rpx; line-height: 1.8; color: var(--c-text); font-weight: 700; }
.content { margin-top: 34rpx; padding: 30rpx; border-radius: 34rpx; background: #fff; box-shadow: 0 18rpx 60rpx rgba(15, 23, 42, 0.06); }
.article-image { display: block; width: 100%; margin-bottom: 24rpx; border-radius: 24rpx; background: #f3f4f6; }
.p { display: block; font-size: 30rpx; color: var(--c-text); line-height: 2.05; font-weight: 500; margin-bottom: 24rpx; text-align: justify; }
.h2 { display: block; margin-top: 20rpx; margin-bottom: 16rpx; font-size: 36rpx; font-weight: 1000; color: var(--c-title); line-height: 1.35; }
</style>
