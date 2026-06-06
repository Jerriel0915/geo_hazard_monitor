package com.zwei.iot.video.domain;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 已绑定视频设备返回对象
 */
@Setter
@Getter
public class BoundVideoDeviceVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long videoDeviceId;

    private String deviceCode;

    private String deviceName;

    private Date bindTime;

    private BigDecimal installLongitude;

    private BigDecimal installLatitude;
}
