export interface IconItem {
    code: string
    name: string
    icon: string
    path: string
}

/**
 * MonitorContent 图标枚举 — 与 jc-icon/green 目录下不带 _map 的文件一一对应。
 * 新增文件后在此追加条目：{code, name, icon}，其中 icon 为文件名去掉 _green.{png|gif} 的部分。
 */
export const MonitorContentIconEnum = {
    BSW: {code: 'BSW', name: '表面水平位移', icon: 'bsw'},
    SSW: {code: 'SSW', name: '深部水平位移', icon: 'ssw'},
    SSW_SX: {code: 'SSW_SX', name: '深部位移—双向', icon: 'ssw_sx'},
    BC: {code: 'BC', name: '表面沉降', icon: 'bc'},
    BC_JD: {code: 'BC_JD', name: '表面沉降—角度', icon: 'bc_jd'},
    QJ: {code: 'QJ', name: '倾角', icon: 'qj'},
    LF: {code: 'LF', name: '裂缝', icon: 'lf'},
    JY: {code: 'JY', name: '降雨量', icon: 'jy'},
    DW: {code: 'DW', name: '地下水水位', icon: 'dw'},
    KY: {code: 'KY', name: '孔隙水压力', icon: 'ky'},
    TL: {code: 'TL', name: '土压力', icon: 'tl'},
    SY: {code: 'SY', name: '渗透压力', icon: 'sy'},
    TH: {code: 'TH', name: '土体含水率', icon: 'th'},
    WD: {code: 'WD', name: '温度', icon: 'wd'},
    JSD: {code: 'JSD', name: '加速度', icon: 'jsd'},
    SC: {code: 'SC', name: '深部沉降', icon: 'sc'},
    LS: {code: 'LS', name: '形变-拉伸', icon: 'ls'},
    YS: {code: 'YS', name: '形变-压缩', icon: 'ys'},
    NQ: {code: 'NQ', name: '形变-挠曲', icon: 'nq'},
    ZL: {code: 'ZL', name: '轴力', icon: 'zl'},
    WJ: {code: 'WJ', name: '弯矩', icon: 'wj'},
    ZZL: {code: 'ZZL', name: '自振频率', icon: 'zzl'},
    GNSS: {code: 'GNSS', name: '表面位移（GNSS）', icon: 'gnss'},
    SP: {code: 'SP', name: '视频', icon: 'sp'},
    NW: {code: 'NW', name: '泥水位', icon: 'nw'},
    DX: {code: 'DX', name: '断线', icon: 'dx'},
    SG: {code: 'SG', name: '声光', icon: 'sg'},
    // 视频设备图标（无独立 vidio_green.png，使用 vidio1_green.png 作为图标选择器默认）
    VIDIO: {code: 'VIDIO', name: '视频设备', icon: 'vidio1'},
} as const

/** Convenience: returns the full icon list with resolved image paths. */
export function getIconList(): IconItem[] {
    return Object.values(MonitorContentIconEnum).map((item) => ({
        code: item.code,
        name: item.name,
        icon: item.icon,
        path: `/jc-icon/green/${item.icon}_green.png`
    }))
}
