package com.zwei.iot.hazardpoint.domain.dto;

import com.zwei.common.annotation.Excel;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 隐患点导出对象
 */
@Setter
@Getter
public class HazardPointExportVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Excel(name = "隐患点编号")
    private String code;

    @Excel(name = "隐患点名称")
    private String name;

    @Excel(name = "分组名称")
    private String groupName;

    @Excel(name = "中心经度", scale = 6)
    private BigDecimal longitude;

    @Excel(name = "中心纬度", scale = 6)
    private BigDecimal latitude;

    @Excel(name = "走向角度", scale = 2)
    private BigDecimal strike;

    @Excel(name = "隐患描述")
    private String description;

    @Excel(name = "状态")
    private String statusName;

    @Excel(name = "绑定设备数量")
    private Integer deviceCount;

    @Excel(name = "创建人")
    private String createBy;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Excel(name = "更新人")
    private String updateBy;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
