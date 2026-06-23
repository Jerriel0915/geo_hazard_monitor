package com.zwei.iot.device.mapper;

import com.zwei.iot.device.domain.MonitorStat;
import org.apache.ibatis.annotations.*;

/**
 * monitor_stats 表 Mapper — 持久化累计计数等统计数据。
 */
@Mapper
public interface MonitorStatMapper {

    @Select("SELECT id, stat_key, stat_value, update_time FROM monitor_stats WHERE stat_key = #{statKey}")
    MonitorStat selectByKey(@Param("statKey") String statKey);

    @Update("UPDATE monitor_stats SET stat_value = #{statValue} WHERE stat_key = #{statKey}")
    int updateValue(@Param("statKey") String statKey, @Param("statValue") Long statValue);

    @Insert("INSERT INTO monitor_stats (stat_key, stat_value) VALUES (#{statKey}, #{statValue})")
    int insert(@Param("statKey") String statKey, @Param("statValue") Long statValue);
}
