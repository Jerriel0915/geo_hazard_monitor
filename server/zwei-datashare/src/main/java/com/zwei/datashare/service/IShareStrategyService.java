package com.zwei.datashare.service;

import com.zwei.datashare.domain.ShareStrategy;
import com.zwei.datashare.domain.ShareStrategyLog;
import com.zwei.datashare.domain.ShareStrategyScript;
import com.zwei.datashare.domain.dto.ShareStrategyCreateRequest;
import com.zwei.datashare.domain.dto.ShareStrategyUpdateRequest;
import com.zwei.datashare.enums.StrategyStatus;

import java.util.List;

public interface IShareStrategyService {

    ShareStrategy create(ShareStrategyCreateRequest request);

    ShareStrategy update(Long id, ShareStrategyUpdateRequest request);

    void delete(Long id);

    ShareStrategy findById(Long id);

    List<ShareStrategy> findList(String name, StrategyStatus status, String method);

    ShareStrategy changeStatus(Long id, StrategyStatus status);

    void execute(Long id);

    List<ShareStrategyLog> findLogs(Long strategyId);

    ShareStrategyScript getScript(Long strategyId);

    void saveScript(Long strategyId, String script, String variables);

    List<ShareStrategy> selectShareStrategyPage(ShareStrategy strategy);
}
