<template>
  <div class="login-container">
    <div class="login-bg"></div>
    <div class="login-header">
      <h1>地质灾害监测预警系统1.0</h1>
    </div>
    <div class="login-wrapper">
      <div class="login-right">
        <div class="login-form">
          <h2>账号登录</h2>
          <el-form :model="loginForm" ref="loginFormRef" label-width="0">
            <el-form-item>
              <el-input
                v-model="loginForm.username"
                placeholder="请输入账号"
                prefix-icon="User"
                class="login-input"
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                prefix-icon="Lock"
                class="login-input"
              />
            </el-form-item>
            <el-form-item>
              <div class="captcha-row">
                <el-input
                  v-model="loginForm.captcha"
                  placeholder="请输入验证码"
                  class="captcha-input"
                />
                <div class="captcha-img" @click="refreshCaptcha">
                  {{ captchaCode }}
                </div>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleLogin" class="login-btn">
                登 录
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
    <div class="login-footer">
      <div class="footer-info">
        <span>ljstar-版权所有</span>
        <span>|</span>
        <span>单位地址：四川成都成华区龙潭寺</span>
        <span>|</span>
        <span>川ICP备12012345号</span>
        <span>|</span>
        <span>电话：028-87654321</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const captchaCode = ref('')
const loginFormRef = ref()

const loginForm = reactive({
  username: '',
  password: '',
  captcha: ''
})

const generateCaptcha = () => {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz0123456789'
  let code = ''
  for (let i = 0; i < 4; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return code
}

const refreshCaptcha = () => {
  captchaCode.value = generateCaptcha()
}

const handleLogin = () => {
  if (!loginForm.username) {
    alert('请输入账号')
    return
  }
  if (!loginForm.password) {
    alert('请输入密码')
    return
  }
  if (!loginForm.captcha) {
    alert('请输入验证码')
    return
  }
  localStorage.setItem('token', 'mock-token')
  router.push('/dashboard')
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<style scoped>
.login-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: url('https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1920&h=1080&fit=crop') center/cover no-repeat;
  filter: brightness(0.8);
}

.login-header {
  position: relative;
  text-align: center;
  padding-top: 80px;
  z-index: 10;
}

.login-header h1 {
  font-size: 32px;
  color: #fff;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
}

.login-wrapper {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 60px;
  z-index: 10;
}

.login-right {
  width: 380px;
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.login-form h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #303133;
  font-size: 24px;
}

.login-input {
  width: 100%;
  height: 42px;
  margin-bottom: 15px;
}

.captcha-row {
  display: flex;
  gap: 12px;
}

.captcha-input {
  flex: 1;
  height: 42px;
}

.captcha-img {
  width: 100px;
  height: 42px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: bold;
  color: #606266;
  letter-spacing: 4px;
  cursor: pointer;
}

.login-btn {
  width: 100%;
  height: 42px;
  font-size: 16px;
  margin-top: 10px;
}

.login-footer {
  position: absolute;
  bottom: 30px;
  left: 0;
  right: 0;
  text-align: center;
  z-index: 10;
}

.footer-info {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
  background: rgba(0, 0, 0, 0.3);
  padding: 10px 20px;
  border-radius: 20px;
  display: inline-flex;
}

.footer-info span {
  display: flex;
  align-items: center;
}

.footer-info span:nth-child(odd) {
  white-space: nowrap;
}
</style>
