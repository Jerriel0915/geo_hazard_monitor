// ============================================
// Panel 指令引擎 - 组合式函数
// ============================================

import { onUnmounted } from 'vue'
import { usePanelStore } from '@/stores/panel'
import { createWebSocketClient } from '@/api'
import type { PanelCommand, PanelCommandResult } from '@/types'

/**
 * Panel 指令引擎组合式函数
 */
export function usePanelCommand() {
  const panelStore = usePanelStore()

  // 创建 WebSocket 客户端实例
  const wsClient = createWebSocketClient()

  /**
   * 执行面板指令
   */
  async function executePanelCommand(
    panelId: string,
    command: PanelCommand
  ): Promise<PanelCommandResult> {
    console.log('[PanelCommand] Executing:', panelId, command)

    // 执行指令
    const result = await panelStore.executePanelCommand(panelId, command)

    // 发送执行结果到后端
    wsClient.sendCommandResult(
      `${panelId}-${command.action}`,
      result.success,
      result.result,
      result.error
    )

    return result
  }

  /**
   * 批量执行面板指令
   */
  async function executeBatch(
    commands: Array<{ panelId: string; command: PanelCommand }>,
    mode: 'sequential' | 'parallel' = 'parallel'
  ): Promise<PanelCommandResult[]> {
    console.log('[PanelCommand] Executing batch:', mode, commands)

    return await panelStore.executeBatch(commands, mode)
  }

  /**
   * 注册面板指令处理器
   */
  function registerHandler(
    panelType: string,
    action: string,
    handler: (panelId: string, params: any) => Promise<any>
  ) {
    panelStore.registerHandler(panelType, action, handler)
  }

  /**
   * 组件卸载时自动注销所有处理器
   */
  const registeredHandlers = new Set<string>()

  function registerAutoHandler(
    panelType: string,
    action: string,
    handler: (panelId: string, params: any) => Promise<any>
  ) {
    const key = `${panelType}:${action}`
    registerHandler(panelType, action, handler)
    registeredHandlers.add(key)

    // 组件卸载时自动注销
    onUnmounted(() => {
      panelStore.unregisterHandler(panelType, action)
      registeredHandlers.delete(key)
    })
  }

  return {
    executePanelCommand,
    executeBatch,
    registerHandler,
    registerAutoHandler
  }
}
