package com.zwei.monitor.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 监测点位(测站点)对象 zw_biz_monitoring_point
 * 
 * @author zwei
 * @date 2025-10-15
 */
@ApiModel("监测点位(测站点)对象")
public class PointDeviceMapping
{
    private static final long serialVersionUID = 1L;
    /** 测点ID */
    @ApiModelProperty("测点ID")
    private Long pointId;
    /** 设备ID */
    @ApiModelProperty("设备ID")
    private Long deviceId;
    
}