<template>
  <div class="layout-container">
    <header class="header">
      <div class="header-left">
        <span class="logo">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon">
            <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6"/>
            <line x1="8" y1="2" x2="8" y2="18"/>
            <line x1="16" y1="6" x2="16" y2="22"/>
          </svg>
        </span>
        <span class="title">地质灾害监测预警系统1.0</span>
        <span class="home-icon-wrapper" @click="goToDashboard">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"
               stroke-linecap="round" stroke-linejoin="round" class="home-icon">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
            <polyline points="9 22 9 12 15 12 15 22"/>
          </svg>
        </span>
      </div>
      <nav class="header-nav">
        <el-menu mode="horizontal" :default-active="activeMenu" @select="handleMenuSelect" class="nav-menu">
          <template v-for="menu in filteredMenuList" :key="menu.name">
            <el-sub-menu :index="menu.name">
              <template #title>
                <span class="menu-icon" v-html="menu.icon"></span>
                <span>{{ menu.label }}</span>
              </template>
              <template v-for="(child, index) in menu.children" :key="index">
                <template v-if="(child as any).divider">
                  <div class="menu-divider"></div>
                </template>
                <template v-else-if="(child as any).children && (child as any).children.length > 0">
                  <el-sub-menu :index="(child as any).name">
                    <template #title>{{ (child as any).label }}</template>
                    <el-menu-item v-for="subChild in (child as any).children" :key="subChild.name"
                                  :index="subChild.name">
                      {{ subChild.label }}
                    </el-menu-item>
                  </el-sub-menu>
                </template>
                <el-menu-item v-else :index="(child as any).name">
                  {{ (child as any).label }}
                </el-menu-item>
              </template>
            </el-sub-menu>
          </template>
        </el-menu>
      </nav>
      <div class="header-right">
        <div class="message-icon-wrapper" @click="toggleMessagePanel">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="message-icon">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
          <span v-if="unreadMessageCount > 0" class="message-badge">{{ unreadMessageCount }}</span>
        </div>
        <el-dropdown @command="handleUserCommand">
          <div class="user-info">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="user-icon">
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
      <div class="tabs-container" ref="tabsContainerRef">
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
    <el-dialog title="基本信息" v-model="infoDialogVisible" width="450px">
      <el-form :model="userInfo" label-width="100px">
        <el-form-item label="用户名">
          <el-input v-model="userInfo.username" disabled />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="userInfo.realName" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="userInfo.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="userInfo.email" />
        </el-form-item>
        <el-form-item label="所属组织">
          <el-select v-model="userInfo.organization" style="width: 100%">
            <el-option label="系统管理部" value="系统管理部" />
            <el-option label="监测运维部" value="监测运维部" />
            <el-option label="数据分析部" value="数据分析部" />
            <el-option label="应急指挥中心" value="应急指挥中心" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="userInfo.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="infoDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUserInfo">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog title="修改密码" v-model="pwdDialogVisible" width="400px">
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

    <div class="message-panel" :class="{ visible: messagePanelVisible }">
      <div class="message-panel-header">
        <span class="message-panel-title">系统消息</span>
        <div class="message-tabs">
          <span :class="['tab', { active: messageTab === 'unread' }]" @click="messageTab = 'unread'">未读 ({{ unreadMessageCount }})</span>
          <span :class="['tab', { active: messageTab === 'read' }]" @click="messageTab = 'read'">已读</span>
        </div>
        <span class="close-btn" @click="messagePanelVisible = false">×</span>
      </div>
      <div class="message-list">
        <div
          v-for="msg in filteredMessages"
          :key="msg.id"
          :class="['message-item', { unread: !msg.read }]"
          @click="markMessageAsRead(msg)"
        >
          <div class="message-icon-wrapper">
            <svg v-if="msg.type === 'alarm'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#faad14" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
            </svg>
            <svg v-else-if="msg.type === 'system'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#1890ff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="3"/>
              <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
            </svg>
            <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#52c41a" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
              <polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
          </div>
          <div class="message-content">
            <div class="message-title">{{ msg.title }}</div>
            <div class="message-desc">{{ msg.content }}</div>
            <div class="message-time">{{ msg.time }}</div>
          </div>
        </div>
        <div v-if="filteredMessages.length === 0" class="empty-message">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#d9d9d9" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="empty-icon">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
          <span>暂无消息</span>
        </div>
      </div>
      <div class="message-panel-footer" v-if="messages.length > 0">
        <el-button size="small" @click="markAllAsRead">全部标为已读</el-button>
      </div>
    </div>
    <div class="message-mask" v-if="messagePanelVisible" @click="messagePanelVisible = false"></div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, onUnmounted, reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import {getAuthInfo, getUserInfo} from '@/utils/userApi'
import {getTopNotices, markRead, markReadAll, type SysNotice} from '@/api/notice'


/** 通知消息（来自 SysNotice 后端） */
interface NoticeMessage {
  id: number
  title: string
  content: string
  time: string
  read: boolean
  type: string
}

const router = useRouter()
const tabsContainerRef = ref<HTMLElement | null>(null)
const tabs = ref<Array<{ name: string; label: string }>>([])
const activeTab = ref('Dashboard')
const infoDialogVisible = ref(false)
const pwdDialogVisible = ref(false)

const messagePanelVisible = ref(false)
const messageTab = ref<'unread' | 'read'>('unread')
const messages = ref<NoticeMessage[]>([])
const unreadMessageCount = ref(0)
let noticeEventSource: EventSource | null = null

const filteredMessages = computed(() => {
  if (messageTab.value === 'unread') {
    return messages.value.filter(m => !m.read)
  }
  return messages.value.filter(m => m.read)
})

function toNoticeMessage(n: SysNotice): NoticeMessage {
  return {
    id: n.noticeId,
    title: n.noticeTitle,
    content: n.noticeContent?.replace(/<[^>]*>/g, '') ?? '',
    time: n.createTime ?? '',
    read: n.isRead ?? false,
    type: n.noticeType === '1' ? 'system' : 'other'
  }
}

async function fetchNotices() {
  try {
    const res = await getTopNotices()
    const data = res.data
    messages.value = (data.list ?? []).map(toNoticeMessage)
    unreadMessageCount.value = data.unreadCount ?? 0
  } catch { /* keep previous data */ }
}

function startNoticeSSE() {
  if (noticeEventSource) noticeEventSource.close()
  noticeEventSource = new EventSource('/api/v1/system/notice/stream')
  noticeEventSource.addEventListener('notice', (event) => {
    try {
      const data = JSON.parse(event.data)
      const msg: NoticeMessage = {
        id: data.noticeId,
        title: data.title,
        content: data.content ?? '',
        time: data.createTime ?? '',
        read: false,
        type: data.type === '1' ? 'system' : 'other'
      }
      messages.value.unshift(msg)
      if (messages.value.length > 20) messages.value.pop()
      unreadMessageCount.value++
    } catch { /* ignore malformed event */ }
  })
  noticeEventSource.onerror = () => {
    noticeEventSource?.close()
    setTimeout(startNoticeSSE, 3000)
  }
}

const currentUser = reactive({
  name: '管理员'
})

const userInfo = reactive({
  username: 'admin',
  realName: '管理员',
  phone: '13800138000',
  email: 'admin@example.com',
  organization: '系统管理部',
  remark: ''
})

const pwdForm = reactive({
  oldPwd: '',
  newPwd: '',
  confirmPwd: ''
})

const activeMenu = ref('')
const isAdmin = ref(false)

const menuList = [
  {
    name: 'Dashboard',
    label: '全息看板',
    icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="21" x2="9" y2="9"/></svg>',
    children: [
      { name: 'Comprehensive', label: '综合视图' },
      { name: 'Alarm', label: '告警视图' },
      { name: 'Operation', label: '运营视图' }
    ]
  },
  {
    name: 'Basic',
    label: '基础管理',
    icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>',
    children: [
      { name: 'HazardPoint', label: '隐患点管理' }
    ]
  },
  {
    name: 'Alarm',
    label: '告警中心',
    icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>',
    children: [
      {name: 'RealtimeAlarm', label: '待办告警'},
      {name: 'AlarmNotification', label: '历史告警'},
      {divider: true},
      { name: 'AlarmCriteria', label: '告警判据' },
      {name: 'AlarmDisposal', label: '综合告警'},
      {divider: true},
      {name: 'NotificationSetting', label: '通知设置'}
    ]
  },
  {
    name: 'Report',
    label: '报告报表',
    icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>',
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
    icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><circle cx="12" cy="12" r="2"/><path d="M16.24 7.76a6 6 0 0 1 0 8.49m-8.48-.01a6 6 0 0 1 0-8.49m11.31-2.82a10 10 0 0 1 0 14.14m-14.14 0a10 10 0 0 1 0-14.14"/></svg>',
    children: [
      { name: 'MonitorType', label: '监测类型' },
      { name: 'Device', label: '设备管理' },
      { name: 'VideoDevice', label: '视频设备管理' },
      {name: 'DataParse', label: '数据解析'},
      {name: 'ServiceStatus', label: '服务状态'}
    ]
  },
  {
    name: 'System',
    label: '系统管理',
    icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>',
    children: [
      { name: 'Organization', label: '组织管理' },
      { name: 'Identity', label: '身份管理' },
      { name: 'Permission', label: '权限管理' },
      { name: 'Log', label: '日志管理' },
      { name: 'Settings', label: '系统设置' }
    ]
  }
]

// 根据角色过滤菜单：非管理员不展示"服务状态"
const filteredMenuList = computed(() => {
  if (isAdmin.value) return menuList
  return menuList.map(menu => {
    if (menu.name === 'IoT') {
      return {
        ...menu,
        children: menu.children.filter(child => child.name !== 'ServiceStatus')
      }
    }
    return menu
  })
})

const menuRouteMap: Record<string, string> = {
  Dashboard: '/dashboard',
  Comprehensive: '/holo-board/comprehensive',
  Alarm: '/holo-board/alarm',
  Operation: '/holo-board/operation',
  HazardPoint: '/basic/hazard-point',
  MonitorType: '/basic/monitor-type',
  Device: '/basic/device',
  VideoDevice: '/basic/video-device',
  RealtimeAlarm: '/alarm/realtime',
  AlarmCriteria: '/alarm/criteria',
  AlarmNotification: '/alarm/notification',
  AlarmDisposal: '/alarm/disposal',
  NotificationSetting: '/alarm/notification-setting',
  Report: '/report/report',
  Query: '/report/query',
  Analysis: '/report/analysis',
  Screen: '/report/screen',
  AlarmEngine: '/iot/alarm-engine',
  DataParse: '/iot/data-parse',
  ServiceStatus: '/iot/service-status',
  Organization: '/system/organization',
  Identity: '/system/identity',
  Permission: '/system/permission',
  Log: '/system/log',
  Settings: '/system/settings',
  UserProfile: '/user/profile'
}

const menuLabelMap: Record<string, string> = {
  Dashboard: '首页',
  Comprehensive: '综合视图',
  Alarm: '告警视图',
  Operation: '运营视图',
  HazardPoint: '隐患点管理',
  MonitorType: '监测类型',
  Device: '设备管理',
  VideoDevice: '视频设备管理',
  RealtimeAlarm: '待办告警',
  AlarmCriteria: '告警判据',
  AlarmNotification: '历史告警',
  AlarmDisposal: '综合告警',
  NotificationSetting: '通知设置',
  Report: '报告管理',
  Query: '查询中心',
  Analysis: '数据分析',
  Screen: '运营大屏',
  AlarmEngine: '告警引擎',
  DataParse: '数据解析',
  ServiceStatus: '服务状态',
  Organization: '组织管理',
  Identity: '身份管理',
  Permission: '权限管理',
  Log: '日志管理',
  Settings: '系统设置',
  UserProfile: '个人信息'
}

const handleMenuSelect = (key: string) => {
  const route = menuRouteMap[key]
  if (route) {
    router.push(route)
    if (!tabs.value.find(tab => tab.name === key)) {
      tabs.value.push({ name: key, label: menuLabelMap[key] })
    }
    activeTab.value = key
    activeMenu.value = key
  }
}

const switchTab = (name: string) => {
  activeTab.value = name
  activeMenu.value = name === 'Dashboard' ? '' : name
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
        const newTab = tabs.value[newIndex]
        activeTab.value = newTab.name
        activeMenu.value = newTab.name === 'Dashboard' ? '' : newTab.name
        router.push(menuRouteMap[newTab.name])
      } else {
        activeMenu.value = ''
        router.push('/dashboard')
      }
    }
  }
}

const handleTabAction = (command: string) => {
  if (command === 'closeAll') {
    tabs.value = [{ name: 'Dashboard', label: '首页' }]
    activeTab.value = 'Dashboard'
    activeMenu.value = ''
    router.push('/dashboard')
  } else if (command === 'closeOther') {
    tabs.value = tabs.value.filter(tab => tab.name === activeTab.value)
  }
}

const handleUserCommand = (command: string) => {
  if (command === 'info') {
    router.push('/user/profile')
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

const toggleMessagePanel = () => {
  messagePanelVisible.value = !messagePanelVisible.value
}

const markMessageAsRead = async (msg: NoticeMessage) => {
  try {
    await markRead(msg.id)
    msg.read = true
    unreadMessageCount.value = Math.max(0, unreadMessageCount.value - 1)
  } catch { /* ignore */ }
}

const markAllAsRead = async () => {
  const unreadIds = messages.value.filter(m => !m.read).map(m => m.id)
  if (unreadIds.length === 0) return
  try {
    await markReadAll(unreadIds.join(','))
    messages.value.forEach(m => { m.read = true })
    unreadMessageCount.value = 0
  } catch { /* ignore */ }
}

onMounted(async () => {
  tabs.value = [{ name: 'Dashboard', label: '首页' }]
  try {
    const auth = await getAuthInfo()
    isAdmin.value = auth.roles.includes('admin') || auth.roles.includes('ROOT')
  } catch {
    isAdmin.value = false
  }
  try {
    const user = await getUserInfo()
    currentUser.name = user.username || user.realName || '管理员'
  } catch {
    // keep default
  }
  // 首次加载通知 + SSE 实时推送
  fetchNotices()
  startNoticeSSE()
})

onUnmounted(() => {
  if (noticeEventSource) {
    noticeEventSource.close()
    noticeEventSource = null
  }
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

const goToDashboard = () => {
  router.push('/dashboard')
  activeMenu.value = ''
  if (!tabs.value.find(tab => tab.name === 'Dashboard')) {
    tabs.value.push({name: 'Dashboard', label: '首页'})
  }
  activeTab.value = 'Dashboard'
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
  --text-primary: #303133;
  --text-secondary: #606266;
  --text-tertiary: #909399;
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
  background: linear-gradient(90deg, #1e88e5 0%, #42a5f5 30%, #64b5f6 60%, #90caf9 100%);
  padding: 0 24px;
  height: 64px;
  color: white;
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
  background: linear-gradient(90deg, #1e88e5, #42a5f5, #64b5f6, #90caf9);
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
  background: rgba(255, 255, 255, 0.15);
  border-radius: var(--radius-md);
  backdrop-filter: blur(4px);
  transition: all 0.3s ease;
}

.logo:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
}

.logo .icon {
  width: 24px;
  height: 24px;
  color: white;
}

.title {
  font-size: 19px;
  font-weight: 600;
  letter-spacing: 1px;
  color: white;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.1);
}

.home-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-left: 12px;
  backdrop-filter: blur(4px);
}

.home-icon-wrapper:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.home-icon {
  width: 20px;
  height: 20px;
  color: white;
  transition: transform 0.3s ease;
}

.home-icon-wrapper:hover .home-icon {
  transform: scale(1.1);
}

.header-nav {
  flex: 1;
  margin-left: 48px;
}

.nav-menu {
  background: transparent;
  border: none;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.12);
  --el-menu-active-color: #ffffff;
}

.nav-menu :deep(.el-sub-menu__title) {
  color: rgba(255, 255, 255, 0.9);
  height: 40px;
  line-height: 40px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  padding: 0 20px;
  margin: 12px 0;
  transition: background-color 0.25s ease, color 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease;
  border-radius: 24px;
  border: 1px solid transparent;
  backdrop-filter: blur(4px);
}

.nav-menu :deep(.el-sub-menu__title:hover) {
  background-color: rgba(255, 255, 255, 0.18) !important;
  color: #ffffff;
  border-color: rgba(255, 255, 255, 0.25);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.nav-menu :deep(.el-sub-menu.is-active .el-sub-menu__title) {
  background-color: rgba(255, 255, 255, 0.18) !important;
  color: #ffffff;
  font-weight: 600;
  border-color: rgba(255, 255, 255, 0.25);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.nav-menu :deep(.el-sub-menu.is-active .menu-icon svg) {
  transform: scale(1.1);
}

.nav-menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.9);
  height: 50px;
  line-height: 50px;
  font-size: 14px;
  padding: 0 20px;
  transition: all 0.25s ease;
  background-color: transparent;
}

.nav-menu :deep(.menu-divider) {
  height: 1px;
  margin: 8px 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
}

.nav-menu :deep(.el-menu-item:hover) {
  background-color: rgba(0, 20, 60, 0.7) !important;
  color: #ffffff;
}

.nav-menu :deep(.el-menu-item.is-active) {
  background-color: rgba(0, 20, 60, 0.85) !important;
  color: #ffffff;
  font-weight: 600;
  border-bottom: 2px solid #ffffff;
}

.menu-icon {
  display: flex;
  align-items: center;
}

.menu-icon :deep(svg) {
  width: 18px;
  height: 18px;
  transition: transform 0.25s ease;
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
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(4px);
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
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

.message-icon-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  cursor: pointer;
  border-radius: 50%;
  margin-right: 12px;
  transition: all 0.3s ease;
}

.message-icon-wrapper:hover {
  background: rgba(255, 255, 255, 0.2);
}

.message-icon {
  width: 22px;
  height: 22px;
}

.message-badge {
  position: absolute;
  top: -2px;
  right: -4px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: #f5222d;
  border-radius: 9px;
  font-size: 12px;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  box-shadow: 0 2px 4px rgba(245, 34, 45, 0.3);
}

.message-panel {
  position: fixed;
  top: 64px;
  right: -400px;
  width: 380px;
  max-height: 500px;
  background: white;
  border-radius: 8px 0 0 8px;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1);
  transition: right 0.3s ease;
  z-index: 1000;
  display: flex;
  flex-direction: column;
}

.message-panel.visible {
  right: 0;
}

.message-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
}

.message-panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.message-tabs {
  display: flex;
  gap: 16px;
}

.message-tabs .tab {
  font-size: 14px;
  color: #666;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.message-tabs .tab:hover {
  background: rgba(24, 144, 255, 0.1);
  color: #1890ff;
}

.message-tabs .tab.active {
  background: #1890ff;
  color: white;
}

.close-btn {
  font-size: 20px;
  color: #999;
  cursor: pointer;
  padding: 4px;
  line-height: 1;
  transition: all 0.2s ease;
}

.close-btn:hover {
  color: #666;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.message-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  margin-bottom: 8px;
  background: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-left: 3px solid transparent;
}

.message-item:hover {
  background: #f0f5ff;
  border-left-color: #1890ff;
}

.message-item.unread {
  background: #fff7e6;
  border-left-color: #faad14;
}

.message-item .message-icon-wrapper svg {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-desc {
  font-size: 13px;
  color: #666;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 6px;
}

.message-time {
  font-size: 12px;
  color: #999;
}

.empty-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #999;
}

.empty-icon {
  width: 48px;
  height: 48px;
  margin-bottom: 12px;
}

.message-panel-footer {
  padding: 12px 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
}

.message-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0);
  z-index: 999;
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
  gap: 6px;
  padding: 8px 10px 8px 14px;
  height: 36px;
  line-height: 20px;
  cursor: pointer;
  border-radius: 10px 0 10px 0;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 14px;
  color: #666666;
  white-space: nowrap;
  background: transparent;
  position: relative;
}

.tab-item + .tab-item::after {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 1px;
  height: 16px;
  background: #dcdfe6;
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
  background: linear-gradient(90deg, #e3f2fd 0%, #f5f7fa 50%, #ffffff 100%);
  padding: 1px;
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

</style>
