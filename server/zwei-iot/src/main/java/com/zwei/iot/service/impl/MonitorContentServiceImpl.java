package com.zwei.iot.service.impl;

import com.zwei.iot.config.CacheWarmupTaskRegistry;
import com.zwei.iot.domain.MonitorContent;
import com.zwei.iot.mapper.MonitorContentMapper;
import com.zwei.iot.service.IMonitorContentService;
import com.zwei.iot.service.IotCacheService;
import com.zwei.iot.warmup.MonitorContentWarmupTask;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 监测内容Service实现类
 * <p>
 * 提供监测内容的管理操作实现，包括查询、新增、修改、删除等。
 *
 * @author zwei
 */
@Service
public class MonitorContentServiceImpl implements IMonitorContentService {

    /**
     * 注入监测内容Mapper
     */
    private final MonitorContentMapper monitorContentMapper;
    private final IotCacheService cacheService;
    private final CacheWarmupTaskRegistry registry;

    @Autowired
    public MonitorContentServiceImpl(MonitorContentMapper monitorContentMapper, IotCacheService cacheService,
                                     CacheWarmupTaskRegistry registry) {
        this.monitorContentMapper = monitorContentMapper;
        this.cacheService = cacheService;
        this.registry = registry;
    }

    @PostConstruct
    public void init() {
        registry.registerTask(new MonitorContentWarmupTask(this, cacheService));
    }

    /**
     * 查询监测内容列表
     *
     * @param monitorContent 查询条件
     * @return 监测内容列表
     */
    @Override
    public List<MonitorContent> selectMonitorContentList(MonitorContent monitorContent) {
        return monitorContentMapper.selectMonitorContentList(monitorContent);
    }

    /**
     * 查询所有监测内容列表（不分页）
     *
     * @param monitorTypeId 监测类型ID（可选）
     * @return 监测内容列表
     */
    @Override
    public List<MonitorContent> selectMonitorContentAll(Long monitorTypeId) {
        return monitorContentMapper.selectMonitorContentAll(monitorTypeId);
    }

    /**
     * 根据ID查询监测内容详情
     *
     * @param id 监测内容ID
     * @return 监测内容详情
     */
    @Override
    public MonitorContent selectMonitorContentById(Long id) {
        // 先尝试从缓存获取
        MonitorContent cached = cacheService.getMonitorContent(id);
        if (cached != null) {
            return cached;
        }
        // 缓存未命中，查询数据库并缓存
        MonitorContent content = monitorContentMapper.selectMonitorContentById(id);
        if (content != null) {
            cacheService.cacheMonitorContent(content);
        }
        return content;
    }

    /**
     * 根据编码查询监测内容
     *
     * @param code 监测内容编码
     * @return 监测内容详情
     */
    @Override
    public MonitorContent selectMonitorContentByCode(String code) {
        return monitorContentMapper.selectMonitorContentByCode(code);
    }

    /**
     * 新增监测内容
     *
     * @param monitorContent 监测内容信息
     * @return 影响行数
     */
    @Override
    public int insertMonitorContent(MonitorContent monitorContent) {
        int result = monitorContentMapper.insertMonitorContent(monitorContent);
        if (result > 0 && monitorContent.getId() != null) {
            cacheService.cacheMonitorContent(monitorContent);
        }
        return result;
    }

    /**
     * 修改监测内容
     *
     * @param monitorContent 监测内容信息
     * @return 影响行数
     */
    @Override
    public int updateMonitorContent(MonitorContent monitorContent) {
        int result = monitorContentMapper.updateMonitorContent(monitorContent);
        if (result > 0 && monitorContent.getId() != null) {
            cacheService.evictMonitorContent(monitorContent.getId());
        }
        return result;
    }

    /**
     * 删除监测内容（逻辑删除）
     *
     * @param id 监测内容ID
     * @return 影响行数
     */
    @Override
    public int deleteMonitorContentById(Long id) {
        int result = monitorContentMapper.deleteMonitorContentById(id);
        if (result > 0) {
            cacheService.evictMonitorContent(id);
        }
        return result;
    }

    /**
     * 批量删除监测内容（逻辑删除）
     *
     * @param ids 需要删除的监测内容ID数组
     * @return 影响行数
     */
    @Override
    public int deleteMonitorContentByIds(Long[] ids) {
        int result = monitorContentMapper.deleteMonitorContentByIds(ids);
        if (result > 0) {
            cacheService.evictMonitorContentList(ids);
        }
        return result;
    }

    /**
     * 校验监测内容编码是否唯一
     *
     * @param monitorContent 监测内容信息
     * @return true-唯一，false-已存在
     */
    @Override
    public boolean checkMonitorContentCodeUnique(MonitorContent monitorContent) {
        Long id = monitorContent.getId() == null ? 0L : monitorContent.getId();
        MonitorContent exist = monitorContentMapper.checkMonitorContentCodeUnique(monitorContent.getCode(), id);
        return exist == null;
    }
}