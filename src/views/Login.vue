<template>
  <div class="login-container">
    <div class="login-bg"></div>
    <div class="login-header">
      <h1>地质灾害监测预警系统1.0</h1>
    </div>
    <div class="login-wrapper">
      <div class="login-left">
        <div class="qrcode-section">
          <div class="qrcode-title">打开微信扫一扫</div>
          <div class="qrcode-desc">手机轻松处理工作</div>
          <div class="qrcode-img">
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
          <div class="qrcode-tip">扫码进入小程序登录</div>
        </div>
      </div>
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
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const loginFormRef = ref()

const loginForm = reactive({
  username: '',
  password: ''
})

const handleLogin = () => {
  if (!loginForm.username) {
    alert('请输入账号')
    return
  }
  if (!loginForm.password) {
    alert('请输入密码')
    return
  }
  localStorage.setItem('token', 'mock-token')
  router.push('/dashboard')
}
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
  gap: 40px;
  margin-top: 60px;
  z-index: 10;
}

.login-left {
  width: 280px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.qrcode-section {
  text-align: center;
}

.qrcode-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.qrcode-desc {
  font-size: 14px;
  color: #909399;
  margin-bottom: 20px;
}

.qrcode-img {
  width: 180px;
  height: 180px;
  margin: 0 auto;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 10px;
  box-sizing: border-box;
}

.qrcode-svg {
  width: 100%;
  height: 100%;
}

.qrcode-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 15px;
}

.login-right {
  width: 380px;
  background: rgba(255, 255, 255, 0.95);
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
