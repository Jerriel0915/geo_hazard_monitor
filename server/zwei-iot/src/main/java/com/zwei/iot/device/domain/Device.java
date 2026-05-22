package com.zwei.iot.device.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 设备表 device
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Device extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 设备编号
     */
    private String code;

    /**
     * 设备名称
     */
    private String name;

    /**
     * 设备图标
     */
    private String icon;

    /**
     * 图标路径
     */
    private String iconPath;

    /**
     * 状态: 1-正常, 2-故障, 3-离线
     */
    private Integer status;

    /**
     * 状态名称（查询时返回）
     */
    private String statusName;

    /**
     * 运行状态: 0-未知, 1-运行中, 2-停止
     */
    private Integer runStatus;

    /**
     * 运行状态名称（查询时返回）
     */
    private String runStatusName;

    /**
     * 最近上报时间
     */
    private String lastReportTime;

    /**
     * 删除标记: 0-正常, 1-删除
     */
    private Integer delFlag;

    /**
     * 传感器列表（查询详情时返回）
     */
    private Object sensors;

    @Override
    public String toString() {
        return "Device{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", icon='" + icon + '\'' +
                ", iconPath='" + iconPath + '\'' +
                ", status=" + status +
                ", statusName='" + statusName + '\'' +
                ", runStatus=" + runStatus +
                ", runStatusName='" + runStatusName + '\'' +
                ", lastReportTime='" + lastReportTime + '\'' +
                ", delFlag=" + delFlag +
                '}';
    }
}