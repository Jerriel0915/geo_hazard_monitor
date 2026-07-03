import type { RouteRecordRaw } from 'vue-router'
import { createRouter, createWebHistory } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/h5x/disposal/:id?',
    name: 'H5Disposal',
    component: () => import('@/views/alarm/H5Disposal.vue'),
    meta: { title: 'H5在线处置' }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      { path: '/dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/Dashboard.vue') },
      { path: '/holo-board/comprehensive', name: 'Comprehensive', component: () => import('@/views/holo-board/Comprehensive.vue') },
      { path: '/holo-board/alarm', name: 'Alarm', component: () => import('@/views/holo-board/Alarm.vue') },
      { path: '/holo-board/operation', name: 'Operation', component: () => import('@/views/holo-board/Operation.vue') },
      { path: '/holo-board/custom', name: 'Custom', component: () => import('@/views/holo-board/Custom.vue') },
      { path: '/basic/hazard-point', name: 'HazardPoint', component: () => import('@/views/basic/HazardPoint.vue') },
      { path: '/basic/monitor-type', name: 'MonitorType', component: () => import('@/views/basic/MonitorType.vue') },
      { path: '/basic/device', name: 'Device', component: () => import('@/views/basic/Device.vue') },
      { path: '/basic/video-device', name: 'VideoDevice', component: () => import('@/views/basic/VideoDevice.vue') },
      { path: '/alarm/realtime', name: 'RealtimeAlarm', component: () => import('@/views/alarm/RealtimeAlarm.vue') },
      { path: '/alarm/criteria', name: 'AlarmCriteria', component: () => import('@/views/alarm/AlarmCriteria.vue') },
      { path: '/alarm/notification', name: 'AlarmNotification', component: () => import('@/views/alarm/AlarmNotification.vue') },
      { path: '/alarm/disposal', name: 'AlarmDisposal', component: () => import('@/views/alarm/CompositeAlarm.vue') },
      { path: '/alarm/composite', name: 'CompositeAlarm', component: () => import('@/views/alarm/CompositeAlarm.vue') },
      { path: '/alarm/algo-library', name: 'AlgoLibrary', component: () => import('@/views/alarm/AlgoLibrary.vue') },
        {
            path: '/alarm/notification-setting',
            name: 'NotificationSetting',
            component: () => import('@/views/alarm/NotificationSetting.vue')
        },
      { path: '/report/report', name: 'Report', component: () => import('@/views/report/Report.vue') },
      { path: '/report/query', name: 'Query', component: () => import('@/views/report/Query.vue') },
      { path: '/report/analysis', name: 'Analysis', component: () => import('@/views/report/Analysis.vue') },
      { path: '/report/share-strategy', name: 'ShareStrategy', component: () => import('@/views/report/ShareStrategy.vue') },
      { path: '/iot/data-parse', name: 'DataParse', component: () => import('@/views/iot/DataParse.vue') },
        {path: '/iot/service-status', name: 'ServiceStatus', component: () => import('@/views/iot/ServiceStatus.vue')},
        {
            path: '/miniprogram/hazard-point',
            name: 'MpHazardPoint',
            component: () => import('@/views/miniprogram/HazardPoint.vue')
        },
        {path: '/miniprogram/device', name: 'MpDevice', component: () => import('@/views/miniprogram/Device.vue')},
        {
            path: '/miniprogram/monitor-data',
            name: 'MpMonitorData',
            component: () => import('@/views/miniprogram/MonitorData.vue')
        },
      { path: '/system/organization', name: 'Organization', component: () => import('@/views/system/Organization.vue') },
      { path: '/system/identity', name: 'Identity', component: () => import('@/views/system/Identity.vue') },
      { path: '/system/permission', name: 'Permission', component: () => import('@/views/system/Permission.vue') },
      { path: '/system/log', name: 'Log', component: () => import('@/views/system/Log.vue') },
      { path: '/system/settings', name: 'Settings', component: () => import('@/views/system/Settings.vue') },
      { path: '/system/notice', name: 'SysNotice', component: () => import('@/views/system/SysNotice.vue') },
      { path: '/user/profile', name: 'UserProfile', component: () => import('@/views/user/UserProfile.vue') },
      // === Terra 智能助手 ===
      {
        path: '/terra/settings',
        name: 'TerraSettings',
        component: () => import('@/views/terra/SettingsLayout.vue'),
        children: [
          { path: '', redirect: '/terra/settings/personality' },
          { path: 'personality', name: 'TerraPersonality', component: () => import('@/views/terra/PersonalitySettings.vue') },
          { path: 'models', name: 'TerraModelConfigs', component: () => import('@/views/terra/ModelConfigList.vue') },
          { path: 'skills', name: 'TerraSkills', component: () => import('@/views/terra/SkillManager.vue') },
          { path: 'tools', name: 'TerraTools', component: () => import('@/views/terra/ToolManager.vue') },
        ]
      }
    ]
  },
  {
    path: '/report/screen',
    name: 'Screen',
    component: () => import('@/views/bigscreen/Screen.vue')
  },
  {
    path: '/bigscreen/disaster',
    name: 'DisasterScreen',
    component: () => import('@/views/bigscreen/DisasterScreen.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  if (to.path !== '/login') {
    if (!localStorage.getItem('token')) {
      // 记录原始目标路径，登录后由 Login.vue 跳回（支持 H5 流程：未登录访问 /h5/disposal/:id → 登录后自动返回）
      next({ path: '/login', query: { redirect: to.fullPath } })
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
