/**
 * 计算属性脚本编辑器右侧 API 文档数据 (静态)。
 *
 * 数据来源:
 *  - curData/prevData: ComputedScriptAssembler 拼装后的 Map 结构
 *  - cache.* (21 方法): server/zwei-iot-timeseries/.../ScriptCacheOps.java
 *  - sensor.* (1 方法): server/zwei-iot-timeseries/.../ScriptSensorQuery.java
 *
 * 注: cache 的 21 个 Java 方法存在重载 (如 getInt/getInt+default),
 * 文档侧按"对外语义"合并同名重载, 共 13 条签名覆盖所有 21 方法的使用语义。
 */

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

export const API_DOCS: ApiGroup[] = [
  {
    icon: '📦',
    color: '#409eff',
    name: 'curData',
    methods: [
      { signature: '.props.<attrCode>', note: '当前数据包属性值' },
      { signature: '.dataTime', note: '数据时间戳 (ms)' }
    ]
  },
  {
    icon: '📦',
    color: '#409eff',
    name: 'prevData',
    description: '可空',
    methods: [
      { signature: '.props.<attrCode>', note: '上一条数据包属性值' },
      { signature: '.dataTime', note: '上一条数据时间戳 (ms)' }
    ]
  },
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
      { signature: 'expire(key, timeout, unit?) → boolean' },
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
        signature: 'query(deviceId, sensorCode, time, attrCode)',
        note: '异常时返回 null,不中断脚本'
      }
    ]
  }
]
