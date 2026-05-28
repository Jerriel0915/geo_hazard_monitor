package com.zwei.iot.hazardpoint.domain.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 视频设备安装位置信息
 */
@Setter
@Getter
public class VideoInstallPosition implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "视频设备ID不能为空")
    private Long videoDeviceId;

    @DecimalMin(value = "-180.000000", message = "安装经度不能小于-180")
    @DecimalMax(value = "180.000000", message = "安装经度不能大于180")
    @Digits(integer = 3, fraction = 6, message = "安装经度最多支持6位小数")
    private BigDecimal installLongitude;

    @DecimalMin(value = "-90.000000", message = "安装纬度不能小于-90")
    @DecimalMax(value = "90.000000", message = "安装纬度不能大于90")
    @Digits(integer = 2, fraction = 6, message = "安装纬度最多支持6位小数")
    private BigDecimal installLatitude;
}
