const fs = require('fs')
const path = require('path')

// 功能注释：检查 pages.json 和源码中的页面跳转路径，避免启动后点击菜单才发现页面不存在。
const root = path.resolve(__dirname, '..')
const ignoredDirs = new Set(['node_modules', 'unpackage'])
const pageConfig = JSON.parse(fs.readFileSync(path.join(root, 'pages.json'), 'utf8'))
const declaredPages = new Set((pageConfig.pages || []).map((item) => `/${item.path}`))
const sourceFiles = []

function walk(dir) {
  for (const item of fs.readdirSync(dir, { withFileTypes: true })) {
    if (item.isDirectory()) {
      if (!ignoredDirs.has(item.name)) walk(path.join(dir, item.name))
      continue
    }
    if (item.isFile() && /\.(vue|js|json)$/.test(item.name)) {
      sourceFiles.push(path.join(dir, item.name))
    }
  }
}

function stripQuery(route) {
  return route.split('?')[0]
}

walk(root)

let failed = false
for (const route of declaredPages) {
  const filePath = path.join(root, `${route.slice(1)}.vue`)
  if (!fs.existsSync(filePath)) {
    failed = true
    console.error(`Missing page file declared in pages.json: ${route}`)
  }
}

for (const file of sourceFiles) {
  const source = fs.readFileSync(file, 'utf8')
  const matches = source.match(/\/pages\/[A-Za-z0-9_/-]+(?:\?[A-Za-z0-9_=&${}./%-]+)?/g) || []
  for (const rawRoute of matches) {
    const route = stripQuery(rawRoute)
    if (!declaredPages.has(route)) {
      failed = true
      console.error(`Unknown page route in ${path.relative(root, file)}: ${rawRoute}`)
    }
  }
}

if (failed) {
  process.exitCode = 1
} else {
  console.log(`Route check passed: ${declaredPages.size} pages, ${sourceFiles.length} source files.`)
}
