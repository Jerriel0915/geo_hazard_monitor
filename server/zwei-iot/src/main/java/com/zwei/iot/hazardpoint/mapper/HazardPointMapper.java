package com.zwei.iot.hazardpoint.mapper;

import com.zwei.iot.hazardpoint.domain.HazardPoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 隐患点Mapper接口
 *
 * @author zwei
 */
@Mapper
public interface HazardPointMapper
{
    /**
     * 根据条件分页查询隐患点列表
     *
     * @param hazardPoint 隐患点信息
     * @return 隐患点集合
     */
    public List<HazardPoint> selectHazardPointList(HazardPoint hazardPoint);

    /**
     * 根据ID查询隐患点
     *
     * @param id 隐患点ID
     * @return 隐患点信息
     */
    public HazardPoint selectHazardPointById(Long id);

    /**
     * 根据编号查询隐患点
     *
     * @param code 隐患点编号
     * @return 隐患点信息
     */
    public HazardPoint selectHazardPointByCode(String code);

    /**
     * 新增隐患点
     *
     * @param hazardPoint 隐患点信息
     * @return 结果
     */
    public int insertHazardPoint(HazardPoint hazardPoint);

    /**
     * 修改隐患点
     *
     * @param hazardPoint 隐患点信息
     * @return 结果
     */
    public int updateHazardPoint(HazardPoint hazardPoint);

    /**
     * 删除隐患点
     *
     * @param id 隐患点ID
     * @return 结果
     */
    public int deleteHazardPointById(Long id);

    /**
     * 批量删除隐患点
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteHazardPointByIds(Long[] ids);

    /**
     * 校验隐患点编号是否唯一
     *
     * @param code 隐患点编号
     * @return 结果
     */
    public HazardPoint checkHazardPointCodeUnique(String code);

    /**
     * 批量更新隐患点状态
     *
     * @param ids 隐患点ID列表
     * @param status 状态
     * @return 结果
     */
    int batchUpdateHazardPointStatus(@Param("ids") List<Long> ids, @Param("status") Integer status);

    /**
     * 更新隐患点状态（单条）
     *
     * @param id 隐患点ID
     * @param status 状态
     * @return 结果
     */
    int updateHazardPointStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 刷新隐患点绑定设备数量
     *
     * @param id 隐患点ID
     * @return 结果
     */
    int refreshDeviceCountById(@Param("id") Long id);

    // ==================== 统计查询 ====================

    int countAll();

    List<java.util.Map<String, Object>> countByStatus();

    List<java.util.Map<String, Object>> countByMonth(@Param("months") int months);
}
