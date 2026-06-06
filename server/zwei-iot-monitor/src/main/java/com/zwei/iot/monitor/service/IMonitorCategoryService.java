package com.zwei.iot.monitor.service;

import com.zwei.iot.monitor.domain.MonitorCategory;
import java.util.List;

public interface IMonitorCategoryService {
    List<MonitorCategory> selectMonitorCategoryPage(MonitorCategory monitorCategory, int pageNum, int pageSize);
    List<MonitorCategory> selectMonitorCategoryAll();
    MonitorCategory selectMonitorCategoryById(Long id);
    MonitorCategory selectMonitorCategoryByCode(String code);
    int insertMonitorCategory(MonitorCategory monitorCategory);
    int updateMonitorCategory(MonitorCategory monitorCategory);
    int deleteMonitorCategoryById(Long id);
    int deleteMonitorCategoryByIds(Long[] ids);
    boolean checkMonitorCategoryCodeUnique(MonitorCategory monitorCategory);
}
