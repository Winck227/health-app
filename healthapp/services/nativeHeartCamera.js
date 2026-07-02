// 功能注释：封装 Android CameraX 原生心率插件调用，避免页面直接处理插件缺失异常。
const PLUGIN_NAME = 'HeartRateCamera'

let cachedPlugin
let hasTriedRequire = false

export function getNativeHeartCameraPlugin() {
  if (hasTriedRequire) return cachedPlugin
  hasTriedRequire = true
  try {
    if (typeof uni.requireNativePlugin !== 'function') {
      cachedPlugin = null
      return null
    }
    cachedPlugin = uni.requireNativePlugin(PLUGIN_NAME)
    return cachedPlugin || null
  } catch (error) {
    cachedPlugin = null
    return null
  }
}

export function hasNativeHeartCameraPlugin() {
  const plugin = getNativeHeartCameraPlugin()
  return Boolean(plugin && typeof plugin.start === 'function')
}

export function startNativeHeartCamera(options, callback) {
  const plugin = getNativeHeartCameraPlugin()
  if (!plugin || typeof plugin.start !== 'function') {
    throw new Error('当前安装包未集成 HeartRateCamera 原生插件，请使用自定义基座或云打包 APK')
  }
  plugin.start(options || {}, callback)
}

export function stopNativeHeartCamera() {
  const plugin = getNativeHeartCameraPlugin()
  if (plugin && typeof plugin.stop === 'function') {
    plugin.stop()
  }
}
