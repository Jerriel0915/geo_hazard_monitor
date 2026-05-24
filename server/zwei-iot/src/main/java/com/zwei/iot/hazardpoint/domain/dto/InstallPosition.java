package com.zwei.iot.hazardpoint.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 设备安装位置信息
 *
 * @author zwei
 */
@Setter
@Getter
public class InstallPosition implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备ID */
    private Long deviceId;

    /** 安装经度 */
    private BigDecimal installLongitude;

    /** 安装纬度 */
    private BigDecimal installLatitude;

}
