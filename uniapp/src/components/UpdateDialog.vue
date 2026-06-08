<template>
  <uni-popup ref="popup" type="dialog">
    <uni-popup-dialog
      v-if="versionInfo"
      :type="forceUpdate ? 'warn' : 'info'"
      :title="title"
      :content="content"
      :confirm-text="confirmText"
      :show-cancel="!forceUpdate"
      @confirm="handleConfirm"
      @close="handleClose"
    >
      <template #default>
        <view class="update-dialog">
          <view class="version-info">
            <text class="version-label">新版本：</text>
            <text class="version-value">{{ versionInfo.versionName }}</text>
          </view>
          <view class="update-log">
            <text class="log-label">更新说明：</text>
            <text class="log-content">{{ versionInfo.updateLog }}</text>
          </view>
          <view v-if="downloading" class="progress-bar">
            <progress :percent="downloadProgress" stroke-width="8" activeColor="#3068e4" />
            <text class="progress-text">{{ downloadProgress }}%</text>
          </view>
        </view>
      </template>
    </uni-popup-dialog>
  </uni-popup>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { AppVersionInfo } from '@/utils/appVersion'
import { downloadAndInstallApk } from '@/utils/appVersion'

interface Props {
  versionInfo: AppVersionInfo
}

const props = defineProps<Props>()
const emit = defineEmits<{
  close: []
}>()

const popup = ref()
const downloading = ref(false)
const downloadProgress = ref(0)

const forceUpdate = computed(() => props.versionInfo.forceUpdate)
const title = computed(() => '发现新版本')
const content = computed(() => '') // 使用自定义内容模板
const confirmText = computed(() => downloading.value ? '下载中...' : '立即更新')

const show = () => {
  console.log('[UpdateDialog] show() 被调用')
  console.log('[UpdateDialog] popup ref:', popup.value)
  console.log('[UpdateDialog] versionInfo:', props.versionInfo)
  popup.value?.open()
}

const hide = () => {
  popup.value?.close()
}

const handleConfirm = async () => {
  if (downloading.value) {
    return
  }

  downloading.value = true
  downloadProgress.value = 0

  try {
    await downloadAndInstallApk(props.versionInfo.downloadUrl, (progress) => {
      downloadProgress.value = progress
    })
    // 安装成功后不需要关闭弹窗，系统会自动安装
  } catch (error) {
    uni.showToast({
      title: '下载失败',
      icon: 'none'
    })
    downloading.value = false
  }
}

const handleClose = () => {
  if (!forceUpdate.value) {
    hide()
    emit('close')
  }
}

defineExpose({
  show,
  hide
})
</script>

<style scoped>
.update-dialog {
  padding: 20rpx;
}

.version-info {
  margin-bottom: 20rpx;
}

.version-label {
  font-weight: bold;
}

.version-value {
  color: #3068e4;
}

.update-log {
  margin-bottom: 20rpx;
}

.log-label {
  font-weight: bold;
  display: block;
  margin-bottom: 10rpx;
}

.log-content {
  font-size: 28rpx;
  line-height: 1.6;
  color: #666;
}

.progress-bar {
  margin-top: 20rpx;
}

.progress-text {
  display: block;
  text-align: center;
  margin-top: 10rpx;
  font-size: 28rpx;
  color: #999;
}
</style>
