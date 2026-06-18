<!-- src/pages/alarm-handle.vue -->
<template>
  <view class="page-container">
    <!-- 头部 -->
    <view class="header">
      <view class="header-bg" :style="{ height: `calc(${statusBarHeight}px + 130rpx)` }"></view>
      <view class="header-content" :style="{ marginTop: `${statusBarHeight}px` }">
        <view class="header-nav">
          <view class="back-btn" @click="goBack">←</view>
          <text class="header-title">处置反馈</text>
        </view>
      </view>
    </view>

    <!-- 内容 -->
    <scroll-view class="content-scroll" scroll-y>
      <view class="form-section">
        <!-- 简洁告警概要 -->
        <view class="alarm-summary" v-if="alarmData">
          <view class="summary-left">
            <text class="summary-name">{{ alarmData.hazardPointName || '-' }}</text>
            <text class="summary-device" v-if="alarmData.deviceName">{{ alarmData.deviceName }}</text>
          </view>
          <view class="summary-level" :style="{ background: getAlarmLevelColor(alarmData.alarmLevel) }">
            {{ getAlarmLevelText(alarmData.alarmLevel) }}
          </view>
        </view>

        <!-- 反馈内容 -->
        <view class="form-block">
          <text class="form-label">反馈内容</text>
          <textarea
            v-model="feedback.content"
            class="form-textarea"
            placeholder="请输入处置反馈说明（非必填）"
            placeholder-class="placeholder"
            :maxlength="500"
            auto-height
          />
          <view class="char-count">{{ feedback.content.length }}/500</view>
        </view>
      </view>

      <view class="bottom-spacer" />
    </scroll-view>

    <!-- 底部操作栏 -->
    <view class="action-bar" :style="{ paddingBottom: `${safeAreaBottom + 16}rpx` }">
      <view class="btn-cancel" @click="goBack">取消</view>
      <view class="btn-submit" :class="{ disabled: submitting }" @click="handleSubmit">
        {{ submitting ? '提交中...' : '提交' }}
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useSafeArea } from '@/composables/useSafeArea'
import {
  alarmApi,
  getAlarmLevelColor,
  getAlarmLevelText,
} from '@/utils/alarm'
import type { AlarmRecordItem } from '@/utils/alarm'

const { statusBarHeight, safeAreaBottom } = useSafeArea()

const alarmId = ref<number>(0)
const alarmData = ref<AlarmRecordItem | null>(null)
const submitting = ref(false)

const feedback = reactive({
  content: '',
})

onLoad(async (options) => {
  if (!options?.alarmId) {
    uni.showToast({ title: '缺少告警 ID', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 1200)
    return
  }
  alarmId.value = Number(options.alarmId)
  await loadAlarm()
})

const loadAlarm = async () => {
  if (!alarmId.value) return
  try {
    alarmData.value = await alarmApi.getAlarmRecordDetail(alarmId.value)
  } catch (e: any) {
    uni.showToast({ title: e?.message || '加载告警失败', icon: 'none' })
  }
}

const handleSubmit = async () => {
  if (submitting.value) return
  if (!alarmId.value) {
    uni.showToast({ title: '缺少告警 ID', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await alarmApi.disposeAlarm(alarmId.value, {
      status: 2,
      description: feedback.content,
      remarks: feedback.content,
    })
    uni.showToast({ title: '反馈提交成功', icon: 'success' })
    // 返回上一页（alarm-detail.vue 的 onShow 会自动重新加载）
    setTimeout(() => {
      uni.navigateBack()
    }, 800)
  } catch (e: any) {
    uni.showToast({ title: e?.message || '提交失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  uni.navigateBack()
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

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
  border-radius: 0 0 15rpx 15rpx;
  overflow: hidden;
}

.header-content {
  position: relative;
  z-index: 1;
  padding: 0 32rpx 24rpx;
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.back-btn {
  width: 64rpx;
  height: 64rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  color: #ffffff;
}

.header-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #ffffff;
}

.content-scroll {
  flex: 1;
  height: 0;
}

.form-section {
  padding: 24rpx 32rpx;
}

/* 告警概要 */
.alarm-summary {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16rpx;
}

.summary-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  min-width: 0;
}

.summary-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #1a1a2e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-device {
  font-size: 22rpx;
  color: #6b7280;
}

.summary-level {
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  color: #ffffff;
  flex-shrink: 0;
}

/* 反馈表单 */
.form-block {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
}

.form-label {
  font-size: 28rpx;
  font-weight: 500;
  color: #1a1a2e;
  margin-bottom: 16rpx;
  display: block;
}

.form-textarea {
  width: 100%;
  min-height: 240rpx;
  font-size: 26rpx;
  color: #1a1a2e;
  line-height: 1.6;
  padding: 16rpx;
  box-sizing: border-box;
  background: #f7f8fc;
  border-radius: 12rpx;
  border: 1rpx solid #e5e7eb;
}

.placeholder {
  color: #9ca3af;
}

.char-count {
  text-align: right;
  font-size: 22rpx;
  color: #9ca3af;
  margin-top: 8rpx;
}

.bottom-spacer {
  height: 60rpx;
}

/* 底部操作栏 */
.action-bar {
  display: flex;
  gap: 16rpx;
  padding: 16rpx 32rpx;
  background: #ffffff;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
}

.btn-cancel {
  flex: 1;
  padding: 24rpx 0;
  border-radius: 16rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 600;
  background: #f5f5f5;
  color: #6b7280;

  &:active { opacity: 0.8; }
}

.btn-submit {
  flex: 1.5;
  padding: 24rpx 0;
  border-radius: 16rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 600;
  color: #ffffff;
  background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);

  &.disabled {
    opacity: 0.6;
  }

  &:active { opacity: 0.9; }
}
</style>
