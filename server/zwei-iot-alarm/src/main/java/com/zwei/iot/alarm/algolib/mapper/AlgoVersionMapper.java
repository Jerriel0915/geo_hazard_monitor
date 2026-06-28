package com.zwei.iot.alarm.algolib.mapper;

import com.zwei.iot.alarm.algolib.domain.AlgoVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 算法版本 Mapper。
 *
 * @author zwei
 */
@Mapper
public interface AlgoVersionMapper {

    /**
     * 按算法 ID 查询未删除版本（按 create_time DESC）
     */
    List<AlgoVersion> selectByAlgoId(Long algoId);

    AlgoVersion selectById(Long id);

    /**
     * 校验版本号在指定算法下唯一（排除逻辑删除记录）
     */
    AlgoVersion checkVersionUnique(@Param("algoId") Long algoId,
                                   @Param("versionNo") String versionNo);

    int insert(AlgoVersion version);

    /**
     * 逻辑删除指定算法 ID 下所有版本（删除算法时级联调用）
     */
    int softDeleteByAlgoId(Long algoId);

    /**
     * 逻辑删除单个版本
     */
    int softDeleteById(Long id);

    AlgoVersion selectByAlgoIdAndVersionNo(@Param("algoId") Long algoId,
                                            @Param("versionNo") String versionNo);

    AlgoVersion selectLatestByAlgoId(Long algoId);
}
