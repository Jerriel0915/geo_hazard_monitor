package com.zwei.iot.monitor.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class MonitorCategory extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private String code;
    private String name;
    private String icon;
    private Integer sortOrder;
    private Integer status;
    private Integer delFlag;
}
