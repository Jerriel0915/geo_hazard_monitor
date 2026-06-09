package com.zwei.iot.alarm.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 告警判据表 alarm_criteria — V3.0 level_config 重构。
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmCriteria extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Long monitorTypeId;
    private String monitorTypeName;
    private Long monitorContentId;
    private String monitorContentCode;
    private Long hazardPointId;

    /**
     * 四级告警条件配置 JSON。
     * <pre>
     * {"blue":{"logicOperator":"AND","conditions":[...],"description":"..."}, ...}
     * </pre>
     */
    private String levelConfig;

    private Integer persistCount;
    private Integer silencePeriod;
    private Integer isEnabled;
    private Integer version;
    private Integer delFlag;
}
