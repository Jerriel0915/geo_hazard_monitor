package com.zwei.iot.video.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 视频设备表 video_device
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class VideoDevice extends BaseEntity {
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
     * 图标代码
     */
    private String icon;

    /**
     * 图标路径
     */
    private String iconPath;

    /**
     * 协议类型编码
     */
    private String protocolCode;

    /**
     * 协议类型名称
     */
    private String protocolName;

    /**
     * 视频流地址
     */
    private String streamUrl;

    /**
     * 状态: 0-离线, 1-在线, 2-故障
     */
    private Integer status;

    /**
     * 最近在线时间
     */
    private String lastOnlineTime;

    /**
     * 安装时间
     */
    private String installTime;

    /**
     * 删除标记: 0-正常, 1-删除
     */
    private Integer delFlag;

    @Override
    public String toString() {
        return "VideoDevice{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", icon='" + icon + '\'' +
                ", iconPath='" + iconPath + '\'' +
                ", protocolCode='" + protocolCode + '\'' +
                ", protocolName='" + protocolName + '\'' +
                ", streamUrl='" + streamUrl + '\'' +
                ", status=" + status +
                ", lastOnlineTime='" + lastOnlineTime + '\'' +
                ", installTime='" + installTime + '\'' +
                ", delFlag=" + delFlag +
                '}';
    }
}