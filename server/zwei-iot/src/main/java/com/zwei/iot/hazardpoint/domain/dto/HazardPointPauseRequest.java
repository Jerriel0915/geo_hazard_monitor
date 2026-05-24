package com.zwei.iot.hazardpoint.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 停测/恢复请求参数
 */
@Setter
@Getter
public class HazardPointPauseRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "pause不能为空")
    private Boolean pause;
}
