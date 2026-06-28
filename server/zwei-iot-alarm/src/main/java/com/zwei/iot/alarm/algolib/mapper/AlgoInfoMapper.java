package com.zwei.iot.alarm.algolib.mapper;

import com.zwei.iot.alarm.algolib.domain.AlgoInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 算法信息 Mapper。
 *
 * @author zwei
 */
@Mapper
public interface AlgoInfoMapper {

    /**
     * 分页查询（含版本统计、最新版本号、最新上传时间）
     */
    List<AlgoInfo> selectList(AlgoInfo query);

    /**
     * 详情（不含版本统计字段，由 Service 二次查询版本填充）
     */
    AlgoInfo selectById(Long id);

    /**
     * 校验 code 唯一（排除指定 id 与已逻辑删除记录）
     *
     * @return 命中的算法（null 表示唯一）
     */
    AlgoInfo checkCodeUnique(@Param("code") String code, @Param("id") Long id);

    AlgoInfo selectByCode(@Param("code") String code);

    int insert(AlgoInfo algoInfo);

    int update(AlgoInfo algoInfo);

    /**
     * 逻辑删除算法（del_flag=1）
     */
    int softDelete(Long id);
}
