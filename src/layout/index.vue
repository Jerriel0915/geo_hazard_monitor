<template>
  <div class="layout-container">
    <header class="header">
      <div class="header-left">
        <span class="logo">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon">
            <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6"/>
            <line x1="8" y1="2" x2="8" y2="18"/>
            <line x1="16" y1="6" x2="16" y2="22"/>
          </svg>
        </span>
        <span class="title">地质灾害监测预警系统1.0</span>
      </div>
      <nav class="header-nav">
        <el-menu mode="horizontal" default-active="Dashboard" @select="handleMenuSelect" class="nav-menu">
          <template v-for="menu in menuList" :key="menu.name">
            <el-sub-menu :index="menu.name">
              <template #title>
                <span class="menu-icon" v-html="menu.icon"></span>
                <span>{{ menu.label }}</span>
              </template>
              <el-menu-item v-for="child in menu.children" :key="child.name" :index="child.name">
                {{ child.label }}
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>
      </nav>
      <div class="header-right">
        <el-dropdown @command="handleUserCommand">
          <div class="user-info">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="user-icon">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>
            <span>{{ currentUser.name }}</span>
            <span class="arrow-icon">▼</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="info">基本信息</el-dropdown-item>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>
    <div class="page-tabs">
      <div class="tabs-scroll-btn" @click="scrollTabs('left')">
        <span>‹</span>
      </div>
      <div class="tabs-container" ref="tabsContainerRef" @scroll="handleTabScroll">
        <div
          v-for="tab in tabs"
          :key="tab.name"
          class="tab-item"
          :class="{ active: activeTab === tab.name }"
          @click="switchTab(tab.name)"
        >
          <span>{{ tab.label }}</span>
          <span
            v-if="tabs.length > 1"
            class="tab-close"
            @click.stop="closeTab(tab.name)"
          >×</span>
        </div>
      </div>
      <div class="tabs-scroll-btn" @click="scrollTabs('right')">
        <span>›</span>
      </div>
      <div class="tabs-actions">
        <el-dropdown @command="handleTabAction">
          <el-button size="small" type="text">
            更多操作
            <span class="arrow-icon">▼</span>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="closeAll">关闭所有</el-dropdown-item>
              <el-dropdown-item command="closeOther">关闭其他</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
    <el-dialog title="基本信息" :visible.sync="infoDialogVisible" width="400px">
      <el-form :model="userInfo" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="userInfo.username" disabled />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="userInfo.realName" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="userInfo.email" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="userInfo.phone" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="infoDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUserInfo">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog title="修改密码" :visible.sync="pwdDialogVisible" width="400px">
      <el-form :model="pwdForm" label-width="80px">
        <el-form-item label="原密码">
          <el-input type="password" v-model="pwdForm.oldPwd" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input type="password" v-model="pwdForm.newPwd" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input type="password" v-model="pwdForm.confirmPwd" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="changePassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'


const router = useRouter()
const tabsContainerRef = ref<HTMLElement | null>(null)
const tabs = ref<Array<{ name: string; label: string }>>([])
const activeTab = ref('Dashboard')
const infoDialogVisible = ref(false)
const pwdDialogVisible = ref(false)

const currentUser = reactive({
  name: '管理员'
})

const userInfo = reactive({
  username: 'admin',
  realName: '管理员',
  email: 'admin@example.com',
  phone: '13800138000'
})

const pwdForm = reactive({
  oldPwd: '',
  newPwd: '',
  confirmPwd: ''
})

const menuList = [
  {
    name: 'Dashboard',
    label: '全息看板',
    icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="21" x2="9" y2="9"/></svg>',
    children: [
      { name: 'Comprehensive', label: '综合视图' },
      { name: 'Alarm', label: '告警视图' },
      { name: 'Operation', label: '运营视图' },
      { name: 'Custom', label: '自定义视图' }
    ]
  },
  {
    name: 'Basic',
    label: '基础管理',
    icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>',
    children: [
      { name: 'HazardPoint', label: '隐患点管理' },
      { name: 'MonitorType', label: '监测类型' },
      { name: 'Device', label: '设备管理' },
      { name: 'VideoDevice', label: '视频设备管理' }
    ]
  },
  {
    name: 'Alarm',
    label: '告警中心',
    icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>',
    children: [
      { name: 'RealtimeAlarm', label: '实时告警' },
      { name: 'AlarmCriteria', label: '告警判据管理' },
      { name: 'AlarmNotification', label: '告警查看和通知' },
      { name: 'AlarmDisposal', label: '告警处置' }
    ]
  },
  {
    name: 'Report',
    label: '报告报表',
    icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>',
    children: [
      { name: 'Report', label: '报告管理' },
      { name: 'Query', label: '查询中心' },
      { name: 'Analysis', label: '数据分析' },
      { name: 'Screen', label: '运营大屏' }
    ]
  },
  {
    name: 'IoT',
    label: '物联网',
    icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="2"/><path d="M16.24 7.76a6 6 0 0 1 0 8.49m-8.48-.01a6 6 0 0 1 0-8.49m11.31-2.82a10 10 0 0 1 0 14.14m-14.14 0a10 10 0 0 1 0-14.14"/></svg>',
    children: [
      { name: 'DeviceAccess', label: '设备接入' },
      { name: 'AlarmEngine', label: '告警引擎' },
      { name: 'DataParse', label: '数据解析' }
    ]
  },
  {
    name: 'System',
    label: '系统管理',
    icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>',
    children: [
      { name: 'Organization', label: '组织管理' },
      { name: 'Identity', label: '身份管理' },
      { name: 'Permission', label: '权限管理' },
      { name: 'Log', label: '日志管理' },
      { name: 'Settings', label: '系统设置' }
    ]
  }
]

const menuRouteMap: Record<string, string> = {
  Dashboard: '/dashboard',
  Comprehensive: '/holo-board/comprehensive',
  Alarm: '/holo-board/alarm',
  Operation: '/holo-board/operation',
  Custom: '/holo-board/custom',
  HazardPoint: '/basic/hazard-point',
  MonitorType: '/basic/monitor-type',
  Device: '/basic/device',
  VideoDevice: '/basic/video-device',
  RealtimeAlarm: '/alarm/realtime',
  AlarmCriteria: '/alarm/criteria',
  AlarmNotification: '/alarm/notification',
  AlarmDisposal: '/alarm/disposal',
  Report: '/report/report',
  Query: '/report/query',
  Analysis: '/report/analysis',
  Screen: '/report/screen',
  DeviceAccess: '/iot/access',
  AlarmEngine: '/iot/alarm-engine',
  DataParse: '/iot/data-parse',
  Organization: '/system/organization',
  Identity: '/system/identity',
  Permission: '/system/permission',
  Log: '/system/log',
  Settings: '/system/settings',
  MiniHazardPoint: '/miniprogram/hazard-point',
  MiniDevice: '/miniprogram/device',
  MiniEvent: '/miniprogram/event',
  MiniMonitorData: '/miniprogram/monitor-data'
}

const menuLabelMap: Record<string, string> = {
  Dashboard: '首页',
  Comprehensive: '综合视图',
  Alarm: '告警视图',
  Operation: '运营视图',
  Custom: '自定义视图',
  HazardPoint: '隐患点管理',
  MonitorType: '监测类型',
  Device: '设备管理',
  VideoDevice: '视频设备管理',
  RealtimeAlarm: '实时告警',
  AlarmCriteria: '告警判据管理',
  AlarmNotification: '告警查看和通知',
  AlarmDisposal: '告警处置',
  Report: '报告管理',
  Query: '查询中心',
  Analysis: '数据分析',
  Screen: '运营大屏',
  DeviceAccess: '设备接入',
  AlarmEngine: '告警引擎',
  DataParse: '数据解析',
  Organization: '组织管理',
  Identity: '身份管理',
  Permission: '权限管理',
  Log: '日志管理',
  Settings: '系统设置',
  MiniHazardPoint: '隐患点',
  MiniDevice: '设备库',
  MiniEvent: '事件大厅',
  MiniMonitorData: '监测数据'
}

const handleMenuSelect = (key: string) => {
  const route = menuRouteMap[key]
  if (route) {
    router.push(route)
    if (!tabs.value.find(tab => tab.name === key)) {
      tabs.value.push({ name: key, label: menuLabelMap[key] })
    }
    activeTab.value = key
  }
}

const switchTab = (name: string) => {
  activeTab.value = name
  const route = menuRouteMap[name]
  if (route) {
    router.push(route)
  }
}

const closeTab = (name: string) => {
  const index = tabs.value.findIndex(tab => tab.name === name)
  if (index !== -1) {
    tabs.value.splice(index, 1)
    if (activeTab.value === name) {
      if (tabs.value.length > 0) {
        const newIndex = index > 0 ? index - 1 : 0
        activeTab.value = tabs.value[newIndex].name
        router.push(menuRouteMap[tabs.value[newIndex].name])
      } else {
        router.push('/dashboard')
      }
    }
  }
}

const handleTabAction = (command: string) => {
  if (command === 'closeAll') {
    tabs.value = [{ name: 'Dashboard', label: '首页' }]
    activeTab.value = 'Dashboard'
    router.push('/dashboard')
  } else if (command === 'closeOther') {
    tabs.value = tabs.value.filter(tab => tab.name === activeTab.value)
  }
}

const handleUserCommand = (command: string) => {
  if (command === 'info') {
    infoDialogVisible.value = true
  } else if (command === 'password') {
    pwdDialogVisible.value = true
  } else if (command === 'logout') {
    localStorage.removeItem('token')
    router.push('/login')
  }
}

const saveUserInfo = () => {
  infoDialogVisible.value = false
  alert('信息保存成功')
}

const changePassword = () => {
  pwdDialogVisible.value = false
  alert('密码修改成功')
}

onMounted(() => {
  tabs.value = [{ name: 'Dashboard', label: '首页' }]
})

const scrollTabs = (direction: 'left' | 'right') => {
  if (tabsContainerRef.value) {
    const container = tabsContainerRef.value
    const scrollAmount = 200
    if (direction === 'left') {
      container.scrollBy({ left: -scrollAmount, behavior: 'smooth' })
    } else {
      container.scrollBy({ left: scrollAmount, behavior: 'smooth' })
    }
  }
}
</script>

<style scoped>
:root {
  --primary-color: #1890ff;
  --primary-light: #40a9ff;
  --primary-dark: #096dd9;
  --success-color: #52c41a;
  --warning-color: #faad14;
  --error-color: #f5222d;
  --text-primary: #1f1f1f;
  --text-secondary: #666666;
  --text-tertiary: #999999;
  --bg-primary: #f0f2f5;
  --bg-secondary: #ffffff;
  --border-color: #e8e8e8;
  --shadow-sm: 0 2px 8px rgba(0, 0, 0, 0.06);
  --shadow-md: 0 4px 16px rgba(0, 0, 0, 0.08);
  --shadow-lg: 0 8px 24px rgba(0, 0, 0, 0.1);
  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;
}

.layout-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(90deg, #64b5f6 0%, #90caf9 30%, #bbdefb 60%, #e3f2fd 100%);
  padding: 0 24px;
  height: 64px;
  color: #1f1f1f;
  box-shadow: var(--shadow-sm);
  position: relative;
  z-index: 100;
}

.header::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, #64b5f6, #90caf9, #bbdefb, #e3f2fd);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  background: rgba(24, 144, 255, 0.15);
  border-radius: var(--radius-md);
  backdrop-filter: blur(4px);
  transition: all 0.3s ease;
}

.logo:hover {
  background: rgba(24, 144, 255, 0.25);
  transform: translateY(-2px);
}

.logo .icon {
  width: 24px;
  height: 24px;
  color: #1890ff;
}

.title {
  font-size: 19px;
  font-weight: 600;
  letter-spacing: 1px;
  color: #1e3a8a;
}

.header-nav {
  flex: 1;
  margin-left: 48px;
}

.nav-menu {
  background: transparent;
  border: none;
}

.nav-menu .el-menu-item {
  color: #1f1f1f;
  height: 64px;
  line-height: 64px;
  font-size: 14px;
  padding: 0 20px;
  transition: all 0.3s ease;
}

.nav-menu .el-menu-item:hover,
.nav-menu .el-menu-item.is-active {
  background-color: rgba(24, 144, 255, 0.1);
  color: #1890ff;
}

.nav-menu .el-sub-menu__title {
  color: #1f1f1f;
  height: 64px;
  line-height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  padding: 0 20px;
  transition: all 0.3s ease;
}

.menu-icon {
  display: flex;
  align-items: center;
}

.menu-icon :deep(svg) {
  width: 18px;
  height: 18px;
  transition: transform 0.3s ease;
}

.nav-menu .el-sub-menu__title:hover,
.nav-menu .el-sub-menu.is-active .el-sub-menu__title {
  background-color: rgba(24, 144, 255, 0.1);
  color: #1890ff;
}

.nav-menu .el-sub-menu.is-active .menu-icon :deep(svg) {
  transform: scale(1.1);
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  cursor: pointer;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(4px);
  transition: all 0.3s ease;
  border: 1px solid rgba(24, 144, 255, 0.1);
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.1);
}

.user-icon {
  width: 28px;
  height: 28px;
}

.arrow-icon {
  font-size: 12px;
  transition: transform 0.3s ease;
}

.user-info:hover .arrow-icon {
  transform: rotate(180deg);
}

.page-tabs {
  display: flex;
  align-items: center;
  background: linear-gradient(180deg, #ffffff 0%, #fafafa 100%);
  border-bottom: 1px solid #e8e8e8;
  padding: 4px 16px;
  height: 44px;
  box-shadow: inset 0 -1px 0 rgba(0, 0, 0, 0.04);
}

.tabs-scroll-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #666666;
  font-size: 16px;
  border-radius: var(--radius-md);
  transition: all 0.25s ease;
  flex-shrink: 0;
  margin-right: 4px;
}

.tabs-scroll-btn:hover {
  background: linear-gradient(135deg, #f0f5ff 0%, #e6f0ff 100%);
  color: #1890ff;
  transform: scale(1.1);
}

.tabs-container {
  display: flex;
  align-items: center;
  gap: 6px;
  overflow-x: auto;
  flex: 1;
  scroll-behavior: smooth;
  padding: 0 8px;
}

.tabs-container::-webkit-scrollbar {
  display: none;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  height: 36px;
  line-height: 20px;
  cursor: pointer;
  border-radius: var(--radius-md);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 14px;
  color: #666666;
  white-space: nowrap;
  background: transparent;
  position: relative;
}

.tab-item::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 2px;
  background: #1890ff;
  border-radius: 1px;
  transition: width 0.3s ease;
}

.tab-item:hover {
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
  color: #333333;
  transform: translateY(-1px);
}

.tab-item:hover::before {
  width: 60%;
}

.tab-item.active {
  background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
  color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
}

.tab-item.active::before {
  width: 80%;
}

.tab-close {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 14px;
  transition: all 0.25s ease;
  opacity: 0.6;
}

.tab-item:hover .tab-close {
  opacity: 1;
}

.tab-close:hover {
  background: rgba(0, 0, 0, 0.1);
  transform: rotate(90deg);
}

.tabs-actions {
  margin-left: 12px;
  padding-left: 12px;
  border-left: 1px solid #e8e8e8;
}

.tabs-actions .el-button {
  font-size: 13px;
  color: #666666;
  transition: all 0.25s ease;
}

.tabs-actions .el-button:hover {
  color: #1890ff;
}

.main-content {
  flex: 1;
  background: linear-gradient(180deg, #f5f7fa 0%, #f0f2f5 100%);
  padding: 20px;
  overflow: auto;
  position: relative;
}

.main-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, #d9d9d9, transparent);
}

.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

:deep(.el-dropdown-menu) {
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  border: none;
  padding: 4px 0;
}

:deep(.el-dropdown-item) {
  padding: 10px 20px;
  font-size: 14px;
  transition: all 0.2s ease;
}

:deep(.el-dropdown-item:hover) {
  background: linear-gradient(135deg, #f0f5ff 0%, #e6f0ff 100%);
  color: #1890ff;
}

:deep(.el-dropdown-item.is-divided) {
  border-top: 1px solid #f0f0f0;
  margin-top: 4px;
  padding-top: 14px;
}

:deep(.el-dialog) {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
}

:deep(.el-dialog__header) {
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
  border-bottom: 1px solid #e8e8e8;
  padding: 16px 20px;
}

:deep(.el-dialog__title) {
  font-size: 16px;
  font-weight: 600;
  color: #1f1f1f;
}

:deep(.el-dialog__body) {
  padding: 20px;
}

:deep(.el-form-item) {
  margin-bottom: 16px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #333333;
}

:deep(.el-input__wrapper) {
  border-radius: var(--radius-md);
  transition: all 0.25s ease;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

:deep(.el-button) {
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  transition: all 0.25s ease;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
}

:deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #40a9ff 0%, #1890ff 100%);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.4);
  transform: translateY(-1px);
}
</style>
