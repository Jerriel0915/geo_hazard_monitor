<template>
  <view v-if="visible" class="confirm-dialog-overlay" @click="handleOverlayClick">
    <view class="confirm-dialog" @click.stop>
      <view class="confirm-dialog-header">
        <text class="confirm-dialog-title">{{ title }}</text>
      </view>
      <view class="confirm-dialog-body">
        <text class="confirm-dialog-content">{{ content }}</text>
      </view>
      <view class="confirm-dialog-footer">
        <view class="confirm-dialog-btn cancel" @click="handleCancel">
          <text>取消</text>
        </view>
        <view class="confirm-dialog-btn confirm" @click="handleConfirm">
          <text>确定</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
defineProps({
  visible: Boolean,
  title: {
    type: String,
    default: '提示'
  },
  content: String,
  showCancel: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['confirm', 'cancel', 'close'])

const handleConfirm = () => {
  emit('confirm')
}

const handleCancel = () => {
  emit('cancel')
  emit('close')
}

const handleOverlayClick = () => {
  emit('cancel')
  emit('close')
}
</script>

<style lang="scss" scoped>
.confirm-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.confirm-dialog {
  width: 600rpx;
  background: #ffffff;
  border-radius: 24rpx;
  overflow: hidden;
}

.confirm-dialog-header {
  padding: 32rpx;
  text-align: center;
  border-bottom: 1rpx solid #f0f0f0;
}

.confirm-dialog-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a2e;
}

.confirm-dialog-body {
  padding: 32rpx;
}

.confirm-dialog-content {
  font-size: 28rpx;
  color: #6b7280;
  line-height: 44rpx;
  white-space: pre-line;
  text-align: left;
}

.confirm-dialog-footer {
  display: flex;
  border-top: 1rpx solid #f0f0f0;
}

.confirm-dialog-btn {
  flex: 1;
  padding: 28rpx;
  text-align: center;
  font-size: 30rpx;

  &.cancel {
    color: #6b7280;
    border-right: 1rpx solid #f0f0f0;
  }

  &.confirm {
    color: #3068e4;
    font-weight: 500;
  }
}
</style>
