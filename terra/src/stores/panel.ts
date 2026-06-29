// ============================================
// Panel Store - 面板管理和指令路由
// ============================================

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { PanelConfig, PanelInstance, PanelCommand, PanelCommandResult, PanelRenderState } from '@/types'

export const usePanelStore = defineStore('panel', () => {
  // 面板实例映射
  const panels = ref<Map<string, PanelInstance>>(new Map())

  // 面板指令处理器注册表
  const commandHandlers = ref<Map<string, Map<string, Function>>>(new Map())

  // 当前活跃的面板 ID 列表
  const activePanelIds = ref<string[]>([])

  /**
   * 获取所有活跃面板
   */
  const activePanels = computed(() => {
    return activePanelIds.value
      .map(id => panels.value.get(id))
      .filter((panel): panel is PanelInstance => panel !== undefined)
  })

  /**
   * 获取面板渲染状态
   */
  const renderStates = ref<Map<string, PanelRenderState>>(new Map())

  /**
   * 获取面板渲染状态
   */
  function getRenderState(panelId: string): PanelRenderState {
    if (!renderStates.value.has(panelId)) {
      renderStates.value.set(panelId, {
        visible: true,
        loading: false,
        error: null
      })
    }
    return renderStates.value.get(panelId)!
  }

  /**
   * 更新面板渲染状态
   */
  function updateRenderState(panelId: string, updates: Partial<PanelRenderState>) {
    const currentState = getRenderState(panelId)
    renderStates.value.set(panelId, { ...currentState, ...updates })
  }

  /**
   * 注册面板
   */
  function registerPanel(instance: PanelInstance) {
    panels.value.set(instance.id, instance)
    if (!activePanelIds.value.includes(instance.id)) {
      activePanelIds.value.push(instance.id)
    }
    console.log('[PanelStore] Registered panel:', instance.id)
  }

  /**
   * 注销面板
   */
  function unregisterPanel(panelId: string) {
    panels.value.delete(panelId)
    const index = activePanelIds.value.indexOf(panelId)
    if (index > -1) {
      activePanelIds.value.splice(index, 1)
    }
    renderStates.value.delete(panelId)
    console.log('[PanelStore] Unregistered panel:', panelId)
  }

  /**
   * 获取面板实例
   */
  function getPanel(panelId: string): PanelInstance | undefined {
    return panels.value.get(panelId)
  }

  /**
   * 显示面板
   */
  function showPanel(panelId: string) {
    const panel = panels.value.get(panelId)
    if (panel) {
      panel.config.visible = true
      updateRenderState(panelId, { visible: true })
    }
  }

  /**
   * 隐藏面板
   */
  function hidePanel(panelId: string) {
    const panel = panels.value.get(panelId)
    if (panel) {
      panel.config.visible = false
      updateRenderState(panelId, { visible: false })
    }
  }

  /**
   * 聚焦面板（TerraMens 关注点）
   */
  function focusPanel(panelId: string) {
    // 将面板移到最前
    const panel = panels.value.get(panelId)
    if (panel) {
      panel.config.zIndex = Math.max(...Array.from(panels.value.values()).map(p => p.config.zIndex)) + 1
    }
  }

  /**
   * 注册面板指令处理器
   */
  function registerHandler(
    panelType: string,
    action: string,
    handler: (panelId: string, params: any) => Promise<any>
  ) {
    if (!commandHandlers.value.has(panelType)) {
      commandHandlers.value.set(panelType, new Map())
    }
    commandHandlers.value.get(panelType)!.set(action, handler)
    console.log(`[PanelStore] Registered handler: ${panelType}:${action}`)
  }

  /**
   * 注销面板指令处理器
   */
  function unregisterHandler(panelType: string, action: string) {
    const typeHandlers = commandHandlers.value.get(panelType)
    if (typeHandlers) {
      typeHandlers.delete(action)
      if (typeHandlers.size === 0) {
        commandHandlers.value.delete(panelType)
      }
    }
  }

  /**
   * 执行面板指令
   */
  async function executePanelCommand(
    panelId: string,
    command: PanelCommand
  ): Promise<PanelCommandResult> {
    const panel = panels.value.get(panelId)
    if (!panel) {
      return {
        panelId,
        command,
        success: false,
        error: `Panel ${panelId} not found`,
        timestamp: Date.now()
      }
    }

    const typeHandlers = commandHandlers.value.get(panel.type)
    if (!typeHandlers) {
      return {
        panelId,
        command,
        success: false,
        error: `No handlers registered for panel type: ${panel.type}`,
        timestamp: Date.now()
      }
    }

    const handler = typeHandlers.get(command.action)
    if (!handler) {
      return {
        panelId,
        command,
        success: false,
        error: `Unknown command: ${command.action}`,
        timestamp: Date.now()
      }
    }

    try {
      const result = await handler(panelId, command.params || {})
      return {
        panelId,
        command,
        success: true,
        result,
        timestamp: Date.now()
      }
    } catch (error) {
      return {
        panelId,
        command,
        success: false,
        error: error instanceof Error ? error.message : String(error),
        timestamp: Date.now()
      }
    }
  }

  /**
   * 批量执行面板指令
   */
  async function executeBatch(
    commands: Array<{ panelId: string; command: PanelCommand }>,
    mode: 'sequential' | 'parallel' = 'parallel'
  ): Promise<PanelCommandResult[]> {
    if (mode === 'sequential') {
      const results: PanelCommandResult[] = []
      for (const { panelId, command } of commands) {
        const result = await executePanelCommand(panelId, command)
        results.push(result)
      }
      return results
    } else {
      // 并行执行
      return await Promise.all(
        commands.map(({ panelId, command }) => executePanelCommand(panelId, command))
      )
    }
  }

  /**
   * 更新面板数据
   */
  function updatePanelData(panelId: string, data: any) {
    const panel = panels.value.get(panelId)
    if (panel) {
      panel.config.data = data
    }
  }

  /**
   * 清空所有面板
   */
  function clearAllPanels() {
    panels.value.clear()
    activePanelIds.value = []
    renderStates.value.clear()
  }

  return {
    // 状态
    activePanels,
    renderStates,

    // 方法
    getRenderState,
    updateRenderState,
    registerPanel,
    unregisterPanel,
    getPanel,
    showPanel,
    hidePanel,
    focusPanel,
    registerHandler,
    unregisterHandler,
    executePanelCommand,
    executeBatch,
    updatePanelData,
    clearAllPanels
  }
})
