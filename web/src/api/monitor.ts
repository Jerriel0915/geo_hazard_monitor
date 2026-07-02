import request from '@/utils/request'
import type {AjaxResult} from './system'

// ===================== 类型定义 =====================

/** MQTT 服务器统计 */
export interface MqttStatsConnections {
    accepted: number
    closed: number
    size: number
}

export interface MqttStatsMessages {
    bytesPerTcpReceive: number
    handledBytes: number
    handledPackets: number
    packetsPerTcpReceive: number
    receivedBytes: number
    receivedPackets: number
    sendBytes: number
    sendPackets: number
}

export interface MqttStatsNodes {
    clientNodes: number
    connections: number
    users: number
}

export interface MqttStats {
    upstream: boolean
    connections: MqttStatsConnections
    messages: MqttStatsMessages
    nodes: MqttStatsNodes
    startTime: number | null
    // 扁平便捷字段（向后兼容）
    connectionsSize: number
    connectionsAccepted: number
    connectionsClosed: number
    messagesHandledPackets: number
    messagesHandledBytes: number
    messagesReceivedPackets: number
    messagesReceivedBytes: number
    messagesSendPackets: number
    messagesSendBytes: number
}

/** MQTT 监听器 */
export interface MqttListener {
    type: string
    ip: string
    port: number
    enabled: boolean
    remark: string
}

/** MQTT 运行配置 */
export interface MqttConfig {
    heartbeatTimeout: number
    readBufferSize: string
    maxBytesInMessage: string
    authEnabled: boolean
    debug: boolean
    statEnable: boolean
}

/** 分页查询参数 */
export interface MqttClientPageParams {
    page?: number
    limit?: number
}

/** MQTT 客户端列表项 */
export interface MqttClientItem {
    clientId: string
    username: string
    connected: boolean
    ipAddress: string
    port: number
    protoName: string
    protoVer: number
    createdAt: number
    connectedAt: number
    deviceId: number | null
    deviceName: string | null
    deviceCode: string | null
    deviceRunStatus: number | null
    hazardPointName: string | null
    lastAuthIp: string
    lastAuthTime: string
}

/** MQTT 客户端分页结果 */
export interface MqttClientPageResult {
    pageNumber: number
    pageSize: number
    totalRow: number
    list: MqttClientItem[]
}

/** MQTT 订阅 */
export interface MqttSubscription {
    clientId: string
    topicFilter: string
    mqttQoS: number
}

/** MQTT 客户端详情 */
export interface MqttClientDetail {
    info: MqttClientItem
    subscriptions: MqttSubscription[]
}

/** 批量踢出结果 */
export interface MqttKickBatchResult {
    success: number
    fail: number
    total: number
}

// ===================== API 函数 =====================

/** 获取 MQTT 服务器统计 */
export function getMqttStats(): Promise<AjaxResult<MqttStats>> {
    return request.get('/monitor/mqtt/stats')
}

/** 获取 MQTT 监听器列表 */
export function getMqttListeners(): Promise<AjaxResult<MqttListener[]>> {
    return request.get('/monitor/mqtt/listeners')
}

/** 获取 MQTT 运行配置 */
export function getMqttConfig(): Promise<AjaxResult<MqttConfig>> {
    return request.get('/monitor/mqtt/config')
}

/** 分页查询在线客户端 */
export function getMqttClients(params?: MqttClientPageParams): Promise<AjaxResult<MqttClientPageResult>> {
    return request.get('/monitor/mqtt/clients/page', {params})
}

/** 获取客户端详情（含订阅） */
export function getMqttClientDetail(clientId: string): Promise<AjaxResult<MqttClientDetail>> {
    return request.get(`/monitor/mqtt/clients/${encodeURIComponent(clientId)}`)
}

/** 踢出指定客户端 */
export function kickMqttClient(clientId: string): Promise<AjaxResult<null>> {
    return request.delete(`/monitor/mqtt/clients/${encodeURIComponent(clientId)}`)
}

/** 批量踢出客户端 */
export function kickMqttClients(clientIds: string[]): Promise<AjaxResult<MqttKickBatchResult>> {
    return request.delete('/monitor/mqtt/clients/batch', {data: clientIds})
}

// ===================== 数据日志 =====================

/** MQTT 数据日志条目 */
export interface MqttMessageLogItem {
    receiveTime: number
    clientId: string
    username: string
    topic: string
    payload: string
    payloadSize: number
}

/** MQTT 数据日志分页查询参数 */
export interface MqttMessageLogParams {
    page?: number
    pageSize?: number
    clientId?: string
    topic?: string
}

/** MQTT 数据日志分页结果 */
export interface MqttMessageLogPageResult {
    pageNumber: number
    pageSize: number
    totalRow: number
    list: MqttMessageLogItem[]
}

/** 分页查询 MQTT 数据日志 */
export function getMqttMessages(params?: MqttMessageLogParams): Promise<AjaxResult<MqttMessageLogPageResult>> {
    return request.get('/monitor/mqtt/messages/page', {params})
}

// ===================== 异常报文 =====================

/** MQTT 异常报文条目（已认证但解析/报送失败） */
export interface MqttExceptionLogItem {
    id: number
    /** 接收时间（yyyy-MM-dd HH:mm:ss） */
    receiveTime: string
    clientId: string | null
    username: string | null
    deviceId: number | null
    topic: string
    payload: string | null
    payloadSize: number
    /** 失败阶段: TOPIC / FORMAT / STRATEGY / PARSE / UNKNOWN */
    rejectStage: string
    rejectReason: string
    errorStack: string | null
    createTime: string
}

/** 异常报文分页查询参数 */
export interface MqttExceptionLogParams {
    page?: number
    pageSize?: number
    clientId?: string
    topic?: string
    rejectReason?: string
    /** yyyy-MM-dd HH:mm:ss */
    startTime?: string
    /** yyyy-MM-dd HH:mm:ss */
    endTime?: string
}

/** 异常报文分页结果 */
export interface MqttExceptionLogPageResult {
    pageNumber: number
    pageSize: number
    totalRow: number
    list: MqttExceptionLogItem[]
}

/** 异常报文保留期配置 */
export interface ExceptionRetentionConfig {
    enabled: boolean
    retentionDays: number
}

/** 分页查询异常报文 */
export function getMqttExceptions(params?: MqttExceptionLogParams): Promise<AjaxResult<MqttExceptionLogPageResult>> {
    return request.get('/monitor/mqtt/exceptions/page', {params})
}

/** 导出异常报文（Excel） */
export const exportMqttExceptions = (params?: MqttExceptionLogParams) =>
    request.raw.post('/monitor/mqtt/exceptions/export', params || {}, {responseType: 'blob'})

/** 查询异常报文保留期配置 */
export function getExceptionRetentionConfig(): Promise<AjaxResult<ExceptionRetentionConfig>> {
    return request.get('/monitor/mqtt/exceptions/retention-config')
}

/** 更新异常报文保留期配置 */
export function updateExceptionRetentionConfig(body: Partial<ExceptionRetentionConfig>): Promise<AjaxResult<null>> {
    return request.put('/monitor/mqtt/exceptions/retention-config', body)
}

// ===================== 大屏仪表盘统计 =====================

export interface DashboardOverview {
    device: { total: number; byStatus: Record<string, number>; byRunStatus: Record<string, number> }
    sensor: { total: number; enabled: number; disabled: number; onlineRate: number }
    deviceOnlineRate: { total: number; online: number; onlineRate: number }
    hazardPoint: { total: number; byStatus: Record<string, number> }
    monitorType: { total: number }
    videoDevice: { total: number; byStatus: Record<string, number> }
    totalMonitorCount: number
}

export interface RateByTypeVO {
    windowMinutes?: number
    total: number
    online: number
    offline: number
    onlineRate: number
    byType: Array<{
        monitorTypeId: number
        monitorTypeName: string
        sortOrder: number
        total: number
        online: number
        offline: number
        onlineRate: number
    }>
}

export interface HazardPointTrendVO {
    months: string[]
    counts: number[]
    cumulativeCounts: number[]
}

export interface SensorDistributionVO {
    list: Array<{ monitorTypeId: number; monitorTypeName: string; sensorCount: number }>
}

export function getDashboardOverview(): Promise<AjaxResult<DashboardOverview>> {
    return request.get('/monitor/dashboard/overview')
}

export function getDeviceOnlineRate(): Promise<AjaxResult<RateByTypeVO>> {
    return request.get('/monitor/dashboard/device-online-rate')
}

export function getDeviceActiveRate(windowMinutes?: number): Promise<AjaxResult<RateByTypeVO>> {
    return request.get('/monitor/dashboard/device-active-rate', {params: {windowMinutes}})
}

export function getSensorOnlineRate(): Promise<AjaxResult<RateByTypeVO>> {
    return request.get('/monitor/dashboard/sensor-online-rate')
}

export function getSensorActiveRate(windowMinutes?: number): Promise<AjaxResult<RateByTypeVO>> {
    return request.get('/monitor/dashboard/sensor-active-rate', {params: {windowMinutes}})
}

export function getHazardPointTrend(months?: number): Promise<AjaxResult<HazardPointTrendVO>> {
    return request.get('/monitor/dashboard/hazard-point-trend', {params: {months}})
}

export function getSensorDistribution(): Promise<AjaxResult<SensorDistributionVO>> {
    return request.get('/monitor/dashboard/sensor-distribution')
}

/** 健康度评分项 */
export interface HealthScoreItem {
    name: string
    value: number
    weight: number
    color: string
    dataSource: string
}

/** 健康度评分 */
export interface HealthScoreVO {
    overallScore: number
    items: HealthScoreItem[]
}

/** 大屏一体化聚合 VO */
export interface DashboardFullVO {
    overview: DashboardOverview
    deviceOnlineRate: RateByTypeVO
    deviceActiveRate: RateByTypeVO
    sensorOnlineRate: RateByTypeVO
    hazardPointTrend: HazardPointTrendVO
    sensorDistribution: SensorDistributionVO
    healthScore: HealthScoreVO
}

/** 大屏一体化聚合（替代 6 次独立请求为 1 次） */
export function getDashboardFull(windowMinutes?: number): Promise<AjaxResult<DashboardFullVO>> {
    return request.get('/monitor/dashboard/full', {params: {windowMinutes}})
}

/** 获取系统健康度评分 */
export function getHealthScore(): Promise<AjaxResult<HealthScoreVO>> {
    return request.get('/monitor/dashboard/health-score')
}
