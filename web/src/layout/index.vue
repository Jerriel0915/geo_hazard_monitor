<template>
  <div class="layout-container" :style="layoutContainerStyle">
    <!-- 非 Chrome 浏览器兼容性提示 -->
    <div v-if="isNotChrome && !browserTipDismissed" class="edge-warning-bar">
      <span class="edge-warning-icon">⚠</span>
      <span>检测到您正在使用非 Chrome 内核浏览器，可能存在兼容性问题，建议使用 <strong>Chrome</strong> 浏览器以获得最佳体验。</span>
      <label class="edge-warning-never" @click.stop>
        <input type="checkbox" v-model="neverShowAgain" /> 不再提醒
      </label>
      <button class="edge-warning-close" @click="dismissBrowserTip">✕</button>
    </div>
    <header class="header">
      <div class="header-left">
        <span class="logo">
          <img v-if="systemLogo" :src="systemLogo" alt="logo" class="logo-img" />
          <svg v-else viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg">
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
        </span>
        <span class="title">{{ systemName }}</span>
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
        <div class="header-icon-btn" @click="openBigScreen" title="大屏展示">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="header-svg-icon">
            <rect x="2" y="3" width="20" height="14" rx="2" ry="2"/>
            <line x1="8" y1="21" x2="16" y2="21"/>
            <line x1="12" y1="17" x2="12" y2="21"/>
          </svg>
        </div>
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
      <div v-show="tabsOverflow" class="tabs-scroll-btn" @click="scrollTabs('left')">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
          <polyline points="15 18 9 12 15 6"/>
        </svg>
      </div>
      <div class="tabs-container" ref="tabsContainerRef">
        <!-- 首页：锁定首位，不可拖动也不可关闭 -->
        <div
            v-if="dashboardTab"
            :key="dashboardTab.name"
            class="tab-item tab-item--locked"
            :class="{ active: activeTab === dashboardTab.name }"
            @click="switchTab(dashboardTab.name)"
        >
          <span>{{ dashboardTab.label }}</span>
        </div>
        <!-- 其他 tabs：可拖动重排 -->
        <draggable
            v-model="draggableTabs"
            item-key="name"
            tag="div"
            class="tabs-draggable-wrap"
            :animation="200"
            ghost-class="tab-ghost"
            chosen-class="tab-chosen"
            drag-class="tab-drag"
        >
          <template #item="{ element: tab }">
            <div
                class="tab-item"
                :class="{ active: activeTab === tab.name }"
                @click="switchTab(tab.name)"
            >
              <span>{{ tab.label }}</span>
              <span
                  class="tab-close"
                  @click.stop="closeTab(tab.name)"
              ><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                    stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="12" height="12"><line x1="18"
                                                                                                                 y1="6"
                                                                                                                 x2="6"
                                                                                                                 y2="18"/><line
                  x1="6" y1="6" x2="18" y2="18"/></svg></span>
            </div>
          </template>
        </draggable>
      </div>
      <div v-show="tabsOverflow" class="tabs-scroll-btn" @click="scrollTabs('right')">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
          <polyline points="9 18 15 12 9 6"/>
        </svg>
      </div>
      <div class="tabs-actions">
        <el-dropdown @command="handleTabAction">
          <el-button size="small" link>
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
    <el-dialog title="基本信息" v-model="infoDialogVisible" width="480px">
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
        <el-button type="primary" :loading="infoSaving" @click="saveUserInfo">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog title="修改密码" v-model="pwdDialogVisible" width="480px">
      <el-form :model="pwdForm" label-width="80px">
        <el-form-item label="原密码">
          <el-input type="password" show-password v-model="pwdForm.oldPassword" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input type="password" show-password v-model="pwdForm.newPassword" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input type="password" show-password v-model="pwdForm.confirmPassword" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdChanging" @click="changePassword">确定</el-button>
      </template>
    </el-dialog>

    <div class="message-panel" :class="{ visible: messagePanelVisible }">
      <div class="message-panel-header">
        <span class="message-panel-title">通知中心</span>
        <div class="message-tabs">
          <span :class="['tab', { active: notifyTab === 'event' }]" @click="switchNotifyTab('event')">
            事件<span v-if="eventUnreadCount > 0" class="tab-count">({{ eventUnreadCount }})</span>
          </span>
          <span :class="['tab', { active: notifyTab === 'notice' }]" @click="switchNotifyTab('notice')">
            公告<span v-if="noticeUnreadCount > 0" class="tab-count">({{ noticeUnreadCount }})</span>
          </span>
        </div>
        <span class="close-btn" @click="messagePanelVisible = false"><svg xmlns="http://www.w3.org/2000/svg"
                                                                          viewBox="0 0 24 24" fill="none"
                                                                          stroke="currentColor" stroke-width="2"
                                                                          stroke-linecap="round" stroke-linejoin="round"
                                                                          width="12" height="12"><line x1="18" y1="6"
                                                                                                       x2="6" y2="18"/><line
            x1="6" y1="6" x2="18" y2="18"/></svg></span>
      </div>
      <!-- 公告状态筛选栏：仅在公告 Tab 激活时显示 -->
      <div v-if="notifyTab === 'notice'" class="status-filter-bar">
        <span :class="['filter-option', { active: noticeStatusFilter === '0' }]"
              @click="setNoticeFilter('0')">当前公告</span>
        <span :class="['filter-option', { active: noticeStatusFilter === '1' }]"
              @click="setNoticeFilter('1')">历史公告</span>
      </div>
      <div class="message-list">
        <!-- 事件 Tab -->
        <template v-if="notifyTab === 'event'">
          <div
            v-for="msg in eventMessages"
            :key="'event-' + msg.id"
            :class="['message-item', { unread: !msg.read }]"
            @click="handleEventClick(msg)"
          >
            <div class="message-icon-wrapper">
              <svg v-if="msg.sourceType !== 'offline'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
              </svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#e6a23c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="2" y="3" width="20" height="14" rx="2" ry="2"/>
                <line x1="8" y1="21" x2="16" y2="21"/>
              </svg>
            </div>
            <div class="message-content">
              <div class="message-title">{{ msg.title }}</div>
              <div class="message-desc">{{ msg.content }}</div>
              <div class="message-time">{{ msg.time }}</div>
            </div>
          </div>
          <div v-if="eventMessages.length === 0" class="empty-message">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#d9d9d9" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="empty-icon">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
            <span>暂无事件通知</span>
          </div>
        </template>
        <!-- 公告 Tab -->
        <template v-else>
          <div
            v-for="msg in noticeMessages"
            :key="'notice-' + msg.id"
            :class="['message-item', { unread: !msg.read }]"
            @click="handleNoticeClick(msg)"
          >
            <div class="message-icon-wrapper">
              <svg v-if="msg.type === 'system'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#1890ff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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
          <div v-if="noticeMessages.length === 0" class="empty-message">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#d9d9d9" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="empty-icon">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
            <span v-if="noticeStatusFilter === '0'">暂无公告</span>
            <template v-else>
              <span>暂无历史公告</span>
              <span class="empty-sub-text">公告关闭后会出现在这里</span>
            </template>
          </div>
        </template>
      </div>
      <div class="message-panel-footer" v-if="currentTabHasMessages">
        <span class="pager" v-if="currentTabTotalPages > 1">
          <span class="pager-btn"
                :class="{ disabled: currentPageRef.current <= 1 }"
                @click="goPrevPage">‹</span>
          <span class="pager-info">{{ currentPageRef.current }}/{{ currentTabTotalPages }}</span>
          <span class="pager-btn"
                :class="{ disabled: currentPageRef.current >= currentTabTotalPages }"
                @click="goNextPage">›</span>
        </span>
        <span class="pager-placeholder" v-else></span>
        <el-button v-if="notifyTab === 'event' || noticeStatusFilter === '0'"
                   size="small" @click="markAllAsRead">全部标为已读</el-button>
        <span v-else class="pager-placeholder"></span>
      </div>
    </div>
    <div class="message-mask" v-if="messagePanelVisible" @click="messagePanelVisible = false"></div>

    <!-- 公告详情弹框（点击公告后展示，替代独立路由） -->
    <el-dialog
      v-model="noticeDetailVisible"
      :title="noticeDetail.noticeTitle || '公告详情'"
      width="640px"
      class="notice-detail-dialog"
      align-center
      append-to-body
    >
      <div v-loading="noticeDetailLoading" class="notice-detail-body">
        <template v-if="noticeDetail.noticeId">
          <div class="notice-detail-meta">
            <el-tag size="small" :type="noticeDetail.noticeType === '1' ? 'info' : 'success'">
              {{ noticeDetail.noticeType === '1' ? '系统' : '其他' }}
            </el-tag>
            <span class="meta-item">发布人：{{ noticeDetail.createBy || '-' }}</span>
            <span class="meta-item">发布时间：{{ noticeDetail.createTime || '-' }}</span>
          </div>
          <el-divider />
          <div v-html="sanitizeNoticeHtml(noticeDetail.noticeContent || '')" class="notice-detail-content"></div>
        </template>
        <el-empty v-else-if="!noticeDetailLoading" description="公告不存在或已被删除" />
      </div>
    </el-dialog>

    <!-- Terra 智能助手 -->
    <TerraWidget />
    <TerraChatPanel />
  </div>
</template>

<script setup lang="ts">
import {getTopNotices, getNoticeById, markRead as markNoticeRead, markReadAll as markAllNoticeRead, type SysNotice} from '@/api/notice'
import {
  getAlarmNotificationPage,
  getAlarmNotificationUnreadCount,
  markAlarmNotificationRead,
  markAllAlarmNotificationsRead,
  type AlarmNotificationItem
} from '@/api/alarmNotification'
import {loadPermissions} from '@/utils/permission'
import {
  getAuthInfo,
  getUserInfo,
  updateUserInfo as updateUserProfile,
  changePassword as changeUserPassword
} from '@/utils/userApi'
import {ElMessage, ElNotification} from 'element-plus'
import {computed, nextTick, onMounted, onUnmounted, reactive, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import draggable from 'vuedraggable'
import request from '@/utils/request'
import {getMenuTree, type MenuItem} from '@/api/system'
import {getMenuIconSvg, resolveRouteName} from '@/utils/menuIcon'
import TerraWidget from '@/components/terra/TerraWidget.vue'
import TerraChatPanel from '@/components/terra/TerraChatPanel.vue'


/** 通知中心统一消息结构（公告 + 事件共用） */
interface NotifyMessage {
  id: number
  title: string
  content: string
  time: string
  read: boolean
  type: string                       // 公告：'system' | 'other'；事件：'alarm'
  sourceType?: 'alarm' | 'offline'   // 事件 Tab 用：跳转目标
  sourceId?: number                  // 事件 Tab 用：跳转目标 ID
}

// 非 Chrome 浏览器检测 + localStorage 持久化"不再提醒"
const BROWSER_TIP_KEY = 'browser_tip_dismissed'
const isNotChrome = ref(
  typeof navigator !== 'undefined'
  && !/Chrome\//.test(navigator.userAgent)
  && !localStorage.getItem(BROWSER_TIP_KEY)
)
const browserTipDismissed = ref(false)
const neverShowAgain = ref(false)

const dismissBrowserTip = () => {
  browserTipDismissed.value = true
  if (neverShowAgain.value) {
    localStorage.setItem(BROWSER_TIP_KEY, '1')
  }
}

const warningBarVisible = computed(() => isNotChrome.value && !browserTipDismissed.value)
const layoutContainerStyle = computed(() => ({
  '--layout-warning-height': warningBarVisible.value ? '36px' : '0px'
}))

const router = useRouter()
const route = useRoute()
const tabsContainerRef = ref<HTMLElement | null>(null)
const tabsOverflow = ref(false)
let tabsResizeObserver: ResizeObserver | null = null

const checkTabsOverflow = () => {
  const el = tabsContainerRef.value
  tabsOverflow.value = el ? el.scrollWidth > el.clientWidth : false
}
const tabs = ref<Array<{ name: string; label: string }>>([])
const activeTab = ref('Dashboard')

/** 首页 tab（锁定不可拖、不可关） */
/** 系统名称 / Logo / 版权 — 从 sys_config 动态加载 */
const systemName = ref('地质灾害监测预警系统')
const systemLogo = ref('')
const systemCopyright = ref('')

const dashboardTab = computed(() => tabs.value.find(t => t.name === 'Dashboard'))

/** 其他 tabs（可拖动），通过 getter/setter 桥接到 tabs.value */
const draggableTabs = computed<Array<{ name: string; label: string }>>({
  get: () => tabs.value.filter(t => t.name !== 'Dashboard'),
  set: (newOrder) => {
    const dash = tabs.value.find(t => t.name === 'Dashboard') ?? {name: 'Dashboard', label: '首页'}
    tabs.value = [dash, ...newOrder]
  }
})
const infoDialogVisible = ref(false)
const pwdDialogVisible = ref(false)

const messagePanelVisible = ref(false)
/** Tab 默认 'event'（更紧急） */
const notifyTab = ref<'event' | 'notice'>('event')
const noticeMessages = ref<NotifyMessage[]>([])
const eventMessages = ref<NotifyMessage[]>([])
const noticeUnreadCount = ref(0)
const eventUnreadCount = ref(0)
/** 公告状态筛选: '0'=当前公告 '1'=历史公告 */
const noticeStatusFilter = ref<'0' | '1'>('0')
/** 分页状态：事件/公告各持一份，SSE 推送后回第 1 页 */
const eventPage = reactive({ current: 1, size: 10, total: 0 })
const noticePage = reactive({ current: 1, size: 10, total: 0 })
let noticeEventSource: EventSource | null = null
let alarmEventSource: EventSource | null = null
let noticeReconnectTimer: ReturnType<typeof setTimeout> | null = null
let alarmReconnectTimer: ReturnType<typeof setTimeout> | null = null
let sseStopped = false
let noticeRetryCount = 0
let alarmRetryCount = 0
const MAX_SSE_RETRIES = 10
const SSE_BASE_DELAY = 3000
const SSE_MAX_DELAY = 30000

/** 顶部铃铛角标总数 */
const unreadMessageCount = computed(() => noticeUnreadCount.value + eventUnreadCount.value)

/** 当前 Tab 是否有消息（控制底部"全部标为已读"按钮） */
const currentTabHasMessages = computed(() =>
  notifyTab.value === 'event' ? eventMessages.value.length > 0 : noticeMessages.value.length > 0
)

/** 当前 Tab 的分页对象（事件或公告） */
const currentPageRef = computed(() => notifyTab.value === 'event' ? eventPage : noticePage)
/** 当前 Tab 的总页数（至少 1，避免 1/0 显示） */
const currentTabTotalPages = computed(() => Math.max(1, Math.ceil(currentPageRef.value.total / currentPageRef.value.size)))

function switchNotifyTab(tab: 'event' | 'notice') {
  notifyTab.value = tab
}

function toNoticeMessage(n: SysNotice): NotifyMessage {
  return {
    id: n.noticeId,
    title: n.noticeTitle,
    content: n.noticeContent?.replace(/<[^>]*>/g, '') ?? '',
    time: n.createTime ?? '',
    read: n.isRead ?? false,
    type: n.noticeType === '1' ? 'system' : 'other'
  }
}

function toEventMessage(n: AlarmNotificationItem): NotifyMessage {
  return {
    id: n.id,
    title: n.title,
    content: n.content ?? '',
    time: n.createTime ?? '',
    read: n.readTime != null,
    type: 'alarm',
    sourceType: n.sourceType,
    sourceId: n.sourceId
  }
}

async function fetchNoticeMessages() {
  try {
    const res = await getTopNotices(noticePage.current, noticePage.size, noticeStatusFilter.value)
    // 后端响应：{code,msg,data: SysNotice[], total, unreadCount, timestamp}
    noticeMessages.value = (res.data ?? []).map(toNoticeMessage)
    noticePage.total = res.total ?? 0
    noticeUnreadCount.value = res.unreadCount ?? 0
  } catch { /* keep previous data */ }
}

/** 切换公告状态筛选（当前/历史） */
function setNoticeFilter(status: '0' | '1') {
  if (noticeStatusFilter.value === status) return
  noticeStatusFilter.value = status
  noticePage.current = 1
  fetchNoticeMessages()
}

async function fetchEventMessages() {
  try {
    const [pageRes, unreadRes] = await Promise.all([
      getAlarmNotificationPage(eventPage.current, eventPage.size),
      getAlarmNotificationUnreadCount()
    ])
    eventMessages.value = (pageRes.data ?? []).map(toEventMessage)
    eventPage.total = pageRes.total ?? 0
    eventUnreadCount.value = unreadRes.data?.unreadCount ?? 0
  } catch { /* keep previous data */ }
}

/** 重新加载当前 Tab（翻页或外部触发） */
async function reloadCurrentTab() {
  if (notifyTab.value === 'event') await fetchEventMessages()
  else await fetchNoticeMessages()
}

/** 上一页：第 1 页禁用 */
async function goPrevPage() {
  if (currentPageRef.value.current <= 1) return
  currentPageRef.value.current--
  await reloadCurrentTab()
}

/** 下一页：超过总页数禁用 */
async function goNextPage() {
  if (currentPageRef.value.current >= currentTabTotalPages.value) return
  currentPageRef.value.current++
  await reloadCurrentTab()
}

function startNoticeSSE() {
  if (sseStopped) return
  if (noticeEventSource) noticeEventSource.close()
  const token = localStorage.getItem('token')
  if (!token) return
  noticeEventSource = new EventSource(`/api/v1/system/notice/stream?token=${encodeURIComponent(token)}`)
  noticeEventSource.addEventListener('notice', (event) => {
    try {
      const data = JSON.parse(event.data)
      // 新公告到达 → 切回"当前"模式 + 第 1 页
      noticeStatusFilter.value = '0'
      noticePage.current = 1
      fetchNoticeMessages()
    } catch { /* ignore malformed event */ }
  })
  noticeEventSource.onerror = () => {
    noticeEventSource?.close()
    if (sseStopped) return
    if (noticeEventSource?.readyState === EventSource.CLOSED) {
      noticeRetryCount++
      if (noticeRetryCount > MAX_SSE_RETRIES) return
    }
    if (noticeReconnectTimer) clearTimeout(noticeReconnectTimer)
    const delay = Math.min(SSE_BASE_DELAY * Math.pow(2, Math.max(0, noticeRetryCount - 1)), SSE_MAX_DELAY)
    noticeReconnectTimer = setTimeout(startNoticeSSE, delay)
  }
  noticeEventSource.addEventListener('open', () => { noticeRetryCount = 0 })
}

/** 告警 SSE：监听 alarm-notify 单点事件 + alarm 全量广播 */
function startAlarmSSE() {
  if (sseStopped) return
  if (alarmEventSource) alarmEventSource.close()
  const token = localStorage.getItem('token')
  if (!token) return
  alarmEventSource = new EventSource(`/api/v1/alarm/stream?token=${encodeURIComponent(token)}`)
  // SYSTEM 通知定向推送（按接收人路由）
  alarmEventSource.addEventListener('alarm-notify', (event) => {
    try {
      const data = JSON.parse(event.data)
      ElNotification({
        title: data.title ?? '告警通知',
        message: data.content ?? '',
        type: 'warning',
        duration: 5000
      })
      eventPage.current = 1
      fetchEventMessages()
    } catch { /* ignore */ }
  })
  // 告警原始事件全量广播（兜底，确保前端有感知）
  alarmEventSource.addEventListener('alarm', (event) => {
    try {
      const data = JSON.parse(event.data)
      ElNotification({
        title: '告警',
        message: data.alarmMessage ?? '',
        type: 'error',
        duration: 5000
      })
      eventPage.current = 1
      fetchEventMessages()
    } catch { /* ignore */ }
  })
  alarmEventSource.onerror = () => {
    alarmEventSource?.close()
    if (sseStopped) return
    if (alarmEventSource?.readyState === EventSource.CLOSED) {
      alarmRetryCount++
      if (alarmRetryCount > MAX_SSE_RETRIES) return
    }
    if (alarmReconnectTimer) clearTimeout(alarmReconnectTimer)
    const delay = Math.min(SSE_BASE_DELAY * Math.pow(2, Math.max(0, alarmRetryCount - 1)), SSE_MAX_DELAY)
    alarmReconnectTimer = setTimeout(startAlarmSSE, delay)
  }
  alarmEventSource.addEventListener('open', () => { alarmRetryCount = 0 })
}

const currentUser = reactive({
  name: '管理员'
})

const userInfo = reactive({
  id: 0,
  username: '',
  realName: '',
  phone: '',
  email: '',
  sex: '',
  organization: '',
  remark: ''
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdChanging = ref(false)
const infoSaving = ref(false)

const activeMenu = ref('')
const isAdmin = ref(false)

// ========== 动态菜单 ==========

/** 导航菜单项（模板渲染格式） */
interface NavMenuItem {
  name: string
  label: string
  icon: string
  children?: NavMenuItem[]
  routeName?: string
}

/** 原始菜单树（API 返回） */
const rawMenuTree = ref<MenuItem[]>([])
/** 转换后的导航菜单列表 */
const navMenuList = ref<NavMenuItem[]>([])
/** routeName → label 快速查找表 */
const routeLabelMap = ref<Record<string, string>>({})
/** 有效的 routeName 集合（已注册的 vue-router 路由名） */
const validRouteNames = ref<Set<string>>(new Set())

/** 将 API MenuItem 树转换为 NavMenuItem 树 */
function buildNavMenu(menus: MenuItem[]): NavMenuItem[] {
  const result: NavMenuItem[] = []
  for (const menu of menus) {
    // 跳过按钮类型和隐藏的菜单
    if (menu.type === 2) continue
    if (menu.visible === 1 && menu.type === 0) {
      // 隐藏的目录：仍然处理其子节点（子节点可能可见）
      if (menu.children?.length) {
        result.push(...buildNavMenu(menu.children))
      }
      continue
    }
    if (menu.visible === 1) continue

    const routeName = resolveRouteName(menu.code, menu.name)
    const item: NavMenuItem = {
      name: routeName || String(menu.id),
      label: menu.name,
      icon: getMenuIconSvg(menu.icon),
      routeName
    }
    // 注册 label
    if (routeName) {
      routeLabelMap.value[routeName] = menu.name
    }
    // 递归处理子节点
    if (menu.children?.length) {
      const children = buildNavMenu(menu.children)
      if (children.length > 0) {
        item.children = children
      }
    }
    result.push(item)
  }
  return result
}

/** 递归收集所有有效路由名（有 routeName 的叶子节点） */
function collectRouteNames(items: NavMenuItem[]) {
  for (const item of items) {
    if (item.routeName) {
      validRouteNames.value.add(item.routeName)
    }
    if (item.children?.length) {
      collectRouteNames(item.children)
    }
  }
}

/** 从 API 加载菜单树 */
async function loadNavMenus() {
  try {
    const tree = await getMenuTree()
    rawMenuTree.value = tree
    routeLabelMap.value = {}
    validRouteNames.value = new Set()
    navMenuList.value = buildNavMenu(tree)
    collectRouteNames(navMenuList.value)
    // 确保首页标签始终存在
    routeLabelMap.value['Dashboard'] = '首页'
    validRouteNames.value.add('Dashboard')
  } catch {
    // 菜单加载失败时保留空列表
    navMenuList.value = []
  }
}

// 直接使用动态菜单（后端 getMenuTree 已按角色过滤）
const filteredMenuList = computed(() => navMenuList.value)

/** 根据 key（routeName）查找菜单 label */
function findMenuLabel(key: string): string {
  return routeLabelMap.value[key] || key
}

const handleMenuSelect = (key: string) => {
  if (validRouteNames.value.has(key)) {
    router.push({ name: key })
    if (!tabs.value.find(tab => tab.name === key)) {
      tabs.value.push({ name: key, label: findMenuLabel(key) })
    }
    activeTab.value = key
    activeMenu.value = key
  }
}

const switchTab = (name: string) => {
  activeTab.value = name
  activeMenu.value = name === 'Dashboard' ? '' : name
  if (validRouteNames.value.has(name)) {
    router.push({ name })
  }
}

const closeTab = (name: string) => {
  // 首页锁定：不允许关闭
  if (name === 'Dashboard') return
  const index = tabs.value.findIndex(tab => tab.name === name)
  if (index !== -1) {
    tabs.value.splice(index, 1)
    if (activeTab.value === name) {
      if (tabs.value.length > 0) {
        const newIndex = index > 0 ? index - 1 : 0
        const newTab = tabs.value[newIndex]
        activeTab.value = newTab.name
        activeMenu.value = newTab.name === 'Dashboard' ? '' : newTab.name
        if (validRouteNames.value.has(newTab.name)) {
          router.push({ name: newTab.name })
        }
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
    // 保留首页 + 当前激活页
    tabs.value = tabs.value.filter(
        tab => tab.name === activeTab.value || tab.name === 'Dashboard'
    )
  }
}

const handleUserCommand = async (command: string) => {
  if (command === 'info') {
    router.push('/user/profile')
  } else if (command === 'password') {
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    pwdDialogVisible.value = true
  } else if (command === 'logout') {
    localStorage.removeItem('token')
    router.push('/login')
  }
}

const saveUserInfo = async () => {
  if (!userInfo.realName?.trim()) {
    ElMessage.warning('请输入真实姓名')
    return
  }
  const phonePattern = /^1[3-9]\d{9}$/
  if (userInfo.phone && !phonePattern.test(userInfo.phone)) {
    ElMessage.warning('请输入正确的手机号码')
    return
  }
  const emailPattern = /^[\w.-]+@[\w.-]+\.[A-Za-z]{2,}$/
  if (userInfo.email && !emailPattern.test(userInfo.email)) {
    ElMessage.warning('请输入正确的邮箱地址')
    return
  }

  infoSaving.value = true
  try {
    await updateUserProfile(userInfo.id, {
      realName: userInfo.realName,
      phone: userInfo.phone,
      email: userInfo.email,
      sex: userInfo.sex
    })
    ElMessage.success('信息保存成功')
    infoDialogVisible.value = false
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败，请重试')
  } finally {
    infoSaving.value = false
  }
}

const changePassword = async () => {
  if (!pwdForm.oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (!pwdForm.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (pwdForm.newPassword.length < 6 || pwdForm.newPassword.length > 20) {
    ElMessage.warning('新密码长度需为 6-20 位')
    return
  }
  if (pwdForm.newPassword === pwdForm.oldPassword) {
    ElMessage.warning('新密码不能与原密码相同')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  pwdChanging.value = true
  try {
    await changeUserPassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    pwdDialogVisible.value = false
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    // 密码已变更，旧 token 失效，引导重新登录
    setTimeout(() => {
      localStorage.removeItem('token')
      router.push('/login')
    }, 1500)
  } catch (error: any) {
    ElMessage.error(error?.message || '密码修改失败，请重试')
  } finally {
    pwdChanging.value = false
  }
}

const toggleMessagePanel = () => {
  messagePanelVisible.value = !messagePanelVisible.value
}

const openBigScreen = () => {
  window.open('/bigscreen/disaster', '_blank')
}

/** 公告详情弹框（替代独立路由，避免上下文切换） */
const noticeDetailVisible = ref(false)
const noticeDetailLoading = ref(false)
const noticeDetail = ref<Partial<SysNotice>>({})

/**
 * XSS 净化 — 移除常见攻击向量。
 * 覆盖：script/iframe/object/embed/svg/style/meta/link 标签、事件处理器属性、javascript: URI。
 * 注：完整净化应使用 DOMPurify，当前用于管理员后台公告（半可信输入）。
 */
function sanitizeNoticeHtml(html: string): string {
  let s = html ?? ''
  // 完整标签（含内容）
  s = s.replace(/<(script|iframe|object|embed|svg|style|meta|link)\b[^>]*>.*?<\/\1>/gis, '')
  // 自闭合/空标签
  s = s.replace(/<(script|iframe|object|embed|svg|style|meta|link)\b[^>]*\/?>/gi, '')
  // 事件处理器属性 on*= (onclick, onerror, onload...)
  s = s.replace(/\s+on\w+\s*=\s*(?:"[^"]*"|'[^']*'|[^\s>]*)/gi, '')
  // javascript: URI 在 href/src/action 属性中
  s = s.replace(/(?:href|src|action)\s*=\s*(?:"[^"]*javascript:[^"]*"|'[^']*javascript:[^']*')/gi, '')
  return s
}

const handleNoticeClick = async (msg: NotifyMessage) => {
  // 乐观更新已读状态（详情页加载后异步持久化到后端）
  const wasUnread = !msg.read
  if (wasUnread) {
    msg.read = true
    noticeUnreadCount.value = Math.max(0, noticeUnreadCount.value - 1)
  }
  messagePanelVisible.value = false
  noticeDetailVisible.value = true
  noticeDetailLoading.value = true
  noticeDetail.value = {}
  try {
    const res = await getNoticeById(msg.id)
    noticeDetail.value = res.data ?? {}
    // 异步标记已读（非阻塞，失败时乐观更新仍保留以避免 UI 抖动）
    if (wasUnread) {
      markNoticeRead(msg.id).catch(() => { /* ignore: 乐观更新已生效 */ })
    }
  } catch {
    ElNotification({ title: '提示', message: '公告加载失败', type: 'warning', duration: 3000 })
  } finally {
    noticeDetailLoading.value = false
  }
}

const handleEventClick = async (msg: NotifyMessage) => {
  if (!msg.read) {
    try {
      await markAlarmNotificationRead(msg.id)
      eventUnreadCount.value = Math.max(0, eventUnreadCount.value - 1)
      // 已读后从列表移除（与后端 selectUserRecent 过滤一致）
      eventMessages.value = eventMessages.value.filter(m => m.id !== msg.id)
    } catch { /* ignore */ }
  }
  if (msg.sourceType === 'offline') {
    router.push({path: '/basic/device', query: msg.sourceId ? {deviceId: String(msg.sourceId)} : {}})
  } else {
    // threshold / comprehensive / 历史 alarm → 均跳转告警详情
    router.push({path: '/alarm/realtime', query: msg.sourceId ? {alarmId: String(msg.sourceId)} : {}})
  }
  messagePanelVisible.value = false
}

const markAllAsRead = async () => {
  if (notifyTab.value === 'event') {
    try {
      await markAllAlarmNotificationsRead()
      // 全部已读 → 列表清空（后端查询也会过滤已读项）
      eventMessages.value = []
      eventUnreadCount.value = 0
      eventPage.current = 1
      eventPage.total = 0
    } catch { /* ignore */ }
  } else {
    const unreadIds = noticeMessages.value.filter(m => !m.read).map(m => m.id)
    if (unreadIds.length === 0) return
    try {
      await markAllNoticeRead(unreadIds.join(','))
      noticeMessages.value.forEach(m => { m.read = true })
      noticeUnreadCount.value = 0
      noticePage.current = 1
      noticePage.total = 0
    } catch { /* ignore */ }
  }
}

/** localStorage key：tabs 状态持久化 */
const TABS_STORAGE_KEY = 'zwei.layout.tabs'

/** 校验 storage 中读出的 tabs 数据结构 */
const isValidTab = (t: unknown): t is { name: string; label: string } => {
  return !!t
      && typeof t === 'object'
      && typeof (t as any).name === 'string'
      && typeof (t as any).label === 'string'
      && validRouteNames.value.has((t as any).name) // 名称必须在有效路由中
}

/** 同步 tabs/activeTab/activeMenu 到当前路由 */
const syncTabWithRoute = (routeName: string | null | undefined) => {
  if (!routeName) return
  const label = routeLabelMap.value[routeName]
  if (!label) return // 路由未在 tabs 体系中（如 H5Disposal/SysNotice 等），不处理
  if (!tabs.value.find(t => t.name === routeName)) {
    tabs.value.push({name: routeName, label})
  }
  activeTab.value = routeName
  activeMenu.value = routeName === 'Dashboard' ? '' : routeName
}

onMounted(async () => {
  // 0. 加载动态菜单（必须在 tabs 恢复前完成，因为 isValidTab/syncTabWithRoute 依赖它）
  await loadNavMenus()

  // 1. 从 localStorage 恢复已打开的 tabs
  try {
    const raw = localStorage.getItem(TABS_STORAGE_KEY)
    if (raw) {
      const saved = JSON.parse(raw)
      if (Array.isArray(saved)) {
        const valid = saved.filter(isValidTab)
        if (valid.length > 0) tabs.value = valid
      }
    }
  } catch { /* ignore parse error */
  }

  // 2. 兜底：必须始终包含首页且置于首位
  if (!tabs.value.find(t => t.name === 'Dashboard')) {
    tabs.value = [{name: 'Dashboard', label: '首页'}, ...tabs.value]
  } else if (tabs.value[0]?.name !== 'Dashboard') {
    const others = tabs.value.filter(t => t.name !== 'Dashboard')
    tabs.value = [{name: 'Dashboard', label: '首页'}, ...others]
  }

  // 3. 关键修复：以当前实际路由覆盖 activeTab，并确保对应 tab 存在
  syncTabWithRoute(String(route.name || 'Dashboard'))

  try {
    await loadPermissions()
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
  // 首次加载通知（公告 + 事件）+ SSE 实时推送
  fetchNoticeMessages()
  fetchEventMessages()
  startNoticeSSE()
  startAlarmSSE()

  // 加载系统外观配置（系统名称 / Logo / 版权）
  try {
    const [nameRes, logoRes, copyrightRes] = await Promise.all([
      request.get<any>('/system/config/configKey/sys_name'),
      request.get<any>('/system/config/configKey/sys_logo'),
      request.get<any>('/system/config/configKey/sys_copyright'),
    ]);
    const n = (nameRes as any)?.data ?? (nameRes as any)?.msg
    if (n && typeof n === 'string' && n.trim()) systemName.value = n.trim()
    const l = (logoRes as any)?.data ?? (logoRes as any)?.msg
    if (l && typeof l === 'string' && l.trim()) systemLogo.value = l.trim()
    const c = (copyrightRes as any)?.data ?? (copyrightRes as any)?.msg
    if (c && typeof c === 'string' && c.trim()) systemCopyright.value = c.trim()
  } catch { /* 未配置时使用默认值 */ }

  // 4. 页签溢出检测 — 自动显示/隐藏左右滚动按钮
  if (tabsContainerRef.value) {
    tabsResizeObserver = new ResizeObserver(checkTabsOverflow)
    tabsResizeObserver.observe(tabsContainerRef.value)
    checkTabsOverflow()
  }
})

onUnmounted(() => {
  if (tabsResizeObserver) {
    tabsResizeObserver.disconnect()
    tabsResizeObserver = null
  }
  sseStopped = true
  if (noticeEventSource) {
    noticeEventSource.close()
    noticeEventSource = null
  }
  if (alarmEventSource) {
    alarmEventSource.close()
    alarmEventSource = null
  }
  if (noticeReconnectTimer) {
    clearTimeout(noticeReconnectTimer)
    noticeReconnectTimer = null
  }
  if (alarmReconnectTimer) {
    clearTimeout(alarmReconnectTimer)
    alarmReconnectTimer = null
  }
})

// 路由变化时同步 tabs/activeTab/activeMenu（支持浏览器前进后退、菜单外的 router.push）
watch(() => route.name, (name) => {
  if (name) syncTabWithRoute(String(name))
})

// tabs 变化时持久化到 localStorage，并重新检测溢出（nextTick 等 DOM 更新后）
watch(tabs, (val) => {
  try {
    localStorage.setItem(TABS_STORAGE_KEY, JSON.stringify(val))
  } catch { /* 配额超限等异常忽略 */
  }
  nextTick(checkTabsOverflow)
}, {deep: true})

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
/* Edge browser warning bar */
.edge-warning-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 20px;
  background: linear-gradient(135deg, #fff7e6, #fff1cc);
  border-bottom: 1px solid #ffd666;
  font-size: 13px;
  color: #874d00;
}

.edge-warning-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.edge-warning-bar strong {
  color: #d46b08;
}

.edge-warning-never {
  margin-left: 12px;
  font-size: 12px;
  color: #bfbfbf;
  cursor: pointer;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.edge-warning-never:hover {
  color: #8c8c8c;
}

.edge-warning-close {
  margin-left: auto;
  background: none;
  border: none;
  font-size: 16px;
  color: #bfbfbf;
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
}

.edge-warning-close:hover {
  color: #ff4d4f;
}

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
}

.logo-img {
  width: 44px;
  height: 44px;
  object-fit: contain;
  border-radius: 6px;
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
  color: white;
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

.header-icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  cursor: pointer;
  border-radius: 50%;
  margin-right: 4px;
  transition: all 0.3s ease;
}

.header-icon-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.header-svg-icon {
  width: 22px;
  height: 22px;
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
  max-height: calc(100vh - 64px);
  background: white;
  border-radius: 8px 0 0 8px;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1);
  transition: right 0.3s ease;
  z-index: 1000;
  display: flex;
  flex-direction: column;
}

.tab-count {
  display: inline-block;
  margin-left: 2px;
  font-size: 11px;
  color: #f56c6c;
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

.empty-sub-text {
  font-size: 12px;
  color: #bbb;
  margin-top: 4px;
}

/* 公告状态筛选栏 */
.status-filter-bar {
  display: flex;
  gap: 24px;
  padding: 4px 20px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
  font-size: 13px;
}

.filter-option {
  position: relative;
  padding-bottom: 4px;
  color: #999;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s ease;
  user-select: none;
}

.filter-option:hover {
  color: #666;
}

.filter-option.active {
  color: #1890ff;
  font-weight: 500;
  border-bottom-color: #1890ff;
}

.message-panel-footer {
  padding: 12px 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pager-placeholder {
  /* 占位：当只有一页时保持 footer 两端对齐 */
  display: inline-block;
  width: 1px;
}

.pager {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #606266;
  user-select: none;
}

.pager-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 4px;
  cursor: pointer;
  color: #303133;
  font-size: 14px;
  line-height: 1;
  transition: background-color 0.15s;
}

.pager-btn:hover {
  background-color: #f0f0f0;
}

.pager-btn.disabled {
  color: #c0c4cc;
  cursor: not-allowed;
  pointer-events: none;
}

.pager-info {
  min-width: 36px;
  text-align: center;
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

.notice-detail-body {
  min-height: 120px;
}

.notice-detail-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  color: #909399;
  font-size: 13px;
}

.notice-detail-meta .meta-item {
  white-space: nowrap;
}

.notice-detail-content {
  color: #303133;
  font-size: 14px;
  line-height: 1.7;
  /* 允许公告内联 HTML 中的图片自适应 */
  max-height: 60vh;
  overflow-y: auto;
}

.notice-detail-content :deep(img) {
  max-width: 100%;
  height: auto;
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

/* 可拖动 tabs 容器：用 display: contents 让 children 直接参与 .tabs-container 的 flex 布局 */
.tabs-draggable-wrap {
  display: contents;
}

/* 首页锁定 tab：保留 pointer，不参与拖动 */
.tab-item--locked {
  cursor: pointer;
}

/* Sortable.js ghost: 拖动时原位置的占位 */
.tab-ghost {
  opacity: 0.4;
  background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%) !important;
  border: 1px dashed #1890ff !important;
}

/* Sortable.js chosen: 被按住选中等待拖动 */
.tab-chosen {
  cursor: grabbing !important;
}

/* Sortable.js drag: 正在被拖动的元素 */
.tab-drag {
  opacity: 0.8;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.25);
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
  min-height: 0;
  background: linear-gradient(90deg, #e3f2fd 0%, #f5f7fa 50%, #ffffff 100%);
  padding: 1px;
  overflow: hidden;
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
