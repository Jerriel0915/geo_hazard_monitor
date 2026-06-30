package com.zwei.iot.device.service;

import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.dto.DeviceCopyRequest;
import com.zwei.iot.device.domain.dto.DeviceCreateRequest;
import com.zwei.iot.device.domain.dto.DeviceUpdateRequest;

import java.util.List;

/**
 * 设备Service接口
 *
 * @author zwei
 */
public interface IDeviceService {
    /**
     * 分页查询设备列表
     *
     * @param device   设备查询条件
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 设备列表
     */
    List<Device> selectDevicePage(Device device, int pageNum, int pageSize);

    /**
     * 查询所有设备列表
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
     * 后台新增设备并自动生成接入账号
     *
     * @param request  设备创建请求
     * @param operator 操作人
     * @return 已创建设备
     */
    Device createDevice(DeviceCreateRequest request, String operator);

    /**
     * 更新设备基础信息
     *
     * @param id       设备ID
     * @param request  更新请求
     * @param operator 操作人
     * @return 更新后的设备
     */
    Device updateDevice(Long id, DeviceUpdateRequest request, String operator);

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
     * 复制设备
     *
     * @param id 设备ID
     * @param request 复制请求（含新设备编号、名称）
     * @return 新设备ID
     */
    Long copyDevice(Long id, DeviceCopyRequest request);

    /**
     * 校验设备编码是否唯一
     *
     * @param device 设备信息
     * @return true-唯一，false-已存在
     */
    boolean checkDeviceCodeUnique(Device device);

    /**
     * 获取设备传感器列表
     *
     * @param deviceId 设备ID
     * @return 传感器列表
     */
    List<DeviceSensor> selectSensorListByDeviceId(Long deviceId);

    /**
     * 批量获取多个设备的传感器列表（含属性，避免 N+1）。
     *
     * @param deviceIds 设备ID列表
     * @return 所有设备下的传感器列表（每个传感器已填充 attrList）
     */
    List<DeviceSensor> selectSensorListByDeviceIds(List<Long> deviceIds);

    /**
     * 查询设备账号信息
     *
     * @param deviceId 设备ID
     * @return 设备信息
     */
    Device getDeviceAuthAccount(Long deviceId, String operator, String clientIp);

    /**
     * 重置设备密码
     *
     * @param deviceId  设备ID
     * @param operator  操作人
     * @param resetReason 重置原因
     * @return 更新后的设备
     */
    Device resetDeviceAuthPassword(Long deviceId, String operator, String resetReason, Boolean forceOffline, String clientIp);

    /**
     * 变更设备账号状态
     *
     * @param deviceId   设备ID
     * @param authStatus 账号状态
     * @param operator   操作人
     * @param reason     变更原因
     * @return 更新后的设备
     */
    Device changeDeviceAuthStatus(Long deviceId, Integer authStatus, String operator, String reason, String clientIp);

    /**
     * 设备维修状态操作（报修/修复/停用/启用）。
     * <p>在一个事务中完成状态变更 + 维修日志记录，保证原子性。</p>
     *
     * @param deviceId      设备ID
     * @param operationType 操作类型：1=报修, 2=修复, 3=停用, 4=启用
     * @param operatorName  操作人姓名
     * @param operatorPhone 联系电话
     * @param operationDate 操作日期（yyyy-MM-dd HH:mm:ss）
     * @param description   操作描述
     * @param createBy      系统操作人（登录用户）
     * @return 操作结果文本
     */
    String maintenanceDevice(Long deviceId, Integer operationType, String operatorName, String operatorPhone,
                             String operationDate, String description, String createBy);
}
