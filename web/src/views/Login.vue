<template>
  <div class="login-container">
    <div class="login-bg"></div>
    <div class="login-header">
      <h1>地质灾害监测预警系统1.0</h1>
    </div>
    <div class="login-wrapper">
      <div class="login-card">
        <!-- 左侧：微信小程序入口 -->
        <div class="login-left">
          <div class="qrcode-section">
            <div class="qrcode-title">打开微信扫一扫</div>
            <div class="qrcode-desc">手机轻松处理工作</div>
            <div class="qrcode-img">
              <!--此处为二维码 -->
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
        <!-- 右侧：账号登录 -->
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
              <!-- 算术验证码 -->
              <el-form-item class="captcha-item" v-if="captchaEnabled">
                <div class="captcha-wrapper">
                  <el-input
                    v-model="loginForm.captcha"
                    placeholder="请输入验证码"
                    prefix-icon="Grid"
                    class="captcha-input"
                    @keyup.enter="login"
                  />
                  <div class="captcha-code" @click="getCaptcha">
                    <img :src="captchaImage" alt="验证码" class="captcha-img" />
                    <span class="captcha-refresh" title="点击刷新">↻</span>
                  </div>
                </div>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="login" class="login-btn">
                  登 录
                </el-button>
              </el-form-item>
            </el-form>
          </div>
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
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()

const loginFormRef = ref()

// 登录表单数据
const loginForm = reactive({
  username: '',       //账号
  password: '',       //密码
  captcha: '',        //验证码
})


const captchaImage = ref('')        //验证码图片
let captchaAnswer = 0               //验证码答案
let captchaKey = ''                 //验证码key
const captchaEnabled = ref(false)           //验证码是否启用，默认启用

//向后端获取验证码
const getCaptcha = async () => {
  try{

    const res = await axios.get('/api/v1/system/auth/captcha')

    //验证码key
    captchaKey = res.data.data.captchaKey

    //验证码图片
    captchaImage.value = 'data:image/png;base64,' + res.data.data.captchaImage
    //验证码是否启用，默认启用
    captchaEnabled.value = res.data.data.captchaEnabled

    //清空验证码输入框
    loginForm.captcha = ''
  }catch(err){
    ElMessage.error('获取验证码失败')
  }
}

// 登录函数
const login = async () => {
  try {
    // 1. 构建登录参数
    const loginData: Record<string, any> = {
      username: loginForm.username,
      password: loginForm.password
    }

    // 2. 开启验证码才携带
    if (captchaEnabled.value) {
      loginData.code = loginForm.captcha
      loginData.uuid = captchaKey
    }

    // 3. 发送请求
    const res = await axios.post('/api/v1/system/auth/login', loginData)

    //4. 登录成功逻辑将token存储到本地存储
    localStorage.setItem('token', res.data.data.token)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error) {
    // 请求异常
    ElMessage.error('登录请求失败')
    getCaptcha()
  }
}

onMounted(() => {
  getCaptcha()
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

/* 整体卡片容器 */
.login-card {
  display: flex;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  width: 720px;
  min-height: 420px;
}

/* 左侧：微信小程序 */
.login-left {
  width: 280px;
  background: linear-gradient(180deg, #4a90d9 0%, #357abd 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30px;
}

.qrcode-section {
  text-align: center;
  color: #fff;
}

.qrcode-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}

.qrcode-desc {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 20px;
}

.qrcode-img {
  width: 160px;
  height: 160px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  padding: 8px;
  box-sizing: border-box;
}

.qrcode-svg {
  width: 100%;
  height: 100%;
}

.qrcode-tip {
  font-size: 12px;
  opacity: 0.8;
  margin-top: 15px;
}

/* 右侧：账号登录 */
.login-right {
  flex: 1;
  padding: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-form {
  width: 100%;
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
}

/* 验证码样式 */
.captcha-item {
  margin-bottom: 15px;
}

.captcha-wrapper {
  display: flex;
  gap: 10px;
  align-items: center;
}

.captcha-input {
  flex: 1;
  height: 42px;
}

.captcha-code {
  width: 140px;
  height: 42px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
  transition: opacity 0.2s;
}

.captcha-code:hover {
  opacity: 0.9;
}

.captcha-text {
  font-size: 18px;
  font-weight: bold;
  color: #fff;
  letter-spacing: 1px;
}

.captcha-refresh {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.captcha-img {
  width: 100%;
  height: 100%;
  object-fit: cover;   
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
