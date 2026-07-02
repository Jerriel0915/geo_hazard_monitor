<!-- src/pages/profile.vue -->
<template>
  <view class="page-container">
    <!-- 下载进度浮层 -->
    <view v-if="downloading" class="download-overlay">
      <view class="download-dialog">
        <view class="download-title">正在下载更新...</view>
        <view class="download-info">
          <text class="version-text">{{ pendingUpdate?.versionName }}</text>
          <text class="progress-text">{{ downloadProgress }}%</text>
        </view>
        <view class="progress-container">
          <view class="progress-bar-bg">
            <view class="progress-bar-fill" :style="{ width: downloadProgress + '%' }"></view>
          </view>
        </view>
        <view class="download-size" v-if="pendingUpdate">
          {{ formatFileSize(pendingUpdate.fileSize) }}
        </view>
      </view>
    </view>

    <!-- 渐变头部 -->
    <PageHeader>
      <view class="user-card">
        <view class="avatar">
          <text class="avatar-text">{{ avatarText }}</text>
        </view>
        <view class="user-info">
          <text class="user-name">{{ user.nickname || user.username || '未登录' }}</text>
          <text class="user-phone">{{ user.phone || '未绑定手机' }}</text>
        </view>
      </view>
    </PageHeader>

    <!-- 内容区域 -->
    <scroll-view class="page-body" scroll-y>
      <!-- 设置菜单 -->
      <view class="section">
        <text class="section-title"></text>
        <view class="menu-list">
          <view class="menu-item">
            <view class="menu-left">
              <zui-svg-icon icon="bell" width="36rpx" />
              <text class="menu-text">告警通知</text>
            </view>
            <switch color="#3068e4" :checked="alarmSubscribed" @change="toggleAlarmSubscription" />
          </view>
          <view class="menu-item" @tap="handleCheckUpdate">
            <view class="menu-left">
              <zui-svg-icon icon="refresh" width="36rpx" />
              <text class="menu-text">检查更新</text>
            </view>
            <view class="menu-right">
              <text class="current-version">v{{ currentVersion }}</text>
              <zui-svg-icon icon="arrow-right" width="28rpx" color="#9ca3af" />
            </view>
          </view>
          <view class="menu-item" @tap="goToAbout">
            <view class="menu-left">
              <zui-svg-icon icon="info" width="36rpx" />
              <text class="menu-text">关于我们</text>
            </view>
            <zui-svg-icon icon="arrow-right" width="28rpx" color="#9ca3af" />
          </view>
        </view>
      </view>

      <!-- 退出登录按钮 -->
      <view class="logout-section">
        <button class="logout-btn" @tap="handleLogout">退出登录</button>
      </view>

      <!-- 底部留白 -->
      <view class="bottom-spacer" />
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import miniappApi from '@/utils/miniapp'
import authApi from '@/utils/auth'
import { checkUpdate, getCurrentVersionName, downloadAndInstallApk } from '@/utils/appVersion'
import type { AppVersionInfo } from '@/utils/appVersion'

const user = ref({
  id: null as number | null,
  username: '',
  nickname: '',
  phone: '',
  avatar: ''
})

const alarmSubscribed = ref(false)
const currentVersion = ref('1.0.0')
const pendingUpdate = ref<AppVersionInfo | null>(null)
const downloading = ref(false)
const downloadProgress = ref(0)

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

const avatarText = computed(() => {
  const name = user.value.nickname || user.value.username || '未'
  return name.charAt(0).toUpperCase()
})

onMounted(() => {
  loadUserInfo()
  loadSubscriptionStatus()
  currentVersion.value = getCurrentVersionName()
})

const loadUserInfo = () => {
  try {
    const userStr = uni.getStorageSync('user')
    if (userStr) {
      user.value = JSON.parse(userStr)
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
  }
}

const loadSubscriptionStatus = () => {
  try {
    const subscribed = uni.getStorageSync('alarmSubscribed')
    alarmSubscribed.value = subscribed === 'true' || subscribed === true
  } catch (error) {
    console.error('加载订阅状态失败:', error)
  }
}

const toggleAlarmSubscription = async (e: any) => {
  const checked = e.detail.value

  // #ifdef MP-WEIXIN
  if (checked) {
    try {
      const res = await uni.requestSubscribeMessage({
        tmplIds: ['您的告警通知模板ID']
      })

      if ((res as any)['您的告警通知模板ID'] === 'accept') {
        await miniappApi.saveSubscription('ALARM', user.value.id)
        alarmSubscribed.value = true
        uni.setStorageSync('alarmSubscribed', 'true')
        uni.showToast({ title: '订阅成功', icon: 'success' })
      } else {
        alarmSubscribed.value = false
        uni.showToast({ title: '您拒绝了订阅', icon: 'none' })
      }
    } catch (error) {
      console.error('订阅失败:', error)
      alarmSubscribed.value = false
      uni.showToast({ title: '订阅失败', icon: 'none' })
    }
  } else {
    try {
      await miniappApi.cancelSubscription('ALARM')
      alarmSubscribed.value = false
      uni.setStorageSync('alarmSubscribed', 'false')
      uni.showToast({ title: '已取消订阅', icon: 'success' })
    } catch (error) {
      console.error('取消订阅失败:', error)
      uni.showToast({ title: '取消失败', icon: 'none' })
    }
  }
  // #endif

  // #ifdef H5
  alarmSubscribed.value = checked
  uni.setStorageSync('alarmSubscribed', checked ? 'true' : 'false')
  uni.showToast({ title: checked ? '已开启' : '已关闭', icon: 'success' })
  // #endif
}

const goToAbout = () => {
  uni.showModal({
    title: '关于我们',
    content: '边坡监测预警系统 v1.0.0\n\n用于交通边坡的位移、雨量等监测数据的实时采集与预警。',
    showCancel: false
  })
}

const handleCheckUpdate = async () => {
  uni.showLoading({ title: '检查中...' })

  try {
    const { needUpdate, versionInfo } = await checkUpdate()

    uni.hideLoading()

    if (needUpdate && versionInfo) {
      pendingUpdate.value = versionInfo
      // 使用模态对话框显示更新信息
      const showUpdateModal = () => {
        uni.showModal({
          title: '发现新版本',
          content: `最新版本：${versionInfo.versionName}\n\n更新说明：\n${versionInfo.updateLog}\n\n文件大小：${formatFileSize(versionInfo.fileSize)}`,
          showCancel: !versionInfo.forceUpdate,
          cancelText: '稍后提醒',
          confirmText: '立即更新',
          success: (res) => {
            if (res.confirm) {
              startDownload(versionInfo)
            } else {
              // 用户点击了取消或返回
              if (versionInfo.forceUpdate) {
                // 强制更新，重新显示对话框
                setTimeout(() => {
                  showUpdateModal()
                }, 100)
              }
            }
          },
          fail: () => {
            // 如果是强制更新，对话框显示失败时也要重试
            if (versionInfo.forceUpdate) {
              setTimeout(() => {
                showUpdateModal()
              }, 100)
            }
          }
        })
      }
      showUpdateModal()
    } else {
      uni.showToast({
        title: '已是最新版本',
        icon: 'success'
      })
    }
  } catch (error) {
    uni.hideLoading()
    console.error('检查更新失败:', error)
    uni.showToast({
      title: '检查更新失败',
      icon: 'none'
    })
  }
}

const startDownload = async (versionInfo: AppVersionInfo) => {
  downloading.value = true
  downloadProgress.value = 0

  try {
    console.log('[startDownload] 开始下载')
    await downloadAndInstallApk(versionInfo.downloadUrl, (progress) => {
      console.log('[startDownload] 进度更新:', progress)
      downloadProgress.value = progress
      // 当进度达到100%时，关闭进度框
      if (progress >= 100) {
        setTimeout(() => {
          downloading.value = false
        }, 500)
      }
    })
    console.log('[startDownload] 下载安装完成')
  } catch (error) {
    console.error('[startDownload] 失败:', error)
    uni.showToast({
      title: '下载失败',
      icon: 'none'
    })
    downloading.value = false
  }
}

const handleLogout = async () => {
  const res = await new Promise<UniApp.ShowModalRes>((resolve) => {
    uni.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: resolve as any
    })
  })
  if (!res.confirm) return

  try {
    await authApi.logout()
  } catch (error) {
    console.error('登出接口失败（忽略，继续清理本地）:', error)
  }

  uni.removeStorageSync('accessToken')
  uni.removeStorageSync('user')
  uni.removeStorageSync('alarmSubscribed')
  uni.reLaunch({ url: '/pages/login' })
}
</script>

<style lang="scss" scoped>
.page-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #eef1f8 0%, #e8ecf4 100%);
}

.header {
  position: relative;
  flex-shrink: 0;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding-top: 44rpx;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
}

.avatar-text {
  font-size: 48rpx;
  font-weight: bold;
  color: #ffffff;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.user-name {
  font-size: 36rpx;
  font-weight: 600;
  color: #ffffff;
}

.user-phone {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
}

.section {
  margin: 0 32rpx 24rpx;
}

.section-title {
  font-size: 26rpx;
  color: #6b7280;
  margin-bottom: 16rpx;
  display: block;
  text-transform: uppercase;
}

.menu-list {
  background: #ffffff;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 24rpx;
  border-bottom: 1rpx solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }

  &:active {
    background-color: #f7f8fc;
  }
}

.menu-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.menu-right {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.current-version {
  font-size: 26rpx;
  color: #9ca3af;
}

.menu-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36rpx;
  height: 36rpx;
}

.menu-text {
  font-size: 30rpx;
  color: #1a1a2e;

  &.danger {
    color: #f5222d;
  }
}

.menu-arrow {
  font-size: 28rpx;
  color: #9ca3af;
}

.menu-text {
  font-size: 30rpx;
  color: #1a1a2e;

  &.danger {
    color: #f5222d;
  }
}

.menu-arrow {
  font-size: 28rpx;
  color: #9ca3af;
}

.page-body {
  flex: 1;
  height: 0;
}

.logout-section {
  margin: 24rpx 32rpx;
}

.logout-btn {
  width: 100%;
  height: 88rpx;
  background: #ffffff;
  color: #f5222d;
  border: none;
  border-radius: 24rpx;
  font-size: 32rpx;
  font-weight: 600;
  box-shadow: 0 8rpx 32rpx rgba(245, 34, 45, 0.12);

  &:active {
    background: #fff1f0;
  }

  &::after {
    border: none;
  }
}

.bottom-spacer {
  height: 32rpx;
}

/* 下载进度浮层 */
.download-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.download-dialog {
  width: 560rpx;
  background: #ffffff;
  border-radius: 24rpx;
  padding: 48rpx 40rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.2);
}

.download-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a2e;
  text-align: center;
  margin-bottom: 32rpx;
}

.download-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.version-text {
  font-size: 28rpx;
  color: #666;
}

.progress-text {
  font-size: 32rpx;
  font-weight: 600;
  color: #3068e4;
}

.progress-container {
  margin-bottom: 20rpx;
}

.progress-bar-bg {
  width: 100%;
  height: 16rpx;
  background: #f0f0f0;
  border-radius: 8rpx;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
  border-radius: 8rpx;
  transition: width 0.3s ease;
}

.download-size {
  text-align: center;
  font-size: 24rpx;
  color: #999;
}
</style>
