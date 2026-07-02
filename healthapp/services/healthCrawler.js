import { request } from '@/services/request.js'
import categoryConfig from '@/config/categories.json'
import siteConfig from '@/config/health-sites.json'

const DEFAULT_TIMEOUT = 15000
const DEFAULT_RETRY = 2
const DEFAULT_RETRY_DELAY = 600
const DEFAULT_UA = 'Mozilla/5.0 (Linux; Android 12; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36 uni-app-health-crawler/1.0'
const SITE_ITEM = siteConfig && Array.isArray(siteConfig.sites) ? siteConfig.sites[0] || {} : {}
const SITE_NAME = SITE_ITEM.name || '39健康网'
const SITE_BASE = SITE_ITEM.baseUrl || 'https://so.39.net'
const SEARCH_URL = SITE_ITEM.searchUrl || 'https://so.39.net/search/s?words={keyword}&start={page}'

// 功能注释：延迟执行，供重试退避使用。
function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

// 功能注释：把任意值转成字符串。
function toStr(value) {
  return value === null || value === undefined ? '' : String(value)
}

// 功能注释：解码常见 HTML 实体。
function decodeHtml(value) {
  return toStr(value)
    .replace(/&nbsp;/gi, ' ')
    .replace(/&amp;/gi, '&')
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/&quot;/gi, '"')
    .replace(/&#39;/gi, "'")
    .replace(/&#(\d+);/g, (_, code) => {
      const num = Number(code)
      return Number.isFinite(num) ? String.fromCharCode(num) : ''
    })
}

// 功能注释：清理标签和多余空白。
function cleanText(value) {
  return decodeHtml(value)
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/<style[\s\S]*?<\/style>/gi, '')
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/<[^>]+>/g, ' ')
    .replace(/\u00a0/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

// 功能注释：移除脚本、样式和注释。
function stripHtml(value) {
  return toStr(value)
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/<style[\s\S]*?<\/style>/gi, '')
    .replace(/<noscript[\s\S]*?<\/noscript>/gi, '')
    .replace(/<!--[\s\S]*?-->/g, '')
}

// 功能注释：兼容 uni.request / 控制台中可能出现的 JSON 转义 HTML，确保后续正则面对的是标准 HTML。
function normalizeHtmlSource(value) {
  let text = toStr(value)
  const trimmed = text.trim()
  if ((trimmed.startsWith('\"') && trimmed.endsWith('\"')) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
    try {
      text = JSON.parse(trimmed)
    } catch (error) {}
  }
  return toStr(text)
    .replace(/\\u003c/gi, '<')
    .replace(/\\u003e/gi, '>')
    .replace(/\\u0026/gi, '&')
    .replace(/\\"/g, '"')
    .replace(/\\'/g, "'")
    .replace(/\\\//g, '/')
    .replace(/\\r\\n/g, '\n')
    .replace(/\\n/g, '\n')
    .replace(/\\t/g, '\t')
}

// 功能注释：把相对地址规范化为绝对地址。
function normalizeUrl(url, baseUrl) {
  const value = cleanText(url).replace(/["'<>]+$/g, '')
  if (!value) return ''
  // 39 搜索结果大量返回 http:// 子域名文章页；不要强制改成 https，部分旧文章 https 会 404。
  if (/^https?:\/\//i.test(value)) return value
  if (value.indexOf('//') === 0) return `https:${value}`
  try {
    if (typeof URL !== 'undefined') {
      return new URL(value, baseUrl).href
    }
    const origin = toStr(baseUrl).match(/^(https?:\/\/[^/]+)/i)
    if (!origin || !origin[1]) return ''
    if (value.charAt(0) === '/') return origin[1] + value
    return origin[1] + '/' + value.replace(/^\//, '')
  } catch (error) {
    return ''
  }
}

// 功能注释：把非数组值转换为空数组。
function safeArray(value) {
  return Array.isArray(value) ? value : []
}

// 功能注释：按 URL 去重。
function uniqueByUrl(list) {
  const seen = {}
  return safeArray(list).filter(item => {
    const key = item && item.url ? String(item.url) : ''
    if (!key || seen[key]) return false
    seen[key] = true
    return true
  })
}

// 功能注释：补全抓取请求头。

// 功能注释：从HTML中提取优先显示的img地址。
function extractImages(html, baseUrl) {
  const htmlText = toStr(html)
  const results = []
  const reg = /<img[^>]+(?:data-src|data-original|data-lazy|src)=["'']([^"'']+)["''][^>]*>/gi
  let match = reg.exec(htmlText)
  while (match) {
    const imageUrl = normalizeUrl(match[1], baseUrl)
    if (imageUrl && !results.includes(imageUrl)) {
      results.push(imageUrl)
    }
    if (results.length >= 6) break
    match = reg.exec(htmlText)
  }
  return results
}function normalizeHeader(header) {
  const normalized = {
    Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
    ...(header || {})
  }
  normalized['User-Agent'] = DEFAULT_UA
  return normalized
}

// 功能注释：带重试的抓取请求。
async function crawlerRequest(options) {
  const retry = Number.isInteger(options && options.retry) ? options.retry : DEFAULT_RETRY
  const retryDelay = Number.isInteger(options && options.retryDelay) ? options.retryDelay : DEFAULT_RETRY_DELAY
  const finalUrl = toStr(options && options.url)
  let lastError = null

  for (let index = 0; index <= retry; index += 1) {
    try {
      const res = await request(finalUrl, {
        ...options,
        responseType: options.responseType || 'text',
        rawResponse: true,
        skipAuth: true,
        contentType: '',
        header: normalizeHeader(options.header)
      })
      return res
    } catch (error) {
      lastError = error
      if (index < retry) {
        await sleep(retryDelay * (index + 1))
      }
    }
  }

  throw lastError || new Error('request failed')
}

// 功能注释：读取分类配置。
function getCategories() {
  const raw = categoryConfig && Array.isArray(categoryConfig.categories) ? categoryConfig.categories : []
  return raw.map(item => ({
    name: cleanText(item.name),
    keywords: safeArray(item.keywords).map(cleanText).filter(Boolean)
  })).filter(item => item.name && item.keywords.length)
}

// 功能注释：拼接搜索地址。
function getSearchUrl(keyword, page = 1) {
  const safeKeyword = encodeURIComponent(cleanText(keyword))
  const safePage = Math.max(1, Number(page) || 1)
  return SEARCH_URL.replace('{keyword}', safeKeyword).replace('{page}', String(safePage))
}

// 功能注释：判断搜索页中的链接是否像文章链接。39 搜索页真机 HTML 已不再固定使用 con_item 结构，所以需要通用链接解析兜底。
function isLikelyArticleLink(url, title) {
  const safeUrl = toStr(url)
  const safeTitle = cleanText(title)
  if (!safeUrl || !safeTitle) return false
  if (safeTitle.length < 5) return false
  if (!/https?:\/\/[^/]+\.39\.net\//i.test(safeUrl)) return false
  if (/https?:\/\/(so|www|search|passport|my|wapyyk|job|corp|image|static|tools|webnetpolice)\.39\.net\//i.test(safeUrl)) return false
  if (/\.(jpg|jpeg|png|gif|webp|svg)(\?|$)/i.test(safeUrl)) return false
  if (/^(全部|问答|百科|药品|医院|医生|登录|注册|下一页|上一页|网站简介|人才招聘|联系我们|39健康网|Image)$/i.test(safeTitle)) return false
  if (/没有帮助|向医生提问|搜索技术由|合作\s*-\s*导航/i.test(safeTitle)) return false
  if (/推荐搜索|Copyright|未经授权|中国优质医疗/.test(safeTitle)) return false
  return /[\u4e00-\u9fa5]/.test(safeTitle)
}

// 功能注释：从 URL 子域名估算来源名。
function sourceNameFromUrl(url) {
  const text = toStr(url)
  const hostMatch = text.match(/^https?:\/\/([^./]+)\.39\.net/i)
  const sub = hostMatch && hostMatch[1] ? hostMatch[1] : ''
  const pathMatch = text.match(/^https?:\/\/m\.39\.net\/([^/?#]+)/i)
  const mobileSection = pathMatch && pathMatch[1] ? pathMatch[1] : ''
  const map = {
    xl: '心理',
    byby: '不孕不育',
    jbk: '39疾病百科',
    mouth: '口腔疾病',
    care: '保健',
    man: '男性',
    js: '精神疾病',
    baby: '育儿',
    women: '女性',
    diet: '减肥',
    food: '饮食',
    fitness: '健身',
    sleep: '睡眠',
    old: '老人',
    drug: '药品',
    pf: '皮肤',
    gan: '肝病',
    heart: '心血管'
  }
  if (sub === 'm' && mobileSection) {
    return map[mobileSection] || SITE_NAME
  }
  return map[sub] || SITE_NAME
}

// 功能注释：把一段搜索结果块裁剪成适合列表显示的摘要。
function extractSummaryFromBlock(block, title) {
  let source = toStr(block)
  const dateIndex = source.search(/20\d{2}[-\/]\d{1,2}[-\/]\d{1,2}/)
  if (dateIndex >= 0) {
    source = source.slice(0, dateIndex)
  }
  const titleText = cleanText(title)
  let text = cleanText(source)
    .replace(titleText, ' ')
    .replace(/^\s*\d+[.、]?\s*/, ' ')
    .replace(/^(心理|不孕不育|39疾病百科|口腔疾病|保健|男性|精神疾病|育儿|女性|饮食|健身|睡眠|老人|药品)\s*/g, ' ')
    .replace(/戒烟_39健康搜|39搜索|登录\s*\/\s*注册|全部\s*问答\s*百科\s*药品\s*医院\s*医生|没有帮助？向医生提问>>|搜索技术由\s*提供|39健康网\s*-\s*合作\s*-\s*导航/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
  if (text.length > 180) text = `${text.slice(0, 180)}...`
  return text
}

// 功能注释：通用解析 39 搜索结果页。新版 39 搜索页在 App 真机返回的是普通列表，不包含 con_item。
function extractListItemsByAnchors(html) {
  const htmlText = toStr(html)
  const anchors = []
  const anchorReg = /<a\b[^>]*href=["']([^"']+)["'][^>]*>([\s\S]*?)<\/a>/gi
  let match = anchorReg.exec(htmlText)
  while (match) {
    const url = normalizeUrl(match[1], SITE_BASE)
    const title = cleanText(match[2])
    if (isLikelyArticleLink(url, title)) {
      anchors.push({ index: match.index, endIndex: anchorReg.lastIndex, url, title })
    }
    match = anchorReg.exec(htmlText)
  }

  const results = []
  anchors.forEach((item, index) => {
    const next = anchors[index + 1]
    const block = htmlText.slice(item.index, next ? next.index : Math.min(htmlText.length, item.endIndex + 1600))
    const publishTime = cleanText((block.match(/(20\d{2}[-\/]\d{1,2}[-\/]\d{1,2})/) || [])[1])
    const content = extractSummaryFromBlock(block, item.title)
    if (item.url && item.title) {
      results.push({
        title: item.title,
        content,
        publishTime,
        source: sourceNameFromUrl(item.url),
        images: extractImages(block, SITE_BASE),
        tags: [],
        siteName: SITE_NAME,
        url: item.url,
        sourceUrl: ''
      })
    }
  })
  return uniqueByUrl(results)
}

// 功能注释：解析 39 搜索页移动端结构。移动页使用 a.superA + h3/p，不是 con_item。
function extractMobileListItems(html) {
  const htmlText = normalizeHtmlSource(html)
  const results = []
  const anchorReg = /<a\b[^>]*class\s*=\s*(?:["'][^"']*superA[^"']*["']|[^\s>]*superA[^\s>]*)[^>]*>([\s\S]*?)<\/a>/gi
  let match = anchorReg.exec(htmlText)

  while (match) {
    const anchorHtml = match[0] || ''
    const inner = match[1] || ''
    const url = normalizeUrl((anchorHtml.match(/href\s*=\s*["']([^"']+)["']/i) || [])[1], SITE_BASE)
    const title = cleanText((inner.match(/<h3\b[^>]*>([\s\S]*?)<\/h3>/i) || [])[1] || inner)
    const content = cleanText((inner.match(/<p\b[^>]*>([\s\S]*?)<\/p>/i) || [])[1]) || extractSummaryFromBlock(inner, title)
    const context = htmlText.slice(match.index, Math.min(htmlText.length, anchorReg.lastIndex + 1000))
    const sourceMatch = context.match(/<b\b[^>]*class\s*=\s*(?:["'][^"']*bk[^"']*["']|[^\s>]*bk[^\s>]*)[^>]*>\s*<a\b[^>]*href\s*=\s*["']([^"']+)["'][^>]*>([\s\S]*?)<\/a>/i)
    const dateMatch = context.match(/<em>\s*((?:19|20)\d{2}[-\/]\d{1,2}[-\/]\d{1,2})\s*<\/em>/i)
    const sourceUrl = normalizeUrl(sourceMatch && sourceMatch[1], SITE_BASE)
    const articleSource = cleanText(sourceMatch && sourceMatch[2])
    const publishTime = cleanText(dateMatch && dateMatch[1])

    if (isLikelyArticleLink(url, title)) {
      results.push({
        title,
        content,
        publishTime,
        source: articleSource || sourceNameFromUrl(url),
        images: extractImages(context, SITE_BASE),
        tags: [],
        siteName: SITE_NAME,
        url,
        sourceUrl
      })
    }

    match = anchorReg.exec(htmlText)
  }

  return uniqueByUrl(results)
}

// 功能注释：从搜索页 HTML 中解析列表项。优先解析 39 搜索页固定结构：ol#mylist* > li > dl.con_item。
function extractListItems(html) {
  const htmlText = normalizeHtmlSource(html)
  const results = []
  const dlReg = /<dl\b[^>]*class\s*=\s*(?:["'][^"']*con_item[^"']*["']|[^\s>]*con_item[^\s>]*)[^>]*>([\s\S]*?)<\/dl>/gi
  let match = dlReg.exec(htmlText)
  while (match) {
    const block = match[1] || ''
    const titleMatch = block.match(/<dt\b[^>]*>[\s\S]*?<a\b[^>]*href\s*=\s*["']([^"']+)["'][^>]*>([\s\S]*?)<\/a>[\s\S]*?<\/dt>/i)
      || block.match(/<a\b[^>]*href\s*=\s*["']([^"']+)["'][^>]*>([\s\S]*?)<\/a>/i)
    const summaryMatch = block.match(/<p\b[^>]*class\s*=\s*["'][^"']*con_item_des[^"']*["'][^>]*>([\s\S]*?)<\/p>/i)
      || block.match(/<dd\b[^>]*>[\s\S]*?<p\b[^>]*>([\s\S]*?)<\/p>/i)
      || block.match(/<p\b[^>]*>([\s\S]*?)<\/p>/i)
    const sourceMatch = block.match(/<span\b[^>]*class\s*=\s*["'][^"']*con_item_ref[^"']*["'][^>]*>[\s\S]*?<a\b[^>]*href\s*=\s*["']([^"']+)["'][^>]*>([\s\S]*?)<\/a>[\s\S]*?<\/span>/i)
    const dateMatch = block.match(/<span\b[^>]*class\s*=\s*["'][^"']*con_item_date[^"']*["'][^>]*>([\s\S]*?)<\/span>/i)
      || block.match(/(20\d{2}[-\/]\d{1,2}[-\/]\d{1,2})/)
    const url = normalizeUrl(titleMatch && titleMatch[1], SITE_BASE)
    const title = cleanText(titleMatch && titleMatch[2])
    const content = cleanText(summaryMatch && summaryMatch[1]) || extractSummaryFromBlock(block, title)
    const sourceUrl = normalizeUrl(sourceMatch && sourceMatch[1], SITE_BASE)
    const articleSource = cleanText(sourceMatch && sourceMatch[2])
    const publishTime = cleanText(dateMatch && dateMatch[1])
    if (isLikelyArticleLink(url, title)) {
      results.push({
        title,
        content,
        publishTime,
        source: articleSource || sourceNameFromUrl(url),
        images: extractImages(block, SITE_BASE),
        tags: [],
        siteName: SITE_NAME,
        url,
        sourceUrl
      })
    }
    match = dlReg.exec(htmlText)
  }

  const primary = uniqueByUrl(results)
  if (primary.length) return primary

  const mobile = extractMobileListItems(htmlText)
  if (mobile.length) return mobile

  const fallback = extractListItemsByAnchors(htmlText)
  return fallback
}

// 功能注释：按中文标点切分句子。
function splitSentences(text) {
  return cleanText(text)
    .split(/[。！？；;？]/)
    .map(item => cleanText(item))
    .filter(Boolean)
}

// 功能注释：为文章生成摘要和结构化分析。
function analyzeArticle(article, keyword) {
  const summary = cleanText(article.content) || cleanText(article.title)
  const sentences = splitSentences(`${article.title}。${article.content}`)
  return {
    keyword: cleanText(keyword),
    summary,
    keyPoints: sentences.slice(0, 3),
    sections: [
      {
        title: '核心内容',
        content: summary
      },
      {
        title: '来源信息',
        content: `${article.source || SITE_NAME}${article.publishTime ? ` · ${article.publishTime}` : ''}`
      }
    ].filter(item => item.content),
    plainText: summary
  }
}

// 功能注释：把抓取结果统一成文章对象。
function normalizeArticle(data) {
  return {
    title: cleanText(data.title),
    content: cleanText(data.content),
    publishTime: cleanText(data.publishTime),
    source: cleanText(data.source) || SITE_NAME,
    images: safeArray(data.images).filter(Boolean),
    tags: safeArray(data.tags).filter(Boolean),
    siteName: SITE_NAME,
    url: cleanText(data.url)
  }
}

// 功能注释：提取下一页起始偏移。
function extractNextStart(html) {
  const source = toStr(html)
  const match = source.match(/<a href="[^"]*start=(\d+)"[^>]*title="下一页[^"]*"/i)
  return match && match[1] ? Number(match[1]) : null
}

function findFirstIndex(text, markers, fromIndex = 0) {
  const source = toStr(text).toLowerCase()
  let best = -1
  safeArray(markers).forEach(marker => {
    const needle = toStr(marker).toLowerCase()
    if (!needle) return
    const index = source.indexOf(needle, fromIndex)
    if (index >= 0 && (best < 0 || index < best)) {
      best = index
    }
  })
  return best
}

function extractDetailBodyHtml(html) {
  const source = normalizeHtmlSource(html)
  // 遇到推荐区、相关文章区就停止截取，避免把正文和推荐模块混在一起。
  const candidates = [
    {
      starts: ['<div class="pjingbiancontent"', "<div class='pjingbiancontent'"],
      stops: [
        '<section id="article-recommend"',
        '<section class="recommend"',
        '<section class="art_rec tab"',
        '<section class="footer"',
        '<div class="art_rec art_relative',
        '<div class="art_rec art_relatedpeople',
        '<div class="art_rec art_headline',
        '<div class="art_rec art_newslist',
        '<div class="art_rec art_asklist',
        '<div class="art_rec art_rank',
        '<div class="art_page"',
        '<div class="art_show loc_artend"',
        '<div class="art_show loc_smalltl"'
      ]
    },
    {
      starts: ['<div class="art_content"', "<div class='art_content'"],
      stops: [
        '<div class="art_rec art_relative',
        '<div class="art_rec art_relatedpeople',
        '<div class="art_rec art_headline',
        '<div class="art_rec art_newslist',
        '<div class="art_rec art_asklist',
        '<div class="art_rec art_rank',
        '<div class="art_page"',
        '<div class="art_show loc_artend"',
        '<div class="art_show loc_smalltl"',
        '<section id="article-recommend"',
        '<section class="recommend"',
        '<section class="art_rec tab"',
        '<section class="footer"'
      ]
    },
    {
      starts: ['<div class="article-content"', "<div class='article-content'"],
      stops: [
        '<section id="article-recommend"',
        '<section class="recommend"',
        '<section class="art_rec tab"',
        '<section class="footer"',
        '<div class="art_rec art_relative',
        '<div class="art_rec art_relatedpeople',
        '<div class="art_rec art_headline',
        '<div class="art_rec art_newslist',
        '<div class="art_rec art_asklist',
        '<div class="art_rec art_rank',
        '<div class="art_page"'
      ]
    },
    {
      starts: ['<div class="article"', "<div class='article'"],
      stops: [
        '<section id="article-recommend"',
        '<section class="recommend"',
        '<section class="art_rec tab"',
        '<section class="footer"',
        '<div class="art_rec art_relative',
        '<div class="art_rec art_relatedpeople',
        '<div class="art_rec art_headline',
        '<div class="art_rec art_newslist',
        '<div class="art_rec art_asklist',
        '<div class="art_rec art_rank',
        '<div class="art_page"'
      ]
    }
  ]

  for (const candidate of candidates) {
    const startIndex = findFirstIndex(source, candidate.starts)
    if (startIndex >= 0) {
      const endIndex = findFirstIndex(source, candidate.stops, startIndex + 20)
      return source.slice(startIndex, endIndex >= 0 ? endIndex : source.length)
    }
  }

  const bodyStart = findFirstIndex(source, ['<body'])
  if (bodyStart >= 0) {
    const bodyStop = findFirstIndex(source, [
      '<section id="article-recommend"',
      '<section class="recommend"',
      '<section class="art_rec tab"',
      '<section class="footer"',
      '<div class="art_rec art_relative',
      '<div class="art_rec art_relatedpeople',
      '<div class="art_rec art_headline',
      '<div class="art_rec art_newslist',
      '<div class="art_rec art_asklist',
      '<div class="art_rec art_rank"',
      '<div class="art_page"'
    ], bodyStart + 5)
    return source.slice(bodyStart, bodyStop >= 0 ? bodyStop : source.length)
  }

  return source
}

// 功能注释：从详情页 HTML 中提取正文段落。
function extractParagraphs(html) {
  const source = stripHtml(extractDetailBodyHtml(html))
  const result = []
  const paragraphReg = /<p[^>]*>([\s\S]*?)<\/p>/gi
  let match = paragraphReg.exec(source)
  while (match) {
    const text = cleanText(match[1])
    if (text.length >= 20 && !/责编|责任编辑|点击数|上一篇|下一篇/.test(text) && !result.includes(text)) {
      result.push(text)
    }
    if (result.length >= 8) break
    match = paragraphReg.exec(source)
  }
  return result
}

// 功能注释：提取详情页标题。
function extractPageTitle(html) {
  const title = cleanText((toStr(html).match(/<h1[^>]*>([\s\S]*?)<\/h1>/i) || [])[1])
    || cleanText((toStr(html).match(/<title[^>]*>([\s\S]*?)<\/title>/i) || [])[1])
  return title.replace(/[-_].*?39健康网.*$/i, '').replace(/_39健康网.*$/i, '').trim()
}

// 功能注释：抓取搜索页并解析文章列表。
async function fetchSearchPage(keyword, page = 1) {
  const safeKeyword = cleanText(keyword)
  const safePage = Math.max(1, Number(page) || 1)
  if (!safeKeyword) {
    throw new Error('关键词不能为空')
  }

  const sourceUrl = getSearchUrl(safeKeyword, safePage)
  const requestUrl = sourceUrl

  const res = await crawlerRequest({
    url: sourceUrl,
    header: SITE_ITEM.requestHeader || {},
    timeout: DEFAULT_TIMEOUT,
    retry: DEFAULT_RETRY
  })

  const rawHtml = normalizeHtmlSource(res && res.data)
  const items = extractListItems(rawHtml).map(item => {
    const article = normalizeArticle(item)
    return {
      ...article,
      analysis: analyzeArticle(article, safeKeyword)
    }
  })
  const pageTitle = cleanText((rawHtml.match(/<title[^>]*>([\s\S]*?)<\/title>/i) || [])[1])
  const looksLikeAppHtml = pageTitle === '健康管理' || rawHtml.indexOf('<div id="app"></div>') !== -1
  const looksLikeProxyPath404 = pageTitle.indexOf('很抱歉') !== -1 && rawHtml.indexOf('您访问的地址不存在') !== -1
  const debug = {
    statusCode: res && res.statusCode ? res.statusCode : '',
    sourceUrl,
    requestUrl,
    keyword: safeKeyword,
    page: safePage,
    startOffset: safePage,
    htmlLength: rawHtml.length,
    hasConItem: rawHtml.indexOf('con_item') !== -1,
    pageTitle,
    matchCount: (rawHtml.match(/<dl\b[^>]*con_item/gi) || []).length,
    parsedCount: items.length,
    rawHtmlPreview: stripHtml(rawHtml).slice(0, 1500),
    firstItems: items.slice(0, 3)
  }

  if (looksLikeAppHtml) {
    throw new Error('文章抓取失败：当前返回的是项目首页')
  }

  if (looksLikeProxyPath404) {
    throw new Error('文章抓取失败：外部站点返回错误页')
  }


  return {
    items,
    nextStart: extractNextStart(rawHtml),
    url: sourceUrl,
    requestUrl,
    keyword: safeKeyword,
    page: safePage,
    startOffset: safePage,
    debug
  }
}

// 功能注释：按关键字连续抓取多页。
export async function fetchSearchPages(keyword, startPage = 1, pageCount = 2) {
  const safeStart = Math.max(1, Number(startPage) || 1)
  const safeCount = Math.max(1, Number(pageCount) || 2)
  const pages = []
  const errors = []

  for (let offset = 0; offset < safeCount; offset += 1) {
    const page = safeStart + offset
    try {
      const result = await fetchSearchPage(keyword, page)
      pages.push(result)
    } catch (error) {
      errors.push({ page, message: error?.message || '抓取失败' })
    }
  }

  const items = pages.flatMap((page) => Array.isArray(page.items) ? page.items : [])
  return {
    keyword,
    startPage: safeStart,
    nextPage: safeStart + safeCount,
    pageCount: safeCount,
    items,
    pages,
    errors
  }
}

// 功能注释：抓取外部文章详情页并解析正文。
export async function fetchArticleDetailByUrl(url, fallback = {}) {
  const target = normalizeUrl(url, SITE_BASE)
  if (!target) {
    throw new Error('文章地址不能为空')
  }

  const res = await crawlerRequest({
    url: target,
    header: SITE_ITEM.requestHeader || {},
    timeout: DEFAULT_TIMEOUT,
    retry: DEFAULT_RETRY
  })
  const html = normalizeHtmlSource(res && res.data)
  const title = extractPageTitle(html) || cleanText(fallback.title) || '健康资讯'
  const bodyHtml = extractDetailBodyHtml(html)
  const paragraphs = extractParagraphs(bodyHtml)
  const images = extractImages(bodyHtml, target)
  const summary = paragraphs[0] || cleanText(fallback.content) || cleanText(fallback.summary) || title
  const imageBlocks = images.map(imageUrl => ({ type: 'image', text: imageUrl }))
  const blocks = [
    { type: 'h2', text: title },
    ...imageBlocks,
    ...paragraphs.map(text => ({ type: 'p', text }))
  ]

  if (paragraphs.length === 0) {
    blocks.push({ type: 'p', text: summary })
  }

  return {
    id: `ext-${Math.abs(target.split('').reduce((hash, char) => ((hash << 5) - hash) + char.charCodeAt(0), 0))}`,
    category: '健康资讯',
    title,
    quote: summary,
    themeClass: 'green',
    readMinutes: Math.max(1, Math.ceil((title + paragraphs.join('')).length / 500)),
    icon: '文',
    filterKey: 'all',
    source: cleanText(fallback.source) || SITE_NAME,
    url: target,
    publishedAt: cleanText(fallback.publishTime),
    paragraphs: blocks,
    content: blocks.filter(item => item.type === 'p').map(item => item.text).join('\n'),
    // 功能注释：只使用源文章详情页中的图片；源文无图则保持空数组。
    images
  }
}

export default {
  fetchSearchPages,
  fetchArticleDetailByUrl
}
