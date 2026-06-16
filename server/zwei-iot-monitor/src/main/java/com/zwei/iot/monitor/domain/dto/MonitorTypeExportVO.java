package com.zwei.iot.monitor.domain.dto;

import com.zwei.common.annotation.Excel;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 监测类型导出对象
 */
@Setter
@Getter
public class MonitorTypeExportVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Excel(name = "类型编码")
    private String code;

    @Excel(name = "类型名称")
    private String name;

    @Excel(name = "图标")
    private String icon;

    @Excel(name = "描述")
    private String description;

    @Excel(name = "排序号")
    private Integer sortOrder;

    @Excel(name = "状态")
    private String statusName;

    @Excel(name = "创建人")
    private String createBy;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Excel(name = "更新人")
    private String updateBy;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
