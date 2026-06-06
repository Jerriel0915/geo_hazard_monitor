package com.zwei.iot.hazardpoint.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 设备ID列表请求参数
 */
@Setter
@Getter
public class DeviceIdsRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "设备ID列表不能为空")
    private List<@NotNull(message = "设备ID不能为空") Long> deviceIds;
}
