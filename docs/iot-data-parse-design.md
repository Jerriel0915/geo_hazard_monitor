# 物联网数据解析模块设计文档

## 一、需求概述

数据解析模块用于处理同协议、异主题、异结构的数据源汇聚入库问题，扩展系统对设备和协议兼容性的能力。

### 核心功能
1. MQTT报文解析策略管理
2. Blockly可视化编程支持
3. 运行日志查看
4. 应用范围配置（厂商/设备绑定）
5. 预置国标解析策略

## 二、数据库设计

### 2.1 解析策略表 (iot_data_parse_strategy)

```sql
CREATE TABLE `iot_data_parse_strategy` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '策略名称',
  `server_url` varchar(500) NOT NULL COMMENT 'MQTT服务地址',
  `topic` varchar(200) NOT NULL COMMENT '订阅主题',
  `description` text COMMENT '描述',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态 0-停用 1-启用',
  `app_scope` varchar(20) NOT NULL DEFAULT 'global' COMMENT '应用范围 global-全局 vendor-指定厂商 device-指定设备',
  `script_code` mediumtext COMMENT '解析脚本代码',
  `blockly_workspace` longtext COMMENT 'Blockly工作区JSON数据',
  `is_preset` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否预置策略 0-否 1-是',
  `last_run_time` datetime DEFAULT NULL COMMENT '最近运行时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人ID',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标记 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_topic` (`topic`),
  KEY `idx_status` (`status`),
  KEY `idx_app_scope` (`app_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据解析策略表';
```

### 2.2 策略-厂商关联表 (iot_data_parse_strategy_vendor)

```sql
CREATE TABLE `iot_data_parse_strategy_vendor` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `strategy_id` bigint(20) NOT NULL COMMENT '策略ID',
  `vendor_id` bigint(20) NOT NULL COMMENT '厂商ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_strategy_vendor` (`strategy_id`, `vendor_id`),
  KEY `idx_strategy_id` (`strategy_id`),
  KEY `idx_vendor_id` (`vendor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='解析策略-厂商关联表';
```

### 2.3 策略-设备关联表 (iot_data_parse_strategy_device)

```sql
CREATE TABLE `iot_data_parse_strategy_device` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `strategy_id` bigint(20) NOT NULL COMMENT '策略ID',
  `device_id` bigint(20) NOT NULL COMMENT '设备ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_strategy_device` (`strategy_id`, `device_id`),
  KEY `idx_strategy_id` (`strategy_id`),
  KEY `idx_device_id` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='解析策略-设备关联表';
```

### 2.4 运行日志表 (iot_data_parse_log)

```sql
CREATE TABLE `iot_data_parse_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `strategy_id` bigint(20) NOT NULL COMMENT '策略ID',
  `log_level` varchar(20) NOT NULL COMMENT '日志级别 INFO/WARN/ERROR',
  `message` text NOT NULL COMMENT '日志消息',
  `data` text COMMENT '关联数据(JSON)',
  `topic` varchar(200) DEFAULT NULL COMMENT '消息主题',
  `device_id` varchar(100) DEFAULT NULL COMMENT '设备ID',
  `parse_result` text COMMENT '解析结果(JSON)',
  `execution_time` int(11) DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `error_stack` text COMMENT '错误堆栈',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_strategy_id` (`strategy_id`),
  KEY `idx_log_level` (`log_level`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_device_id` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='解析运行日志表';
```

### 2.5 预置策略 - 国标协议解析

```sql
INSERT INTO `iot_data_parse_strategy` (`name`, `server_url`, `topic`, `description`, `status`, `app_scope`, `script_code`, `is_preset`) VALUES
(
  '国标协议解析',
  'tcp://mqtt.server:1883',
  '$dp',
  '国标协议数据解析策略，支持多厂商设备数据解析，参考: http://ghiot.cigem.cn:8080/doc/overview/overview.html#4-%E6%95%B0%E6%8D%AE%E6%A0%BC%E5%BC%8F%E6%A0%87%E5%87%86',
  1,
  'global',
  '// 国标协议解析脚本
function parse(message) {
  const result = {};
  result.timestamp = Date.now();
  result.sourceTopic = message.topic;
  result.payload = message.payload;
  
  if (message.topic === "$dp") {
    result.type = "dataPoint";
    result.deviceId = message.payload.deviceId;
    result.data = parseDataPoint(message.payload);
  }
  
  return result;
}

function parseDataPoint(payload) {
  const data = {};
  data.timestamp = payload.timestamp;
  data.values = payload.values || {};
  return data;
}',
  1
);
```

## 三、后端架构设计

### 3.1 模块结构

```
zwei-iot/
├── src/main/java/com/zwei/iot/
│   ├── dataparse/
│   │   ├── controller/
│   │   │   └── DataParseController.java
│   │   ├── service/
│   │   │   ├── DataParseStrategyService.java
│   │   │   ├── DataParseLogService.java
│   │   │   ├── DataParseEngineService.java
│   │   │   └── MqttMessageListener.java
│   │   ├── entity/
│   │   │   ├── DataParseStrategy.java
│   │   │   ├── DataParseStrategyVendor.java
│   │   │   ├── DataParseStrategyDevice.java
│   │   │   └── DataParseLog.java
│   │   ├── mapper/
│   │   │   ├── DataParseStrategyMapper.java
│   │   │   ├── DataParseStrategyVendorMapper.java
│   │   │   ├── DataParseStrategyDeviceMapper.java
│   │   │   └── DataParseLogMapper.java
│   │   ├── dto/
│   │   │   ├── DataParseStrategyQueryDTO.java
│   │   │   ├── DataParseStrategyDTO.java
│   │   │   ├── DataParseTestRequest.java
│   │   │   └── DataParseTestResponse.java
│   │   ├── engine/
│   │   │   ├── ScriptEngineFactory.java
│   │   │   ├── JavaScriptEngine.java
│   │   │   └── BuiltInFunctions.java
│   │   ├── toolbox/
│   │   │   ├── ToolboxManager.java
│   │   │   ├── DataQueryFunctions.java
│   │   │   ├── AlgorithmFunctions.java
│   │   │   ├── StorageFunctions.java
│   │   │   └── OutputFunctions.java
│   │   └── config/
│   │       └── DataParseAutoConfiguration.java
```

### 3.2 核心代码实现

#### 3.2.1 数据解析策略服务 (DataParseStrategyService.java)

```java
package com.zwei.iot.dataparse.service;

import com.zwei.common.exception.BusinessException;
import com.zwei.common.utils.DateUtils;
import com.zwei.iot.dataparse.dto.DataParseStrategyDTO;
import com.zwei.iot.dataparse.dto.DataParseStrategyQueryDTO;
import com.zwei.iot.dataparse.entity.DataParseStrategy;
import com.zwei.iot.dataparse.entity.DataParseStrategyDevice;
import com.zwei.iot.dataparse.entity.DataParseStrategyVendor;
import com.zwei.iot.dataparse.mapper.DataParseStrategyDeviceMapper;
import com.zwei.iot.dataparse.mapper.DataParseStrategyMapper;
import com.zwei.iot.dataparse.mapper.DataParseStrategyVendorMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DataParseStrategyService {

    @Resource
    private DataParseStrategyMapper strategyMapper;

    @Resource
    private DataParseStrategyVendorMapper vendorMapper;

    @Resource
    private DataParseStrategyDeviceMapper deviceMapper;

    @Resource
    private DataParseEngineService engineService;

    /**
     * 分页查询策略列表
     */
    public List<DataParseStrategy> listByPage(DataParseStrategyQueryDTO query) {
        return strategyMapper.selectByCondition(query);
    }

    /**
     * 根据ID查询策略详情
     */
    public DataParseStrategyDTO getById(Long id) {
        DataParseStrategy strategy = strategyMapper.selectById(id);
        if (strategy == null) {
            throw new BusinessException("策略不存在");
        }

        DataParseStrategyDTO dto = new DataParseStrategyDTO();
        BeanUtils.copyProperties(strategy, dto);

        // 查询关联的厂商
        if ("vendor".equals(strategy.getAppScope())) {
            List<Long> vendorIds = vendorMapper.selectVendorIdsByStrategyId(id);
            dto.setVendorIds(vendorIds);
        }

        // 查询关联的设备
        if ("device".equals(strategy.getAppScope())) {
            List<Long> deviceIds = deviceMapper.selectDeviceIdsByStrategyId(id);
            dto.setDeviceIds(deviceIds);
        }

        return dto;
    }

    /**
     * 新增策略
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(DataParseStrategyDTO dto) {
        DataParseStrategy strategy = new DataParseStrategy();
        BeanUtils.copyProperties(dto, strategy);
        strategyMapper.insert(strategy);

        // 保存关联关系
        saveRelations(strategy.getId(), dto);

        // 启动策略监听
        if (strategy.getStatus() == 1) {
            engineService.startStrategy(strategy);
        }

        return strategy.getId();
    }

    /**
     * 更新策略
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(DataParseStrategyDTO dto) {
        DataParseStrategy existing = strategyMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException("策略不存在");
        }

        // 先停止旧策略
        if (existing.getStatus() == 1) {
            engineService.stopStrategy(existing.getId());
        }

        // 更新策略
        DataParseStrategy strategy = new DataParseStrategy();
        BeanUtils.copyProperties(dto, strategy);
        strategyMapper.updateById(strategy);

        // 更新关联关系
        deleteRelations(dto.getId());
        saveRelations(dto.getId(), dto);

        // 启动新策略
        if (strategy.getStatus() == 1) {
            engineService.startStrategy(strategy);
        }
    }

    /**
     * 删除策略
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DataParseStrategy strategy = strategyMapper.selectById(id);
        if (strategy == null) {
            throw new BusinessException("策略不存在");
        }

        // 停止策略
        if (strategy.getStatus() == 1) {
            engineService.stopStrategy(id);
        }

        // 删除关联关系
        deleteRelations(id);

        // 逻辑删除
        strategyMapper.deleteById(id);
    }

    /**
     * 启用/停用策略
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id, Integer status) {
        DataParseStrategy strategy = strategyMapper.selectById(id);
        if (strategy == null) {
            throw new BusinessException("策略不存在");
        }

        if (status == 1) {
            engineService.startStrategy(strategy);
        } else {
            engineService.stopStrategy(id);
        }

        strategy.setStatus(status);
        strategyMapper.updateById(strategy);
    }

    /**
     * 复制策略
     */
    @Transactional(rollbackFor = Exception.class)
    public Long copy(Long id) {
        DataParseStrategy original = strategyMapper.selectById(id);
        if (original == null) {
            throw new BusinessException("原策略不存在");
        }

        DataParseStrategy copy = new DataParseStrategy();
        BeanUtils.copyProperties(original, copy);
        copy.setId(null);
        copy.setName(original.getName() + " (副本)");
        copy.setIsPreset(0);
        copy.setStatus(0);
        copy.setLastRunTime(null);
        strategyMapper.insert(copy);

        // 复制关联关系
        DataParseStrategyDTO dto = new DataParseStrategyDTO();
        dto.setAppScope(original.getAppScope());
        if ("vendor".equals(original.getAppScope())) {
            dto.setVendorIds(vendorMapper.selectVendorIdsByStrategyId(id));
        }
        if ("device".equals(original.getAppScope())) {
            dto.setDeviceIds(deviceMapper.selectDeviceIdsByStrategyId(id));
        }
        saveRelations(copy.getId(), dto);

        return copy.getId();
    }

    /**
     * 保存关联关系
     */
    private void saveRelations(Long strategyId, DataParseStrategyDTO dto) {
        if ("vendor".equals(dto.getAppScope()) && !CollectionUtils.isEmpty(dto.getVendorIds())) {
            for (Long vendorId : dto.getVendorIds()) {
                DataParseStrategyVendor relation = new DataParseStrategyVendor();
                relation.setStrategyId(strategyId);
                relation.setVendorId(vendorId);
                vendorMapper.insert(relation);
            }
        }

        if ("device".equals(dto.getAppScope()) && !CollectionUtils.isEmpty(dto.getDeviceIds())) {
            for (Long deviceId : dto.getDeviceIds()) {
                DataParseStrategyDevice relation = new DataParseStrategyDevice();
                relation.setStrategyId(strategyId);
                relation.setDeviceId(deviceId);
                deviceMapper.insert(relation);
            }
        }
    }

    /**
     * 删除关联关系
     */
    private void deleteRelations(Long strategyId) {
        vendorMapper.deleteByStrategyId(strategyId);
        deviceMapper.deleteByStrategyId(strategyId);
    }
}
```

#### 3.2.2 解析引擎服务 (DataParseEngineService.java)

```java
package com.zwei.iot.dataparse.service;

import com.alibaba.fastjson.JSON;
import com.zwei.common.utils.DateUtils;
import com.zwei.iot.dataparse.engine.BuiltInFunctions;
import com.zwei.iot.dataparse.engine.JavaScriptEngine;
import com.zwei.iot.dataparse.entity.DataParseLog;
import com.zwei.iot.dataparse.entity.DataParseStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DataParseEngineService {

    private static final Logger logger = LoggerFactory.getLogger(DataParseEngineService.class);

    private final Map<Long, JavaScriptEngine> engineMap = new ConcurrentHashMap<>();

    @Resource
    private DataParseLogService logService;

    @Resource
    private BuiltInFunctions builtInFunctions;

    /**
     * 启动策略
     */
    public void startStrategy(DataParseStrategy strategy) {
        try {
            // 创建JavaScript引擎
            JavaScriptEngine engine = new JavaScriptEngine(strategy);
            engine.setBuiltInFunctions(builtInFunctions);
            engine.compile();

            // 订阅MQTT消息
            // mqttSubscriber.subscribe(strategy.getServerUrl(), strategy.getTopic(), 
            //     (topic, message) -> handleMessage(strategy, engine, topic, message));

            engineMap.put(strategy.getId(), engine);
            logger.info("策略启动成功: {}", strategy.getName());

            // 记录日志
            logService.info(strategy.getId(), "策略启动成功", null);
        } catch (Exception e) {
            logger.error("策略启动失败: {}", strategy.getName(), e);
            logService.error(strategy.getId(), "策略启动失败: " + e.getMessage(), null, e);
        }
    }

    /**
     * 停止策略
     */
    public void stopStrategy(Long strategyId) {
        JavaScriptEngine engine = engineMap.remove(strategyId);
        if (engine != null) {
            // 取消MQTT订阅
            // mqttSubscriber.unsubscribe(engine.getTopic());
            engine.close();
            logger.info("策略停止成功: {}", strategyId);
            logService.info(strategyId, "策略已停止", null);
        }
    }

    /**
     * 处理MQTT消息
     */
    public Object handleMessage(DataParseStrategy strategy, JavaScriptEngine engine, 
                                  String topic, Object message) {
        long startTime = System.currentTimeMillis();
        DataParseLog log = new DataParseLog();
        log.setStrategyId(strategy.getId());
        log.setTopic(topic);

        try {
            // 执行解析
            Object result = engine.execute(topic, message);
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.setLogLevel("INFO");
            log.setMessage("解析成功");
            log.setExecutionTime((int) executionTime);
            log.setParseResult(JSON.toJSONString(result));
            
            logger.debug("解析成功, 耗时: {}ms", executionTime);
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.setLogLevel("ERROR");
            log.setMessage("解析失败: " + e.getMessage());
            log.setExecutionTime((int) executionTime);
            log.setErrorStack(e.getMessage());
            
            logger.error("解析失败: {}", strategy.getName(), e);
            throw e;
        } finally {
            logService.save(log);
            // 更新最后运行时间
            updateLastRunTime(strategy.getId());
        }
    }

    /**
     * 测试脚本
     */
    public Map<String, Object> testScript(Long strategyId, String scriptCode, 
                                          String topic, Object testData) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 创建临时引擎
            DataParseStrategy tempStrategy = new DataParseStrategy();
            tempStrategy.setId(strategyId);
            tempStrategy.setScriptCode(scriptCode);
            
            JavaScriptEngine tempEngine = new JavaScriptEngine(tempStrategy);
            tempEngine.setBuiltInFunctions(builtInFunctions);
            tempEngine.compile();
            
            Object result = tempEngine.execute(topic, testData);
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 返回测试结果
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("success", true);
            response.put("executionTime", executionTime);
            response.put("result", result);
            
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    private void updateLastRunTime(Long strategyId) {
        // 更新策略的最后运行时间
        // strategyMapper.updateLastRunTime(strategyId, DateUtils.getNowDate());
    }

    @PostConstruct
    public void init() {
        // 启动时加载所有启用的策略
        // List<DataParseStrategy> strategies = strategyMapper.selectByStatus(1);
        // for (DataParseStrategy strategy : strategies) {
        //     startStrategy(strategy);
        // }
    }

    @PreDestroy
    public void destroy() {
        // 停止所有策略
        for (Long strategyId : engineMap.keySet()) {
            stopStrategy(strategyId);
        }
    }
}
```

#### 3.2.3 JavaScript引擎 (JavaScriptEngine.java)

```java
package com.zwei.iot.dataparse.engine;

import com.zwei.iot.dataparse.entity.DataParseStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.*;
import java.util.Map;

public class JavaScriptEngine {

    private static final Logger logger = LoggerFactory.getLogger(JavaScriptEngine.class);

    private final DataParseStrategy strategy;
    private ScriptEngine engine;
    private Invocable invocable;
    private BuiltInFunctions builtInFunctions;

    public JavaScriptEngine(DataParseStrategy strategy) {
        this.strategy = strategy;
    }

    public void setBuiltInFunctions(BuiltInFunctions builtInFunctions) {
        this.builtInFunctions = builtInFunctions;
    }

    /**
     * 编译脚本
     */
    public void compile() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        engine = manager.getEngineByName("JavaScript");
        invocable = (Invocable) engine;

        // 注入内置函数
        engine.put("dataQuery", builtInFunctions.getDataQueryFunctions());
        engine.put("algorithm", builtInFunctions.getAlgorithmFunctions());
        engine.put("storage", builtInFunctions.getStorageFunctions());
        engine.put("output", builtInFunctions.getOutputFunctions());
        engine.put("logger", builtInFunctions.getLogger());

        // 执行脚本
        String script = strategy.getScriptCode();
        if (script != null && !script.isEmpty()) {
            engine.eval(script);
        }
    }

    /**
     * 执行解析
     */
    public Object execute(String topic, Object message) throws ScriptException, NoSuchMethodException {
        // 构建消息对象
        Map<String, Object> messageObj = new java.util.HashMap<>();
        messageObj.put("topic", topic);
        messageObj.put("payload", message);

        // 调用parse函数
        return invocable.invokeFunction("parse", messageObj);
    }

    public String getTopic() {
        return strategy.getTopic();
    }

    public void close() {
        engine = null;
        invocable = null;
    }
}
```

#### 3.2.4 内置函数库 (BuiltInFunctions.java)

```java
package com.zwei.iot.dataparse.engine;

import org.springframework.stereotype.Component;

@Component
public class BuiltInFunctions {

    public DataQueryFunctions getDataQueryFunctions() {
        return new DataQueryFunctions();
    }

    public AlgorithmFunctions getAlgorithmFunctions() {
        return new AlgorithmFunctions();
    }

    public StorageFunctions getStorageFunctions() {
        return new StorageFunctions();
    }

    public OutputFunctions getOutputFunctions() {
        return new OutputFunctions();
    }

    public LoggerFunctions getLogger() {
        return new LoggerFunctions();
    }

    /**
     * 数据查询函数
     */
    public static class DataQueryFunctions {
        public Object getDeviceInfo(String deviceId) {
            // 查询设备信息
            return null;
        }

        public Object getVendorInfo(String vendorId) {
            // 查询厂商信息
            return null;
        }

        public Object getHazardPointInfo(String hazardPointId) {
            // 查询隐患点信息
            return null;
        }
    }

    /**
     * 算法函数
     */
    public static class AlgorithmFunctions {
        public Object dataClean(Object data) {
            // 数据清洗
            return data;
        }

        public Object formatConvert(Object data, String format) {
            // 格式转换
            return data;
        }

        public Object anomalyDetect(Object data) {
            // 异常检测
            return data;
        }

        public Object aggregateCalc(Object data) {
            // 聚合计算
            return data;
        }

        public Object trendAnalysis(Object data) {
            // 趋势分析
            return data;
        }
    }

    /**
     * 存储函数
     */
    public static class StorageFunctions {
        public void saveMonitorData(Object data) {
            // 保存监测数据
        }

        public void saveDeviceStatus(Object data) {
            // 保存设备状态
        }

        public void saveAlarmEvent(Object data) {
            // 保存告警事件
        }
    }

    /**
     * 输出函数
     */
    public static class OutputFunctions {
        public void sendToStrategy(String strategyId, Object data) {
            // 发送到其他策略
        }

        public void sendToHttp(String url, Object data) {
            // 发送到HTTP接口
        }

        public void sendToQueue(String queueName, Object data) {
            // 发送到消息队列
        }
    }

    /**
     * 日志函数
     */
    public static class LoggerFunctions {
        public void info(String message) {
            // 记录日志
        }

        public void warn(String message) {
            // 记录警告
        }

        public void error(String message) {
            // 记录错误
        }
    }
}
```

#### 3.2.5 控制器 (DataParseController.java)

```java
package com.zwei.iot.dataparse.controller;

import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.TableDataInfo;
import com.zwei.common.utils.DateUtils;
import com.zwei.iot.dataparse.dto.DataParseStrategyDTO;
import com.zwei.iot.dataparse.dto.DataParseStrategyQueryDTO;
import com.zwei.iot.dataparse.dto.DataParseTestRequest;
import com.zwei.iot.dataparse.entity.DataParseLog;
import com.zwei.iot.dataparse.entity.DataParseStrategy;
import com.zwei.iot.dataparse.service.DataParseEngineService;
import com.zwei.iot.dataparse.service.DataParseLogService;
import com.zwei.iot.dataparse.service.DataParseStrategyService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/iot/dataParse")
public class DataParseController {

    @Resource
    private DataParseStrategyService strategyService;

    @Resource
    private DataParseLogService logService;

    @Resource
    private DataParseEngineService engineService;

    /**
     * 分页查询策略列表
     */
    @GetMapping("/list")
    public TableDataInfo list(DataParseStrategyQueryDTO query) {
        List<DataParseStrategy> list = strategyService.listByPage(query);
        return TableDataInfo.success(list, list.size());
    }

    /**
     * 获取策略详情
     */
    @GetMapping("/{id}")
    public AjaxResult getDetail(@PathVariable Long id) {
        return AjaxResult.success(strategyService.getById(id));
    }

    /**
     * 新增策略
     */
    @PostMapping
    public AjaxResult create(@RequestBody DataParseStrategyDTO dto) {
        return AjaxResult.success(strategyService.create(dto));
    }

    /**
     * 更新策略
     */
    @PutMapping
    public AjaxResult update(@RequestBody DataParseStrategyDTO dto) {
        strategyService.update(dto);
        return AjaxResult.success();
    }

    /**
     * 删除策略
     */
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        strategyService.delete(id);
        return AjaxResult.success();
    }

    /**
     * 启用/停用策略
     */
    @PutMapping("/{id}/status")
    public AjaxResult toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        strategyService.toggleStatus(id, status);
        return AjaxResult.success();
    }

    /**
     * 复制策略
     */
    @PostMapping("/{id}/copy")
    public AjaxResult copy(@PathVariable Long id) {
        return AjaxResult.success(strategyService.copy(id));
    }

    /**
     * 测试脚本
     */
    @PostMapping("/test")
    public AjaxResult testScript(@RequestBody DataParseTestRequest request) {
        Map<String, Object> result = engineService.testScript(
            request.getStrategyId(), 
            request.getScriptCode(), 
            request.getTopic(), 
            request.getTestData()
        );
        return AjaxResult.success(result);
    }

    /**
     * 查询运行日志
     */
    @GetMapping("/{id}/logs")
    public TableDataInfo getLogs(@PathVariable Long id,
                                   @RequestParam(required = false) String logLevel,
                                   @RequestParam(required = false) String startTime,
                                   @RequestParam(required = false) String endTime) {
        List<DataParseLog> logs = logService.listByCondition(id, logLevel, startTime, endTime);
        return TableDataInfo.success(logs, logs.size());
    }

    /**
     * 清空日志
     */
    @DeleteMapping("/{id}/logs")
    public AjaxResult clearLogs(@PathVariable Long id) {
        logService.clearByStrategyId(id);
        return AjaxResult.success();
    }

    /**
     * 导出日志
     */
    @GetMapping("/{id}/logs/export")
    public void exportLogs(@PathVariable Long id) {
        logService.exportByStrategyId(id);
    }
}
```

## 四、MQTT集成方案

### 4.1 MQTT订阅器

```java
package com.zwei.iot.dataparse.service;

import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MqttMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(MqttMessageListener.class);

    private final Map<String, MqttClient> clientMap = new ConcurrentHashMap<>();

    @Resource
    private DataParseEngineService engineService;

    /**
     * 订阅主题
     */
    public void subscribe(String serverUrl, String topic, 
                          MessageHandler handler) {
        String clientKey = serverUrl + ":" + topic;
        
        try {
            if (clientMap.containsKey(clientKey)) {
                return;
            }

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(20);

            MqttClient client = new MqttClient(serverUrl, 
                MqttClient.generateClientId());
            client.connect(options);

            client.subscribe(topic, (msgTopic, message) -> {
                try {
                    String payload = new String(message.getPayload());
                    logger.debug("收到消息: topic={}, payload={}", msgTopic, payload);
                    handler.handle(msgTopic, payload);
                } catch (Exception e) {
                    logger.error("处理消息失败", e);
                }
            });

            clientMap.put(clientKey, client);
            logger.info("订阅成功: {}", topic);
        } catch (MqttException e) {
            logger.error("订阅失败", e);
        }
    }

    /**
     * 取消订阅
     */
    public void unsubscribe(String serverUrl, String topic) {
        String clientKey = serverUrl + ":" + topic;
        MqttClient client = clientMap.remove(clientKey);
        if (client != null) {
            try {
                client.unsubscribe(topic);
                client.disconnect();
                logger.info("取消订阅: {}", topic);
            } catch (MqttException e) {
                logger.error("取消订阅失败", e);
            }
        }
    }

    @FunctionalInterface
    public interface MessageHandler {
        void handle(String topic, String message) throws Exception;
    }
}
```

## 五、Blockly集成方案

### 5.1 Blockly工具块定义

```javascript
// 数据查询工具块
Blockly.Blocks['data_query_device'] = {
  init: function() {
    this.appendDummyInput()
        .appendField("查询设备信息")
        .appendField(new Blockly.FieldTextInput("dev001"), "deviceId");
    this.setOutput(true, "Object");
    this.setColour(160);
  }
};

// 数据存储工具块
Blockly.Blocks['storage_monitor_data'] = {
  init: function() {
    this.appendValueInput("DATA")
        .setCheck("Object")
        .appendField("保存监测数据");
    this.setPreviousStatement(true, null);
    this.setNextStatement(true, null);
    this.setColour(290);
  }
};

// 条件判断工具块
Blockly.Blocks['logic_if'] = {
  init: function() {
    this.appendValueInput("IF0")
        .setCheck("Boolean")
        .appendField("如果");
    this.appendStatementInput("DO0")
        .setAlign(Blockly.ALIGN_RIGHT)
        .appendField("那么");
    this.setPreviousStatement(true, null);
    this.setNextStatement(true, null);
    this.setColour(210);
  }
};
```

### 5.2 Blockly代码生成器

```javascript
Blockly.JavaScript['data_query_device'] = function(block) {
  var deviceId = block.getFieldValue('deviceId');
  var code = 'dataQuery.getDeviceInfo("' + deviceId + '")';
  return [code, Blockly.JavaScript.ORDER_ATOMIC];
};

Blockly.JavaScript['storage_monitor_data'] = function(block) {
  var data = Blockly.JavaScript.valueToCode(block, 'DATA', 
    Blockly.JavaScript.ORDER_ATOMIC);
  var code = 'storage.saveMonitorData(' + data + ');\n';
  return code;
};
```

## 六、数据流程图

```
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│   MQTT Broker   │─────▶│   MQTT监听器    │─────▶│   消息队列      │
│ (消息源)        │      │  (接收消息)     │      │  (缓冲处理)     │
└─────────────────┘      └─────────────────┘      └────────┬────────┘
                                                          │
                                                          ▼
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│   存储入库      │◀─────│  JavaScript引擎 │◀─────│  脚本执行       │
│ (监测/告警等)  │      │  (解析执行)     │      │  (策略匹配)     │
└─────────────────┘      └────────┬────────┘      └─────────────────┘
                                 │
                                 ▼
                        ┌─────────────────┐
                        │   日志记录       │
                        │  (运行/错误)     │
                        └─────────────────┘
```

## 七、API接口文档

### 7.1 策略管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /iot/dataParse/list | 分页查询策略列表 |
| GET | /iot/dataParse/{id} | 获取策略详情 |
| POST | /iot/dataParse | 新增策略 |
| PUT | /iot/dataParse | 更新策略 |
| DELETE | /iot/dataParse/{id} | 删除策略 |
| PUT | /iot/dataParse/{id}/status | 启用/停用策略 |
| POST | /iot/dataParse/{id}/copy | 复制策略 |

### 7.2 脚本测试

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /iot/dataParse/test | 测试解析脚本 |

### 7.3 日志查询

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /iot/dataParse/{id}/logs | 查询运行日志 |
| DELETE | /iot/dataParse/{id}/logs | 清空运行日志 |
| GET | /iot/dataParse/{id}/logs/export | 导出运行日志 |

## 八、注意事项

1. **安全性**: 脚本执行环境需要做安全限制，防止恶意脚本
2. **性能**: 大量设备数据时需要考虑异步处理和性能优化
3. **可靠性**: 策略异常时不影响其他策略，支持自动恢复
4. **监控**: 需要监控策略执行状态和运行统计
5. **版本管理**: 策略变更需要版本控制和回滚功能

## 九、后续优化

1. 支持更多编程语言（Python、Groovy等）
2. 策略模板市场
3. 策略可视化调试
4. 性能监控和告警
5. 分布式策略执行
