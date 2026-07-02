function getErrorMessage(error, fallback = '操作失败') {
  if (typeof error === 'string' && error.trim()) return error.trim()
  const message = error?.message || error?.errMsg || error?.data?.message || ''
  return String(message || fallback)
}

export function showSuccess(title = '操作成功') {
  uni.showToast({
    title,
    icon: 'success',
    duration: 1600
  })
}

export function showError(error, fallback = '操作失败') {
  uni.showToast({
    title: getErrorMessage(error, fallback),
    icon: 'none',
    duration: 2200
  })
}
