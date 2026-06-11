package com.zwei.iot.device.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Product extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 产品唯一标识 */
    private String productKey;

    /** 关联设备ID */
    private Long deviceId;

    /** 物模型JSON（完整TSL定义） */
    private String tslJson;

    /** 物模型版本号 */
    private String tslVersion;

    /** 删除标记:0-正常,1-已删除 */
    private Integer delFlag;
}
