<template>
  <div class="login-container">
    <!-- Background Layer -->
    <div class="login-bg">
      <!-- Geometric glass shapes -->
      <div class="geo-shape shape-1"></div>
      <div class="geo-shape shape-2"></div>
      <div class="geo-shape shape-3"></div>
      <div class="geo-shape shape-4"></div>
    </div>

    <!-- Main Content -->
    <div class="login-main">
      <!-- Left Panel - Branding -->
      <div class="login-left">
        <div class="brand-content">
          <div class="brand-logo">
            <div class="logo-icon">
              <svg viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M40 5L70 25V55L40 75L10 55V25L40 5Z" fill="url(#logoGrad)" opacity="0.9"/>
                <path d="M40 15L60 30V50L40 65L20 50V30L40 15Z" fill="white" opacity="0.3"/>
                <path d="M40 25L50 32V48L40 55L30 48V32L40 25Z" fill="white" opacity="0.5"/>
                <circle cx="40" cy="40" r="8" fill="white" opacity="0.8"/>
                <defs>
                  <linearGradient id="logoGrad" x1="10" y1="5" x2="70" y2="75" gradientUnits="userSpaceOnUse">
                    <stop stop-color="#42a5f5"/>
                    <stop offset="1" stop-color="#1e88e5"/>
                  </linearGradient>
                </defs>
              </svg>
            </div>
            <div class="logo-text">
              <span class="logo-title">地质灾害监测预警系统v1.0</span>
              <span class="logo-subtitle"></span>
            </div>
          </div>
          <h1 class="brand-slogan">守护自然安全<br/>构建智慧监测</h1>
          <p class="brand-desc">基于物联网与大数据的智能化地质灾害监测预警平台，实现实时监测、专业预警的全链路服务</p>
          <div class="brand-features">
            <div class="feature-item">
              <div class="feature-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
                     stroke-linejoin="round">
                  <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
                </svg>
              </div>
              <span>实时监测</span>
            </div>
            <div class="feature-item">
              <div class="feature-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
                     stroke-linejoin="round">
                  <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
                  <line x1="12" y1="9" x2="12" y2="13"/>
                  <line x1="12" y1="17" x2="12.01" y2="17"/>
                </svg>
              </div>
              <span>智能预警</span>
            </div>
            <div class="feature-item">
              <div class="feature-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
                     stroke-linejoin="round">
                  <path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/>
                </svg>
              </div>
              <span>数据洞察</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Panel - Login Card -->
      <div class="login-right">
        <div class="login-card">
          <!-- Card Header -->
          <div class="card-header">
            <div class="header-text">
              <h2>欢迎回来</h2>
              <p>请登录您的账户继续</p>
            </div>
          </div>

          <!-- Login Tabs -->
          <div class="login-tabs">
            <button
              class="tab-btn"
              :class="{ active: loginMethod === 'account' }"
              @click="loginMethod = 'account'"
            >
              账号登录
            </button>
            <button
              class="tab-btn"
              :class="{ active: loginMethod === 'sms' }"
              @click="loginMethod = 'sms'"
            >
              短信登录
            </button>
            <button
              class="tab-btn"
              :class="{ active: loginMethod === 'wechat' }"
              @click="loginMethod = 'wechat'"
            >
              微信登录
            </button>
          </div>

          <!-- Account Login Form -->
          <div v-show="loginMethod === 'account'" class="form-section">
            <el-form :model="loginForm" ref="loginFormRef" label-width="0">
              <div class="input-group">
                <div class="input-wrapper">
                  <span class="input-prefix">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
                         stroke-linejoin="round">
                      <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
                      <circle cx="12" cy="7" r="4"/>
                    </svg>
                  </span>
                  <input
                    v-model="loginForm.username"
                    type="text"
                    placeholder="请输入用户名"
                    class="form-input"
                  />
                </div>
              </div>

              <div class="input-group">
                <div class="input-wrapper">
                  <span class="input-prefix">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
                         stroke-linejoin="round">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                      <path d="M7 11V7a5 5 0 0110 0v4"/>
                    </svg>
                  </span>
                  <input
                    v-model="loginForm.password"
                    type="text"
                    :class="['form-input', { 'password-hidden': !showPassword }]"
                    placeholder="请输入密码"
                  />
                  <span class="input-suffix" @click="showPassword = !showPassword">
                    <svg v-if="!showPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                         stroke-linecap="round" stroke-linejoin="round">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                      <circle cx="12" cy="12" r="3"/>
                    </svg>
                    <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                         stroke-linecap="round" stroke-linejoin="round">
                      <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/>
                      <line x1="1" y1="1" x2="23" y2="23"/>
                    </svg>
                  </span>
                </div>
              </div>

              <!-- Captcha -->
              <div class="input-group captcha-group" v-if="captchaEnabled">
                <div class="input-wrapper captcha-wrapper">
                  <span class="input-prefix">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
                         stroke-linejoin="round">
                      <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                      <path d="M9 9h.01M15 9h.01M9 15h.01M15 15h.01"/>
                    </svg>
                  </span>
                  <input
                    v-model="loginForm.captcha"
                    type="text"
                    placeholder="请输入验证码"
                    class="form-input"
                    @keyup.enter="login"
                  />
                  <div class="captcha-img" @click="getCaptcha" title="点击刷新">
                    <img :src="captchaImage" alt="验证码" />
                  </div>
                </div>
              </div>

              <!-- Remember & Forgot -->
              <div class="form-options">
                <el-checkbox v-model="rememberMe">记住我</el-checkbox>
                <a href="#" class="forgot-link">忘记密码？</a>
              </div>

              <!-- Login Button -->
              <button type="button" class="login-btn" :class="{ loading: loginLoading }" :disabled="loginLoading"
                      @click="login">
                <span v-if="loginLoading" class="btn-spinner"></span>
                {{ loginLoading ? '登录中...' : '登 录' }}
              </button>
            </el-form>
          </div>

          <!-- SMS Login Form -->
          <div v-show="loginMethod === 'sms'" class="form-section">
            <div class="input-group">
              <div class="input-wrapper">
                <span class="input-prefix">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
                       stroke-linejoin="round">
                    <path d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07 19.5 19.5 0 01-6-6 19.79 19.79 0 01-3.07-8.67A2 2 0 014.11 2h3a2 2 0 012 1.72 12.84 12.84 0 00.7 2.81 2 2 0 01-.45 2.11L8.09 9.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45 12.84 12.84 0 002.81.7A2 2 0 0122 16.92z"/>
                  </svg>
                </span>
                <input
                  v-model="loginForm.phone"
                  type="text"
                  placeholder="请输入手机号"
                  class="form-input"
                />
              </div>
            </div>

            <div class="input-group captcha-group">
              <div class="input-wrapper captcha-wrapper">
                <span class="input-prefix">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
                       stroke-linejoin="round">
                    <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                    <path d="M9 9h.01M15 9h.01M9 15h.01M15 15h.01"/>
                  </svg>
                </span>
                <input
                  v-model="loginForm.smsCode"
                  type="text"
                  placeholder="请输入短信验证码"
                  class="form-input"
                  @keyup.enter="login"
                />
                <button class="sms-btn" :disabled="smsCooldown > 0" @click="sendSms">
                  {{ smsCooldown > 0 ? `${smsCooldown}s` : '获取验证码' }}
                </button>
              </div>
            </div>

            <el-checkbox v-model="rememberMe">记住我</el-checkbox>

            <button type="button" class="login-btn" :class="{ loading: loginLoading }" :disabled="loginLoading"
                    @click="login">
              <span v-if="loginLoading" class="btn-spinner"></span>
              {{ loginLoading ? '登录中...' : '登 录' }}
            </button>
          </div>

          <!-- WeChat Login Form -->
          <div v-show="loginMethod === 'wechat'" class="form-section wechat-section">
            <div class="wechat-content">
              <div class="wechat-qr">
                <svg viewBox="0 0 200 200" class="qrcode-svg">
                  <rect width="200" height="200" fill="white"/>
                  <rect x="30" y="30" width="30" height="30" fill="#333"/>
                  <rect x="30" y="140" width="30" height="30" fill="#333"/>
                  <rect x="140" y="30" width="30" height="30" fill="#333"/>
                  <rect x="155" y="155" width="15" height="15" fill="#333"/>
                  <rect x="140" y="155" width="15" height="15" fill="#333"/>
                  <rect x="155" y="140" width="15" height="15" fill="#333"/>
                  <rect x="70" y="70" width="60" height="60" fill="white"/>
                  <rect x="80" y="80" width="40" height="40" fill="#333"/>
                  <rect x="92" y="92" width="16" height="16" fill="white"/>
                  <rect x="30" y="70" width="20" height="20" fill="#333"/>
                  <rect x="50" y="30" width="20" height="20" fill="#333"/>
                  <rect x="50" y="50" width="20" height="20" fill="#333"/>
                  <rect x="30" y="110" width="20" height="20" fill="#333"/>
                  <rect x="30" y="130" width="20" height="20" fill="#333"/>
                  <rect x="50" y="150" width="20" height="20" fill="#333"/>
                  <rect x="150" y="70" width="20" height="20" fill="#333"/>
                  <rect x="170" y="30" width="20" height="20" fill="#333"/>
                  <rect x="170" y="50" width="20" height="20" fill="#333"/>
                  <rect x="150" y="110" width="20" height="20" fill="#333"/>
                  <rect x="150" y="130" width="20" height="20" fill="#333"/>
                  <rect x="170" y="150" width="20" height="20" fill="#333"/>
                  <rect x="70" y="30" width="20" height="20" fill="#333"/>
                  <rect x="90" y="30" width="20" height="20" fill="#333"/>
                  <rect x="110" y="30" width="20" height="20" fill="#333"/>
                  <rect x="130" y="30" width="20" height="20" fill="#333"/>
                  <rect x="70" y="170" width="20" height="20" fill="#333"/>
                  <rect x="90" y="170" width="20" height="20" fill="#333"/>
                  <rect x="110" y="170" width="20" height="20" fill="#333"/>
                  <rect x="130" y="170" width="20" height="20" fill="#333"/>
                  <rect x="30" y="70" width="20" height="20" fill="#333"/>
                  <rect x="30" y="90" width="20" height="20" fill="#333"/>
                  <rect x="30" y="110" width="20" height="20" fill="#333"/>
                  <rect x="170" y="70" width="20" height="20" fill="#333"/>
                  <rect x="170" y="90" width="20" height="20" fill="#333"/>
                  <rect x="170" y="110" width="20" height="20" fill="#333"/>
                </svg>
              </div>
              <p class="wechat-tip">打开微信扫一扫登录</p>
              <p class="wechat-desc">手机轻松处理工作</p>
            </div>
          </div>

        </div>
      </div>
    </div>

    <!-- Footer -->
    <div class="login-footer">
      <p>Copyright © 2024 地质灾害监测预警系统 All Rights Reserved</p>
    </div>

  </div>
</template>

<script setup lang="ts">
import axios from 'axios'
import {ElMessage} from 'element-plus'
import {onMounted, onUnmounted, reactive, ref} from 'vue'
import {useRouter} from 'vue-router'

const router = useRouter()

const loginFormRef = ref()
const loginMethod = ref('account')
const showPassword = ref(false)
const loginLoading = ref(false)

// 登录表单数据
const loginForm = reactive({
  username: '',
  password: '',
  captcha: '',
  phone: '',
  smsCode: '',
})

const captchaImage = ref('')
let captchaKey = ''
const captchaEnabled = ref(false)
const rememberMe = ref(false)
const smsCooldown = ref(0)
let smsTimer: ReturnType<typeof setInterval> | null = null

//向后端获取验证码
const getCaptcha = async () => {
  try {
    const res = await axios.get('/api/v1/auth/captcha')
    if (res.data?.code && res.data.code !== 200) {
      ElMessage.error(res.data.msg || '获取验证码失败')
      return
    }
    captchaKey = res.data.data.captchaKey
    captchaImage.value = 'data:image/png;base64,' + res.data.data.captchaImage
    captchaEnabled.value = res.data.data.captchaEnabled
    loginForm.captcha = ''
  } catch (err: any) {
    console.error('获取验证码失败:', err)
    ElMessage.error(err?.response?.data?.msg || err?.message || '获取验证码失败')
  }
}

// 发送短信验证码
const sendSms = async () => {
  if (!loginForm.phone) {
    ElMessage.warning('请输入手机号')
    return
  }
  if (smsCooldown.value > 0) return

  try {
    const res = await axios.post('/api/v1/auth/sms', {phone: loginForm.phone})
    if (res.data?.code && res.data.code !== 200) {
      ElMessage.error(res.data.msg || '发送失败')
      return
    }
    ElMessage.success('短信验证码已发送')
    smsCooldown.value = 60
    smsTimer = setInterval(() => {
      smsCooldown.value--
      if (smsCooldown.value <= 0 && smsTimer) {
        clearInterval(smsTimer)
        smsTimer = null
      }
    }, 1000)
  } catch (err: any) {
    console.error('发送短信验证码失败:', err)
    ElMessage.error(err?.response?.data?.msg || err?.message || '发送失败')
  }
}

// 登录函数
const login = async () => {
  if (loginLoading.value) return
  loginLoading.value = true
  try {
    const loginData: Record<string, any> = {
      rememberMe: rememberMe.value
    }

    if (loginMethod.value === 'account') {
      loginData.username = loginForm.username
      loginData.password = loginForm.password
      if (captchaEnabled.value) {
        loginData.code = loginForm.captcha
        loginData.uuid = captchaKey
      }
    } else {
      loginData.phone = loginForm.phone
      loginData.smsCode = loginForm.smsCode
    }

    const res = await axios.post('/api/v1/auth/login', loginData)
    if (res.data?.code && res.data.code !== 200) {
      ElMessage.error(res.data.msg || '登录失败')
      if (captchaEnabled.value) getCaptcha()
      return
    }
    localStorage.setItem('token', res.data.data.token)

    const userRes = await axios.get('/api/v1/auth/getInfo', {
      headers: { Authorization: `Bearer ${res.data.data.token}` }
    })
    const user = userRes.data.data?.user
    if (user) {
      localStorage.setItem('userInfo', JSON.stringify({
        id: user.userId,
        username: user.username,
        realName: user.nickName,
        orgId: user.deptId,
        orgName: user.deptName
      }))
    }

    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error: any) {
    console.error('登录失败:', error)
    ElMessage.error(error?.response?.data?.msg || error?.message || '登录失败')
    if (captchaEnabled.value) getCaptcha()
  } finally {
    loginLoading.value = false
  }
}

onMounted(() => {
  getCaptcha()
})

onUnmounted(() => {
  if (smsTimer) clearInterval(smsTimer)
})
</script>

<style scoped>
/* CSS Variables */
:root {
  --primary: #1890ff;
  --primary-dark: #096dd9;
  --primary-light: #40a9ff;
  --bg-light: #FAFAFA;
  --text-dark: #1a1a2e;
  --text-muted: #6B7280;
  --border-light: #E5E7EB;
  --card-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.15);
  --transition: 0.3s ease;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.login-container {
  position: fixed;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  font-family: 'PingFang SC', 'Microsoft YaHei', -apple-system, BlinkMacSystemFont, sans-serif;
}

/* Background */
.login-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #42a5f5 0%, #1e88e5 50%, #1565c0 100%);
  overflow: hidden;
}

.geo-shape {
  position: absolute;
  border-radius: 24px;
  backdrop-filter: blur(20px);
  animation: float 20s ease-in-out infinite;
}

.shape-1 {
  width: 400px;
  height: 400px;
  top: -100px;
  left: -100px;
  background: linear-gradient(135deg, rgba(255,255,255,0.15), rgba(255,255,255,0.05));
  transform: rotate(15deg);
}

.shape-2 {
  width: 300px;
  height: 300px;
  top: 50%;
  left: 10%;
  background: linear-gradient(135deg, rgba(255,255,255,0.12), rgba(255,255,255,0.03));
  animation-delay: -5s;
}

.shape-3 {
  width: 250px;
  height: 250px;
  bottom: -50px;
  right: 20%;
  background: linear-gradient(135deg, rgba(30,136,229,0.2), rgba(30,136,229,0.05));
  animation-delay: -10s;
}

.shape-4 {
  width: 180px;
  height: 180px;
  top: 30%;
  right: 5%;
  background: linear-gradient(135deg, rgba(255,255,255,0.1), rgba(255,255,255,0.02));
  animation-delay: -15s;
}

@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  25% { transform: translateY(-20px) rotate(5deg); }
  50% { transform: translateY(0) rotate(0deg); }
  75% { transform: translateY(20px) rotate(-5deg); }
}

/* Main Layout */
.login-main {
  position: relative;
  display: flex;
  width: 100%;
  height: 100%;
  z-index: 10;
}

/* Left Panel */
.login-left {
  flex: 1.2;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.brand-content {
  max-width: 520px;
  color: white;
  animation: fadeInUp 0.8s ease;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 40px;
}

.logo-icon {
  width: 64px;
  height: 64px;
  filter: drop-shadow(0 8px 20px rgba(0,0,0,0.2));
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.logo-title {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 2px;
}

.logo-subtitle {
  font-size: 14px;
  opacity: 0.8;
  margin-top: 4px;
}

.brand-slogan {
  font-size: 48px;
  font-weight: 800;
  line-height: 1.2;
  margin-bottom: 24px;
  text-shadow: 0 4px 20px rgba(0,0,0,0.15);
}

.brand-desc {
  font-size: 16px;
  line-height: 1.8;
  opacity: 0.9;
  margin-bottom: 40px;
  max-width: 450px;
}

.brand-features {
  display: flex;
  gap: 32px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  font-weight: 500;
}

.feature-icon {
  width: 40px;
  height: 40px;
  background: rgba(255,255,255,0.15);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
}

.feature-icon svg {
  width: 20px;
  height: 20px;
}

/* Right Panel */
.login-right {
  flex: 0.8;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: #F1F5F9;
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.35);
  animation: fadeInUp 0.8s ease 0.2s both;
}

/* Card Header */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
}

.header-text h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-dark);
  margin-bottom: 8px;
}

.header-text p {
  font-size: 14px;
  color: var(--text-muted);
}

/* Login Tabs */
.login-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 28px;
  background: var(--bg-light);
  padding: 4px;
  border-radius: 12px;
}

.tab-btn {
  flex: 1;
  padding: 12px 16px;
  border: none;
  background: transparent;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-muted);
  cursor: pointer;
  border-radius: 8px;
  transition: var(--transition);
}

.tab-btn.active {
  background: white;
  color: var(--primary);
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

/* Form */
.form-section {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.input-group {
  margin-bottom: 20px;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  background: #FFFFFF;
  border-radius: 12px;
  transition: var(--transition);
  border: 2px solid #E2E8F0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.input-wrapper:focus-within {
  border-color: var(--primary);
  background: white;
  box-shadow: 0 0 0 3px rgba(24, 144, 255, 0.15), 0 2px 6px rgba(0,0,0,0.06);
}

.input-prefix {
  width: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
}

.input-prefix svg {
  width: 20px;
  height: 20px;
}

.form-input {
  flex: 1;
  padding: 14px 0;
  border: none;
  background: transparent;
  font-size: 15px;
  color: var(--text-dark);
  outline: none;
}

.form-input::placeholder {
  color: #9CA3AF;
}

.form-input.password-hidden {
  -webkit-text-security: disc;
  font-family: sans-serif;
}

.input-suffix {
  padding-right: 12px;
  cursor: pointer;
  color: var(--text-muted);
}

.input-suffix svg {
  width: 20px;
  height: 20px;
}

/* Captcha */
.captcha-wrapper .form-input {
  padding-right: 8px;
}

.captcha-img {
  width: 100px;
  height: 42px;
  margin-right: 4px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
}

.captcha-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* SMS Button */
.sms-btn {
  padding: 8px 16px;
  background: var(--primary);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: var(--transition);
}

.sms-btn:hover:not(:disabled) {
  background: var(--primary-dark);
}

.sms-btn:disabled {
  background: #D1D5DB;
  cursor: not-allowed;
}

/* Options */
.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.forgot-link {
  font-size: 14px;
  color: var(--primary);
  text-decoration: none;
  transition: var(--transition);
}

.forgot-link:hover {
  color: var(--primary-dark);
}

/* Login Button */
.login-btn {
  width: 100%;
  padding: 16px;
  background: #1890ff;
  color: #fff;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(24, 144, 255, 0.4), 0 2px 6px rgba(0,0,0,0.1);
  text-shadow: 0 1px 2px rgba(0,0,0,0.2);
  position: relative;
  overflow: hidden;
}

.login-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
  transition: left 0.5s ease;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(24, 144, 255, 0.5), 0 4px 10px rgba(0,0,0,0.15);
  background: #40a9ff;
}

.login-btn:hover::before {
  left: 100%;
}

.login-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
}

.login-btn:disabled,
.login-btn.loading {
  background: #91caff;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

.login-btn.loading::before {
  display: none;
}

.btn-spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
  margin-right: 8px;
  vertical-align: -3px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* WeChat Login */
.wechat-section {
  padding: 20px 0;
}

.wechat-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.wechat-qr {
  width: 180px;
  height: 180px;
  background: white;
  border-radius: 16px;
  padding: 12px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
  margin-bottom: 20px;
}

.wechat-qr .qrcode-svg {
  width: 100%;
  height: 100%;
}

.wechat-tip {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-dark);
  margin-bottom: 8px;
}

.wechat-desc {
  font-size: 14px;
  color: var(--text-muted);
}

/* Footer */
.login-footer {
  position: absolute;
  bottom: 24px;
  left: 0;
  right: 0;
  text-align: center;
  z-index: 10;
}

.login-footer p {
  font-size: 12px;
  color: rgba(255,255,255,0.7);
}

/* Responsive */
@media (max-width: 900px) {
  .login-left {
    display: none;
  }

  .login-right {
    flex: 1;
  }
}

@media (max-width: 480px) {
  .login-card {
    padding: 28px 20px;
    border-radius: 20px;
  }

  .brand-slogan {
    font-size: 36px;
  }

  .header-text h2 {
    font-size: 24px;
  }

  .login-tabs {
    margin-bottom: 20px;
  }
}
</style>