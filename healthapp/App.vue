<script>
import { fetchSession, logUserAction } from '@/services/healthApi.js'
import { flushStepCounterStorage, startStepCounter } from '@/services/stepCounter.js'
import { clearAuthSession, hasAuthToken } from '@/utils/auth.js'
import { getLastMainPageRoute, getLoginRoute } from '@/utils/router.js'

export default {
  async onLaunch() {
    startStepCounter(0).catch(() => {})
    if (!hasAuthToken()) {
      uni.reLaunch({ url: getLoginRoute() })
      return
    }
    try {
      await fetchSession()
      logUserAction('app_launch_restore_session')
      uni.reLaunch({ url: getLastMainPageRoute() })
    } catch (error) {
      clearAuthSession()
      uni.reLaunch({ url: getLoginRoute() })
    }
  },
  onShow() {
    startStepCounter(0).catch(() => {})
  },
  onHide() {
    flushStepCounterStorage()
  }
}
</script>

<style>
page {
  background: #F6F8FB;
  color: #0F172A;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC",
    "Hiragino Sans GB", "Microsoft YaHei", Arial, sans-serif;
}

:root,
page {
  --c-primary: #12B76A;
  --c-primary-weak: #EAFBF3;
  --c-bg: #F6F8FB;
  --c-card: #FFFFFF;
  --c-title: #0F172A;
  --c-text: #334155;
  --c-muted: #94A3B8;
  --c-divider: #EEF2F7;
  --c-orange: #FF7A2F;
  --c-pink: #FF3B6B;
  --c-blue: #4F67FF;
  --shadow-card: 0 20rpx 60rpx rgba(15, 23, 42, 0.06);
  --shadow-btn: 0 32rpx 64rpx rgba(15, 23, 42, 0.12);
  --r-lg: 40rpx;
  --r-md: 32rpx;
  --r-sm: 24rpx;
}

.u-card {
  background: var(--c-card);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow-card);
}

.u-pill {
  border-radius: 9999rpx;
}
</style>
