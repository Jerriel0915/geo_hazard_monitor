package com.zwei.iot.hazardpoint.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 绑定设备VO（用于返回已绑定设备列表）
 *
 * @author zwei
 */
@Setter
@Getter
public class BoundDeviceVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 关联记录ID */
    private Long id;

    /** 设备ID */
    private Long deviceId;

    /** 设备编号 */
    private String deviceCode;

    /** 设备名称 */
    private String deviceName;

    /** 绑定时间 */
    private Date bindTime;

    /** 安装经度 */
    private BigDecimal installLongitude;

    /** 安装纬度 */
    private BigDecimal installLatitude;

    /** 设备状态 */
    private Integer deviceStatus;

    /** 传感器列表 */
    private List<SensorVO> sensors;

    /**
     * 传感器VO（简化版，用于绑定设备返回）
     */
    @Setter
    @Getter
    public static class SensorVO {
        private Long id;
        private String name;
        private String iconPath;

    }
}
