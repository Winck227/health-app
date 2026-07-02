const { spawnSync } = require('child_process')
const fs = require('fs')
const path = require('path')

// 功能注释：检查项目内 JS 文件语法，跳过依赖和打包产物，便于答辩前快速自测。
const root = path.resolve(__dirname, '..')
const ignoredDirs = new Set(['node_modules', 'unpackage'])
const files = []

function walk(dir) {
  for (const item of fs.readdirSync(dir, { withFileTypes: true })) {
    if (item.isDirectory()) {
      if (!ignoredDirs.has(item.name)) walk(path.join(dir, item.name))
      continue
    }
    if (item.isFile() && item.name.endsWith('.js')) {
      files.push(path.join(dir, item.name))
    }
  }
}

walk(root)

let failed = false
for (const file of files) {
  const result = spawnSync(process.execPath, ['--check', file], { stdio: 'inherit' })
  if (result.status !== 0) failed = true
}

if (failed) {
  process.exitCode = 1
} else {
  console.log(`JS syntax check passed: ${files.length} files.`)
}
