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
    // 默认绿色；仅当显式离线（onlineStatus === 0）时显示灰色
    return onlineStatus === 0 ? 'gray' : 'green'
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
 * 根据设备当前状态动态构造图标 URL。
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
    const iconPath = device.iconPath || ''
    // 外部 URL 或空路径不参与动态配色
    if (!iconPath || !iconPath.startsWith('/jc-icon/')) return iconPath
    const color = getDeviceIconColor(device.status, device.onlineStatus)
    const baseName = resolveIconBaseName(device)
    return `/jc-icon/${color}/${baseName}_${color}.png`
}
