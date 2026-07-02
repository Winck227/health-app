const viteEnv = import.meta.env || {}

export const API_CONFIG = {
  BASE_URL: viteEnv.VITE_API_BASE_URL || 'http://127.0.0.1:3030',
  TIMEOUT: 12000
}
