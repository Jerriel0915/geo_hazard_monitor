import type { ToolResult } from './types'

type ToolHandler = (params: Record<string, unknown>) => Promise<ToolResult>

export class TerraToolExecutor {
  private handlers = new Map<string, ToolHandler>()

  register(toolName: string, handler: ToolHandler) {
    this.handlers.set(toolName, handler)
  }

  unregister(toolName: string) {
    this.handlers.delete(toolName)
  }

  async execute(toolName: string, params: Record<string, unknown>): Promise<ToolResult> {
    const handler = this.handlers.get(toolName)
    if (!handler) {
      return { success: false, error: `未注册的前端工具: ${toolName}` }
    }
    try {
      return await handler(params)
    } catch (e: any) {
      return { success: false, error: e.message || '工具执行异常' }
    }
  }

  has(toolName: string): boolean {
    return this.handlers.has(toolName)
  }
}
