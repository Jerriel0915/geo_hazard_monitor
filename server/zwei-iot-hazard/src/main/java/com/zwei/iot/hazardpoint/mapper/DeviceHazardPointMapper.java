package com.zwei.iot.hazardpoint.mapper;

import com.zwei.iot.device.domain.brief.DeviceBrief;
import com.zwei.iot.hazardpoint.domain.DeviceHazardPoint;
import com.zwei.iot.hazardpoint.domain.dto.BoundDeviceVO;
import com.zwei.iot.hazardpoint.domain.dto.UnboundDeviceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备隐患点关联Mapper接口
 *
 * @author zwei
 */
@Mapper
public interface DeviceHazardPointMapper {

    /**
     * 查询已绑定设备列表
     *
     * @param hazardPointId 隐患点ID
     * @return 已绑定设备列表
     */
    List<BoundDeviceVO> selectBoundDevicesByHazardPointId(@Param("hazardPointId") Long hazardPointId);

    /**
     * 查询未绑定设备列表
     *
     * @param hazardPointId 隐患点ID
     * @param keyword        关键词（设备/传感器名称模糊查询）
     * @return 未绑定设备列表
     */
    List<UnboundDeviceVO> selectUnboundDevices(@Param("hazardPointId") Long hazardPointId, @Param("keyword") String keyword);

    /**
     * 批量插入设备隐患点绑定记录
     *
     * @param bindList 绑定记录列表
     * @return 影响行数
     */
    int insertBatch(@Param("list") List<DeviceHazardPoint> bindList);

    /**
     * 批量插入或更新设备隐患点绑定记录（基于唯一键 uk_device_hazard_point 幂等）。
     * 已存在时仅更新安装位置和更新者信息，保留原创建时间。
     *
     * @param bindList 绑定记录列表
     * @return 影响行数
     */
    int insertOrUpdate(@Param("list") List<DeviceHazardPoint> bindList);

    /**
     * 根据隐患点ID删除绑定记录
     *
     * @param hazardPointId 隐患点ID
     * @return 影响行数
     */
    int deleteByHazardPointId(@Param("hazardPointId") Long hazardPointId);

    /**
     * 根据隐患点ID和设备ID列表删除绑定记录
     *
     * @param hazardPointId 隐患点ID
     * @param deviceIds     设备ID列表
     * @return 影响行数
     */
    int deleteByDeviceIdsAndHazardPointId(@Param("hazardPointId") Long hazardPointId, @Param("deviceIds") List<Long> deviceIds);

    /**
     * 根据设备ID列表删除绑定记录
     *
     * @param deviceIds 设备ID列表
     * @return 影响行数
     */
    int deleteByDeviceIds(@Param("deviceIds") List<Long> deviceIds);

    /**
     * 根据设备ID列表查询受影响的隐患点ID
     *
     * @param deviceIds 设备ID列表
     * @return 隐患点ID列表
     */
    List<Long> selectHazardPointIdsByDeviceIds(@Param("deviceIds") List<Long> deviceIds);

    /**
     * 查询隐患点已绑定的设备ID列表
     *
     * @param hazardPointId 隐患点ID
     * @return 设备ID列表
     */
    List<Long> selectDeviceIdsByHazardPointId(@Param("hazardPointId") Long hazardPointId);

    /**
     * 查询设备绑定次数
     *
     * @param deviceId 设备ID
     * @return 绑定次数
     */
    int countByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * 查询隐患点绑定的设备摘要 (含在线状态与传感器数, 供 report 模块消费)。
     *
     * @param hazardPointId 隐患点ID
     * @return 设备摘要列表
     */
    List<DeviceBrief> selectDeviceBriefByHazardPoint(@Param("hazardPointId") Long hazardPointId);
}
