const fs = require('fs')
const path = require('path')
const { compileScript, compileTemplate, parse } = require('@vue/compiler-sfc')

// 功能注释：检查 Vue 单文件组件能否被解析，提前发现模板标签不闭合等问题。
const root = path.resolve(__dirname, '..')
const ignoredDirs = new Set(['node_modules', 'unpackage'])
const files = []

function walk(dir) {
  for (const item of fs.readdirSync(dir, { withFileTypes: true })) {
    if (item.isDirectory()) {
      if (!ignoredDirs.has(item.name)) walk(path.join(dir, item.name))
      continue
    }
    if (item.isFile() && item.name.endsWith('.vue')) {
      files.push(path.join(dir, item.name))
    }
  }
}

walk(root)

let failed = false
for (const file of files) {
  const source = fs.readFileSync(file, 'utf8')
  const result = parse(source, { filename: file })
  if (result.errors.length) {
    failed = true
    console.error(`Vue parse failed: ${file}`)
    result.errors.forEach((error) => console.error(error.message || String(error)))
    continue
  }

  try {
    const id = path.relative(root, file)
    if (result.descriptor.script || result.descriptor.scriptSetup) {
      // 编译 script 能提前发现 <script setup> 里的重复声明、非法宏等问题。
      compileScript(result.descriptor, { id })
    }
    if (result.descriptor.template) {
      // parse 只能确认标签闭合；模板编译还能检查 v-for、表达式、插槽等运行前错误。
      const templateResult = compileTemplate({
        source: result.descriptor.template.content,
        filename: file,
        id
      })
      if (templateResult.errors.length) {
        failed = true
        console.error(`Vue template compile failed: ${file}`)
        templateResult.errors.forEach((error) => console.error(error.message || String(error)))
      }
    }
  } catch (error) {
    failed = true
    console.error(`Vue compile failed: ${file}`)
    console.error(error.message || String(error))
  }
}

if (failed) {
  process.exitCode = 1
} else {
  console.log(`Vue SFC compile check passed: ${files.length} files.`)
}
