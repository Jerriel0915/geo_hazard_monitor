package com.zwei.iot.hazardpoint.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * 隐患点对象 hazard_point
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
public class HazardPoint extends BaseEntity
{
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 隐患点编号 */
    private String code;

    /** 隐患点名称 */
    private String name;

    /** 分组ID */
    private Long groupId;

    /** 分组名称 */
    private String groupName;

    /** 中心经度 */
    private BigDecimal longitude;

    /** 中心纬度 */
    private BigDecimal latitude;

    /** 走向角度 */
    private BigDecimal strike;

    /** 隐患描述 */
    private String description;

    /** 状态: 1-监测中, 2-停测中, 3-已完结 */
    private Integer status;

    /** 状态名称 */
    private String statusName;

    /** 绑定设备数量 */
    private Integer deviceCount;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    public HazardPoint()
    {

    }

    public HazardPoint(Long id)
    {
        this.id = id;
    }

    @Override
    public String toString()
    {
        return "HazardPoint{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", groupId=" + groupId +
                ", groupName='" + groupName + '\'' +
                ", longitude=" + longitude +
                ", latitude=" + latitude +
                ", strike=" + strike +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", statusName='" + statusName + '\'' +
                ", deviceCount=" + deviceCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        HazardPoint that = (HazardPoint) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getCode(), that.getCode()) && Objects.equals(getName(), that.getName()) && Objects.equals(getGroupId(), that.getGroupId()) && Objects.equals(getGroupName(), that.getGroupName()) && Objects.equals(getLongitude(), that.getLongitude()) && Objects.equals(getLatitude(), that.getLatitude()) && Objects.equals(getStrike(), that.getStrike()) && Objects.equals(getDescription(), that.getDescription()) && Objects.equals(getStatus(), that.getStatus()) && Objects.equals(getStatusName(), that.getStatusName()) && Objects.equals(getDeviceCount(), that.getDeviceCount()) && Objects.equals(getDelFlag(), that.getDelFlag());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getCode(), getName(), getGroupId(), getGroupName(), getLongitude(), getLatitude(), getStrike(), getDescription(), getStatus(), getStatusName(), getDeviceCount(), getDelFlag());
    }
}
