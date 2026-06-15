package com.zwei.iot.monitor.service.impl;

import com.zwei.iot.monitor.domain.MonitorContent;
import com.zwei.iot.monitor.mapper.MonitorContentMapper;
import com.zwei.iot.monitor.service.IMonitorContentService;
import com.zwei.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 监测内容Service实现类
 *
 * @author zwei
 */
@Service
public class MonitorContentServiceImpl implements IMonitorContentService {

    private final MonitorContentMapper monitorContentMapper;

    @Autowired
    public MonitorContentServiceImpl(MonitorContentMapper monitorContentMapper) {
        this.monitorContentMapper = monitorContentMapper;
    }

    /**
     * 查询监测内容列表
     */
    @Override
    public List<MonitorContent> selectMonitorContentList(MonitorContent monitorContent) {
        return monitorContentMapper.selectMonitorContentList(monitorContent);
    }

    /**
     * 查询所有监测内容列表（不分页）
     */
    @Override
    @Cacheable(value = "monitorContentList", key = "'all:' + (#monitorTypeId == null ? 'ALL' : #monitorTypeId)")
    public List<MonitorContent> selectMonitorContentAll(Long monitorTypeId) {
        return monitorContentMapper.selectMonitorContentAll(monitorTypeId);
    }

    /**
     * 根据ID查询监测内容详情
     */
    @Override
    @Cacheable(value = "monitorContent", key = "#id")
    public MonitorContent selectMonitorContentById(Long id) {
        return monitorContentMapper.selectMonitorContentById(id);
    }

    /**
     * 根据编码查询监测内容
     */
    @Override
    public MonitorContent selectMonitorContentByCode(String code) {
        return monitorContentMapper.selectMonitorContentByCode(code);
    }

    /**
     * 新增监测内容
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "monitorContent", key = "#monitorContent.id"),
            @CacheEvict(value = "monitorContentList", allEntries = true),
            @CacheEvict(value = "monitorType", allEntries = true)
    })
    public int insertMonitorContent(MonitorContent monitorContent) {
        // Auto-assign sortOrder if not provided: set to MAX + 1 for this monitor_type
        if (monitorContent.getSortOrder() == null) {
            Integer maxOrder = monitorContentMapper.selectMaxSortOrderByMonitorTypeId(
                    monitorContent.getMonitorTypeId());
            monitorContent.setSortOrder(maxOrder + 1);
        } else {
            // If explicitly provided, validate uniqueness (UNIQUE constraint is the safety net)
            validateSortOrderUniqueness(monitorContent, null);
        }
        return monitorContentMapper.insertMonitorContent(monitorContent);
    }

    /**
     * 修改监测内容
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "monitorContent", key = "#monitorContent.id"),
            @CacheEvict(value = "monitorContentList", allEntries = true),
            @CacheEvict(value = "monitorType", allEntries = true)
    })
    public int updateMonitorContent(MonitorContent monitorContent) {
        if (monitorContent.getSortOrder() != null) {
            validateSortOrderUniqueness(monitorContent, monitorContent.getId());
        }
        return monitorContentMapper.updateMonitorContent(monitorContent);
    }

    /**
     * 检查 sort_order 在同 monitor_type 下是否与其他行冲突。
     * 用于创建时或更新时预校验唯一性（UNIQUE 约束为最终兜底）。
     *
     * @param monitorContent 待校验的实体（需含 monitorTypeId 与 sortOrder）
     * @param excludeId      排除自身 ID（更新时传入，新建时传 null）
     */
    private void validateSortOrderUniqueness(MonitorContent monitorContent, Long excludeId) {
        if (monitorContent.getSortOrder() == null) {
            return;
        }
        MonitorContent conflict = monitorContentMapper.checkSortOrderExists(
                monitorContent.getMonitorTypeId(),
                monitorContent.getSortOrder(),
                excludeId);
        if (conflict != null) {
            throw new ServiceException(
                    "sort_order=" + monitorContent.getSortOrder()
                            + " 在监测类型下已存在（id=" + conflict.getId() + "）");
        }
    }

    /**
     * 删除监测内容（逻辑删除）
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "monitorContent", key = "#id"),
            @CacheEvict(value = "monitorContentList", allEntries = true),
            @CacheEvict(value = "monitorType", allEntries = true)
    })
    public int deleteMonitorContentById(Long id) {
        return monitorContentMapper.deleteMonitorContentById(id);
    }

    /**
     * 按监测类型删除监测内容（逻辑删除）
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "monitorContent", allEntries = true),
            @CacheEvict(value = "monitorContentList", allEntries = true),
            @CacheEvict(value = "monitorType", allEntries = true)
    })
    public int deleteMonitorContentByMonitorTypeId(Long monitorTypeId) {
        return monitorContentMapper.deleteMonitorContentByMonitorTypeId(monitorTypeId);
    }

    /**
     * 批量删除监测内容（逻辑删除）
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "monitorContent", allEntries = true),
            @CacheEvict(value = "monitorContentList", allEntries = true),
            @CacheEvict(value = "monitorType", allEntries = true)
    })
    public int deleteMonitorContentByIds(Long[] ids) {
        return monitorContentMapper.deleteMonitorContentByIds(ids);
    }

    /**
     * 按监测类型批量删除监测内容（逻辑删除）
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "monitorContent", allEntries = true),
            @CacheEvict(value = "monitorContentList", allEntries = true),
            @CacheEvict(value = "monitorType", allEntries = true)
    })
    public int deleteMonitorContentByMonitorTypeIds(Long[] monitorTypeIds) {
        return monitorContentMapper.deleteMonitorContentByMonitorTypeIds(monitorTypeIds);
    }

    /**
     * 校验监测内容编码是否唯一
     */
    @Override
    public boolean checkMonitorContentCodeUnique(MonitorContent monitorContent) {
        Long id = monitorContent.getId() == null ? 0L : monitorContent.getId();
        MonitorContent exist = monitorContentMapper.checkMonitorContentCodeUnique(monitorContent.getCode(), id);
        return exist == null;
    }
}
