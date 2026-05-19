package com.zwei.iot.monitor.service.impl;

import com.zwei.iot.cache.config.CacheWarmupTaskRegistry;
import com.zwei.iot.cache.service.IotCacheService;
import com.zwei.iot.cache.warmup.MonitorTypeWarmupTask;
import com.zwei.iot.monitor.domain.MonitorType;
import com.zwei.iot.monitor.mapper.MonitorTypeMapper;
import com.zwei.iot.monitor.service.IMonitorTypeService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 监测类型Service实现类
 * <p>
 * 提供监测类型的管理操作实现，包括查询、新增、修改、删除等。
 *
 * @author zwei
 */
@Service
public class MonitorTypeServiceImpl implements IMonitorTypeService {
    /**
     * 注入监测类型Mapper
     */
    private final MonitorTypeMapper monitorTypeMapper;
    private final IotCacheService cacheService;
    private final CacheWarmupTaskRegistry registry;

    @Autowired
    public MonitorTypeServiceImpl(MonitorTypeMapper monitorTypeMapper, IotCacheService cacheService,
                                  CacheWarmupTaskRegistry registry) {
        this.monitorTypeMapper = monitorTypeMapper;
        this.cacheService = cacheService;
        this.registry = registry;
    }

    @PostConstruct
    public void init() {
        registry.registerTask(new MonitorTypeWarmupTask(this, cacheService));
    }

    /**
     * 分页查询监测类型列表
     *
     * @param monitorType 查询条件
     * @param pageNum     页码
     * @param pageSize    每页数量
     * @return 分页结果
     */
    @Override
    public List<MonitorType> selectMonitorTypePage(MonitorType monitorType, int pageNum, int pageSize) {
        return monitorTypeMapper.selectMonitorTypeList(monitorType);
    }

    /**
     * 查询所有监测类型列表（不分页）
     *
     * @return 所有监测类型列表
     */
    @Override
    public List<MonitorType> selectMonitorTypeAll() {
        return monitorTypeMapper.selectMonitorTypeAll();
    }

    /**
     * 根据ID查询监测类型详情
     *
     * @param id 监测类型ID
     * @return 监测类型详情
     */
    @Override
    public MonitorType selectMonitorTypeById(Long id) {
        // 先尝试从缓存获取
        MonitorType cached = cacheService.getMonitorType(id);
        if (cached != null) {
            return cached;
        }
        // 缓存未命中，查询数据库并缓存
        MonitorType monitorType = monitorTypeMapper.selectMonitorTypeById(id);
        if (monitorType != null) {
            cacheService.cacheMonitorType(monitorType);
        }
        return monitorType;
    }

    /**
     * 根据编码查询监测类型
     *
     * @param code 监测类型编码
     * @return 监测类型详情
     */
    @Override
    public MonitorType selectMonitorTypeByCode(String code) {
        return monitorTypeMapper.selectMonitorTypeByCode(code);
    }

    /**
     * 新增监测类型
     *
     * @param monitorType 监测类型信息
     * @return 影响行数
     */
    @Override
    public int insertMonitorType(MonitorType monitorType) {
        int result = monitorTypeMapper.insertMonitorType(monitorType);
        if (result > 0 && monitorType.getId() != null) {
            cacheService.cacheMonitorType(monitorType);
        }
        return result;
    }

    /**
     * 修改监测类型
     *
     * @param monitorType 监测类型信息
     * @return 影响行数
     */
    @Override
    public int updateMonitorType(MonitorType monitorType) {
        int result = monitorTypeMapper.updateMonitorType(monitorType);
        if (result > 0 && monitorType.getId() != null) {
            cacheService.evictMonitorType(monitorType.getId());
        }
        return result;
    }

    /**
     * 删除监测类型（逻辑删除）
     *
     * @param id 监测类型ID
     * @return 影响行数
     */
    @Override
    public int deleteMonitorTypeById(Long id) {
        int result = monitorTypeMapper.deleteMonitorTypeById(id);
        if (result > 0) {
            cacheService.evictMonitorType(id);
        }
        return result;
    }

    /**
     * 批量删除监测类型（逻辑删除）
     *
     * @param ids 需要删除的监测类型ID数组
     * @return 影响行数
     */
    @Override
    public int deleteMonitorTypeByIds(Long[] ids) {
        int result = monitorTypeMapper.deleteMonitorTypeByIds(ids);
        if (result > 0) {
            cacheService.evictMonitorTypeList(ids);
        }
        return result;
    }

    /**
     * 校验监测类型编码是否唯一
     *
     * @param monitorType 监测类型信息
     * @return true-唯一，false-已存在
     */
    @Override
    public boolean checkMonitorTypeCodeUnique(MonitorType monitorType) {
        Long id = monitorType.getId() == null ? 0L : monitorType.getId();
        MonitorType exist = monitorTypeMapper.checkMonitorTypeCodeUnique(monitorType.getCode(), id);
        return exist == null;
    }
}
