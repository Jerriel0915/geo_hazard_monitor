/**
 * 脚本编辑器右侧 API 文档数据 (静态) — 支持多模式。
 *
 * 数据来源:
 *  - curData/prevData (calc): ComputedScriptAssembler 拼装后的 Map 结构
 *    (5 字段: deviceCode/sensorCode/dataTime/props/properties, 其中 properties 是 props 别名)
 *  - hazardPointIds/currentTime (alarm): 综合告警策略脚本绑定注入
 *  - cache.* (21 方法): server/zwei-iot-timeseries/.../ScriptCacheOps.java
 *  - sensor.* (1 方法): server/zwei-iot-timeseries/.../ScriptSensorQuery.java
 *    入参 deviceCode (与 curData.deviceCode 同源), 内部解析 deviceId 查询 IoTDB;
 *    返回 SensorSnapshot: { time: long, values: Map<String,Double> }
 *
 * 注: cache 的 21 个 Java 方法存在重载 (如 getInt/getInt+default),
 * 文档侧按"对外语义"合并同名重载, 共 13 条签名覆盖所有 21 方法的使用语义。
 */

/** 脚本模式: calc=计算属性, alarm=综合告警策略 */
export type ScriptMode = 'calc' | 'alarm'

export interface ApiMethod {
  /** 方法签名, 如 "getInt(key, default?)" 或 ".props.<attrCode>" */
  signature: string
  /** 可选说明, 如 "异常时返回 null" */
  note?: string
}

export interface ApiGroup {
  /** 图标 emoji, 如 "📦" / "🛠" / "📡" */
  icon: string
  /** 主题色 (十六进制), 用于组标题与代码着色 */
  color: string
  /** 组名 (变量名), 如 "curData" / "cache" */
  name: string
  /** 可选描述, 如 "Redis 二次封装" */
  description?: string
  /** 该组的公开方法列表 */
  methods: ApiMethod[]
}

// ── calc 模式专属分组 ──

const CALC_SPECIFIC_GROUPS: ApiGroup[] = [
  {
    icon: '📦',
    color: '#409eff',
    name: 'curData',
    methods: [
      { signature: '.deviceCode', note: '设备编码' },
      { signature: '.sensorCode', note: '传感器编码' },
      { signature: '.props.<attrCode>', note: '当前数据包属性值' },
      { signature: '.properties.<attrCode>', note: 'props 别名 (同引用)' },
      { signature: '.dataTime', note: '数据时间戳 (ms)' }
    ]
  },
  {
    icon: '📦',
    color: '#409eff',
    name: 'prevData',
    description: '可空',
    methods: [
      { signature: '.deviceCode', note: '上一条设备编码' },
      { signature: '.sensorCode', note: '上一条传感器编码' },
      { signature: '.props.<attrCode>', note: '上一条数据包属性值' },
      { signature: '.properties.<attrCode>', note: 'props 别名 (同引用)' },
      { signature: '.dataTime', note: '上一条数据时间戳 (ms)' }
    ]
  }
]

// ── alarm 模式专属分组 ──

const ALARM_SPECIFIC_GROUPS: ApiGroup[] = [
  {
    icon: '⚠️',
    color: '#e6a23c',
    name: 'hazardPointIds',
    description: 'List<Long> — 绑定的隐患点 ID',
    methods: [
      { signature: '.size()', note: '隐患点数量' },
      { signature: '[i]', note: '按索引取 ID' },
      { signature: 'for (id in hazardPointIds) { ... }' }
    ]
  },
  {
    icon: '🕐',
    color: '#e6a23c',
    name: 'currentTime',
    description: 'String — 当前时间 (yyyy-MM-dd HH:mm:ss)',
    methods: [
      { signature: 'new Date(currentTime)' }
    ]
  },
  {
    icon: '📨',
    color: '#e6a23c',
    name: 'event',
    description: '触发事件对象 (CRON 模式下为 null)',
    methods: [
      { signature: '.deviceId', note: '设备 ID (DataIngest)' },
      { signature: '.deviceCode', note: '设备编码 (DataIngest)' },
      { signature: '.sensorCode', note: '传感器编码 (DataIngest)' },
      { signature: '.properties', note: '属性值列表 (DataIngest)' },
      { signature: '.dataTime', note: '数据时间戳 (DataIngest)' },
      { signature: '.alarmId', note: '告警记录 ID (AlarmTrigger)' },
      { signature: '.hazardPointId', note: '隐患点 ID (AlarmTrigger)' },
      { signature: '.alarmLevel', note: '告警等级 (AlarmTrigger)' },
      { signature: '.alarmMessage', note: '告警消息 (AlarmTrigger)' }
    ]
  },
  {
    icon: '📝',
    color: '#e6a23c',
    name: 'log',
    description: '脚本日志工具',
    methods: [
      { signature: '.info(msg)', note: '记录 INFO 日志' },
      { signature: '.warn(msg)', note: '记录 WARN 日志' },
      { signature: '.error(msg)', note: '记录 ERROR 日志' }
    ]
  }
]

// ── 共享分组 (cache + sensor) ──

const SHARED_GROUPS: ApiGroup[] = [
  {
    icon: '🛠',
    color: '#67c23a',
    name: 'cache',
    description: 'Redis 二次封装',
    methods: [
      // 读取 (覆盖 getInt/getLong/getDouble/getFloat/getBigDecimal/getString/getBoolean 14 个重载)
      { signature: 'getInt(key, default?)' },
      { signature: 'getLong(key, default?)' },
      { signature: 'getDouble(key, default?)' },
      { signature: 'getFloat(key, default?)' },
      { signature: 'getBigDecimal(key, default?)' },
      { signature: 'getString(key, default?)' },
      { signature: 'getBoolean(key, default?)' },
      // 写入 (覆盖 set/set+ttl 2 个重载)
      { signature: 'set(key, value)' },
      { signature: 'set(key, value, timeout, unit)' },
      // 管理 (5 个)
      { signature: 'delete(key) → boolean' },
      { signature: 'hasKey(key) → boolean' },
      { signature: 'expire(key, timeout, unit?) → boolean', note: '省略 unit 时 timeout 单位为秒' },
      { signature: 'getExpire(key) → long' }
    ]
  },
  {
    icon: '📡',
    color: '#67c23a',
    name: 'sensor',
    description: 'IoTDB 查询',
    methods: [
      {
        signature: 'query(deviceCode, sensorCode, time, attrCode)',
        note: '异常时返回 null,不中断脚本'
      },
      {
        signature: '↳ .time',
        note: '最近一条数据的时间戳 (ms, long)'
      },
      {
        signature: '↳ .values.<attrCode>',
        note: '属性值 (Double), 无数据时为 null'
      }
    ]
  }
]

/**
 * 根据脚本模式返回对应的 API 文档分组。
 * - calc: curData + prevData + cache + sensor
 * - alarm: hazardPointIds + currentTime + cache + sensor
 */
export function getApiDocs(mode: ScriptMode): ApiGroup[] {
  if (mode === 'alarm') {
    return [...ALARM_SPECIFIC_GROUPS, ...SHARED_GROUPS]
  }
  return [...CALC_SPECIFIC_GROUPS, ...SHARED_GROUPS]
}

/**
 * @deprecated 使用 getApiDocs(mode) 替代。保留向后兼容。
 */
export const API_DOCS: ApiGroup[] = getApiDocs('calc')
