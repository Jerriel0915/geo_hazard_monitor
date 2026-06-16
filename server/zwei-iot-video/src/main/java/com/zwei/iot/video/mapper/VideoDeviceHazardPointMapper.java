package com.zwei.iot.video.mapper;

import com.zwei.iot.video.domain.VideoDeviceHazardPoint;
import com.zwei.iot.video.domain.BoundVideoDeviceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 视频设备隐患点关联Mapper接口
 */
@Mapper
public interface VideoDeviceHazardPointMapper {

    /**
     * 查询已绑定视频设备列表
     *
     * @param hazardPointId 隐患点ID
     * @return 已绑定视频设备列表
     */
    List<BoundVideoDeviceVO> selectBoundVideoDevicesByHazardPointId(@Param("hazardPointId") Long hazardPointId);

    /**
     * 批量插入绑定记录
     *
     * @param bindList 绑定记录
     * @return 影响行数
     */
    int insertBatch(@Param("list") List<VideoDeviceHazardPoint> bindList);

    /**
     * 根据隐患点ID和视频设备ID列表删除绑定记录
     *
     * @param hazardPointId 隐患点ID
     * @param videoDeviceIds 视频设备ID列表
     * @return 影响行数
     */
    int deleteByVideoDeviceIdsAndHazardPointId(@Param("hazardPointId") Long hazardPointId,
                                               @Param("videoDeviceIds") List<Long> videoDeviceIds);

    /**
     * 根据视频设备ID列表删除绑定记录
     *
     * @param videoDeviceIds 视频设备ID列表
     * @return 影响行数
     */
    int deleteByVideoDeviceIds(@Param("videoDeviceIds") List<Long> videoDeviceIds);

    /**
     * 根据视频设备ID列表查询关联的隐患点ID
     *
     * @param videoDeviceIds 视频设备ID列表
     * @return 隐患点ID列表
     */
    List<Long> selectHazardPointIdsByVideoDeviceIds(@Param("videoDeviceIds") List<Long> videoDeviceIds);
}
