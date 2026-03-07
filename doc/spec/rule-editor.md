# 规则引擎可视化编辑器模块说明

## 模块概览

- 目标：提供基于 AviatorScript 的物联网规则可视化配置能力，支持条件与动作的拖拽编排、表达式生成、保存与在线测试。
- 前端技术栈：Vue 3 + Element Plus + vuedraggable。本项目已集成这些依赖，无需新增第三方图形库。
- 后端基础：已存在规则引擎与 CRUD 接口，新增“表达式校验与测试”能力以支撑可视化编辑器。

## 前端实现

- 页面入口：/iot/rule/editor（路由已注册）
    - 路由位置：[router/index.js](file:///d:/Code/Projects/zwei/web/src/router/index.js#L163-L172)
- 页面文件：[index.vue](file:///d:/Code/Projects/zwei/web/src/views/rule/editor/index.vue)
- 主要功能：
    - 规则基础信息（名称、产品ID、启用开关）；
    - 条件编辑：属性选择 + 运算符 + 值；条件可拖拽排序；AND/OR 聚合；
    - 表达式生成：将条件自动拼装为 Aviator 表达式；
    - 动作编辑：支持 alert/service/log 三类，字段按类型动态展现；
    - 保存与测试：调用后端接口完成校验与持久化、命中测试。
- 依赖 API：
    - 获取 TSL 属性：GET /iot/product/productTsl/{id}（已有）
    - 规则 CRUD：/iot/rule（已有）
    - 新增扩展：/iot/rule/validate、/iot/rule/test（本次新增）
- 前端 API 封装文件：[rule.js](file:///d:/Code/Projects/zwei/web/src/api/rule/rule.js)

## 后端扩展接口

-
控制器文件：[IotRuleSupportController](file:///d:/Code/Projects/zwei/module-iot/module-iot-rule/src/main/java/com/zwei/module/iot/rule/controller/IotRuleSupportController.java)
-
服务接口：[IRuleSupportService](file:///d:/Code/Projects/zwei/module-iot/module-iot-rule/src/main/java/com/zwei/module/iot/rule/service/IRuleSupportService.java)
-
服务实现：[RuleSupportServiceImpl](file:///d:/Code/Projects/zwei/module-iot/module-iot-rule/src/main/java/com/zwei/module/iot/rule/service/impl/RuleSupportServiceImpl.java)
- DTO：
    - [RuleValidateRequest](file:///d:/Code/Projects/zwei/module-iot/module-iot-rule/src/main/java/com/zwei/module/iot/rule/domain/RuleValidateRequest.java)
    - [RuleTestRequest](file:///d:/Code/Projects/zwei/module-iot/module-iot-rule/src/main/java/com/zwei/module/iot/rule/domain/RuleTestRequest.java)

### 1) 规则表达式校验

- URL：POST /iot/rule/validate
- 权限：iot:rule:validate
- 请求体：

```json
{ "ruleExpression": "temperature > 30 && humidity < 50" }
```

- 响应体：

```json
{ "code": 200, "msg": "success", "ok": true }
```

- 业务逻辑：调用 AviatorEvaluator.validate(expr) 校验语法；失败抛 ServiceException，并返回错误信息。

### 2) 规则测试

- URL：POST /iot/rule/test
- 权限：iot:rule:test
- 请求体：

```json
{
  "ruleExpression": "temperature > 30 && humidity < 50",
  "context": { "temperature": 31, "humidity": 45 }
}
```

- 响应体：

```json
{ "code": 200, "msg": "success", "match": true }
```

- 业务逻辑：AviatorEvaluator.execute(expr, context) 得到布尔值；异常时抛 ServiceException。

## 数据模型与保存格式

- 领域模型：IotRule（已有），动作列表字段 actionList（IotRuleAction）。
- 保存请求示例（前端）：

```json
{
  "ruleName": "高温告警",
  "productKey": "prod-001",
  "status": 0,
  "ruleExpression": "temperature > 30 && humidity < 50",
  "actionList": [
    { "actionType": "alert", "config": "{\"level\":\"WARN\",\"message\":\"温度过高\"}" },
    { "actionType": "log", "config": "{\"message\":\"记录一次高温事件\"}" }
  ]
}
```

- 后端处理：沿用 IotRuleServiceImpl 新增流程（含 Aviator 校验与缓存清理）。
    -
    参考：[IotRuleServiceImpl](file:///d:/Code/Projects/zwei/module-iot/module-iot-rule/src/main/java/com/zwei/module/iot/rule/service/impl/IotRuleServiceImpl.java#L66-L80)

## 用户体验与错误处理

- 表达式为空时提示生成；必填项缺失给出警告；
- 接口失败统一使用 ElMessage 错误提示；
- 响应式布局：条件与动作行在窄屏自动换行，按钮区域自适应。

## 集成与安全

- 控制器与服务均添加 @Slf4j 注解，符合日志规范；
- 权限点：
    - iot:rule:validate
    - iot:rule:test
- Redis 缓存策略保持不变（列表缓存键 iot:rule:product:{productKey}），新增接口不写缓存。

## 后续可扩展

- 可视化编排图形化库集成（如 X6/GoJS/Vue Flow），以节点连接方式表达条件与动作；
- 动作类型字典化与参数表单动态生成；
- 规则调试记录与测试用例归档。

