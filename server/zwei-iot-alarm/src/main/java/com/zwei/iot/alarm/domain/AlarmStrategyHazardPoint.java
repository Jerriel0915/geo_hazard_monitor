package com.zwei.iot.alarm.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 策略-隐患点绑定表 alarm_strategy_hazard_point
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmStrategyHazardPoint implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long strategyId;
    private Long hazardPointId;
    private String createBy;
    private Date createTime;
}
