package com.zwei.iot.monitor.service.impl;

import com.zwei.iot.monitor.domain.MonitorCategory;
import com.zwei.iot.monitor.mapper.MonitorCategoryMapper;
import com.zwei.iot.monitor.service.IMonitorCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MonitorCategoryServiceImpl implements IMonitorCategoryService {
    private final MonitorCategoryMapper mapper;

    @Autowired
    public MonitorCategoryServiceImpl(MonitorCategoryMapper mapper) { this.mapper = mapper; }

    @Override public List<MonitorCategory> selectMonitorCategoryPage(MonitorCategory m, int pn, int ps) { return mapper.selectMonitorCategoryList(m); }
    @Override @Cacheable(value = "monitorCategoryList", key = "'all'") public List<MonitorCategory> selectMonitorCategoryAll() { return mapper.selectMonitorCategoryAll(); }
    @Override @Cacheable(value = "monitorCategory", key = "#id") public MonitorCategory selectMonitorCategoryById(Long id) { return mapper.selectMonitorCategoryById(id); }
    @Override public MonitorCategory selectMonitorCategoryByCode(String code) { return mapper.selectMonitorCategoryByCode(code); }
    @Override @Caching(evict = { @CacheEvict(value = "monitorCategory", key = "#m.id"), @CacheEvict(value = "monitorCategoryList", allEntries = true) }) public int insertMonitorCategory(MonitorCategory m) { return mapper.insertMonitorCategory(m); }
    @Override @Caching(evict = { @CacheEvict(value = "monitorCategory", key = "#m.id"), @CacheEvict(value = "monitorCategoryList", allEntries = true) }) public int updateMonitorCategory(MonitorCategory m) { return mapper.updateMonitorCategory(m); }
    @Override @Caching(evict = { @CacheEvict(value = "monitorCategory", key = "#id"), @CacheEvict(value = "monitorCategoryList", allEntries = true) }) public int deleteMonitorCategoryById(Long id) { return mapper.deleteMonitorCategoryById(id); }
    @Override @Caching(evict = { @CacheEvict(value = "monitorCategory", allEntries = true), @CacheEvict(value = "monitorCategoryList", allEntries = true) }) public int deleteMonitorCategoryByIds(Long[] ids) { return mapper.deleteMonitorCategoryByIds(ids); }
    @Override public boolean checkMonitorCategoryCodeUnique(MonitorCategory m) { return mapper.checkMonitorCategoryCodeUnique(m.getCode(), m.getId() == null ? 0L : m.getId()) == null; }
}
