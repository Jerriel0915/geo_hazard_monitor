package com.zwei.iot.report.mapper;

import com.zwei.iot.report.domain.ReportRecord;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReportRecordMapper {

    int insert(ReportRecord record);

    int updateStatusAndContent(@Param("id") Long id,
                                @Param("status") Integer status,
                                @Param("content") String content,
                                @Param("errorMsg") String errorMsg);

    int updateDeleteFlag(@Param("id") Long id, @Param("delFlag") Integer delFlag);

    ReportRecord selectById(@Param("id") Long id);

    /** 列表查询 (不含 content 字段,避免大字段传输) */
    List<ReportRecord> selectPageList(@Param("type") Integer type,
                                       @Param("hazardPointId") Long hazardPointId,
                                       @Param("periodStart") LocalDate periodStart,
                                       @Param("periodEnd") LocalDate periodEnd,
                                       @Param("status") Integer status,
                                       @Param("keyword") String keyword);

    long countPageList(@Param("type") Integer type,
                       @Param("hazardPointId") Long hazardPointId,
                       @Param("periodStart") LocalDate periodStart,
                       @Param("periodEnd") LocalDate periodEnd,
                       @Param("status") Integer status,
                       @Param("keyword") String keyword);

    /** 幂等检查: 查询同 type+hp+period 的成功记录 */
    ReportRecord selectExistingSuccess(@Param("type") Integer type,
                                        @Param("hazardPointId") Long hazardPointId,
                                        @Param("periodStart") LocalDate periodStart,
                                        @Param("periodEnd") LocalDate periodEnd);
}
