<!-- src/pages/login.vue -->
<template>
  <view class="login-container">
    <view class="login-bg">
      <view class="status-bar-placeholder" :style="{ height: `${statusBarHeight}px` }"></view>
      <view class="bg-circle bg-circle-1"></view>
      <view class="bg-circle bg-circle-2"></view>
    </view>

    <view class="login-content" :style="{ paddingTop: `${statusBarHeight + 80}px` }">
      <view class="login-header">
        <text class="title">{{ systemTitle }}</text>
        <text class="subtitle">边坡监测 · 智能预警</text>
      </view>

      <view class="login-form">
        <view class="form-item">
          <view class="input-icon-wrap">
            <zui-svg-icon icon="phone" width="36rpx" />
          </view>
          <input
            v-model="phone"
            type="number"
            placeholder="请输入手机号"
            class="input"
            maxlength="11"
          />
        </view>

        <view class="form-item">
          <view class="input-icon-wrap">
            <zui-svg-icon icon="lock" width="36rpx" />
          </view>
          <input
            v-model="password"
            :password="!showPassword"
            placeholder="请输入密码"
            class="input"
          />
          <view class="input-suffix" @click="showPassword = !showPassword">
            <zui-svg-icon :icon="showPassword ? 'eye-off' : 'eye'" width="32rpx" />
          </view>
        </view>

        <button class="login-btn" @click="handleLogin" :loading="loading">
          <text v-if="!loading">登 录</text>
        </button>
      </view>
    </view>
    <view class="footer">
      <view class="version">v{{ versionName }}</view>
      <view class="copyright" :style="{ paddingBottom: `${statusBarHeight + 20}px` }">{{ copyright }}</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { useSafeArea } from '@/composables/useSafeArea'
import authApi from '@/utils/auth'
import { onMounted, ref } from 'vue'

const { statusBarHeight } = useSafeArea()

const phone = ref('')
const password = ref('')
const loading = ref(false)
const showPassword = ref(false)
const systemTitle = ref('边坡监测预警系统')
const copyright = ref('© 2025 交通边坡监测预警系统 版权所有')
const versionName = ref('1.0.0')

onMounted(async () => {
  const accessToken = uni.getStorageSync('accessToken')
  if (accessToken) {
    uni.switchTab({ url: '/pages/index' })
  }
})

const handleLogin = async () => {
  if (!phone.value) {
    uni.showToast({ title: '请输入手机号', icon: 'none' })
    return
  }
  if (!password.value) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return
  }

  loading.value = true
  try {
    const data = await authApi.login(phone.value, password.value)

    uni.setStorageSync('accessToken', data.accessToken)
    uni.setStorageSync('refreshToken', data.refreshToken)
    uni.setStorageSync('user', JSON.stringify(data.user))

    uni.showToast({ title: '登录成功', icon: 'success' })

    setTimeout(() => {
      uni.switchTab({ url: '/pages/index' })
    }, 1000)
  } catch (error: any) {
    console.error('登录失败:', error)
    uni.showToast({ title: error.message || '登录失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 60vh;
  background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
}

.status-bar-placeholder {
  width: 100%;
  background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.bg-circle-1 {
  width: 400rpx;
  height: 400rpx;
  top: -100rpx;
  right: -100rpx;
}

.bg-circle-2 {
  width: 300rpx;
  height: 300rpx;
  top: 200rpx;
  left: -80rpx;
}

.login-content {
  position: relative;
  z-index: 1;
  padding: 80rpx 40rpx;
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 60rpx;
}

.logo {
  width: 360px;
  height: 40px;
  margin-bottom: 10px;
  margin-right: 15px;
}

.logo-icon {
  font-size: 72rpx;
}

.title {
  font-size: 44rpx;
  font-weight: bold;
  color: #ffffff;
  margin-bottom: 12rpx;
}

.subtitle {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
}

.login-form {
  background: #ffffff;
  border-radius: 32rpx;
  padding: 48rpx 32rpx;
  box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.1);
}

.form-item {
  display: flex;
  align-items: center;
  background: #f7f8fc;
  border-radius: 16rpx;
  padding: 0 24rpx;
  margin-bottom: 24rpx;
  height: 96rpx;
}

.input-icon-wrap {
  margin-right: 16rpx;
  display: flex;
  align-items: center;
}

.input {
  flex: 1;
  font-size: 30rpx;
  color: #1a1a2e;
}

.input-suffix {
  padding: 16rpx;
  font-size: 32rpx;
}

.login-btn {
  width: 100%;
  height: 96rpx;
  background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  margin-top: 32rpx;

  text {
    font-size: 32rpx;
    font-weight: 500;
    color: #ffffff;
  }
}

.footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 10;
}

.version {
  font-size: 24rpx;
  color: #999999;
  padding: 8rpx 24rpx;
}

.copyright {
  text-align: center;
  font-size: 24rpx;
  color: #666666;
  padding: 8rpx 24rpx 24rpx;
}
</style>
