<template>
  <div class="user-profile">
    <!-- Hero Section -->
    <div class="profile-hero">
      <div class="hero-bg">
        <div class="bg-shape shape-1"></div>
        <div class="bg-shape shape-2"></div>
        <div class="bg-shape shape-3"></div>
      </div>
      <div class="hero-content">
        <div class="avatar-wrapper">
          <div class="avatar-ring"></div>
          <div class="avatar-container">
            <svg v-if="!userInfo.avatar" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="avatar-icon">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>
            <img v-else :src="userInfo.avatar" class="avatar-img" />
          </div>
          <button class="avatar-edit-btn" @click="triggerAvatarEdit">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
              <circle cx="12" cy="13" r="4"/>
            </svg>
          </button>
        </div>
        <div class="user-meta">
          <h1 class="user-name">{{ userInfo.realName || '未设置姓名' }}</h1>
          <p class="user-role">
            <span class="role-badge">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 2L2 7l10 5 10-5-10-5z"/>
                <path d="M2 17l10 5 10-5"/>
                <path d="M2 12l10 5 10-5"/>
              </svg>
              {{ userInfo.orgName || '系统用户' }}
            </span>
          </p>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="profile-content">
      <!-- Left Column -->
      <div class="content-left">
        <!-- Basic Info Card -->
        <div class="info-card" :class="{ editing: isEditing }">
          <div class="card-header">
            <div class="card-title">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
              <span>基本信息</span>
            </div>
            <button class="edit-btn" @click="toggleEdit">
              <svg v-if="!isEditing" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
              </svg>
              <span>{{ isEditing ? '取消' : '编辑' }}</span>
            </button>
          </div>
          <div class="card-body">
            <div class="info-grid">
              <div class="info-item">
                <label class="info-label">用户名</label>
                <div class="info-value">
                  <span v-if="!isEditing">{{ userInfo.username }}</span>
                  <el-input v-else v-model="editForm.username" disabled size="large" />
                </div>
              </div>
              <div class="info-item">
                <label class="info-label">真实姓名</label>
                <div class="info-value">
                  <span v-if="!isEditing">{{ userInfo.realName || '-' }}</span>
                  <el-input v-else v-model="editForm.realName" size="large" placeholder="请输入真实姓名" />
                </div>
              </div>
              <div class="info-item">
                <label class="info-label">手机号码</label>
                <div class="info-value">
                  <span v-if="!isEditing">{{ userInfo.phone || '-' }}</span>
                  <el-input v-else v-model="editForm.phone" size="large" placeholder="请输入手机号码" />
                </div>
              </div>
              <div class="info-item">
                <label class="info-label">电子邮箱</label>
                <div class="info-value">
                  <span v-if="!isEditing">{{ userInfo.email || '-' }}</span>
                  <el-input v-else v-model="editForm.email" size="large" placeholder="请输入电子邮箱" />
                </div>
              </div>
              <div class="info-item full-width">
                <label class="info-label">所属组织</label>
                <div class="info-value">
                  <span v-if="!isEditing">{{ userInfo.orgName || '-' }}</span>
                  <el-select v-else v-model="editForm.orgId" size="large" placeholder="请选择组织" class="org-select">
                    <el-option label="系统管理部" :value="1" />
                    <el-option label="监测运维部" :value="2" />
                    <el-option label="数据分析部" :value="3" />
                    <el-option label="应急指挥中心" :value="4" />
                  </el-select>
                </div>
              </div>
            </div>
          </div>
          <div v-if="isEditing" class="card-footer">
            <el-button size="large" @click="cancelEdit">取消</el-button>
            <el-button type="primary" size="large" @click="saveUserInfo" :loading="saving">保存修改</el-button>
          </div>
        </div>

        <!-- Account Security Card -->
        <div class="info-card security-card">
          <div class="card-header">
            <div class="card-title">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
              <span>账号安全</span>
            </div>
          </div>
          <div class="card-body">
            <div class="security-item" @click="showPasswordDialog = true">
              <div class="security-left">
                <div class="security-icon">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"/>
                  </svg>
                </div>
                <div class="security-text">
                  <span class="security-title">登录密码</span>
                  <span class="security-desc">定期修改密码可保护账号安全</span>
                </div>
              </div>
              <div class="security-action">
                <span>修改</span>
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="9 18 15 12 9 6"/>
                </svg>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Column -->
      <div class="content-right">
        <!-- Stats Card -->
        <div class="info-card stats-card">
          <div class="card-header">
            <div class="card-title">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
              </svg>
              <span>账户概览</span>
            </div>
          </div>
          <div class="card-body">
            <div class="stats-grid">
              <div class="stat-item">
                <span class="stat-value">{{ accountInfo.loginCount }}</span>
                <span class="stat-label">登录次数</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ accountInfo.roleCount }}</span>
                <span class="stat-label">拥有角色</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ accountInfo.alertCount }}</span>
                <span class="stat-label">待处理告警</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ accountInfo.reportCount }}</span>
                <span class="stat-label">查看报告</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Recent Activity Card -->
        <div class="info-card activity-card">
          <div class="card-header">
            <div class="card-title">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12 6 12 12 16 14"/>
              </svg>
              <span>最近活动</span>
            </div>
          </div>
          <div class="card-body">
            <div class="activity-list">
              <div v-for="(activity, index) in recentActivities" :key="index" class="activity-item" :style="{ animationDelay: `${index * 0.1}s` }">
                <div class="activity-dot" :class="activity.type"></div>
                <div class="activity-content">
                  <span class="activity-text">{{ activity.text }}</span>
                  <span class="activity-time">{{ activity.time }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Quick Actions Card -->
        <div class="info-card actions-card">
          <div class="card-header">
            <div class="card-title">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
              </svg>
              <span>快捷操作</span>
            </div>
          </div>
          <div class="card-body">
            <div class="quick-actions">
              <button class="action-btn" @click="navigateToAlarm">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                  <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
                </svg>
                <span>查看告警</span>
              </button>
              <button class="action-btn" @click="navigateToReport">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                  <line x1="16" y1="13" x2="8" y2="13"/>
                  <line x1="16" y1="17" x2="8" y2="17"/>
                </svg>
                <span>生成报告</span>
              </button>
              <button class="action-btn" @click="navigateToSettings">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="3"/>
                  <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
                </svg>
                <span>系统设置</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Password Change Dialog -->
    <el-dialog
      v-model="showPasswordDialog"
      title="修改密码"
      width="480px"
      class="password-dialog"
    >
      <div class="password-form">
        <div class="form-item">
          <label class="form-label">原密码</label>
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            size="large"
            placeholder="请输入原密码"
            show-password
          />
        </div>
        <div class="form-item">
          <label class="form-label">新密码</label>
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            size="large"
            placeholder="请输入新密码"
            show-password
          />
          <div class="password-strength">
            <div class="strength-bar">
              <div class="strength-fill" :class="passwordStrength.level" :style="{ width: passwordStrength.width }"></div>
            </div>
            <span class="strength-text" :class="passwordStrength.level">{{ passwordStrength.text }}</span>
          </div>
        </div>
        <div class="form-item">
          <label class="form-label">确认密码</label>
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            size="large"
            placeholder="请再次输入新密码"
            show-password
          />
        </div>
      </div>
      <template #footer>
        <el-button size="large" @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" size="large" @click="changePassword" :loading="passwordChanging">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserInfo, updateUserInfo, changePassword as updatePasswordApi } from '@/utils/userApi'

const router = useRouter()

// User Info - from /api/v1/auth/getInfo response: { code, msg, data: { user: { userId, username, nickName, phonenumber, email, deptId, deptName, avatar } } }
const userInfo = reactive({
  id: 0,
  username: '',
  realName: '',
  phone: '',
  email: '',
  orgId: 0,
  orgName: '',
  avatar: ''
})

// Edit Form
const isEditing = ref(false)
const saving = ref(false)
const editForm = reactive({
  username: '',
  realName: '',
  phone: '',
  email: '',
  orgId: 0
})

// Password Dialog
const showPasswordDialog = ref(false)
const passwordChanging = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// Account Stats
const accountInfo = reactive({
  loginCount: 128,
  roleCount: 3,
  alertCount: 5,
  reportCount: 24
})

// Recent Activities
const recentActivities = ref([
  { type: 'login', text: '登录系统', time: '2024-01-20 14:30' },
  { type: 'alarm', text: '处理二级告警', time: '2024-01-20 10:15' },
  { type: 'report', text: '查看监测日报', time: '2024-01-19 16:40' },
  { type: 'setting', text: '修改通知设置', time: '2024-01-19 09:20' }
])

// Password Strength
const passwordStrength = computed(() => {
  const pwd = passwordForm.newPassword
  if (!pwd) return { level: '', width: '0%', text: '' }

  let score = 0
  if (pwd.length >= 8) score++
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) score++
  if (/\d/.test(pwd)) score++
  if (/[!@#$%^&*(),.?":{}|<>]/.test(pwd)) score++

  if (score <= 1) return { level: 'weak', width: '33%', text: '弱' }
  if (score <= 2) return { level: 'medium', width: '66%', text: '中' }
  return { level: 'strong', width: '100%', text: '强' }
})

// Methods
const toggleEdit = () => {
  if (isEditing.value) {
    cancelEdit()
  } else {
    editForm.username = userInfo.username
    editForm.realName = userInfo.realName
    editForm.phone = userInfo.phone
    editForm.email = userInfo.email
    editForm.orgId = userInfo.orgId
    isEditing.value = true
  }
}

const cancelEdit = () => {
  isEditing.value = false
}

const saveUserInfo = async () => {
  saving.value = true
  try {
    // Backend expects: nickName (realName), phonenumber (phone), email, sex
    await updateUserInfo(0, {
      realName: editForm.realName,
      phone: editForm.phone,
      email: editForm.email
    } as any)
    userInfo.realName = editForm.realName
    userInfo.phone = editForm.phone
    userInfo.email = editForm.email
    ElMessage.success('个人信息保存成功')
    isEditing.value = false
  } catch {
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

const changePassword = async () => {
  if (!passwordForm.oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (!passwordForm.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('密码长度不能少于6位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  passwordChanging.value = true
  try {
    await updatePasswordApi({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功')
    showPasswordDialog.value = false
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch {
    ElMessage.error('密码修改失败，请重试')
  } finally {
    passwordChanging.value = false
  }
}

const triggerAvatarEdit = () => {
  ElMessage.info('头像上传功能开发中')
}

const navigateToAlarm = () => {
  router.push('/alarm/realtime')
}

const navigateToReport = () => {
  router.push('/report/report')
}

const navigateToSettings = () => {
  router.push('/system/settings')
}

onMounted(async () => {
  try {
    const data = await getUserInfo() as any
    // /api/v1/auth/getInfo returns { user: { userId, username, nickName, phonenumber, email, deptId, deptName, avatar } }
    const user = data.user || data
    userInfo.id = user.userId || user.id || 0
    userInfo.username = user.username || ''
    userInfo.realName = user.nickName || ''
    userInfo.phone = user.phonenumber || ''
    userInfo.email = user.email || ''
    userInfo.orgId = user.deptId || 0
    userInfo.orgName = user.deptName || ''
    userInfo.avatar = user.avatar || ''
  } catch {
    ElMessage.error('获取用户信息失败')
  }
})
</script>

<style scoped>
.user-profile {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  animation: fadeIn 0.5s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Hero Section */
.profile-hero {
  position: relative;
  background: linear-gradient(135deg, #1a365d 0%, #2c5282 50%, #3182ce 100%);
  border-radius: 20px;
  padding: 48px;
  margin-bottom: 32px;
  overflow: hidden;
  box-shadow: 0 10px 40px rgba(26, 54, 93, 0.3);
}

.hero-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.bg-shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.1;
}

.shape-1 {
  width: 400px;
  height: 400px;
  background: white;
  top: -200px;
  right: -100px;
}

.shape-2 {
  width: 300px;
  height: 300px;
  background: white;
  bottom: -150px;
  left: 10%;
}

.shape-3 {
  width: 200px;
  height: 200px;
  background: linear-gradient(45deg, #63b3ed, #90cdf4);
  top: 20%;
  left: -50px;
}

.hero-content {
  position: relative;
  display: flex;
  align-items: center;
  gap: 40px;
}

.avatar-wrapper {
  position: relative;
}

.avatar-ring {
  position: absolute;
  inset: -8px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 0.3; }
  50% { transform: scale(1.05); opacity: 0.5; }
}

.avatar-container {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255,255,255,0.2), rgba(255,255,255,0.1));
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.avatar-icon {
  width: 56px;
  height: 56px;
  color: rgba(255, 255, 255, 0.9);
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-edit-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #48bb78, #38a169);
  border: 3px solid white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(72, 187, 120, 0.4);
}

.avatar-edit-btn:hover {
  transform: scale(1.1);
}

.avatar-edit-btn svg {
  width: 18px;
  height: 18px;
  color: white;
}

.user-meta {
  flex: 1;
}

.user-name {
  font-size: 32px;
  font-weight: 700;
  color: white;
  margin: 0 0 12px 0;
  letter-spacing: 1px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.user-role {
  display: flex;
  align-items: center;
  gap: 12px;
}

.role-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.role-badge svg {
  width: 16px;
  height: 16px;
}

/* Content Layout */
.profile-content {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 24px;
}

.content-left,
.content-right {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* Info Cards */
.info-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  transition: all 0.3s ease;
}

.info-card:hover {
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
}

.info-card.editing {
  box-shadow: 0 8px 30px rgba(24, 144, 255, 0.15);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(180deg, #fafafa 0%, #ffffff 100%);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #1a365d;
}

.card-title svg {
  width: 20px;
  height: 20px;
  color: #3182ce;
}

.edit-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  color: #4a5568;
  cursor: pointer;
  transition: all 0.2s ease;
}

.edit-btn:hover {
  background: linear-gradient(135deg, #3182ce 0%, #2c5282 100%);
  color: white;
  border-color: #3182ce;
}

.edit-btn svg {
  width: 14px;
  height: 14px;
}

.card-body {
  padding: 24px;
}

.card-footer {
  padding: 16px 24px;
  border-top: 1px solid #f0f2f5;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  background: #fafafa;
}

/* Info Grid */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-item.full-width {
  grid-column: span 2;
}

.info-label {
  font-size: 13px;
  color: #718096;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 16px;
  color: #2d3748;
  font-weight: 500;
}

.org-select {
  width: 100%;
}

/* Security Card */
.security-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: linear-gradient(135deg, #f7fafc 0%, #f0f4f8 100%);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.security-item:hover {
  background: linear-gradient(135deg, #e6f0ff 0%, #d9ecff 100%);
  border-color: #90cdf4;
}

.security-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.security-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: linear-gradient(135deg, #3182ce 0%, #2c5282 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(49, 130, 206, 0.3);
}

.security-icon svg {
  width: 22px;
  height: 22px;
  color: white;
}

.security-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.security-title {
  font-size: 15px;
  font-weight: 600;
  color: #2d3748;
}

.security-desc {
  font-size: 13px;
  color: #a0aec0;
}

.security-action {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #3182ce;
  font-weight: 500;
}

.security-action svg {
  width: 16px;
  height: 16px;
}

/* Stats Card */
.stats-card .card-body {
  padding: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, #f8faff 0%, #f0f4f8 100%);
  border-radius: 12px;
  transition: all 0.2s ease;
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(49, 130, 206, 0.1);
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #3182ce;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #718096;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* Activity Card */
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
  animation: slideIn 0.3s ease;
  animation-fill-mode: both;
}

.activity-item:last-child {
  border-bottom: none;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateX(-10px); }
  to { opacity: 1; transform: translateX(0); }
}

.activity-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
}

.activity-dot.login { background: #3182ce; }
.activity-dot.alarm { background: #e53e3e; }
.activity-dot.report { background: #38a169; }
.activity-dot.setting { background: #d69e2e; }

.activity-content {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.activity-text {
  font-size: 14px;
  color: #2d3748;
}

.activity-time {
  font-size: 12px;
  color: #a0aec0;
}

/* Actions Card */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px 12px;
  background: linear-gradient(135deg, #f8faff 0%, #f0f4f8 100%);
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background: linear-gradient(135deg, #3182ce 0%, #2c5282 100%);
  color: white;
  border-color: #3182ce;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(49, 130, 206, 0.25);
}

.action-btn svg {
  width: 24px;
  height: 24px;
  color: #3182ce;
  transition: color 0.2s ease;
}

.action-btn:hover svg {
  color: white;
}

.action-btn span {
  font-size: 13px;
  font-weight: 500;
  color: #4a5568;
  transition: color 0.2s ease;
}

.action-btn:hover span {
  color: white;
}

/* Password Dialog */
.password-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
  color: #2d3748;
}

.password-strength {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}

.strength-bar {
  flex: 1;
  height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  border-radius: 3px;
  transition: all 0.3s ease;
}

.strength-fill.weak { background: #e53e3e; }
.strength-fill.medium { background: #d69e2e; }
.strength-fill.strong { background: #38a169; }

.strength-text {
  font-size: 12px;
  font-weight: 600;
  min-width: 24px;
}

.strength-text.weak { color: #e53e3e; }
.strength-text.medium { color: #d69e2e; }
.strength-text.strong { color: #38a169; }

/* Responsive */
@media (max-width: 1200px) {
  .profile-content {
    grid-template-columns: 1fr;
  }

  .content-right {
    order: -1;
  }

  .quick-actions {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .user-profile {
    padding: 16px;
  }

  .profile-hero {
    padding: 32px 24px;
  }

  .hero-content {
    flex-direction: column;
    text-align: center;
  }

  .user-name {
    font-size: 24px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .info-item.full-width {
    grid-column: span 1;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .quick-actions {
    grid-template-columns: 1fr;
  }
}
</style>

<style>
/* Element Plus Overrides */
.password-dialog .el-dialog {
  border-radius: 16px;
}

.password-dialog .el-dialog__header {
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
  padding: 20px 24px;
  border-bottom: 1px solid #e8e8e8;
}

.password-dialog .el-dialog__title {
  font-weight: 600;
  color: #1a365d;
}

.password-dialog .el-dialog__body {
  padding: 24px;
}

.password-dialog .el-button--primary {
  background: linear-gradient(135deg, #3182ce 0%, #2c5282 100%);
  border: none;
}

.password-dialog .el-button--primary:hover {
  background: linear-gradient(135deg, #2c5282 0%, #1a365d 100%);
}

/* Input overrides */
.el-input__wrapper {
  border-radius: 8px;
}

.el-input__wrapper:hover {
  box-shadow: 0 0 0 2px rgba(49, 130, 206, 0.1);
}

.el-input__wrapper.is-focus {
  box-shadow: 0 0 0 2px rgba(49, 130, 206, 0.2);
}

/* Select overrides */
.org-select .el-input__wrapper {
  border-radius: 8px;
}
</style>
