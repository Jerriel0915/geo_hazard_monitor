package com.zwei.iot.alarm.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 告警判据表 alarm_criteria
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

    /**
     * 主键ID
     */
    private Long id;
    /**
     * 判据名称
     */
    private String name;
    /**
     * 监测类型ID
     */
    private Long monitorTypeId;
    /**
     * 监测类型名称
     */
    private String monitorTypeName;
    /**
     * 监测内容ID
     */
    private Long monitorContentId;
    /**
     * 监测内容编码
     */
    private String monitorContentCode;
    /**
     * 隐患点ID（NULL=全局适用）
     */
    private Long hazardPointId;
    /**
     * 判据条件列表 JSON
     */
    private String conditionsJson;
    /**
     * 多条件逻辑: AND/OR
     */
    private String logicOperator;
    /**
     * 蓝色预警表达式
     */
    private String blueExpression;
    /**
     * 蓝色预警描述
     */
    private String blueDescription;
    /**
     * 黄色预警表达式
     */
    private String yellowExpression;
    /**
     * 黄色预警描述
     */
    private String yellowDescription;
    /**
     * 橙色预警表达式
     */
    private String orangeExpression;
    /**
     * 橙色预警描述
     */
    private String orangeDescription;
    /**
     * 红色预警表达式
     */
    private String redExpression;
    /**
     * 红色预警描述
     */
    private String redDescription;
    /**
     * 持续触发次数
     */
    private Integer persistCount;
    /**
     * 静默周期
     */
    private Integer silencePeriod;
    /**
     * 是否启用
     */
    private Integer isEnabled;
    /**
     * 版本号
     */
    private Integer version;
    /**
     * 删除标记
     */
    private Integer delFlag;
}
