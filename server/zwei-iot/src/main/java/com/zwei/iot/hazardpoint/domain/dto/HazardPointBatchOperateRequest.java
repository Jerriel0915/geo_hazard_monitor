package com.zwei.iot.hazardpoint.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 隐患点批量操作请求参数
 */
@Setter
@Getter
public class HazardPointBatchOperateRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "请选择要操作的隐患点")
    private List<Long> ids;

    @NotBlank(message = "操作类型不能为空")
    private String operation;
}
