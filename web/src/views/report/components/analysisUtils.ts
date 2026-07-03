/**
 * 分析页面共享工具函数。
 * 关联分析与数据宫格共用，消除重复定义。
 */

/** 将日期字符串（YYYY-MM-DD）补全为带时间的完整格式 */
export function formatDateWithTime(dateStr: string, isEnd: boolean): string {
  if (!dateStr) return ''
  const time = isEnd ? '23:59:59' : '00:00:00'
  return `${dateStr} ${time}`
}

/** 获取默认时间范围（最近 7 个自然日） */
export function getDefaultTimeRange(): [string, string] {
  const end = new Date()
  const start = new Date(end.getTime() - 7 * 24 * 3600 * 1000)
  const fmt = (d: Date) => {
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }
  return [fmt(start), fmt(end)]
}

/**
 * 将后端返回的时间值统一转换为可读的日期时间字符串。
 * 后端可能返回字符串（"2026-06-23 14:30:00"）或数值时间戳（毫秒）。
 * 输出统一格式：MM月DD日 HH:mm（用于图表横坐标标签）。
 */
export function formatTimestamp(raw: unknown): string {
  if (raw == null) return ''
  // 字符串直接返回（格式化交给 ECharts formatter）
  if (typeof raw === 'string') return raw
  // 数值时间戳 → 日期字符串
  const n = Number(raw)
  if (!Number.isFinite(n) || n <= 0) return String(raw)
  const d = new Date(n)
  if (Number.isNaN(d.getTime())) return String(raw)
  const pad = (v: number) => String(v).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/**
 * ECharts category 轴标签格式化器：将完整时间字符串简化为 MM月DD日 HH:mm。
 * 输入可能是 "2026-06-23 14:30:00" 或数值时间戳字符串。
 */
export function formatXAxisLabel(val: string): string {
  // 尝试匹配 "YYYY-MM-DD HH:mm:ss" 格式
  const match = val.match(/^(\d{4})-(\d{2})-(\d{2})\s+(\d{2}):(\d{2})/)
  if (match) {
    return `${Number(match[2])}月${Number(match[3])}日 ${match[4]}:${match[5]}`
  }
  // 尝试当作数值时间戳处理
  const n = Number(val)
  if (Number.isFinite(n) && n > 0) {
    const d = new Date(n)
    if (!Number.isNaN(d.getTime())) {
      const pad = (v: number) => String(v).padStart(2, '0')
      return `${d.getMonth() + 1}月${d.getDate()}日 ${pad(d.getHours())}:${pad(d.getMinutes())}`
    }
  }
  // 最后尝试替换 T 分隔符
  const t = val.replace('T', ' ')
  const parts = t.split(/[\s-:]/)
  if (parts.length >= 5) {
    return `${Number(parts[1])}月${Number(parts[2])}日 ${parts[3]}:${parts[4]}`
  }
  return val.length > 12 ? val.substring(5, 16) : val
}
