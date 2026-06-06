package com.zwei.iot.monitor.domain.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.io.Serial;
import java.io.Serializable;

@Setter @Getter
public class MonitorCategoryCreateRequest implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    @NotBlank(message = "大类编码不能为空") @Size(max = 100) private String code;
    @NotBlank(message = "大类名称不能为空") @Size(max = 200) private String name;
    @Size(max = 200) private String icon;
    @Min(0) @Max(Integer.MAX_VALUE) private Integer sortOrder;
    @Min(0) @Max(1) private Integer status;
}
