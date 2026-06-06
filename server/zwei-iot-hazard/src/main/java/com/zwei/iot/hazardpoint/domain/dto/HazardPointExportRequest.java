package com.zwei.iot.hazardpoint.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 隐患点导出请求参数
 */
@Setter
@Getter
public class HazardPointExportRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private List<Long> ids;

    private String code;

    private String name;

    private Long groupId;

    private Integer status;
}
