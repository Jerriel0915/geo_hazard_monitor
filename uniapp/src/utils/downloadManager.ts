/**
 * 全局下载进度管理器
 * 用于在所有页面中统一显示下载进度
 */

interface DownloadInfo {
  versionName: string
  fileSize: number
  downloadUrl: string
}

class DownloadManager {
  private downloading = false
  private downloadProgress = 0
  private downloadInfo: DownloadInfo | null = null
  private overlayElement: any = null

  /**
   * 开始下载并显示进度浮层
   */
  startDownload(versionInfo: DownloadInfo) {
    this.downloadInfo = versionInfo
    this.downloading = true
    this.downloadProgress = 0

    // 创建并显示进度浮层
    this.showOverlay()

    // 通知所有页面显示下载进度
    uni.$emit('showDownloadProgress', versionInfo)
  }

  /**
   * 更新下载进度
   */
  updateProgress(progress: number) {
    this.downloadProgress = progress
    uni.$emit('updateDownloadProgress', progress)

    if (progress >= 100) {
      setTimeout(() => {
        this.hideDownload()
      }, 500)
    }
  }

  /**
   * 隐藏下载进度
   */
  hideDownload() {
    this.downloading = false
    this.downloadProgress = 0
    this.downloadInfo = null

    // 隐藏进度浮层
    this.hideOverlay()

    // 通知所有页面隐藏下载进度
    uni.$emit('hideDownloadProgress')
  }

  /**
   * 获取当前下载状态
   */
  isDownloading() {
    return this.downloading
  }

  /**
   * 获取当前下载进度
   */
  getProgress() {
    return this.downloadProgress
  }

  /**
   * 获取当前下载信息
   */
  getDownloadInfo() {
    return this.downloadInfo
  }

  /**
   * 显示进度浮层（动态创建DOM）
   */
  private showOverlay() {
    // 在实际页面中处理，这里只是发送事件
  }

  /**
   * 隐藏进度浮层
   */
  private hideOverlay() {
    // 在实际页面中处理，这里只是发送事件
  }
}

// 导出单例
export default new DownloadManager()
