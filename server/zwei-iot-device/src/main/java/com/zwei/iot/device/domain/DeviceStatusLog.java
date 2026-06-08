package com.zwei.iot.device.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceStatusLog {
    private Long id;
    private Long deviceId;
    private String deviceCode;
    private Integer oldStatus;
    private Integer newStatus;
    private String statusText;
    private String operatorName;
    private String operatorPhone;
    private LocalDateTime operationDate;
    private String description;
    private String createBy;
    private LocalDateTime createTime;
}
