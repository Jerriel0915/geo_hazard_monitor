package com.zwei.iot.device.mapper;

import com.zwei.iot.device.domain.Device;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备Mapper接口
 *
 * @author zwei
 */
@Mapper
public interface DeviceMapper {
    /**
     * 分页查询设备列表
     *
     * @param device 设备查询条件
     * @return 设备列表
     */
    List<Device> selectDeviceList(Device device);

    /**
     * 查询所有设备列表（不分页）
     *
     * @return 所有设备列表
     */
    List<Device> selectDeviceAll();

    /**
     * 根据ID查询设备详情
     *
     * @param id 设备ID
     * @return 设备详情
     */
    Device selectDeviceById(Long id);

    /**
     * 根据编码查询设备
     *
     * @param code 设备编码
     * @return 设备详情
     */
    Device selectDeviceByCode(String code);

    /**
     * 根据SN查询设备
     *
     * @param sn 设备SN
     * @return 设备详情
     */
    Device selectDeviceBySn(String sn);

    /**
     * 根据接入用户名查询设备
     *
     * @param authUsername 设备接入用户名
     * @return 设备详情
     */
    Device selectDeviceByAuthUsername(String authUsername);

    /**
     * 新增设备
     *
     * @param device 设备信息
     * @return 影响行数
     */
    int insertDevice(Device device);

    /**
     * 修改设备
     *
     * @param device 设备信息
     * @return 影响行数
     */
    int updateDevice(Device device);

    /**
     * 删除设备（逻辑删除）
     *
     * @param id 设备ID
     * @return 影响行数
     */
    int deleteDeviceById(Long id);

    /**
     * 批量删除设备（逻辑删除）
     *
     * @param ids 需要删除的设备ID数组
     * @return 影响行数
     */
    int deleteDeviceByIds(Long[] ids);

    /**
     * 校验设备编码是否唯一
     *
     * @param code 设备编码
     * @param id   排除的设备ID（更新时使用）
     * @return 设备信息（null表示唯一）
     */
    Device checkDeviceCodeUnique(@Param("code") String code, @Param("id") Long id);
}
