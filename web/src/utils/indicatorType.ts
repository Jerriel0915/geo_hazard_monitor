/**
 * 监测内容 indicator_type → valueType 映射
 * 与后端 com.zwei.iot.monitor.constant.IndicatorValueType 保持同步
 */

export type ValueType = 'NUMBER' | 'DATETIME' | 'STRING' | 'BOOLEAN'

export interface IndicatorTypeMeta {
  code: string
  name: string
  unit: string
  valueType: ValueType
}

export const INDICATOR_TYPE_META: Record<string, IndicatorTypeMeta> = {
  wy:  { code: 'wy',  name: '位移',   unit: 'mm',   valueType: 'NUMBER' },
  wd:  { code: 'wd',  name: '温度',   unit: '℃',    valueType: 'NUMBER' },
  jd:  { code: 'jd',  name: '角度',   unit: '°',    valueType: 'NUMBER' },
  yl:  { code: 'yl',  name: '压力',   unit: 'MPa',  valueType: 'NUMBER' },
  sw:  { code: 'sw',  name: '水位',   unit: 'm',    valueType: 'NUMBER' },
  jsd: { code: 'jsd', name: '加速度', unit: 'm/s²', valueType: 'NUMBER' },
  hsl: { code: 'hsl', name: '含水率', unit: '%',    valueType: 'NUMBER' },
  ljn: { code: 'ljn', name: '力矩',   unit: 'N/m²', valueType: 'NUMBER' },
  zdl: { code: 'zdl', name: '震动频率', unit: 'Hz',  valueType: 'NUMBER' },
  dl:  { code: 'dl',  name: '电量',   unit: 'V',    valueType: 'NUMBER' },
  dx:  { code: 'dx',  name: '断线',   unit: '',     valueType: 'BOOLEAN' },
  sg:  { code: 'sg',  name: '声光',   unit: '',     valueType: 'STRING' },
  sp:  { code: 'sp',  name: '视频',   unit: '',     valueType: 'STRING' },
}

export const INDICATOR_TYPE_OPTIONS = Object.values(INDICATOR_TYPE_META)

/** 返回 indicator_type code 对应的 valueType；未知返回 NUMBER */
export function getValueType(code?: string): ValueType {
  if (!code) return 'NUMBER'
  return INDICATOR_TYPE_META[code.trim().toLowerCase()]?.valueType ?? 'NUMBER'
}

export function indicatorTypeLabel(code?: string): string {
  if (!code) return ''
  return INDICATOR_TYPE_META[code.trim().toLowerCase()]?.name ?? ''
}
