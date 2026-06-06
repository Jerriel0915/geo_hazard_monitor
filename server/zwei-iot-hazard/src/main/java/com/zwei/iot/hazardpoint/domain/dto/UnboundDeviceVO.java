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
