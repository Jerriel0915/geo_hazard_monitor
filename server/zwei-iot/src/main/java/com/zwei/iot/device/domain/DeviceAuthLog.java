package com.zwei.iot.device.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 设备认证日志
 */
@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class DeviceAuthLog extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long deviceId;

    private String authUsername;

    private Integer authResult;

    private String clientId;

    private String clientIp;

    private String failureReason;
}
