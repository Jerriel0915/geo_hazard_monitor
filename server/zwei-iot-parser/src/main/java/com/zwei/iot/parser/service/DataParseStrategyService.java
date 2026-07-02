package com.zwei.iot.parser.service;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.parser.domain.DataParseStrategy;
import com.zwei.iot.parser.domain.DataParseStrategyDevice;
import com.zwei.iot.parser.domain.DataParseStrategyVendor;
import com.zwei.iot.parser.dto.DataParseStrategyDTO;
import com.zwei.iot.parser.dto.DataParseStrategyQueryDTO;
import com.zwei.iot.parser.engine.GroovyScriptEngine;
import com.zwei.iot.parser.mapper.DataParseStrategyDeviceMapper;
import com.zwei.iot.parser.mapper.DataParseStrategyMapper;
import com.zwei.iot.parser.mapper.DataParseStrategyVendorMapper;
import com.zwei.iot.parser.support.GroovyScriptValidator;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;

@Service
public class DataParseStrategyService {

    @Resource
    private DataParseStrategyMapper strategyMapper;
    @Resource
    private DataParseStrategyVendorMapper vendorMapper;
    @Resource
    private DataParseStrategyDeviceMapper strategyDeviceMapper;
    @Resource
    private GroovyScriptEngine scriptEngine;

    public List<DataParseStrategy> listByPage(DataParseStrategyQueryDTO query) {
        String keyword = null;
        String sourceType = null;
        Integer status = null;
        String appScope = null;
        if (query != null) {
            // keyword 优先（name 或 topic 模糊匹配）；兼容旧 name 字段
            keyword = (query.getKeyword() != null && !query.getKeyword().isEmpty())
                    ? query.getKeyword() : query.getName();
            sourceType = query.getSourceType();
            status = query.getStatus();
            appScope = query.getAppScope();
        }
        return strategyMapper.selectByCondition(keyword, sourceType, status, appScope);
    }

    public DataParseStrategyDTO getById(Long id) {
        DataParseStrategy strategy = strategyMapper.selectById(id);
        if (strategy == null) {
            throw new ServiceException("策略不存在: id=" + id);
        }
        DataParseStrategyDTO dto = new DataParseStrategyDTO();
        BeanUtils.copyProperties(strategy, dto);
        if ("vendor".equals(strategy.getAppScope())) {
            dto.setVendorIds(vendorMapper.selectVendorIdsByStrategyId(id));
        }
        if ("device".equals(strategy.getAppScope())) {
            dto.setDeviceIds(strategyDeviceMapper.selectDeviceIdsByStrategyId(id));
        }
        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(DataParseStrategyDTO dto) {
        if (strategyMapper.checkNameUnique(dto.getName(), null) != null) {
            throw new ServiceException("创建失败，策略名称已存在");
        }
        String err = GroovyScriptValidator.validate(dto.getScriptCode());
        if (err != null) {
            throw new ServiceException(err);
        }
        DataParseStrategy strategy = new DataParseStrategy();
        BeanUtils.copyProperties(dto, strategy);
        strategyMapper.insert(strategy);
        saveRelations(strategy.getId(), dto);
        return strategy.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(DataParseStrategyDTO dto) {
        DataParseStrategy existing = strategyMapper.selectById(dto.getId());
        if (existing == null) {
            throw new ServiceException("策略不存在: id=" + dto.getId());
        }
        if (strategyMapper.checkNameUnique(dto.getName(), dto.getId()) != null) {
            throw new ServiceException("修改失败，策略名称已存在");
        }
        String err = GroovyScriptValidator.validate(dto.getScriptCode());
        if (err != null) {
            throw new ServiceException(err);
        }
        DataParseStrategy strategy = new DataParseStrategy();
        BeanUtils.copyProperties(dto, strategy);
        strategyMapper.updateById(strategy);
        deleteRelations(dto.getId());
        saveRelations(dto.getId(), dto);
        // 淘汰脚本编译缓存，确保新脚本立即生效 (B1 修复)
        scriptEngine.evictCache(dto.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DataParseStrategy strategy = strategyMapper.selectById(id);
        if (strategy == null) {
            throw new ServiceException("策略不存在: id=" + id);
        }
        deleteRelations(id);
        strategyMapper.deleteById(id);
        scriptEngine.evictCache(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id, Integer status) {
        DataParseStrategy strategy = strategyMapper.selectById(id);
        if (strategy == null) {
            throw new ServiceException("策略不存在: id=" + id);
        }
        strategy.setStatus(status);
        strategyMapper.updateById(strategy);
        // 启停不改变脚本内容, 但仍淘汰缓存以释放内存 (停用策略无需常驻编译类)
        scriptEngine.evictCache(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long copy(Long id) {
        DataParseStrategy original = strategyMapper.selectById(id);
        if (original == null) {
            throw new ServiceException("原策略不存在: id=" + id);
        }
        DataParseStrategy copy = new DataParseStrategy();
        BeanUtils.copyProperties(original, copy);
        copy.setId(null);
        copy.setName(original.getName() + " (副本)");
        copy.setIsPreset(0);
        copy.setStatus(0);
        strategyMapper.insert(copy);

        DataParseStrategyDTO dto = new DataParseStrategyDTO();
        dto.setAppScope(original.getAppScope());
        if ("vendor".equals(original.getAppScope())) {
            dto.setVendorIds(vendorMapper.selectVendorIdsByStrategyId(id));
        }
        if ("device".equals(original.getAppScope())) {
            dto.setDeviceIds(strategyDeviceMapper.selectDeviceIdsByStrategyId(id));
        }
        saveRelations(copy.getId(), dto);
        // 副本是新策略, 无旧缓存可淘汰; 但若复用了原策略编译类引用会错乱, 主动清一次
        scriptEngine.evictCache(copy.getId());
        return copy.getId();
    }

    public List<DataParseStrategy> getEnabledBySourceType(String sourceType) {
        List<DataParseStrategy> strategies = strategyMapper.selectBySourceType(sourceType);
        return strategies != null ? strategies : Collections.emptyList();
    }

    private void saveRelations(Long strategyId, DataParseStrategyDTO dto) {
        if ("vendor".equals(dto.getAppScope()) && !CollectionUtils.isEmpty(dto.getVendorIds())) {
            vendorMapper.batchInsert(strategyId, dto.getVendorIds());
        }
        if ("device".equals(dto.getAppScope()) && !CollectionUtils.isEmpty(dto.getDeviceIds())) {
            strategyDeviceMapper.batchInsert(strategyId, dto.getDeviceIds());
        }
    }

    private void deleteRelations(Long strategyId) {
        vendorMapper.deleteByStrategyId(strategyId);
        strategyDeviceMapper.deleteByStrategyId(strategyId);
    }
}
