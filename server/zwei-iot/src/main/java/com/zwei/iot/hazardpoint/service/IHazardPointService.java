package com.zwei.iot.hazardpoint.service;

import com.zwei.iot.hazardpoint.domain.HazardPoint;

import java.util.List;

/**
 * 隐患点Service接口
 *
 * @author zwei
 */
public interface IHazardPointService
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
    public boolean checkHazardPointCodeUnique(String code);

    /**
     * 停测/恢复隐患点
     *
     * @param id 隐患点ID
     * @param pause true-停测, false-恢复
     * @return 结果
     */
    public int updateHazardPointPause(Long id, boolean pause);

    /**
     * 完结隐患点
     *
     * @param id 隐患点ID
     * @return 结果
     */
    public int completeHazardPoint(Long id);

    /**
     * 批量操作隐患点(停测/恢复/完结)
     *
     * @param ids 隐患点ID数组
     * @param operation 操作类型: pause/resume/complete
     * @return 结果
     */
    public int batchOperateHazardPoint(Long[] ids, String operation);
}
