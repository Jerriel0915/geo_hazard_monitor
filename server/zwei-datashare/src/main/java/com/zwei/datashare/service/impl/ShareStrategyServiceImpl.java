package com.zwei.datashare.service.impl;

import com.zwei.datashare.domain.ShareStrategy;
import com.zwei.datashare.domain.ShareStrategyLog;
import com.zwei.datashare.domain.ShareStrategyScript;
import com.zwei.datashare.domain.dto.ShareStrategyCreateRequest;
import com.zwei.datashare.domain.dto.ShareStrategyUpdateRequest;
import com.zwei.datashare.enums.RunStatus;
import com.zwei.datashare.enums.ShareMethod;
import com.zwei.datashare.enums.StrategyStatus;
import com.zwei.datashare.mapper.ShareStrategyLogMapper;
import com.zwei.datashare.mapper.ShareStrategyMapper;
import com.zwei.datashare.mapper.ShareStrategyScriptMapper;
import com.zwei.datashare.service.IShareStrategyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 鍏变韩绛栫暐Service涓氬姟灞傚鐞? *
 * @author zwei
 */
@Service
public class ShareStrategyServiceImpl implements IShareStrategyService {

    @Autowired
    private ShareStrategyMapper shareStrategyMapper;

    @Autowired
    private ShareStrategyLogMapper shareStrategyLogMapper;

    @Autowired
    private ShareStrategyScriptMapper shareStrategyScriptMapper;

    @Override
    @Transactional
    public ShareStrategy create(ShareStrategyCreateRequest request) {
        ShareStrategy strategy = new ShareStrategy();
        strategy.setCode(request.getCode());
        strategy.setName(request.getName());
        strategy.setDescription(request.getDescription());
        strategy.setMethod(request.getMethod());
        strategy.setAddress(request.getAddress());
        strategy.setTopic(request.getTopic());
        strategy.setUsername(request.getUsername());
        strategy.setPassword(request.getPassword());
        strategy.setParams(request.getParams());
        strategy.setScopeType(request.getScopeType());
        strategy.setScopeIds(request.getScopeIds());
        strategy.setCron(request.getCron());
        strategy.setStatus(StrategyStatus.DISABLED);
        strategy.setSuccessCount(0);
        strategy.setCreateTime(LocalDateTime.now());
        strategy.setUpdateTime(LocalDateTime.now());
        shareStrategyMapper.insert(strategy);
        return strategy;
    }

    @Override
    @Transactional
    public ShareStrategy update(Long id, ShareStrategyUpdateRequest request) {
        ShareStrategy strategy = shareStrategyMapper.selectById(id);
        if (request.getName() != null) strategy.setName(request.getName());
        if (request.getDescription() != null) strategy.setDescription(request.getDescription());
        if (request.getMethod() != null) strategy.setMethod(request.getMethod());
        if (request.getAddress() != null) strategy.setAddress(request.getAddress());
        if (request.getTopic() != null) strategy.setTopic(request.getTopic());
        if (request.getUsername() != null) strategy.setUsername(request.getUsername());
        if (request.getPassword() != null) strategy.setPassword(request.getPassword());
        if (request.getParams() != null) strategy.setParams(request.getParams());
        if (request.getScopeType() != null) strategy.setScopeType(request.getScopeType());
        if (request.getScopeIds() != null) strategy.setScopeIds(request.getScopeIds());
        if (request.getCron() != null) strategy.setCron(request.getCron());
        strategy.setUpdateTime(LocalDateTime.now());
        shareStrategyMapper.updateById(strategy);
        return strategy;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        shareStrategyMapper.deleteById(id);
        shareStrategyLogMapper.deleteByStrategyId(id);
        shareStrategyScriptMapper.deleteByStrategyId(id);
    }

    @Override
    public ShareStrategy findById(Long id) {
        return shareStrategyMapper.selectById(id);
    }

    @Override
    public List<ShareStrategy> findList(String name, StrategyStatus status, String method) {
        return shareStrategyMapper.selectList(name, status, method);
    }

    /**
     * 分页查询共享策略列表
     */
    @Override
    public List<ShareStrategy> selectShareStrategyPage(ShareStrategy strategy) {
        return shareStrategyMapper.selectStrategyList(strategy);
    }

    @Override
    @Transactional
    public ShareStrategy changeStatus(Long id, StrategyStatus status) {
        ShareStrategy strategy = shareStrategyMapper.selectById(id);
        strategy.setStatus(status);
        strategy.setUpdateTime(LocalDateTime.now());
        shareStrategyMapper.updateById(strategy);
        return strategy;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        ShareStrategy strategy = shareStrategyMapper.selectById(id);
        long startTime = System.currentTimeMillis();
        ShareStrategyLog log = new ShareStrategyLog();
        log.setStrategyId(id);
        log.setRunTime(LocalDateTime.now());
        try {
            if (strategy.getMethod() == ShareMethod.UNIFIED_PUSH || strategy.getMethod() == ShareMethod.UNIFIED_SERVICE) {
                executeUnifiedStrategy(strategy);
            } else {
                executeCustomStrategy(strategy);
            }
            log.setStatus(RunStatus.SUCCESS);
            log.setMessage("鎵ц鎴愬姛");
            shareStrategyMapper.incrementSuccessCount(id);
        } catch (Exception e) {
            log.setStatus(RunStatus.ERROR);
            log.setMessage(e.getMessage());
        }
        log.setDuration((int) (System.currentTimeMillis() - startTime));
        log.setCreateTime(LocalDateTime.now());
        shareStrategyLogMapper.insert(log);
        shareStrategyMapper.updateLastRunInfo(id, log.getRunTime().toString(), log.getStatus().name());
    }

    private void executeUnifiedStrategy(ShareStrategy strategy) {
    }

    private void executeCustomStrategy(ShareStrategy strategy) {
        ShareStrategyScript script = shareStrategyScriptMapper.selectByStrategyId(strategy.getId());
    }

    @Override
    public List<ShareStrategyLog> findLogs(Long strategyId) {
        return shareStrategyLogMapper.selectByStrategyId(strategyId);
    }

    @Override
    public ShareStrategyScript getScript(Long strategyId) {
        return shareStrategyScriptMapper.selectByStrategyId(strategyId);
    }

    @Override
    @Transactional
    public void saveScript(Long strategyId, String script, String variables) {
        ShareStrategyScript existing = shareStrategyScriptMapper.selectByStrategyId(strategyId);
        ShareStrategyScript scriptEntity = existing != null ? existing : new ShareStrategyScript();
        scriptEntity.setStrategyId(strategyId);
        scriptEntity.setScript(script);
        scriptEntity.setVariables(variables);
        scriptEntity.setUpdateTime(LocalDateTime.now());
        if (existing == null) {
            scriptEntity.setCreateTime(LocalDateTime.now());
            shareStrategyScriptMapper.insert(scriptEntity);
        } else {
            shareStrategyScriptMapper.updateById(scriptEntity);
        }
    }
}