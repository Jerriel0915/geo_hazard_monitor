// ============================================
// Panel 指令注册器 - 全局单例
// ============================================

import { ref, computed } from 'vue'
import type { PanelCommandResult } from '@/types'

/**
 * 指令处理器接口
 */
export interface CommandHandler {
  action: string
  handler: (params: any) => Promise<any>
  schema?: object
}

/**
 * 指令注册器类
 */
class CommandRegistryClass {
  private handlers = ref<Map<string, CommandHandler>>(new Map())

  /**
   * 注册指令处理器
   */
  register(action: string, handler: CommandHandler['handler'], schema?: object) {
    this.handlers.value.set(action, { action, handler, schema })
    console.log('[CommandRegistry] Registered:', action)
  }

  /**
   * 注销指令处理器
   */
  unregister(action: string) {
    this.handlers.value.delete(action)
    console.log('[CommandRegistry] Unregistered:', action)
  }

  /**
   * 执行指令
   */
  async execute(action: string, params: any): Promise<any> {
    const handler = this.handlers.value.get(action)
    if (!handler) {
      throw new Error(`Unknown command: ${action}`)
    }
    return await handler.handler(params)
  }

  /**
   * 检查指令是否支持
   */
  has(action: string): boolean {
    return this.handlers.value.has(action)
  }

  /**
   * 获取所有支持的指令
   */
  getSupportedActions(): string[] {
    return Array.from(this.handlers.value.keys())
  }

  /**
   * 获取支持的指令（按类别分组）
   */
  getActionsByCategory(): Record<string, string[]> {
    const grouped: Record<string, string[]> = {}
    for (const action of this.handlers.value.keys()) {
      const [category] = action.split(':')
      if (!grouped[category]) {
        grouped[category] = []
      }
      grouped[category].push(action)
    }
    return grouped
  }
}

// 导出单例
export const CommandRegistry = new CommandRegistryClass()

/**
 * 组合式函数
 */
export function useCommandRegistry() {
  return {
    handlers: CommandRegistry.handlers,
    supportedActions: computed(() => CommandRegistry.getSupportedActions()),
    actionsByCategory: computed(() => CommandRegistry.getActionsByCategory()),
    register: CommandRegistry.register.bind(CommandRegistry),
    unregister: CommandRegistry.unregister.bind(CommandRegistry),
    execute: CommandRegistry.execute.bind(CommandRegistry),
    has: CommandRegistry.has.bind(CommandRegistry)
  }
}
