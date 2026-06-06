package com.zwei.iot.video.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 视频设备隐患点关联表 video_device_hazard_point
 */
@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class VideoDeviceHazardPoint extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long videoDeviceId;

    private Long hazardPointId;

    private BigDecimal installLongitude;

    private BigDecimal installLatitude;

    private Date bindTime;

    private String createBy;

    private Date createTime;
}
