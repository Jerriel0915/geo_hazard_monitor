/**
 * 设备图标动态配色工具。
 * <p>根据设备业务状态（status）和实时在线状态（onlineStatus），
 * 从 jc-icon 目录的四个子文件夹中动态选择匹配颜色档位：</p>
 * <ul>
 *   <li><b>green</b> — 正常 + 在线（运行中）</li>
 *   <li><b>gray</b> — 正常 + 离线（设备正常但当前未连接）</li>
 *   <li><b>red</b> — 故障（status=2）</li>
 *   <li><b>repair</b> — 停用/维修中（status=3）</li>
 * </ul>
 *
 * <p>命名规则：{@code /jc-icon/{color}/{baseName}_{color}.png}</p>
 */

import {MonitorContentIconEnum} from '@/constants/monitorIcons'

export type IconColor = 'green' | 'gray' | 'red' | 'repair'

/**
 * 根据设备状态推导图标颜色档位。
 *
 * @param status       业务状态：1=正常, 2=故障, 3=停用
 * @param onlineStatus 实时在线状态：1=在线, 0/null=离线
 * @returns 对应颜色档位
 */
export function getDeviceIconColor(
    status?: number | null,
    onlineStatus?: number | null
): IconColor {
    if (status === 2) return 'red'
    if (status === 3) return 'repair'
    // status=1（正常）或未设置：根据在线状态区分 green / gray
    return onlineStatus === 1 ? 'green' : 'gray'
}

/**
 * 解析图标路径中的基础名称。
 * 兼容三种入参：
 * <ul>
 *   <li>显式基础名（{@code device.icon} 字段）</li>
 *   <li>带后缀的完整路径（{@code /jc-icon/green/bc_green.png}）</li>
 *   <li>无路径无基础名 → 兜底为 {@code device}</li>
 * </ul>
 */
function resolveIconBaseName(input: {
    icon?: string | null
    iconPath?: string | null
}): string {
    if (input.icon && input.icon.trim()) {
        return input.icon.replace(/\.(png|gif)$/i, '').trim()
    }
    const path = input.iconPath || ''
    const fileName = path.substring(path.lastIndexOf('/') + 1)
    if (fileName) {
        // 去掉 _green / _gray / _red / _repair 后缀和扩展名
        return fileName.replace(/_(green|gray|red|repair)\.(png|gif)$/i, '').replace(/\.(png|gif)$/i, '')
    }
    return 'device'
}

/**
 * green 目录下无独立的 vidio_green.png（仅有 vidio1~vidio10）。
 * 当 baseName 为 vidio 或 vidio_green 时纠正为 vidio1。
 */
function correctVideoBaseName(baseName: string): string {
    if (baseName === 'vidio' || baseName === 'vidio_green') return 'vidio1'
    return baseName
}

/**
 * 根据设备当前状态动态构造图标 URL。
 * <p>默认颜色档位为 green（正常状态），仅当设备明确为故障/停用/离线时才切换其他颜色。</p>
 * <p>green 目录下无独立的 vidio_green.png，因此将前缀 vidio 纠正为 vidio1。</p>
 *
 * @param device 设备对象（至少包含 icon/iconPath/status/onlineStatus 之一）
 * @returns 完整图标 URL；非 jc-icon 路径直接原样返回
 */
export function getDeviceIconPath(device: {
    icon?: string | null
    iconPath?: string | null
    status?: number | null
    onlineStatus?: number | null
}): string {
    const rawPath = device.iconPath || ''
    // 外部 URL（非 jc-icon 路径）直接返回，由调用方自行处理
    if (rawPath && !rawPath.startsWith('/jc-icon/')) return rawPath
    // 根据设备状态 + 在线状态动态构造图标路径
    const color = getDeviceIconColor(device.status, device.onlineStatus)
    const baseName = resolveIconBaseName(device)
    const corrected = correctVideoBaseName(baseName)
    return `/jc-icon/${color}/${corrected}_${color}.png`
}

/**
 * 列表/选择器场景下优先展示绿色图标（正常状态）。
 * 无论设备实际 status/onlineStatus 如何，始终返回 green 档位。
 * green 目录下无独立的 vidio_green.png，因此将前缀 vidio 纠正为 vidio1。
 */
export function getDeviceIconPathGreen(device: {
    icon?: string | null
    iconPath?: string | null
}): string {
    const iconPath = device.iconPath || ''
    if (!iconPath || !iconPath.startsWith('/jc-icon/')) return iconPath || `/jc-icon/green/device_green.png`
    const baseName = resolveIconBaseName(device)
    const corrected = correctVideoBaseName(baseName)
    return `/jc-icon/green/${corrected}_green.png`
}

/**
 * 传感器图标：优先使用 sensor.icon 字段，其次根据 monitorTypeCode 反查图标枚举，
 * 最后兜底为 device_green.png。
 * green 目录下无独立的 vidio_green.png，因此将前缀 vidio 纠正为 vidio1。
 */
export function getSensorIconPath(sensor: {
    sensorCode?: string
    sensorName?: string
    monitorTypeCode?: string
    monitorTypeName?: string
    icon?: string | null
    iconPath?: string | null
}): string {
    // 直接有 icon
    if (sensor.icon && sensor.icon.trim()) {
        return `/jc-icon/green/${sensor.icon.trim()}_green.png`
    }
    // 从 iconPath 解析
    if (sensor.iconPath && sensor.iconPath.startsWith('/jc-icon/')) {
        const baseName = resolveIconBaseName({ iconPath: sensor.iconPath })
        const corrected = correctVideoBaseName(baseName)
        return `/jc-icon/green/${corrected}_green.png`
    }
    // 根据 monitorTypeCode 反查 MonitorContentIconEnum
    if (sensor.monitorTypeCode) {
        const code = sensor.monitorTypeCode.toUpperCase()
        const enumEntries = Object.values(MonitorContentIconEnum)
        const found = enumEntries.find(e => e.code === code)
        if (found) {
            return `/jc-icon/green/${found.icon}_green.png`
        }
    }
    return ''
}
