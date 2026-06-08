/**
 * 安全区域 Hook
 * 处理状态栏高度、底部安全区域等
 */
console.log('[useSafeArea.ts] Module started')
import { ref, onMounted } from 'vue'

export function useSafeArea() {
  console.log('[useSafeArea.ts] useSafeArea function called')
  const statusBarHeight = ref(0)
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
      statusBarHeight.value = systemInfo.statusBarHeight || 0
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
