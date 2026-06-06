package com.zwei.iot.monitor.mapper;

import com.zwei.iot.monitor.domain.MonitorCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface MonitorCategoryMapper {
    List<MonitorCategory> selectMonitorCategoryList(MonitorCategory monitorCategory);
    List<MonitorCategory> selectMonitorCategoryAll();
    MonitorCategory selectMonitorCategoryById(Long id);
    MonitorCategory selectMonitorCategoryByCode(String code);
    int insertMonitorCategory(MonitorCategory monitorCategory);
    int updateMonitorCategory(MonitorCategory monitorCategory);
    int deleteMonitorCategoryById(Long id);
    int deleteMonitorCategoryByIds(Long[] ids);
    MonitorCategory checkMonitorCategoryCodeUnique(@Param("code") String code, @Param("id") Long id);
}
