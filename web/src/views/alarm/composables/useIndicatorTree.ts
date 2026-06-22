import {ref, shallowRef} from 'vue'
import {getMonitorTypeDetail, type MonitorContentItem} from '@/api/monitorType'

export interface IndicatorTreeNode {
  value: string
  label: string
  displayLabel: string     // 选中后显示的全路径（去掉第一层分组）
  children?: IndicatorTreeNode[]
  disabled?: boolean
  unit?: string
  meta?: { subjectType: string; valueKind?: string }
}

export interface Condition {
  subject: string
  subjectType?: 'CONTENT' | 'DEVICE' | 'PACKET'
  operator: string
  threshold: number
  unit?: string
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
    const shortLabel = `${c.name}${c.unit ? ` (${c.unit})` : ''}`
    return {
      value: `payload.${c.code}`,
      label: shortLabel,
      displayLabel: shortLabel,
      unit: c.unit || undefined,
      meta: {subjectType: 'CONTENT' as const, valueKind},
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
        {value: 'device.onlineStatus', label: '在线状态', displayLabel: '在线状态', meta: {subjectType: 'DEVICE' as const, valueKind}},
        {value: 'device.lastReportTime', label: '最后上报时间', displayLabel: '最后上报时间', meta: {subjectType: 'DEVICE' as const, valueKind}},
      ],
    },
    {
      value: 'packet', label: '数据包信息', displayLabel: '数据包信息', disabled: true,
      children: [
        {value: 'packet.dataTime', label: '数据时间', displayLabel: '数据时间', meta: {subjectType: 'PACKET' as const, valueKind}},
      ],
    },
  ]
}

/** 深拷贝节点树, 为所有 disabled=false 的节点 value 和 displayLabel 都加上前缀
 *  displayLabel 使用 n.displayLabel (而非 n.label) 作为基底, 保证多次 prefix 叠加不丢失
 */
function prefixDisplayLabels(nodes: IndicatorTreeNode[], prefix: string): IndicatorTreeNode[] {
  return nodes.map(n => {
    const copy: IndicatorTreeNode = {...n}
    if (!n.disabled) {
      // 可选叶子 / 可选节点: value 和 displayLabel 都加前缀
      copy.value = `${prefix}.${n.value}`
      copy.displayLabel = `${prefix}.${n.displayLabel || n.label}`
    } else if (n.children) {
      // 分组节点: 保持自身 label, 递归处理子节点
      copy.children = prefixDisplayLabels(n.children, prefix)
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
          children: prefixDisplayLabels(buildDimensionChildren(contents, 'current'), 'current'),
        },
        {
          value: 'prev', label: '上一值', displayLabel: '上一值', disabled: true,
          children: prefixDisplayLabels(buildDimensionChildren(contents, 'prev'), 'prev'),
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
            children: prefixDisplayLabels(buildDimensionChildren(contents, 'current'), 'current'),
          },
          {
            value: 'prev', label: '上一值', displayLabel: '上一值', disabled: true,
            children: prefixDisplayLabels(buildDimensionChildren(contents, 'prev'), 'prev'),
          },
        ],
      } satisfies IndicatorTreeNode
    })

    // 对每个 sensor 子树再次应用 prefixDisplayLabels, 加上 sensorCode 前缀
    const prefixedTree = tree.map(sensorNode => ({
      ...sensorNode,
      children: prefixDisplayLabels(sensorNode.children!, sensorNode.value),
    }))

    setTree(prefixedTree)
  }

  function clear() {
    setTree([])
  }

  return {treeData, nodeMap, buildFromMonitorType, buildFromSensors, clear}
}
