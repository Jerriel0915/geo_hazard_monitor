# Tasks: 创建 zwei-iot-parser 模块并移除反向依赖

## Phase 1: 创建新模块骨架

- [ ] 1.1 创建 `server/zwei-iot-parser/` 目录结构（src/main/java + src/test/java）
- [ ] 1.2 编写 `server/zwei-iot-parser/pom.xml`（依赖 zwei-common + fastjson2 + lombok）
- [ ] 1.3 在 `server/pom.xml` 注册新模块（modules + dependencyManagement）
- [ ] 1.4 运行 `mvn compile -pl zwei-iot-parser` 验证空模块编译通过

## Phase 2: 定义解析元数据抽象接口

- [ ] 2.1 创建 `com.zwei.iot.parser.metadata.SensorMetadataView` 接口（deviceId/sensorId/sensorNo/attributes）
- [ ] 2.2 创建 `com.zwei.iot.parser.metadata.SensorAttributeView` 接口（attrCode/attrName/unit）
- [ ] 2.3 让 `com.zwei.iot.device.domain.SensorMetadata` 实现 SensorMetadataView
- [ ] 2.4 让 `com.zwei.iot.device.domain.SensorAttribute` 实现 SensorAttributeView

## Phase 3: 迁移核心模型类

- [ ] 3.1 迁移 `StandardMeasurementPoint` → `com.zwei.iot.parser.model`（修改 package，改 import SensorMetadataView）
- [ ] 3.2 迁移 `MonitorTopic` → `com.zwei.iot.parser.model`
- [ ] 3.3 迁移 `MonitorTopicParser` → `com.zwei.iot.parser.support`
- [ ] 3.4 迁移 `MonitorPayloadParser` 接口 → `com.zwei.iot.parser.spi`
- [ ] 3.5 迁移 `SysMonitorPayloadParser` → `com.zwei.iot.parser.impl`（改用 SensorMetadataView）
- [ ] 3.6 迁移 `GbMonitorPayloadParser` → `com.zwei.iot.parser.impl`（改用 SensorMetadataView）

## Phase 4: 创建 Spring 自动配置

- [ ] 4.1 创建 `ParserAutoConfiguration`（@ComponentScan basePackages="com.zwei.iot.parser"）
- [ ] 4.2 创建 `spring.factories` 或使用 `@AutoConfiguration`（Spring Boot 3.x 方式）

## Phase 5: 更新 timeseries 模块（移除旧代码+添加新依赖）

- [ ] 5.1 `zwei-iot-timeseries/pom.xml` 新增 `zwei-iot-parser` 依赖
- [ ] 5.2 删除 timeseries 下的旧文件：`parser/`, `support/MonitorTopic*.java`, `domain/StandardMeasurementPoint.java`
- [ ] 5.3 更新 timeseries 内所有 import 指向新 package
- [ ] 5.4 更新 `MonitorIngestFacade` — 使用 `com.zwei.iot.parser.spi.MonitorPayloadParser`
- [ ] 5.5 更新 `MonitorIngestStreamService` — 使用 `com.zwei.iot.parser.model.StandardMeasurementPoint`
- [ ] 5.6 更新 `MonitorIngestConsumerService` — 使用新 package
- [ ] 5.7 更新 `IotdbTimeSeriesService` — 使用新 package（如有引用）
- [ ] 5.8 更新 `IotdbPathResolver` — 使用新 package（如有引用）

## Phase 6: 增强 MqttMessageReceivedEvent（携带 deviceId）

- [ ] 6.1 `MqttMessageReceivedEvent` 新增 `deviceId` 字段
- [ ] 6.2 更新 `MqttServerMessageListener` — 发布事件时携带 deviceId
- [ ] 6.3 验证 `zwei-log/MqttMessageLogService` 不受影响（新增字段向后兼容）

## Phase 7: 打破反向依赖 — broker 只依赖 parser

- [ ] 7.1 `zwei-iot-broker/pom.xml` 移除 `zwei-iot-timeseries` 依赖
- [ ] 7.2 `zwei-iot-broker/pom.xml` 新增 `zwei-iot-parser` 依赖
- [ ] 7.3 移除 `MqttServerMessageListener` 中对 `MonitorIngestFacade` 的注入和调用
- [ ] 7.4 `MqttServerMessageListener` 改为仅发布事件（无需调用 ingest）

## Phase 8: timeseries 侧新增事件监听

- [ ] 8.1 `MonitorIngestFacade` 新增 `@EventListener` 方法消费 `MqttMessageReceivedEvent`
- [ ] 8.2 确保事件处理为同步（默认），保证消息有序处理

## Phase 9: 测试迁移与验证

- [ ] 9.1 迁移 `SysMonitorPayloadParserTest` 到 parser 模块
- [ ] 9.2 更新测试 import 指向新 package
- [ ] 9.3 运行 `mvn test -pl zwei-iot-parser` 验证测试通过

## Phase 10: 全量编译与验证

- [ ] 10.1 运行 `mvn clean compile` 验证 15 模块全部编译通过
- [ ] 10.2 运行 `mvn dependency:tree -pl zwei-iot-parser` 验证零 IoT 依赖
- [ ] 10.3 确认 broker 模块不再 import timeseries 类
- [ ] 10.4 确认所有 @Component Bean 能被 Spring 扫描到（检查 zwei-admin 扫描范围）

## Phase 11: 文档同步

- [ ] 11.1 更新根 CLAUDE.md 模块索引表 + 模块结构图
- [ ] 11.2 创建 `server/zwei-iot-parser/CLAUDE.md`
- [ ] 11.3 更新 `server/zwei-iot-timeseries/CLAUDE.md`
- [ ] 11.4 更新 `server/zwei-iot-broker/CLAUDE.md`
