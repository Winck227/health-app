<template>
  <view class="page">
    <TopBar title="我的收藏" />
    <text class="subhead">这里只保存文章标题、链接和来源，不保存正文</text>
    <view v-if="pageError" class="u-card empty"><text class="empty-title">加载失败</text><text class="empty-sub">{{ pageError || '暂无数据' }}</text></view>
    <view v-else-if="!items.length" class="u-card empty"><text class="empty-title">暂无收藏</text><text class="empty-sub">在知识详情页点击收藏后，这里会显示文章列表</text></view>
    <view v-else class="list">
      <view v-for="item in items" :key="item.id" class="row u-card" @click="open(item)">
        <view class="no-image-tag">阅</view>
        <view class="body"><text class="cat">{{ item.source || '健康资讯' }}</text><text class="title">{{ item.articleTitle || item.article_title || item.title }}</text><text v-if="item.summary" class="summary">{{ item.summary }}</text></view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import TopBar from '@/components/ui/TopBar.vue'
import { fetchArticleFavorites, logUserAction } from '@/services/healthApi.js'
import { openDetailPage } from '@/utils/router.js'

const items = reactive([])
const pageError = ref('')
function normalizeList(data) { return Array.isArray(data) ? data : (Array.isArray(data?.items) ? data.items : []) }
function sync(list) { items.splice(0, items.length, ...normalizeList(list)) }
async function load() { try { pageError.value = ''; sync(await fetchArticleFavorites()) } catch (error) { pageError.value = error.message || '加载失败'; sync([]) } }
onShow(() => { logUserAction('view_favorites_page'); load() })
function open(item) {
  const url = item?.articleUrl || item?.article_url || item?.url || ''
  logUserAction('open_favorite_article', { id: item?.id, url })
  if (url) openDetailPage(`/pages/knowledge/detail?externalUrl=${encodeURIComponent(url)}`)
}
</script>

<style scoped>
.page { padding: 20rpx 36rpx 40rpx; box-sizing: border-box; }
.subhead { display:block; margin-top:6rpx; font-size:24rpx; color:var(--c-muted); line-height:1.6; }
.empty { margin-top:22rpx; border-radius:52rpx; padding:34rpx; }
.empty-title { display:block; font-size:34rpx; font-weight:1000; color:var(--c-title); }
.empty-sub { display:block; margin-top:12rpx; font-size:26rpx; color:var(--c-muted); font-weight:700; line-height:1.7; }
.list { margin-top:18rpx; }
.row { border-radius:44rpx; padding:24rpx; display:flex; align-items:center; gap:18rpx; margin-bottom:18rpx; }
.no-image-tag { width:82rpx; height:82rpx; border-radius:30rpx; display:flex; align-items:center; justify-content:center; font-size:24rpx; font-weight:1000; color:var(--c-primary); background:rgba(18,183,106,.12); }
.body { flex:1; }
.cat { display:block; font-size:22rpx; color:var(--c-muted); font-weight:800; margin-bottom:6rpx; }
.title { display:block; font-size:30rpx; font-weight:900; color:var(--c-title); line-height:1.4; }
.summary { display:block; margin-top:8rpx; font-size:23rpx; color:var(--c-muted); line-height:1.5; }
</style>
