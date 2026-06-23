import {ref, shallowRef} from 'vue'
import {getMonitorTypeDetail, type MonitorContentItem} from '@/api/monitorType'
import {type ValueType, getValueType} from '@/utils/indicatorType'

export interface IndicatorTreeNode {
  value: string
  label: string
  displayLabel: string     // 选中后显示的全路径（去掉第一层分组）
  children?: IndicatorTreeNode[]
  disabled?: boolean
  unit?: string
  meta?: { subjectType: string; valueKind?: string; valueType?: ValueType }
}

export interface Condition {
  subject: string
  subjectType?: 'CONTENT' | 'DEVICE' | 'PACKET'
  valueType?: ValueType
  operator: string
  threshold: number | string | boolean
  thresholdMax?: number | string
  unit?: string
  /** DATETIME 编辑态字段（仅前端用，序列化时合并入 threshold 字符串） */
  thresholdMode?: 'ABSOLUTE' | 'RELATIVE'
  relDirection?: '+' | '-'
  relValue?: number
  relUnit?: 's' | 'm' | 'h' | 'd'
}

export interface ConditionGroup {
  conditions: Condition[]
  logicOperator: 'AND' | 'OR'
}

export interface LevelFormState {
  groups: ConditionGroup[]
  groupLogic: 'AND' | 'OR'
  persistCount: number
  silencePeriod: number
  description: string
}

/** 构建 dimension 层子节点: payload / device / packet
 *  valueKind 由调用方传入, 区分 'current' / 'prev' 上下文
 */
function buildDimensionChildren(contents: MonitorContentItem[], valueKind: 'current' | 'prev' = 'current'): IndicatorTreeNode[] {
  const payloadChildren: IndicatorTreeNode[] = (contents || []).map(c => {
    const vt = getValueType(c.indicatorType)
    const shortLabel = c.unit ? `${c.name} (${c.unit})` : c.name
    return {
      value: `payload.${c.code}`,
      label: shortLabel,
      displayLabel: shortLabel,
      unit: c.unit || undefined,
      meta: {subjectType: 'CONTENT' as const, valueKind, valueType: vt},
    }
  })
  return [
    {
      value: 'payload', label: '数据载荷信息', displayLabel: '数据载荷信息', disabled: true,
      children: payloadChildren,
    },
    {
      value: 'device', label: '设备基础信息', displayLabel: '设备基础信息', disabled: true,
      children: [
        {value: 'device.onlineStatus', label: '在线状态', displayLabel: '在线状态',
          meta: {subjectType: 'DEVICE' as const, valueKind, valueType: 'BOOLEAN'}},
        {value: 'device.lastReportTime', label: '最后上报时间', displayLabel: '最后上报时间',
          meta: {subjectType: 'DEVICE' as const, valueKind, valueType: 'DATETIME'}},
      ],
    },
    {
      value: 'packet', label: '数据包信息', displayLabel: '数据包信息', disabled: true,
      children: [
        {value: 'packet.dataTime', label: '数据时间', displayLabel: '数据时间',
          meta: {subjectType: 'PACKET' as const, valueKind, valueType: 'DATETIME'}},
      ],
    },
  ]
}

/** 深拷贝节点树:
 *  - 叶子节点 (disabled=false): value 加 valuePrefix, displayLabel 设为 labelPrefix + ownLabel (中文路径)
 *  - 中间分组节点 (disabled=true): 保持自身 label 不变，递归处理子节点，labelPrefix 累加自身 label
 */
function prefixDisplayLabels(nodes: IndicatorTreeNode[], valuePrefix: string, labelPrefix: string): IndicatorTreeNode[] {
  return nodes.map(n => {
    const copy: IndicatorTreeNode = {...n}
    const ownLabel = n.displayLabel || n.label
    if (!n.disabled) {
      copy.value = `${valuePrefix}.${n.value}`
      copy.displayLabel = labelPrefix ? `${labelPrefix} / ${ownLabel}` : ownLabel
    } else if (n.children) {
      const newLabelPrefix = labelPrefix ? `${labelPrefix} / ${ownLabel}` : ownLabel
      copy.children = prefixDisplayLabels(n.children, valuePrefix, newLabelPrefix)
    }
    return copy
  })
}

function buildNodeMap(nodes: IndicatorTreeNode[], map: Map<string, IndicatorTreeNode>) {
  for (const node of nodes) {
    map.set(node.value, node)
    if (node.children) buildNodeMap(node.children, map)
  }
}

export function useIndicatorTree() {
  const treeData = shallowRef<IndicatorTreeNode[]>([])
  const nodeMap = shallowRef<Map<string, IndicatorTreeNode>>(new Map())

  function setTree(nodes: IndicatorTreeNode[]) {
    treeData.value = nodes
    const map = new Map<string, IndicatorTreeNode>()
    buildNodeMap(nodes, map)
    nodeMap.value = map
  }

  async function buildFromMonitorType(typeId: number) {
    try {
      const detail = await getMonitorTypeDetail(typeId)
      const contents = detail.contents || []
      const tree: IndicatorTreeNode[] = [
        {
          value: 'current', label: '当前值', displayLabel: '当前值', disabled: true,
          children: prefixDisplayLabels(buildDimensionChildren(contents, 'current'), 'current', '当前值'),
        },
        {
          value: 'prev', label: '上一值', displayLabel: '上一值', disabled: true,
          children: prefixDisplayLabels(buildDimensionChildren(contents, 'prev'), 'prev', '上一值'),
        },
      ]
      setTree(tree)
    } catch {
      setTree([])
    }
  }

  async function buildFromSensors(sensors: { sensorCode: string; sensorName: string; monitorTypeId: number }[]) {
    const seenTypes = new Map<number, MonitorContentItem[]>()
    for (const s of sensors) {
      if (!seenTypes.has(s.monitorTypeId)) {
        try {
          const detail = await getMonitorTypeDetail(s.monitorTypeId)
          seenTypes.set(s.monitorTypeId, detail.contents || [])
        } catch {
          seenTypes.set(s.monitorTypeId, [])
        }
      }
    }

    const tree: IndicatorTreeNode[] = sensors.map(s => {
      const contents = seenTypes.get(s.monitorTypeId) || []
      return {
        value: s.sensorCode,
        label: s.sensorName,
        displayLabel: s.sensorName,
        disabled: true,
        children: [
          {
            value: 'current', label: '当前值', displayLabel: '当前值', disabled: true,
            children: prefixDisplayLabels(buildDimensionChildren(contents, 'current'), 'current', '当前值'),
          },
          {
            value: 'prev', label: '上一值', displayLabel: '上一值', disabled: true,
            children: prefixDisplayLabels(buildDimensionChildren(contents, 'prev'), 'prev', '上一值'),
          },
        ],
      } satisfies IndicatorTreeNode
    })

    // 外层再以 sensorCode 为 valuePrefix、sensorName 为 labelPrefix 应用一次（叠加传感器名前缀）
    const prefixedTree = tree.map(sensorNode => ({
      ...sensorNode,
      children: prefixDisplayLabels(sensorNode.children!, sensorNode.value, sensorNode.label),
    }))

    setTree(prefixedTree)
  }

  function clear() {
    setTree([])
  }

  return {treeData, nodeMap, buildFromMonitorType, buildFromSensors, clear}
}
