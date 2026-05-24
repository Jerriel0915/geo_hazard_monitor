package com.zwei.iot.hazardpoint.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * ID列表请求参数
 */
@Setter
@Getter
public class BatchIdsRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "ID列表不能为空")
    private List<Long> ids;
}
