package com.zwei.iot.device.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 设备注册日志
 */
@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class DeviceRegistrationLog extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String requestId;

    private String registerCode;

    private String registerSource;

    private String vendorName;

    private Long deviceId;

    private String sn;

    private String resultStatus;

    private String failureReason;

    private String requestBody;
}
