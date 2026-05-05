import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layout/index.vue'),
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
      { path: '/alarm/disposal', name: 'AlarmDisposal', component: () => import('@/views/alarm/AlarmDisposal.vue') },
      { path: '/report/report', name: 'Report', component: () => import('@/views/report/Report.vue') },
      { path: '/report/query', name: 'Query', component: () => import('@/views/report/Query.vue') },
      { path: '/report/analysis', name: 'Analysis', component: () => import('@/views/report/Analysis.vue') },
      { path: '/report/screen', name: 'Screen', component: () => import('@/views/report/Screen.vue') },
      { path: '/iot/alarm-engine', name: 'AlarmEngine', component: () => import('@/views/iot/AlarmEngine.vue') },
      { path: '/iot/data-parse', name: 'DataParse', component: () => import('@/views/iot/DataParse.vue') },
      { path: '/system/organization', name: 'Organization', component: () => import('@/views/system/Organization.vue') },
      { path: '/system/identity', name: 'Identity', component: () => import('@/views/system/Identity.vue') },
      { path: '/system/permission', name: 'Permission', component: () => import('@/views/system/Permission.vue') },
      { path: '/system/log', name: 'Log', component: () => import('@/views/system/Log.vue') },
      { path: '/system/settings', name: 'Settings', component: () => import('@/views/system/Settings.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  if (to.path !== '/login') {
    if (!localStorage.getItem('token')) {
      next('/login')
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
