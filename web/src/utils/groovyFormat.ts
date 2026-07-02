/**
 * 简易 Groovy 代码格式化 — 基于大括号缩进
 * 适用于计算属性/数据解析等短脚本场景
 */
export function formatGroovyCode(code: string): string {
  if (!code || !code.trim()) return code

  const lines = code.split('\n')
  const INDENT = 2
  let indent = 0
  const result: string[] = []

  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed) {
      result.push('')
      continue
    }

    // 闭括号先减少缩进
    const startsWithClose = trimmed.startsWith('}') || trimmed.startsWith(']') || trimmed.startsWith(')')
    if (startsWithClose) {
      indent = Math.max(0, indent - 1)
    }

    // 分号结尾的行保持当前缩进
    result.push(' '.repeat(indent * INDENT) + trimmed)

    // 计算该行的括号净变化
    const opens = (trimmed.match(/[\{([]/g) || []).length
    const closes = (trimmed.match(/[\})\]]/g) || []).length
    indent = Math.max(0, indent + opens - closes)
  }

  let output = result.join('\n').trimEnd()
  if (output) output += '\n'
  return output
}
