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
    /** scope value: "*" = 全部隐患点; "group:{id}" = 按分组; "{数字}" = 指定隐患点ID */
    private String hazardPointId;
    private String createBy;
    private Date createTime;
}
