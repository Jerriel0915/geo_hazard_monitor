/**
 * APP版本管理工具
 */

import api from './api'

interface AppVersionInfo {
  id: number
  versionName: string
  versionCode: number
  fileSize: number
  fileName: string
  updateLog: string
  forceUpdate: boolean
  downloadUrl: string
}

/**
 * 获取APP当前版本号
 */
export function getCurrentVersionCode(): number {
  // H5环境下plus不存在，返回默认版本号99
  // @ts-ignore
  return typeof plus !== 'undefined' ? plus.runtime.versionCode : 99
}

/**
 * 获取APP当前版本名称
 */
export function getCurrentVersionName(): string {
  // H5环境下plus不存在，返回默认版本名称0.9.9
  // @ts-ignore
  return typeof plus !== 'undefined' ? plus.runtime.version : '0.9.9'
}

/**
 * 获取最新版本信息
 */
export function getLatestVersion(): Promise<AppVersionInfo | null> {
  return api.get('/app-versions/latest')
}

/**
 * 检查是否需要更新
 */
export async function checkUpdate(): Promise<{ needUpdate: boolean; versionInfo: AppVersionInfo | null }> {
  try {
    const currentVersion = getCurrentVersionCode()
    console.log('[checkUpdate] 当前版本:', currentVersion)

    const latestVersion = await getLatestVersion()
    console.log('[checkUpdate] 最新版本:', latestVersion)

    if (!latestVersion) {
      return { needUpdate: false, versionInfo: null }
    }

    const needUpdate = latestVersion.versionCode > currentVersion
    console.log('[checkUpdate] 需要更新:', needUpdate)

    return { needUpdate, versionInfo: latestVersion }
  } catch (error) {
    console.error('检查更新失败:', error)
    throw error // 重新抛出错误，让调用方处理
  }
}

/**
 * 下载并安装APK
 */
export function downloadAndInstallApk(downloadUrl: string, onProgress?: (progress: number) => void): Promise<void> {
  return new Promise((resolve, reject) => {
    // H5环境不支持APK安装
    // @ts-ignore
    if (typeof plus === 'undefined') {
      uni.showModal({
        title: '提示',
        content: 'H5环境不支持自动更新，请在App环境中使用此功能',
        showCancel: false
      })
      reject(new Error('H5环境不支持APK安装'))
      return
    }

    // downloadUrl 格式为 /api/app-versions/1/download
    // 需要去掉 /api 前缀，因为 BASE_URL 已经包含了 /api
    const cleanUrl = downloadUrl.replace(/^\/api/, '')
    const fullUrl = api.BASE_URL + cleanUrl

    console.log('[downloadAndInstallApk] 下载URL:', fullUrl)

    // 不使用 uni.showLoading，因为会遮挡进度条
    const downloadTask = uni.downloadFile({
      url: fullUrl,
      success: (res: any) => {
        console.log('[downloadAndInstallApk] 下载完成:', res)
        console.log('[downloadAndInstallApk] statusCode:', res.statusCode)
        console.log('[downloadAndInstallApk] tempFilePath:', res.tempFilePath)

        if (res.statusCode === 200) {
          // 下载完成，通知调用方关闭进度框
          if (onProgress) {
            onProgress(100)
          }

          // 延迟一帧，确保界面更新
          setTimeout(() => {
            // 安装APK
            console.log('[downloadAndInstallApk] 开始安装APK:', res.tempFilePath)
            // @ts-ignore
            plus.runtime.install(res.tempFilePath, {
              force: false
            }, () => {
              console.log('[downloadAndInstallApk] 安装成功，系统将自动替换应用')
              resolve()
            }, (error: any) => {
              console.error('[downloadAndInstallApk] 安装失败或用户取消:', error)
              // 用户取消安装不算失败，直接resolve
              resolve()
            })
          }, 100)
        } else {
          console.error('[downloadAndInstallApk] 下载失败，状态码:', res.statusCode)
          const errorMsg = res.statusCode === 401 ? '下载失败：未授权，请重新登录' :
                          res.statusCode === 403 ? '下载失败：无权限' :
                          res.statusCode === 404 ? '下载失败：文件不存在' :
                          `下载失败：服务器错误(${res.statusCode})`
          uni.showModal({
            title: '下载失败',
            content: errorMsg,
            showCancel: false
          })
          reject(new Error(errorMsg))
        }
      },
      fail: (error: any) => {
        console.error('[downloadAndInstallApk] 下载失败:', error)
        uni.showModal({
          title: '下载失败',
          content: error.errMsg || '网络请求失败',
          showCancel: false
        })
        reject(error)
      }
    })

    if (onProgress) {
      downloadTask.onProgressUpdate((res: any) => {
        console.log('[downloadAndInstallApk] 进度更新:', {
          bytesWritten: res.bytesWritten,
          totalBytesExpectedToWrite: res.totalBytesExpectedToWrite,
          progress: res.progress
        })

        // 优先使用 res.progress，如果不存在则计算
        let progress = res.progress
        if (progress === undefined || progress === null) {
          if (res.totalBytesExpectedToWrite && res.totalBytesExpectedToWrite > 0) {
            progress = Math.round((res.bytesWritten / res.totalBytesExpectedToWrite) * 100)
          } else {
            progress = 0
          }
        }

        console.log('[downloadAndInstallApk] 下载进度:', progress)
        onProgress(progress)
      })
    }
  })
}
