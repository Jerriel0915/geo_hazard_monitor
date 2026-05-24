package com.zwei.iot.hazardpoint.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 绑定设备请求参数
 *
 * @author zwei
 */
@Setter
@Getter
public class BindDeviceRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备ID列表 */
    private List<Long> deviceIds;

    /** 安装位置信息列表 */
    private List<InstallPosition> installPositions;

}
