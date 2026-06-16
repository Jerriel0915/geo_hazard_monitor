<!-- src/pages/login.vue -->
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useSafeArea } from '@/composables/useSafeArea'
import authApi from '@/utils/auth'

const { statusBarHeight } = useSafeArea()

const username = ref('')
const password = ref('')
const captchaCode = ref('')
const captchaKey = ref('')
const captchaImage = ref('')
const loading = ref(false)
const showPassword = ref(false)
const systemTitle = ref('边坡监测预警系统')
const copyright = ref('© 2025 交通边坡监测预警系统 版权所有')
const versionName = ref('1.0.0')

onMounted(async () => {
  const accessToken = uni.getStorageSync('accessToken')
  if (accessToken) {
    uni.switchTab({ url: '/pages/index' })
    return
  }
  await refreshCaptcha()
})

async function refreshCaptcha() {
  try {
    const data = await authApi.getCaptcha()
    if (data.captchaEnabled) {
      captchaKey.value = data.captchaKey
      // 后端返回的是纯 base64 或带 data:image 前缀，统一处理
      captchaImage.value = data.captchaImage.startsWith('data:')
        ? data.captchaImage
        : `data:image/png;base64,${data.captchaImage}`
    }
    else {
      captchaImage.value = ''
    }
  }
  catch (error) {
    console.error('获取验证码失败:', error)
  }
}

async function handleLogin() {
  if (!username.value) {
    uni.showToast({ title: '请输入账号', icon: 'none' })
    return
  }
  if (!password.value) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return
  }
  if (!captchaCode.value) {
    uni.showToast({ title: '请输入验证码', icon: 'none' })
    return
  }

  loading.value = true
  try {
    const loginResult = await authApi.login(
      username.value,
      password.value,
      captchaCode.value,
      captchaKey.value,
    )

    uni.setStorageSync('accessToken', loginResult.token)

    const info = await authApi.getUserInfo()
    const user = {
      id: info.user.userId,
      username: info.user.userName,
      nickname: info.user.nickName,
      phone: info.user.phonenumber,
      avatar: info.user.avatar || '',
    }
    uni.setStorageSync('user', JSON.stringify(user))

    uni.showToast({ title: '登录成功', icon: 'success' })

    setTimeout(() => {
      uni.switchTab({ url: '/pages/index' })
    }, 800)
  }
  catch (error: any) {
    console.error('登录失败:', error)
    uni.showToast({ title: error.message || '登录失败', icon: 'none' })
    captchaCode.value = ''
    await refreshCaptcha()
  }
  finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="login-container">
    <view class="login-bg">
      <view class="status-bar-placeholder" :style="{ height: `${statusBarHeight}px` }" />
      <view class="bg-circle bg-circle-1" />
      <view class="bg-circle bg-circle-2" />
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
            v-model="username"
            type="text"
            placeholder="请输入账号"
            class="input"
            maxlength="32"
          >
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
          >
          <view class="input-suffix" @click="showPassword = !showPassword">
            <zui-svg-icon :icon="showPassword ? 'eye-off' : 'eye'" width="32rpx" />
          </view>
        </view>

        <view class="form-item">
          <view class="input-icon-wrap">
            <zui-svg-icon icon="lock" width="36rpx" />
          </view>
          <input
            v-model="captchaCode"
            type="text"
            placeholder="请输入验证码"
            class="input"
            maxlength="6"
          >
          <view class="captcha-image" @click="refreshCaptcha">
            <image
              v-if="captchaImage"
              :src="captchaImage"
              mode="scaleToFill"
              class="captcha-img"
            />
            <text v-else class="captcha-loading">加载中</text>
          </view>
        </view>

        <button class="login-btn" :loading="loading" @click="handleLogin">
          <text v-if="!loading">登 录</text>
        </button>
      </view>
    </view>
    <view class="footer">
      <view class="version">
        v{{ versionName }}
      </view>
      <view class="copyright" :style="{ paddingBottom: `${statusBarHeight + 20}px` }">
        {{ copyright }}
      </view>
    </view>
  </view>
</template>

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

.captcha-image {
  width: 160rpx;
  height: 60rpx;
  margin-left: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  border-radius: 8rpx;
  overflow: hidden;
}

.captcha-img {
  width: 160rpx;
  height: 60rpx;
}

.captcha-loading {
  font-size: 22rpx;
  color: #9ca3af;
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
