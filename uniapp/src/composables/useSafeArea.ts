/**
 * 安全区域 Hook
 * 处理状态栏高度、底部安全区域等
 */
console.log('[useSafeArea.ts] Module started')
import { onMounted, ref } from 'vue'

// #ifdef H5
// H5 环境无物理状态栏，设置默认安全高度保持 UI 与小程序一致
const H5_STATUS_BAR_HEIGHT = 0
// #endif

export function useSafeArea() {
  console.log('[useSafeArea.ts] useSafeArea function called')

  // #ifdef H5
  const statusBarHeight = ref(H5_STATUS_BAR_HEIGHT)
  // #endif
  // #ifndef H5
  const statusBarHeight = ref(0)
  // #endif

  const safeAreaBottom = ref(0)
  const safeArea = ref({
    top: 0,
    bottom: 0,
    left: 0,
    right: 0
  })

  onMounted(() => {
    console.log('[useSafeArea.ts] onMounted callback')
    try {
      const systemInfo = uni.getSystemInfoSync()
      console.log('[useSafeArea.ts] systemInfo:', systemInfo)

      // #ifdef H5
      // H5: 优先使用系统返回值，为 0 时保持默认
      statusBarHeight.value = systemInfo.statusBarHeight || H5_STATUS_BAR_HEIGHT
      // #endif
      // #ifndef H5
      statusBarHeight.value = systemInfo.statusBarHeight || 0
      // #endif

      safeAreaBottom.value = systemInfo.safeAreaInsets?.bottom || 0

      safeArea.value = {
        top: systemInfo.safeAreaInsets?.top || 0,
        bottom: systemInfo.safeAreaInsets?.bottom || 0,
        left: systemInfo.safeAreaInsets?.left || 0,
        right: systemInfo.safeAreaInsets?.right || 0
      }
      console.log('[useSafeArea.ts] Values set - statusBarHeight:', statusBarHeight.value)
    } catch (error) {
      console.error('[useSafeArea.ts] Error in onMounted:', error)
    }
  })

  console.log('[useSafeArea.ts] Returning from useSafeArea')
  return {
    statusBarHeight,
    safeAreaBottom,
    safeArea
  }
}
