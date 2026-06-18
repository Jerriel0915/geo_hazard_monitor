package com.zwei.iot.monitor.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 监测内容表 monitor_content
 * <p>
 * 监测内容定义每个监测类型下的具体监测指标项，
 * 例如：雨量监测类型下的小时雨量、日雨量等。
 * 监测内容与监测类型为多对一关系。
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class MonitorContent extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 监测类型ID
     */
    private Long monitorTypeId;

    /**
     * 监测类型名称（查询时返回）
     */
    private String monitorTypeName;

    /**
     * 监测内容编码
     */
    private String code;

    /**
     * 监测内容名称
     */
    private String name;

    /**
     * 单位
     */
    private String unit;

    /**
     * 排序号（每个监测类型内从1递增）
     */
    private Integer sortOrder;

    /**
     * 指标类型
     */
    private String indicatorType;

    /**
     * 图标路径
     */
    private String icon;

    /**
     * 最小值范围
     */
    private BigDecimal rangeMin;

    /**
     * 最大值范围
     */
    private BigDecimal rangeMax;

    /**
     * 字段类型: inherent-固有属性, computed-计算属性
     */
    private String fieldType;

    /**
     * 计算属性脚本(Groovy 代码块, 仅 field_type=computed 时必填)
     */
    private String calcScript;

    @Override
    public String toString() {
        return "MonitorContent{" +
                "id=" + id +
                ", monitorTypeId=" + monitorTypeId +
                ", monitorTypeName='" + monitorTypeName + '\'' +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", unit='" + unit + '\'' +
                ", indicatorType='" + indicatorType + '\'' +
                ", icon='" + icon + '\'' +
                ", rangeMin=" + rangeMin +
                ", rangeMax=" + rangeMax +
                ", fieldType='" + fieldType + '\'' +
                ", calcScript='" + (calcScript == null ? "null" : "[" + calcScript.length() + " chars]") + '\'' +
                '}';
    }
}
