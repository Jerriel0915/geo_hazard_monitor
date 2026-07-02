<template>
  <view class="page-container">
    <!-- 渐变头部 -->
    <PageHeader show-back>
      <view class="scan-header-info">
        <text class="scan-header-title">扫码绑定</text>
        <text class="scan-header-subtitle">扫描集装箱二维码即可绑定</text>
      </view>
    </PageHeader>

    <!-- 扫描区域 -->
    <scroll-view class="page-body" scroll-y>
      <view class="content-area">
        <view class="scan-area">
          <view class="scan-frame">
            <view class="corner top-left"></view>
            <view class="corner top-right"></view>
            <view class="corner bottom-left"></view>
            <view class="corner bottom-right"></view>
            <zui-svg-icon icon="camera" width="64rpx" color="rgba(102, 126, 234, 0.8)" />
            <text class="scan-text">点击下方按钮开始扫码</text>
          </view>
        </view>

        <view class="scan-tips">
          <text>扫描集装箱上的二维码，即可绑定该集装箱到您的账户</text>
        </view>

        <button class="scan-btn" @click="startScan">
          <zui-svg-icon icon="search" width="36rpx" color="#ffffff" />
          <text>开始扫码</text>
        </button>
      </view>
    </scroll-view>

    <!-- 确认绑定弹框 -->
    <ConfirmDialog
      :visible="confirmVisible"
      title="确认绑定"
      :content="confirmContent"
      @confirm="handleConfirmBind"
      @cancel="handleCancelBind"
    />
  </view>
</template>

<script setup>
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import PageHeader from '@/components/PageHeader.vue'
import containerApi from '@/utils/container'
import { ref } from 'vue'

const containerNo = ref('')
const binding = ref(false)
const confirmVisible = ref(false)
const confirmContent = ref('')
const pendingContainer = ref(null)

/**
 * 开始扫码
 */
const startScan = () => {
  uni.scanCode({
    scanType: ['qrCode'],
    success: (res) => {
      console.log('扫码结果:', res)

      // 尝试解析二维码内容，提取集装箱编号
      // 支持格式: CONTAINER:C-1001 或 container:C-1001 或直接是编号
      let code = res.result
      if (code.toUpperCase().startsWith('CONTAINER:')) {
        code = code.substring(10) // 去掉 'CONTAINER:' 前缀
      }
      containerNo.value = code.trim()

      // 查询集装箱信息
      queryContainer()
    },
    fail: (err) => {
      console.error('扫码失败:', err)
      uni.showToast({ title: '扫码失败', icon: 'none' })
    }
  })
}

/**
 * 查询集装箱信息并显示确认弹框
 */
const queryContainer = async () => {
  if (!containerNo.value) {
    return
  }

  uni.showLoading({ title: '查询中...' })

  try {
    // 根据编号查询集装箱
    const container = await containerApi.getByNo(containerNo.value)

    uni.hideLoading()

    if (!container) {
      uni.showToast({ title: '未找到该集装箱', icon: 'none' })
      return
    }

    // 显示确认绑定弹框
    showBindConfirmDialog(container)
  } catch (error) {
    uni.hideLoading()
    console.error('查询集装箱失败:', error)
    uni.showToast({ title: '查询失败，请重试', icon: 'none' })
  }
}

/**
 * 显示确认绑定弹框
 */
const showBindConfirmDialog = (container) => {
  const containerName = container.containerName || '未命名'
  const containerNoText = container.containerNo || '未知编号'

  confirmContent.value = `集装箱名称：${containerName}\n集装箱编号：${containerNoText}\n\n是否确认绑定该集装箱？`
  pendingContainer.value = container
  confirmVisible.value = true
}

/**
 * 确认绑定
 */
const handleConfirmBind = () => {
  confirmVisible.value = false
  if (pendingContainer.value) {
    doBind(pendingContainer.value)
  }
}

/**
 * 取消绑定
 */
const handleCancelBind = () => {
  confirmVisible.value = false
  pendingContainer.value = null
}

/**
 * 执行绑定操作
 */
const doBind = async (container) => {
  if (binding.value) return
  binding.value = true

  try {
    // 调用绑定API
    await containerApi.bind(container.id)

    uni.showToast({
      title: '绑定成功',
      icon: 'success',
      duration: 2000
    })

    setTimeout(() => {
      // 返回首页刷新列表
      uni.switchTab({ url: '/pages/index' })
    }, 2000)
  } catch (error) {
    console.error('绑定失败:', error)
    uni.showToast({ title: error.message || '绑定失败，请重试', icon: 'none' })
  } finally {
    binding.value = false
  }
}
</script>

<style lang="scss" scoped>
.page-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #eef1f8 0%, #e8ecf4 100%);
}

.page-body {
  flex: 1;
  height: 0;
}

.scan-header-info {
  padding-top: 8rpx;
}

.scan-header-title {
  font-size: 40rpx;
  font-weight: bold;
  color: #ffffff;
  margin-bottom: 8rpx;
  display: block;
}

.scan-header-subtitle {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
}

.content-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60rpx 32rpx;
}

.scan-area {
  width: 100%;
  max-width: 500rpx;
  height: 400rpx;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.2);
}

.scan-frame {
  position: relative;
  width: 280rpx;
  height: 280rpx;
  border: 2rpx dashed rgba(102, 126, 234, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.corner {
  position: absolute;
  width: 40rpx;
  height: 40rpx;
  border: 6rpx solid #3068e4;
}

.top-left {
  top: -3rpx;
  left: -3rpx;
  border-bottom: none;
  border-right: none;
  border-radius: 12rpx 0 0 0;
}

.top-right {
  top: -3rpx;
  right: -3rpx;
  border-bottom: none;
  border-left: none;
  border-radius: 0 12rpx 0 0;
}

.bottom-left {
  bottom: -3rpx;
  left: -3rpx;
  border-top: none;
  border-right: none;
  border-radius: 0 0 0 12rpx;
}

.bottom-right {
  bottom: -3rpx;
  right: -3rpx;
  border-top: none;
  border-left: none;
  border-radius: 0 0 12rpx 0;
}

.scan-text {
  font-size: 24rpx;
  color: rgba(102, 126, 234, 0.8);
  margin-top: 16rpx;
}

.scan-tips {
  text-align: center;
  font-size: 26rpx;
  color: #6b7280;
  line-height: 40rpx;
  padding: 32rpx 40rpx;
}

.scan-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  width: 70%;
  padding: 28rpx;
  background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
  color: #ffffff;
  border: none;
  border-radius: 48rpx;
  font-size: 32rpx;
  font-weight: 500;
  box-shadow: 0 8rpx 24rpx rgba(102, 126, 234, 0.4);

  &:active {
    transform: scale(0.98);
  }
}
</style>
