package com.zwei.iot.monitor.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.util.List;

/**
 * 监测类型表 monitor_type
 * <p>
 * 监测类型用于定义不同的监测方式，如雨量监测、水位监测、位移监测等。
 * 每个监测类型可以包含多个监测内容。
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class MonitorType extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 监测类型编码
     */
    private String code;
    private Long categoryId;
    private String categoryName;

    /**
     * 监测类型名称
     */
    private String name;

    /**
     * 设备类型: 1-直连设备, 2-传感器, 3-RTU
     */
    private Integer deviceType;

    /**
     * 设备类型名称（查询时返回）
     */
    private String deviceTypeName;

    /**
     * 图标路径
     */
    private String icon;

    /**
     * 描述
     */
    private String description;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 状态: 0-禁用, 1-启用
     */
    private Integer status;

    /**
     * 删除标记: 0-正常, 1-删除
     */
    private Integer delFlag;

    /**
     * 监测内容列表（查询详情时返回）
     */
    private List<MonitorContent> contents;

    @Override
    public String toString() {
        return "MonitorType{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", deviceType=" + deviceType +
                ", deviceTypeName='" + deviceTypeName + '\'' +
                ", icon='" + icon + '\'' +
                ", description='" + description + '\'' +
                ", sortOrder=" + sortOrder +
                ", status=" + status +
                ", delFlag=" + delFlag +
                '}';
    }
}
