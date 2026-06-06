package com.zwei.iot.hazardpoint.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 设备隐患点关联表 device_hazard_point
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class DeviceHazardPoint extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 设备ID */
    private Long deviceId;

    /** 隐患点ID */
    private Long hazardPointId;

    /** 安装经度 */
    private BigDecimal installLongitude;

    /** 安装纬度 */
    private BigDecimal installLatitude;

    /** 绑定时间 */
    private Date bindTime;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    private Date createTime;

}
