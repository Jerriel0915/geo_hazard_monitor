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
