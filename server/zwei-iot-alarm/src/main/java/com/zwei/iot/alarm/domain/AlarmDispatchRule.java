package com.zwei.iot.alarm.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 告警分发规则表 alarm_dispatch_rule
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmDispatchRule extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;
    /**
     * 规则名称
     */
    private String name;
    /**
     * 隐患点ID（NULL=全局默认规则）
     */
    private Long hazardPointId;
    /**
     * 适用告警等级: 逗号分隔
     */
    private String alarmLevels;
    /**
     * 适用告警类型
     */
    private String alarmTypes;
    /**
     * 接收人列表 JSON
     */
    private String recipientsJson;
    /**
     * 通知渠道
     */
    private String channels;
    /**
     * 时间窗口限制
     */
    private String timeWindow;
    /**
     * 是否启用
     */
    private Integer isEnabled;
    /**
     * 删除标记
     */
    private Integer delFlag;
}
