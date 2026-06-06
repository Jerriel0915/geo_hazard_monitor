package com.zwei.iot.hazardpoint.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 隐患点分组表 hazard_point_group
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class HazardPointGroup extends BaseEntity
{
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 分组编码 */
    private String code;

    /** 分组名称 */
    private String name;

    /** 分组描述 */
    private String description;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态: 0-禁用, 1-启用 */
    private Integer status;

    /** 删除标记: 0-正常, 1-删除 */
    private Integer delFlag;

    /** 隐患点数量（查询时返回） */
    private Integer count;

    @Override
    public String toString()
    {
        return "HazardPointGroup{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", sortOrder=" + sortOrder +
                ", status=" + status +
                ", delFlag=" + delFlag +
                ", count=" + count +
                '}';
    }
}
