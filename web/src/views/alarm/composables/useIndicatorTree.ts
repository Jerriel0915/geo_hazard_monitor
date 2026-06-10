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

const DEVICE_NODES: IndicatorTreeNode[] = [
  {
    value: 'device', label: '设备基础信息', displayLabel: '设备基础信息', disabled: true,
    children: [
      {value: 'device.onlineStatus', label: '在线状态', displayLabel: '在线状态', meta: {subjectType: 'DEVICE'}},
      {value: 'device.lastReportTime', label: '最后上报时间', displayLabel: '最后上报时间', meta: {subjectType: 'DEVICE'}},
    ]
  },
  {
    value: 'packet', label: '数据包信息', displayLabel: '数据包信息', disabled: true,
    children: [
      {value: 'packet.dataTime', label: '数据时间', displayLabel: '数据时间', meta: {subjectType: 'PACKET'}},
      {value: 'packet.quality', label: '数据质量', displayLabel: '数据质量', meta: {subjectType: 'PACKET'}},
    ]
  },
]

function buildPayloadLeaves(contents: MonitorContentItem[], valueKind: string, parentLabel: string): IndicatorTreeNode[] {
  return (contents || []).map(c => {
    const shortLabel = `${c.name}${c.unit ? ` (${c.unit})` : ''}`
    return {
      value: `payload.${valueKind}.${c.code}`,
      label: shortLabel,
      displayLabel: `${parentLabel}.${shortLabel}`,
      unit: c.unit || undefined,
      meta: {subjectType: 'CONTENT' as const, valueKind},
    }
  })
}

function buildPayloadNode(contents: MonitorContentItem[]): IndicatorTreeNode {
  return {
    value: 'payload', label: '数据载荷信息', displayLabel: '数据载荷信息', disabled: true,
    children: [
      {value: 'payload.current', label: '当前值', displayLabel: '当前值', disabled: true, children: buildPayloadLeaves(contents, 'current', '当前值')},
      {value: 'payload.realtime', label: '实时值', displayLabel: '实时值', disabled: true, children: buildPayloadLeaves(contents, 'realtime', '实时值')},
      {value: 'payload.previous', label: '上一值', displayLabel: '上一值', disabled: true, children: buildPayloadLeaves(contents, 'previous', '上一值')},
    ]
  }
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
        ...DEVICE_NODES,
        buildPayloadNode(contents),
      ]
      setTree(tree)
    } catch {
      setTree([])
    }
  }

  async function buildFromSensors(sensors: { sensorId: number; sensorName: string; monitorTypeId: number }[]) {
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
        value: `sensor_${s.sensorId}`,
        label: s.sensorName,
        displayLabel: s.sensorName,
        disabled: true,
        children: [
          ...DEVICE_NODES,
          buildPayloadNode(contents),
        ]
      } satisfies IndicatorTreeNode
    })
    setTree(tree)
  }

  function clear() {
    setTree([])
  }

  return {treeData, nodeMap, buildFromMonitorType, buildFromSensors, clear}
}
