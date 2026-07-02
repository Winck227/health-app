function getRuntimePlatform() {
  try {
    const info = uni.getSystemInfoSync()
    return info.uniPlatform || info.platform || ''
  } catch (error) {
    return ''
  }
}

export function isAppRuntime() {
  const platform = getRuntimePlatform()
  return platform === 'app' || platform === 'app-plus'
}

export default {
  isAppRuntime
}
