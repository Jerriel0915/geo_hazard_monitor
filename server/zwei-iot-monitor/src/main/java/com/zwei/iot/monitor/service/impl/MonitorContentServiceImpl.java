package com.zwei.iot.monitor.service.impl;

import com.zwei.iot.monitor.domain.MonitorContent;
import com.zwei.iot.monitor.mapper.MonitorContentMapper;
import com.zwei.iot.monitor.service.IMonitorContentService;
import com.zwei.common.event.MonitorContentChangedEvent;
import com.zwei.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 监测内容Service实现类
 *
 * @author zwei
 */
@Service
public class MonitorContentServiceImpl implements IMonitorContentService {

    private final MonitorContentMapper monitorContentMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public MonitorContentServiceImpl(MonitorContentMapper monitorContentMapper,
                                     ApplicationEventPublisher eventPublisher) {
        this.monitorContentMapper = monitorContentMapper;
        this.eventPublisher = eventPublisher;
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
     * 新增监测内容
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
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
        int rows = monitorContentMapper.insertMonitorContent(monitorContent);
        eventPublisher.publishEvent(new MonitorContentChangedEvent(
                monitorContent.getMonitorTypeId(),
                MonitorContentChangedEvent.ChangeType.INSERT));
        return rows;
    }

    /**
     * 修改监测内容
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = "monitorContent", key = "#monitorContent.id"),
            @CacheEvict(value = "monitorContentList", allEntries = true),
            @CacheEvict(value = "monitorType", allEntries = true)
    })
    public int updateMonitorContent(MonitorContent monitorContent) {
        if (monitorContent.getSortOrder() != null) {
            validateSortOrderUniqueness(monitorContent, monitorContent.getId());
        }
        // 防御：前端 PUT 可能不传 monitorTypeId，从 DB 回填
        Long monitorTypeId = monitorContent.getMonitorTypeId();
        if (monitorTypeId == null) {
            MonitorContent existing = monitorContentMapper.selectMonitorContentById(monitorContent.getId());
            monitorTypeId = existing != null ? existing.getMonitorTypeId() : null;
        }
        int rows = monitorContentMapper.updateMonitorContent(monitorContent);
        if (rows > 0 && monitorTypeId != null) {
            eventPublisher.publishEvent(new MonitorContentChangedEvent(
                    monitorTypeId, MonitorContentChangedEvent.ChangeType.UPDATE));
        }
        return rows;
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
        MonitorContent content = monitorContentMapper.selectMonitorContentById(id);
        Long monitorTypeId = content != null ? content.getMonitorTypeId() : null;
        int rows = monitorContentMapper.deleteMonitorContentById(id);
        if (rows > 0 && monitorTypeId != null) {
            eventPublisher.publishEvent(new MonitorContentChangedEvent(
                    monitorTypeId, MonitorContentChangedEvent.ChangeType.DELETE));
        }
        return rows;
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
        int rows = monitorContentMapper.deleteMonitorContentByMonitorTypeId(monitorTypeId);
        if (rows > 0) {
            eventPublisher.publishEvent(new MonitorContentChangedEvent(
                    monitorTypeId, MonitorContentChangedEvent.ChangeType.DELETE));
        }
        return rows;
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
        List<Long> monitorTypeIds = monitorContentMapper.selectMonitorTypeIdsByContentIds(ids);
        int rows = monitorContentMapper.deleteMonitorContentByIds(ids);
        for (Long monitorTypeId : monitorTypeIds) {
            eventPublisher.publishEvent(new MonitorContentChangedEvent(
                    monitorTypeId, MonitorContentChangedEvent.ChangeType.DELETE));
        }
        return rows;
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
        int rows = monitorContentMapper.deleteMonitorContentByMonitorTypeIds(monitorTypeIds);
        if (rows > 0) {
            for (Long monitorTypeId : monitorTypeIds) {
                eventPublisher.publishEvent(new MonitorContentChangedEvent(
                        monitorTypeId, MonitorContentChangedEvent.ChangeType.DELETE));
            }
        }
        return rows;
    }

    /**
     * 校验监测内容编码在指定监测类型内是否唯一
     */
    @Override
    public boolean checkMonitorContentCodeUnique(MonitorContent monitorContent) {
        Long id = monitorContent.getId() == null ? 0L : monitorContent.getId();
        MonitorContent exist = monitorContentMapper.checkMonitorContentCodeUnique(
                monitorContent.getMonitorTypeId(), monitorContent.getCode(), id);
        return exist == null;
    }

    /**
     * 查询指定监测类型下的所有计算属性。
     * 直接走 mapper, 缓存由 ComputedAttributeRegistry 负责(@Cacheable 在那一层)。
     */
    @Override
    public List<MonitorContent> selectComputedByTypeId(Long monitorTypeId) {
        return monitorContentMapper.selectComputedByTypeId(monitorTypeId);
    }
}
