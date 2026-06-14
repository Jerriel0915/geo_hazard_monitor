package com.zwei.iot.hazardpoint.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 未绑定设备VO（用于返回可选设备列表）
 *
 * @author zwei
 */
@Setter
@Getter
public class UnboundDeviceVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备ID */
    private Long id;

    /** 显示标签（格式：设备编号 - 设备名称） */
    private String label;

    /** 绑定次数 */
    private Integer bindCount;

    /** 设备状态 */
    private Integer status;

    /** 实时在线状态：1=在线, 0/null=离线 */
    private Integer onlineStatus;

    /** 设备图标基础名 */
    private String icon;

    /** 设备图标完整路径 */
    private String iconPath;

    /** 子级传感器列表 */
    private List<SensorVO> children;

    /**
     * 传感器VO（用于未绑定设备列表）
     */
    @Setter
    @Getter
    public static class SensorVO {
        private Long id;
        private String label;
        private String iconPath;

    }
}
