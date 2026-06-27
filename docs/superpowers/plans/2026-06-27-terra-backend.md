# Terra 智能助手后端实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 构建 Terra 智能助手后端 — Maven 双模块（terra-core + terra-agent），包含设置管理 CRUD、Anthropic Messages API 对话引擎、React Loop 工具执行框架。

**架构：** terra-core 定义工具/技能接口供业务模块实现；terra-agent 包含对话引擎，通过 Spring IoC 运行时发现工具实现，不编译期依赖业务模块。对话用 SSE 流式 + React Loop，支持后端同步执行和前端异步回调两种工具执行路径。

**技术栈：** Java 17, Spring Boot 4.0.3, Spring AI (ChatModel 接口), MyBatis, MySQL 8.0, SSE

**规格文档：** `docs/superpowers/specs/2026-06-27-terra-ai-assistant-design.md`

---

## 文件结构

### 创建的文件

```
server/zwei-terra/
├── pom.xml                                           # terra 父模块
├── zwei-terra-core/
│   ├── pom.xml
│   └── src/main/java/com/zwei/terra/core/
│       ├── tool/
│       │   ├── TerraTool.java                        # @TerraTool 注解
│       │   ├── ToolMethod.java                       # @ToolMethod 注解
│       │   ├── TerraBackendTool.java                 # 后端工具标记接口
│       │   ├── ToolDefinition.java                   # 工具定义 DTO
│       │   └── ToolResult.java                       # 工具执行结果 DTO
│       ├── skill/
│       │   ├── SkillManifest.java                    # 技能清单接口
│       │   └── SkillTrigger.java                     # 触发条件 DTO
│       └── model/
│           ├── ChatMessage.java                      # 对话消息 DTO
│           └── TerraConstants.java                   # 常量
├── zwei-terra-agent/
│   ├── pom.xml
│   └── src/main/java/com/zwei/terra/agent/
│       ├── config/
│       │   ├── TerraAutoConfiguration.java           # 自动装配
│       │   └── TerraProperties.java                  # 配置属性
│       ├── domain/
│       │   ├── TerraPersonality.java
│       │   ├── TerraModelConfig.java
│       │   ├── TerraSkill.java
│       │   ├── TerraTool.java
│       │   ├── TerraConversation.java
│       │   └── TerraMessage.java
│       ├── mapper/
│       │   ├── TerraPersonalityMapper.java
│       │   ├── TerraModelConfigMapper.java
│       │   ├── TerraSkillMapper.java
│       │   ├── TerraToolMapper.java
│       │   ├── TerraSkillToolMapper.java
│       │   ├── TerraConversationMapper.java
│       │   └── TerraMessageMapper.java
│       ├── service/
│       │   ├── ITerraPersonalityService.java
│       │   ├── ITerraModelConfigService.java
│       │   ├── ITerraSkillService.java
│       │   ├── ITerraToolService.java
│       │   ├── ITerraConversationService.java
│       │   └── impl/
│       │       ├── TerraPersonalityServiceImpl.java
│       │       ├── TerraModelConfigServiceImpl.java
│       │       ├── TerraSkillServiceImpl.java
│       │       ├── TerraToolServiceImpl.java
│       │       └── TerraConversationServiceImpl.java
│       ├── controller/
│       │   ├── TerraChatController.java
│       │   ├── TerraConversationController.java
│       │   ├── TerraPersonalityController.java
│       │   ├── TerraModelConfigController.java
│       │   ├── TerraSkillController.java
│       │   └── TerraToolController.java
│       ├── tool/
│       │   ├── ToolManager.java                      # 工具注册+执行
│       │   └── ToolRegistration.java                 # 工具注册信息
│       ├── skill/
│       │   ├── SkillResolver.java                    # SKILL.md 解析
│       │   └── SkillSyncService.java                 # 预置技能同步
│       └── chat/
│           ├── AnthropicChatModel.java               # Anthropic HTTP 客户端
│           ├── ChatService.java                      # 对话编排
│           ├── ReactLoop.java                        # React Loop 引擎
│           └── TerraSseEmitter.java                  # SSE 封装

db/upgrade/
└── terra_v1.0.sql                                    # DDL + 初始化数据

# 修改的文件
server/pom.xml                                        # 新增 <module>zwei-terra</module>
server/zwei-admin/pom.xml                             # 新增 terra-agent 依赖
```

---

## 任务 1：创建 terra Maven 模块骨架

**文件：**
- 创建：`server/zwei-terra/pom.xml`
- 创建：`server/zwei-terra/zwei-terra-core/pom.xml`
- 创建：`server/zwei-terra/zwei-terra-agent/pom.xml`
- 修改：`server/pom.xml`（新增 module 注册）
- 修改：`server/zwei-admin/pom.xml`（新增 terra-agent 依赖）

- [ ] **步骤 1：创建 terra 父模块 pom.xml**

创建 `server/zwei-terra/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>zwei</artifactId>
        <groupId>com.zwei</groupId>
        <version>3.9.2</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>zwei-terra</artifactId>
    <description>Terra AI Assistant — 智能助手</description>
    <packaging>pom</packaging>

    <modules>
        <module>zwei-terra-core</module>
        <module>zwei-terra-agent</module>
    </modules>
</project>
```

- [ ] **步骤 2：创建 terra-core 子模块 pom.xml**

创建 `server/zwei-terra/zwei-terra-core/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>zwei-terra</artifactId>
        <groupId>com.zwei</groupId>
        <version>3.9.2</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>zwei-terra-core</artifactId>
    <description>Terra Core — 接口协议层，业务模块依赖此模块实现 tool</description>

    <dependencies>
        <dependency>
            <groupId>com.zwei</groupId>
            <artifactId>zwei-common</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **步骤 3：创建 terra-agent 子模块 pom.xml**

创建 `server/zwei-terra/zwei-terra-agent/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>zwei-terra</artifactId>
        <groupId>com.zwei</groupId>
        <version>3.9.2</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>zwei-terra-agent</artifactId>
    <description>Terra Agent — 智能助手 Agent 实现</description>

    <dependencies>
        <dependency>
            <groupId>com.zwei</groupId>
            <artifactId>zwei-terra-core</artifactId>
            <version>${zwei.version}</version>
        </dependency>
        <dependency>
            <groupId>com.zwei</groupId>
            <artifactId>zwei-framework</artifactId>
        </dependency>
        <!-- Spring AI core (仅接口，不引入完整 starter) -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-core</artifactId>
            <version>1.0.0</version>
        </dependency>
        <!-- HTTP client for Anthropic API -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-webflux</artifactId>
        </dependency>
    </dependencies>
</project>
```

> 注意：`spring-ai-core` 版本需在步骤 5 验证兼容性。如果 1.0.0 不兼容 Spring Boot 4.x，可改为只依赖 `spring-webflux` 自行实现，不依赖 Spring AI 接口。

- [ ] **步骤 4：注册到根 POM**

在 `server/pom.xml` 的 `<modules>` 末尾新增:

```xml
<module>zwei-terra</module>
```

- [ ] **步骤 5：验证 Maven 编译**

运行：
```bash
cd server && mvn clean compile -pl zwei-terra -am -DskipTests
```
预期：BUILD SUCCESS。如果 spring-ai-core 依赖解析失败，暂时从 terra-agent pom.xml 中移除 spring-ai-core 和 spring-webflux 依赖，改为步骤 2 中只保留 terra-core + zwei-common 依赖，后续任务再处理 Spring AI 集成。

- [ ] **步骤 6：添加 terra-agent 到 zwei-admin**

在 `server/zwei-admin/pom.xml` 的 `<dependencies>` 中新增:

```xml
<dependency>
    <groupId>com.zwei</groupId>
    <artifactId>zwei-terra-agent</artifactId>
    <version>${zwei.version}</version>
</dependency>
```

运行：
```bash
cd server && mvn clean compile -pl zwei-admin -am -DskipTests
```
预期：BUILD SUCCESS

- [ ] **步骤 7：Commit**

```bash
git add server/zwei-terra/ server/pom.xml server/zwei-admin/pom.xml
git commit -m "feat(terra): 创建 terra Maven 双模块骨架 (core + agent)"
```

---

## 任务 2：创建数据库 DDL

**文件：**
- 创建：`db/upgrade/terra_v1.0.sql`

- [ ] **步骤 1：编写完整 DDL**

创建 `db/upgrade/terra_v1.0.sql`，包含规格中定义的全部 7 张表 + 初始化数据（核心灵魂 personality、默认模型配置占位）:

```sql
-- ============================================================
-- Terra AI Assistant DDL v1.0
-- 依赖: geo_hazard_monitor 基础数据库
-- ============================================================

-- 1. 人格配置表
CREATE TABLE IF NOT EXISTS terra_personality (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    layer_type      VARCHAR(20)  NOT NULL COMMENT 'core=核心灵魂, role=角色外衣',
    name            VARCHAR(100) NOT NULL COMMENT '层级名称',
    content         TEXT         NOT NULL COMMENT '人格内容（system prompt）',
    is_active       TINYINT(1)   DEFAULT 1,
    is_preset       TINYINT(1)   DEFAULT 0,
    sort_order      INT          DEFAULT 0,
    create_by       VARCHAR(64)  DEFAULT '',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)  DEFAULT '',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag        CHAR(1)      DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='terra 人格配置表';

-- 2. 模型服务商配置表
CREATE TABLE IF NOT EXISTS terra_model_config (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    base_url        VARCHAR(500) NOT NULL,
    api_key         VARCHAR(500) NOT NULL COMMENT 'AES 加密',
    model_name      VARCHAR(200) NOT NULL,
    max_tokens      INT          DEFAULT 4096,
    temperature     DECIMAL(3,2) DEFAULT 0.70,
    is_active       TINYINT(1)   DEFAULT 0,
    sort_order      INT          DEFAULT 0,
    create_by       VARCHAR(64)  DEFAULT '',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)  DEFAULT '',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag        CHAR(1)      DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='terra 模型服务商配置表';

-- 3. 技能元数据表
CREATE TABLE IF NOT EXISTS terra_skill (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
    skill_key         VARCHAR(100) NOT NULL UNIQUE,
    name              VARCHAR(200) NOT NULL,
    description       VARCHAR(500) DEFAULT '',
    directory_path    VARCHAR(500) NOT NULL,
    triggers_summary  VARCHAR(500) DEFAULT '',
    tools_summary     VARCHAR(500) DEFAULT '',
    is_preset         TINYINT(1)   DEFAULT 0,
    is_enabled        TINYINT(1)   DEFAULT 1,
    installed_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    installed_by      VARCHAR(64)  DEFAULT '',
    sort_order        INT          DEFAULT 0,
    create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag          CHAR(1)      DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='terra 技能元数据表';

-- 4. 工具注册表
CREATE TABLE IF NOT EXISTS terra_tool (
    id                  BIGINT       AUTO_INCREMENT PRIMARY KEY,
    tool_key            VARCHAR(100) NOT NULL UNIQUE,
    name                VARCHAR(200) NOT NULL,
    description         VARCHAR(500) DEFAULT '',
    source              VARCHAR(20)  NOT NULL COMMENT 'code=代码注册, config=配置化',
    exec_side           VARCHAR(20)  NOT NULL DEFAULT 'backend' COMMENT 'backend, frontend',
    tool_type           VARCHAR(50)  DEFAULT 'query',
    parameters_schema   JSON         COMMENT 'Anthropic tool input_schema',
    endpoint            VARCHAR(500) COMMENT '配置化工具调用地址',
    timeout_seconds     INT          DEFAULT 30,
    is_preset           TINYINT(1)   DEFAULT 0,
    is_enabled          TINYINT(1)   DEFAULT 1,
    sort_order          INT          DEFAULT 0,
    create_by           VARCHAR(64)  DEFAULT '',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_by           VARCHAR(64)  DEFAULT '',
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag            CHAR(1)      DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='terra 工具注册表';

-- 5. 技能-工具关联表
CREATE TABLE IF NOT EXISTS terra_skill_tool (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill_id    BIGINT NOT NULL,
    tool_id     BIGINT NOT NULL,
    UNIQUE KEY uk_skill_tool (skill_id, tool_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='terra 技能-工具关联表';

-- 6. 对话会话表
CREATE TABLE IF NOT EXISTS terra_conversation (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    title             VARCHAR(200) DEFAULT '新对话',
    status            VARCHAR(20)  DEFAULT 'active',
    last_message_time DATETIME,
    message_count     INT          DEFAULT 0,
    create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag          CHAR(1)      DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='terra 对话会话表';

-- 7. 对话消息表
CREATE TABLE IF NOT EXISTS terra_message (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT      NOT NULL,
    role            VARCHAR(20) NOT NULL COMMENT 'user, assistant, tool',
    content         TEXT,
    tool_calls      JSON        COMMENT 'assistant 发起的工具调用',
    tool_call_id    VARCHAR(100),
    tokens_used     INT         DEFAULT 0,
    create_time     DATETIME    DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='terra 对话消息表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 核心灵魂（系统预置，不可删除）
INSERT INTO terra_personality (layer_type, name, content, is_active, is_preset, sort_order, create_by)
VALUES ('core', 'Terra 核心灵魂',
'你是 Terra，知微地质灾害监测管理系统的智能助手。

## 身份
- 你的名字是 Terra（泰拉），寓意"大地"，与地质灾害监测的使命契合
- 你是知微系统的内置助手，服务于系统用户

## 行为准则
- 用简洁专业的中文回答
- 当用户的问题涉及系统功能时，主动使用工具查询数据或操作前端
- 不确定时坦诚告知，不要编造信息
- 保护用户隐私，不泄露认证信息、密钥等敏感数据',
1, 1, 0, 'system');

-- 默认模型配置（占位，管理员需编辑后激活）
INSERT INTO terra_model_config (name, base_url, api_key, model_name, max_tokens, temperature, is_active, sort_order, create_by)
VALUES ('默认配置', 'https://api.anthropic.com', 'PLACEHOLDER', 'claude-sonnet-4-20250514', 4096, 0.70, 0, 0, 'system');
```

- [ ] **步骤 2：执行 DDL 到本地数据库**

运行：
```bash
mysql -u root -pwodepassword geo_hazard_monitor < db/upgrade/terra_v1.0.sql
```
验证：`SELECT COUNT(*) FROM terra_personality;` 返回 1。

- [ ] **步骤 3：Commit**

```bash
git add db/upgrade/terra_v1.0.sql
git commit -m "feat(terra): 创建 terra 7张表 DDL + 初始化数据"
```

---

## 任务 3：创建 terra-core 接口层

**文件：**
- 创建：`server/zwei-terra/zwei-terra-core/src/main/java/com/zwei/terra/core/tool/TerraTool.java`
- 创建：`server/zwei-terra/zwei-terra-core/src/main/java/com/zwei/terra/core/tool/ToolMethod.java`
- 创建：`server/zwei-terra/zwei-terra-core/src/main/java/com/zwei/terra/core/tool/TerraBackendTool.java`
- 创建：`server/zwei-terra/zwei-terra-core/src/main/java/com/zwei/terra/core/tool/ToolDefinition.java`
- 创建：`server/zwei-terra/zwei-terra-core/src/main/java/com/zwei/terra/core/tool/ToolResult.java`
- 创建：`server/zwei-terra/zwei-terra-core/src/main/java/com/zwei/terra/core/skill/SkillManifest.java`
- 创建：`server/zwei-terra/zwei-terra-core/src/main/java/com/zwei/terra/core/skill/SkillTrigger.java`

- [ ] **步骤 1：创建 @TerraTool 注解**

```java
package com.zwei.terra.core.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Terra 工具注解，标记一个类为 Terra 后端工具。
 * <p>
 * 业务模块实现 {@link TerraBackendTool} 接口并加此注解，
 * terra-agent 的 ToolManager 会自动扫描注册。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TerraTool {
    /** 工具唯一标识 */
    String name();
    /** 工具描述 */
    String description();
    /** 工具类别 */
    String category() default "general";
}
```

- [ ] **步骤 2：创建 @ToolMethod 注解**

```java
package com.zwei.terra.core.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记工具类中的方法为可被 LLM 调用的工具方法。
 * 一个工具类可以有多个 @ToolMethod 方法。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ToolMethod {
    /** 方法描述（会传递给 LLM） */
    String description();
}
```

- [ ] **步骤 3：创建 TerraBackendTool 标记接口**

```java
package com.zwei.terra.core.tool;

/**
 * 后端工具标记接口。
 * <p>
 * 业务模块实现此接口并加 {@link TerraTool} 注解。
 * terra-agent 通过 Spring 容器扫描所有实现类。
 */
public interface TerraBackendTool {
}
```

- [ ] **步骤 4：创建 ToolDefinition DTO**

```java
package com.zwei.terra.core.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * 工具定义 — 描述一个工具的 schema，用于传递给 LLM。
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDefinition {
    /** 工具标识 */
    private String name;
    /** 工具描述 */
    private String description;
    /** 参数 JSON Schema（Anthropic input_schema 格式） */
    private Map<String, Object> parametersSchema;
    /** 执行位置：backend 或 frontend */
    private String execSide;
}
```

- [ ] **步骤 5：创建 ToolResult DTO**

```java
package com.zwei.terra.core.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 工具执行结果。
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult {
    private boolean success;
    private Object result;
    private String error;

    public static ToolResult success(Object result) {
        return ToolResult.builder().success(true).result(result).build();
    }

    public static ToolResult failure(String error) {
        return ToolResult.builder().success(false).error(error).build();
    }
}
```

- [ ] **步骤 6：创建 SkillManifest 接口和 SkillTrigger DTO**

```java
package com.zwei.terra.core.skill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

/**
 * 技能触发条件 DTO。
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillTrigger {
    /** 触发类型：keyword, intent */
    private String type;
    /** 关键词列表（type=keyword 时使用） */
    private List<String> keywords;
    /** 意图标识（type=intent 时使用） */
    private String intent;
}
```

```java
package com.zwei.terra.core.skill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 技能清单 — 从 SKILL.md 解析出的结构化数据。
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillManifest {
    private String name;
    private String description;
    private List<SkillTrigger> triggers;
    private List<String> tools;
    private String instructions;
}
```

- [ ] **步骤 7：验证编译并 Commit**

```bash
cd server && mvn clean compile -pl zwei-terra-core -am -DskipTests
git add server/zwei-terra/zwei-terra-core/
git commit -m "feat(terra-core): 创建工具注解、接口和共享 DTO"
```

---

## 任务 4：创建 terra-agent Domain + Mapper 层

**文件：**
- 创建：6 个 Domain 实体类 + 7 个 Mapper 接口 + 7 个 Mapper XML

- [ ] **步骤 1：创建 Domain 实体类**

为每张表创建实体类，继承 `BaseEntity`，使用 `@SuperBuilder`。以 `TerraModelConfig` 为例:

```java
package com.zwei.terra.agent.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * Terra 模型服务商配置
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TerraModelConfig extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private Integer maxTokens;
    private BigDecimal temperature;
    private Integer isActive;
    private Integer sortOrder;
    private String delFlag;
}
```

按同样模式创建其余 5 个实体：`TerraPersonality`、`TerraSkill`、`TerraTool`、`TerraConversation`、`TerraMessage`。

`TerraMessage` 特殊：不需要继承 BaseEntity（无 createBy/updateBy 字段），只需 id + conversationId + role + content + toolCalls + toolCallId + tokensUsed + createTime。

- [ ] **步骤 2：创建 Mapper 接口**

为每个实体创建 `@Mapper` 接口。以 `TerraModelConfigMapper` 为例:

```java
package com.zwei.terra.agent.mapper;

import com.zwei.terra.agent.domain.TerraModelConfig;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface TerraModelConfigMapper {
    List<TerraModelConfig> selectList(TerraModelConfig query);
    TerraModelConfig selectById(Long id);
    TerraModelConfig selectActive();
    int insert(TerraModelConfig config);
    int update(TerraModelConfig config);
    int deactivateAll();
    int deleteById(Long id);
    TerraModelConfig checkNameUnique(String name, Long id);
}
```

按同样模式创建其余 6 个 Mapper。关键方法:
- `TerraPersonalityMapper`: selectList, selectById, selectActiveCore, selectActiveRoles, insert, update, deleteById
- `TerraSkillMapper`: selectList, selectByKey, selectById, selectEnabled, insert, update, deleteById
- `TerraToolMapper`: selectList, selectByKey, selectById, selectEnabled, insert, update, deleteById
- `TerraSkillToolMapper`: selectBySkillId, insert, deleteBySkillId
- `TerraConversationMapper`: selectByUserId, selectById, insert, update, deleteById
- `TerraMessageMapper`: selectByConversationId, insert, updateConversationStats

- [ ] **步骤 3：创建 Mapper XML**

创建 `server/zwei-terra/zwei-terra-agent/src/main/resources/mapper/` 目录。
为每个 Mapper 接口创建对应的 XML 文件。以 `TerraModelConfigMapper.xml` 为例:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.zwei.terra.agent.mapper.TerraModelConfigMapper">

    <resultMap id="ModelConfigResult" type="com.zwei.terra.agent.domain.TerraModelConfig">
        <id column="id" property="id"/>
        <result column="name" property="name"/>
        <result column="base_url" property="baseUrl"/>
        <result column="api_key" property="apiKey"/>
        <result column="model_name" property="modelName"/>
        <result column="max_tokens" property="maxTokens"/>
        <result column="temperature" property="temperature"/>
        <result column="is_active" property="isActive"/>
        <result column="sort_order" property="sortOrder"/>
        <result column="create_by" property="createBy"/>
        <result column="create_time" property="createTime"/>
        <result column="update_by" property="updateBy"/>
        <result column="update_time" property="updateTime"/>
        <result column="del_flag" property="delFlag"/>
    </resultMap>

    <select id="selectList" parameterType="com.zwei.terra.agent.domain.TerraModelConfig" resultMap="ModelConfigResult">
        SELECT * FROM terra_model_config
        <where>
            del_flag = '0'
            <if test="name != null and name != ''">AND name LIKE CONCAT('%', #{name}, '%')</if>
            <if test="isActive != null">AND is_active = #{isActive}</if>
        </where>
        ORDER BY sort_order ASC, create_time DESC
    </select>

    <select id="selectById" parameterType="java.lang.Long" resultMap="ModelConfigResult">
        SELECT * FROM terra_model_config WHERE id = #{id} AND del_flag = '0'
    </select>

    <select id="selectActive" resultMap="ModelConfigResult">
        SELECT * FROM terra_model_config WHERE is_active = 1 AND del_flag = '0' LIMIT 1
    </select>

    <insert id="insert" parameterType="com.zwei.terra.agent.domain.TerraModelConfig" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO terra_model_config (name, base_url, api_key, model_name, max_tokens, temperature,
            is_active, sort_order, create_by, create_time)
        VALUES (#{name}, #{baseUrl}, #{apiKey}, #{modelName}, #{maxTokens}, #{temperature},
            #{isActive}, #{sortOrder}, #{createBy}, NOW())
    </insert>

    <update id="update" parameterType="com.zwei.terra.agent.domain.TerraModelConfig">
        UPDATE terra_model_config SET
        <if test="name != null">name = #{name},</if>
        <if test="baseUrl != null">base_url = #{baseUrl},</if>
        <if test="apiKey != null">api_key = #{apiKey},</if>
        <if test="modelName != null">model_name = #{modelName},</if>
        <if test="maxTokens != null">max_tokens = #{maxTokens},</if>
        <if test="temperature != null">temperature = #{temperature},</if>
        <if test="isActive != null">is_active = #{isActive},</if>
        <if test="sortOrder != null">sort_order = #{sortOrder},</if>
        update_by = #{updateBy}, update_time = NOW()
        WHERE id = #{id}
    </update>

    <update id="deactivateAll">
        UPDATE terra_model_config SET is_active = 0 WHERE del_flag = '0'
    </update>

    <update id="deleteById" parameterType="java.lang.Long">
        UPDATE terra_model_config SET del_flag = '1' WHERE id = #{id}
    </update>

    <select id="checkNameUnique" resultMap="ModelConfigResult">
        SELECT * FROM terra_model_config WHERE name = #{name} AND del_flag = '0'
        <if test="id != null">AND id != #{id}</if>
        LIMIT 1
    </select>
</mapper>
```

其余 Mapper XML 按同样模式编写。关键注意事项:
- 所有表使用 `del_flag = '0'` 条件过滤已删除
- 逻辑删除：`UPDATE SET del_flag = '1'`
- `useGeneratedKeys="true"` 获取自增 ID
- MyBatis XML 文件放在 `zwei-terra-agent/src/main/resources/mapper/` 目录

- [ ] **步骤 4：配置 MyBatis 扫描路径**

创建 `server/zwei-terra/zwei-terra-agent/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
com.zwei.terra.agent.config.TerraAutoConfiguration
```

创建 `TerraAutoConfiguration.java`:

```java
package com.zwei.terra.agent.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

/**
 * Terra Agent 自动装配。
 * zwei-admin 启动时自动扫描 com.zwei.terra 包下所有组件。
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.zwei.terra")
@PropertySource(value = "classpath:terra-default.yml", ignoreResourceNotFound = true)
public class TerraAutoConfiguration {
}
```

确保 `zwei-admin` 的 MyBatis 配置中 mapper-locations 包含 `classpath*:mapper/**/*.xml`（通常已配置为全局扫描）。

- [ ] **步骤 5：验证编译并 Commit**

```bash
cd server && mvn clean compile -pl zwei-terra-agent -am -DskipTests
git add server/zwei-terra/zwei-terra-agent/
git commit -m "feat(terra-agent): 创建 Domain 实体 + Mapper 接口 + XML + 自动装配"
```

---

## 任务 5：实现设置管理 Service + Controller（人格 + 模型配置）

**文件：**
- 创建：`TerraPersonalityServiceImpl.java` + `TerraPersonalityController.java`
- 创建：`TerraModelConfigServiceImpl.java` + `TerraModelConfigController.java`

- [ ] **步骤 1：创建 TerraProperties 配置属性类**

```java
package com.zwei.terra.agent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "terra")
@Getter
@Setter
public class TerraProperties {
    private Skills skills = new Skills();
    private Chat chat = new Chat();

    @Getter @Setter
    public static class Skills {
        private String basePath = System.getProperty("user.home") + "/terra/skills";
    }

    @Getter @Setter
    public static class Chat {
        private int maxReactRounds = 10;
        private int defaultTimeoutSeconds = 30;
        private int maxHistoryMessages = 20;
    }
}
```

- [ ] **步骤 2：实现人格 Service + Controller**

`ITerraPersonalityService.java`:
```java
package com.zwei.terra.agent.service;

import com.zwei.terra.agent.domain.TerraPersonality;
import java.util.List;

public interface ITerraPersonalityService {
    List<TerraPersonality> selectList();
    String buildSystemPrompt();
    void updateRole(TerraPersonality personality, String operator);
    void toggleActive(Long id, String operator);
}
```

`TerraPersonalityServiceImpl.java`:
```java
package com.zwei.terra.agent.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.terra.agent.domain.TerraPersonality;
import com.zwei.terra.agent.mapper.TerraPersonalityMapper;
import com.zwei.terra.agent.service.ITerraPersonalityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TerraPersonalityServiceImpl implements ITerraPersonalityService {

    @Autowired
    private TerraPersonalityMapper personalityMapper;

    @Override
    public List<TerraPersonality> selectList() {
        return personalityMapper.selectList(new TerraPersonality());
    }

    @Override
    public String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        // 核心灵魂
        TerraPersonality core = personalityMapper.selectActiveCore();
        if (core != null) {
            sb.append(core.getContent());
        }
        // 角色外衣
        List<TerraPersonality> roles = personalityMapper.selectActiveRoles();
        for (TerraPersonality role : roles) {
            sb.append("\n\n").append(role.getContent());
        }
        return sb.toString();
    }

    @Override
    public void updateRole(TerraPersonality personality, String operator) {
        TerraPersonality existing = personalityMapper.selectById(personality.getId());
        if (existing == null) {
            throw new ServiceException("人格配置不存在");
        }
        if ("core".equals(existing.getLayerType()) && existing.getIsPreset() == 1) {
            // 核心灵魂只允许修改 content
            existing.setContent(personality.getContent());
        } else {
            existing.setName(personality.getName());
            existing.setContent(personality.getContent());
            existing.setSortOrder(personality.getSortOrder());
        }
        existing.setUpdateBy(operator);
        personalityMapper.update(existing);
    }

    @Override
    public void toggleActive(Long id, String operator) {
        TerraPersonality p = personalityMapper.selectById(id);
        if (p == null) throw new ServiceException("人格配置不存在");
        if ("core".equals(p.getLayerType()) && p.getIsPreset() == 1) {
            throw new ServiceException("核心灵魂不可停用");
        }
        p.setIsActive(p.getIsActive() == 1 ? 0 : 1);
        p.setUpdateBy(operator);
        personalityMapper.update(p);
    }
}
```

`TerraPersonalityController.java`:
```java
package com.zwei.terra.agent.controller;

import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.terra.agent.domain.TerraPersonality;
import com.zwei.terra.agent.service.ITerraPersonalityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/terra/personality")
public class TerraPersonalityController extends BaseController {

    @Autowired
    private ITerraPersonalityService personalityService;

    @GetMapping
    @PreAuthorize("@ss.hasPermi('terra:settings')")
    public AjaxResult list() {
        return success(personalityService.selectList());
    }

    @PutMapping
    @PreAuthorize("@ss.hasPermi('terra:settings')")
    public AjaxResult update(@RequestBody TerraPersonality personality) {
        personalityService.updateRole(personality, getUsername());
        return success();
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("@ss.hasPermi('terra:settings')")
    public AjaxResult toggle(@PathVariable Long id) {
        personalityService.toggleActive(id, getUsername());
        return success();
    }
}
```

- [ ] **步骤 3：实现模型配置 Service + Controller**

`ITerraModelConfigService.java`:
```java
package com.zwei.terra.agent.service;

import com.zwei.terra.agent.domain.TerraModelConfig;
import java.util.List;

public interface ITerraModelConfigService {
    List<TerraModelConfig> selectList();
    TerraModelConfig selectById(Long id);
    TerraModelConfig getActiveConfig();
    TerraModelConfig create(TerraModelConfig config, String operator);
    void update(TerraModelConfig config, String operator);
    void delete(Long id);
    void activate(Long id, String operator);
}
```

`TerraModelConfigServiceImpl.java` 关键方法（activate 互斥逻辑）:
```java
@Override
@Transactional(rollbackFor = Exception.class)
public void activate(Long id, String operator) {
    TerraModelConfig config = configMapper.selectById(id);
    if (config == null) throw new ServiceException("模型配置不存在");
    configMapper.deactivateAll();  // 先全部取消激活
    TerraModelConfig update = new TerraModelConfig();
    update.setId(id);
    update.setIsActive(1);
    update.setUpdateBy(operator);
    configMapper.update(update);   // 再激活指定配置
}
```

Controller 标准 CRUD，`POST /api/v1/terra/model-configs/{id}/activate` 调用 activate。

> 注意：`api_key` 在返回前端时需要脱敏（只返回 `sk-****xxxx` 格式），在 Service 层做处理。

- [ ] **步骤 4：验证编译并 Commit**

```bash
cd server && mvn clean compile -pl zwei-terra-agent -am -DskipTests
git add server/zwei-terra/zwei-terra-agent/
git commit -m "feat(terra): 人格配置 + 模型配置 Service + Controller"
```

---

## 任务 6：实现技能管理 + 工具管理 Service + Controller

**文件：**
- 创建：`SkillResolver.java`（SKILL.md 解析器）
- 创建：`SkillSyncService.java`（预置技能同步）
- 创建：`TerraSkillServiceImpl.java` + `TerraSkillController.java`
- 创建：`TerraToolServiceImpl.java` + `TerraToolController.java`

- [ ] **步骤 1：实现 SkillResolver — SKILL.md 解析**

```java
package com.zwei.terra.agent.skill;

import com.zwei.terra.core.skill.SkillManifest;
import com.zwei.terra.core.skill.SkillTrigger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析 SKILL.md 文件为 SkillManifest。
 * 支持 YAML front matter + Markdown body 格式。
 */
@Component
@Slf4j
public class SkillResolver {

    private static final Pattern FRONT_MATTER_PATTERN =
        Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n(.*)$", Pattern.DOTALL);

    public SkillManifest parse(Path skillMdPath) throws IOException {
        String content = Files.readString(skillMdPath);
        Matcher matcher = FRONT_MATTER_PATTERN.matcher(content);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid SKILL.md: missing YAML front matter");
        }
        String yaml = matcher.group(1);
        String body = matcher.group(2).trim();

        // 简化解析（避免引入 SnakeYAML 依赖）
        SkillManifest.SkillManifestBuilder builder = SkillManifest.builder();
        builder.instructions(body);

        // 逐行解析 YAML
        List<SkillTrigger> triggers = new ArrayList<>();
        List<String> tools = new ArrayList<>();
        for (String line : yaml.split("\n")) {
            line = line.trim();
            if (line.startsWith("name:")) {
                builder.name(line.substring(5).trim());
            } else if (line.startsWith("description:")) {
                builder.description(line.substring(12).trim());
            } else if (line.startsWith("- ") && line.contains(":")) {
                // 简化的 triggers / tools 列表解析
                // 完整实现需要 YAML parser
            }
        }
        builder.triggers(triggers);
        builder.tools(tools);
        return builder.build();
    }
}
```

- [ ] **步骤 2：实现 SkillSyncService — 预置技能同步**

```java
package com.zwei.terra.agent.skill;

import com.zwei.terra.agent.config.TerraProperties;
import com.zwei.terra.agent.domain.TerraSkill;
import com.zwei.terra.agent.mapper.TerraSkillMapper;
import com.zwei.terra.core.skill.SkillManifest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

@Component
@Slf4j
public class SkillSyncService {

    @Autowired
    private TerraProperties properties;

    @Autowired
    private TerraSkillMapper skillMapper;

    @Autowired
    private SkillResolver skillResolver;

    @EventListener(ApplicationReadyEvent.class)
    public void syncPresetSkills() {
        Path presetDir = Paths.get(properties.getSkills().getBasePath(), "preset");
        if (!Files.exists(presetDir)) {
            log.info("Terra preset skills directory not found: {}", presetDir);
            return;
        }
        try (Stream<Path> dirs = Files.list(presetDir)) {
            dirs.filter(Files::isDirectory).forEach(this::syncOnePreset);
        } catch (IOException e) {
            log.error("Failed to scan preset skills", e);
        }
    }

    private void syncOnePreset(Path skillDir) {
        Path skillMd = skillDir.resolve("SKILL.md");
        if (!Files.exists(skillMd)) return;
        try {
            SkillManifest manifest = skillResolver.parse(skillMd);
            String skillKey = skillDir.getFileName().toString();
            TerraSkill existing = skillMapper.selectByKey(skillKey);
            if (existing == null) {
                TerraSkill skill = new TerraSkill();
                skill.setSkillKey(skillKey);
                skill.setName(manifest.getName());
                skill.setDescription(manifest.getDescription());
                skill.setDirectoryPath("preset/" + skillKey);
                skill.setIsPreset(1);
                skill.setIsEnabled(1);
                skill.setInstalledBy("system");
                skillMapper.insert(skill);
                log.info("Synced preset skill: {}", skillKey);
            }
        } catch (Exception e) {
            log.error("Failed to sync preset skill: {}", skillDir, e);
        }
    }
}
```

- [ ] **步骤 3：实现技能和工具的 Service + Controller**

`TerraSkillServiceImpl` 核心方法：
- `selectList()` — 查询所有技能
- `install(Path zipPath, String operator)` — 解压到 custom/ 目录，解析 SKILL.md，入库
- `uninstall(Long id, String operator)` — 检查 is_preset，删除目录 + 记录
- `toggle(Long id, String operator)` — 检查 is_preset（预置不可停用），切换 is_enabled

`TerraToolServiceImpl` 核心方法：
- `selectList()` — 查询所有工具（合并 code 工具 + config 工具）
- `create(TerraTool, operator)` — 只能创建 source='config' 的工具
- `delete(Long id)` — 只能删除 source='config' 的工具
- `toggle(Long id, operator)` — 切换 is_enabled

Controller 按标准 CRUD 模式编写。

- [ ] **步骤 4：验证编译并 Commit**

```bash
cd server && mvn clean compile -pl zwei-terra-agent -am -DskipTests
git add server/zwei-terra/zwei-terra-agent/
git commit -m "feat(terra): 技能管理 + 工具管理 Service + Controller"
```

---

## 任务 7：实现 ToolManager（工具注册与执行）

**文件：**
- 创建：`server/zwei-terra/zwei-terra-agent/src/main/java/com/zwei/terra/agent/tool/ToolRegistration.java`
- 创建：`server/zwei-terra/zwei-terra-agent/src/main/java/com/zwei/terra/agent/tool/ToolManager.java`

- [ ] **步骤 1：创建 ToolRegistration**

```java
package com.zwei.terra.agent.tool;

import com.zwei.terra.core.tool.ToolDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.reflect.Method;

/**
 * 工具注册信息 — 包含工具定义和执行入口。
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolRegistration {
    private String toolKey;
    private ToolDefinition definition;

    // code 工具的执行入口
    private Object bean;            // Spring Bean 实例
    private Method method;          // @ToolMethod 标注的方法

    // config 工具的执行入口
    private String endpoint;        // HTTP 调用地址

    public boolean isCodeTool() {
        return bean != null;
    }
}
```

- [ ] **步骤 2：实现 ToolManager**

```java
package com.zwei.terra.agent.tool;

import com.zwei.common.exception.ServiceException;
import com.zwei.terra.agent.domain.TerraTool;
import com.zwei.terra.agent.mapper.TerraToolMapper;
import com.zwei.terra.core.tool.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工具管理器 — 负责发现、注册和执行工具。
 * - 代码工具：Spring 容器扫描 @TerraTool Bean
 * - 配置工具：从 terra_tool 表加载
 */
@Component
@Slf4j
public class ToolManager {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TerraToolMapper toolMapper;

    private final Map<String, ToolRegistration> codeTools = new HashMap<>();
    private final Map<String, ToolRegistration> configTools = new HashMap<>();

    @PostConstruct
    public void scanCodeTools() {
        // 扫描所有 @TerraTool 标注的 Bean
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(TerraTool.class);
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object bean = entry.getValue();
            TerraTool annotation = bean.getClass().getAnnotation(TerraTool.class);
            String toolKey = annotation.name();

            // 查找 @ToolMethod 方法
            for (Method method : bean.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(ToolMethod.class)) {
                    ToolMethod tmAnnotation = method.getAnnotation(ToolMethod.class);
                    String methodKey = toolKey + "." + method.getName();

                    ToolDefinition def = ToolDefinition.builder()
                        .name(methodKey)
                        .description(tmAnnotation.description())
                        .execSide("backend")
                        .parametersSchema(buildParameterSchema(method))
                        .build();

                    ToolRegistration reg = ToolRegistration.builder()
                        .toolKey(methodKey)
                        .definition(def)
                        .bean(bean)
                        .method(method)
                        .build();

                    codeTools.put(methodKey, reg);
                    log.info("Registered code tool: {}", methodKey);
                }
            }

            // 确保 DB 中存在对应记录（is_preset=1, source='code'）
            ensureCodeToolInDb(toolKey, annotation);
        }
        log.info("ToolManager scanned {} code tools", codeTools.size());
    }

    /** 加载配置化工具到内存 */
    public void loadConfigTools() {
        List<TerraTool> tools = toolMapper.selectEnabled();
        for (TerraTool tool : tools) {
            if ("config".equals(tool.getSource())) {
                ToolDefinition def = ToolDefinition.builder()
                    .name(tool.getToolKey())
                    .description(tool.getDescription())
                    .execSide(tool.getExecSide())
                    .build();
                ToolRegistration reg = ToolRegistration.builder()
                    .toolKey(tool.getToolKey())
                    .definition(def)
                    .endpoint(tool.getEndpoint())
                    .build();
                configTools.put(tool.getToolKey(), reg);
            }
        }
    }

    /** 获取所有启用工具的定义（供 LLM 使用） */
    public List<ToolDefinition> getEnabledToolDefinitions() {
        List<ToolDefinition> result = new ArrayList<>();
        // code tools
        for (ToolRegistration reg : codeTools.values()) {
            result.add(reg.getDefinition());
        }
        // config tools（需重新加载以获取最新状态）
        loadConfigTools();
        for (ToolRegistration reg : configTools.values()) {
            result.add(reg.getDefinition());
        }
        return result;
    }

    /** 执行后端工具 */
    public ToolResult execute(String toolKey, Map<String, Object> params) {
        ToolRegistration reg = codeTools.get(toolKey);
        if (reg != null && reg.isCodeTool()) {
            try {
                Object result = reg.getMethod().invoke(reg.getBean(), mapParameters(reg.getMethod(), params));
                return ToolResult.success(result);
            } catch (Exception e) {
                log.error("Tool execution failed: {}", toolKey, e);
                return ToolResult.failure(e.getMessage());
            }
        }
        reg = configTools.get(toolKey);
        if (reg != null && reg.getEndpoint() != null) {
            // 配置化工具通过 HTTP 调用
            return executeHttpTool(reg, params);
        }
        return ToolResult.failure("Tool not found: " + toolKey);
    }

    /** 判断是否为前端工具 */
    public boolean isFrontendTool(String toolKey) {
        ToolRegistration reg = configTools.get(toolKey);
        return reg != null && "frontend".equals(reg.getDefinition().getExecSide());
    }

    private ToolResult executeHttpTool(ToolRegistration reg, Map<String, Object> params) {
        // 使用 RestTemplate/WebClient 调用 reg.getEndpoint()
        // 返回 ToolResult
        // 完整实现在此任务中完成
        // ...
        return ToolResult.failure("HTTP tool execution not yet implemented");
    }

    private void ensureCodeToolInDb(String toolKey, TerraTool annotation) {
        TerraTool existing = toolMapper.selectByKey(toolKey);
        if (existing == null) {
            TerraTool record = new TerraTool();
            record.setToolKey(toolKey);
            record.setName(annotation.name());
            record.setDescription(annotation.description());
            record.setSource("code");
            record.setExecSide("backend");
            record.setIsPreset(1);
            record.setIsEnabled(1);
            record.setCreateBy("system");
            toolMapper.insert(record);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildParameterSchema(Method method) {
        // 简化：将方法参数名和类型转为 JSON Schema
        // 完整实现需要 ParameterNameResolver
        return Map.of("type", "object", "properties", Map.of());
    }

    private Object[] mapParameters(Method method, Map<String, Object> params) {
        // 将 Map 参数映射到方法形参
        // 简化实现
        return new Object[0];
    }
}
```

> 注意：`buildParameterSchema` 和 `mapParameters` 需要完整实现。可使用 Spring 的 `ParameterNameDiscoverer` 获取参数名，根据 Java 类型映射到 JSON Schema 类型。

- [ ] **步骤 3：验证编译并 Commit**

```bash
cd server && mvn clean compile -pl zwei-terra-agent -am -DskipTests
git add server/zwei-terra/zwei-terra-agent/
git commit -m "feat(terra): ToolManager — 代码扫描 + 配置加载 + 工具执行"
```

---

## 任务 8：实现 AnthropicChatModel（Anthropic HTTP 客户端）

**文件：**
- 创建：`server/zwei-terra/zwei-terra-agent/src/main/java/com/zwei/terra/agent/chat/AnthropicChatModel.java`
- 创建：`server/zwei-terra/zwei-terra-agent/src/main/java/com/zwei/terra/agent/chat/TerraSseEmitter.java`

- [ ] **步骤 1：实现 AnthropicChatModel**

这是核心组件 — 直接 HTTP 调用 Anthropic Messages API，支持流式。

```java
package com.zwei.terra.agent.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zwei.common.exception.ServiceException;
import com.zwei.terra.agent.domain.TerraModelConfig;
import com.zwei.terra.core.tool.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Anthropic Messages API HTTP 客户端。
 * 直接调用 POST {base-url}/v1/messages，支持 SSE 流式响应。
 */
@Component
@Slf4j
public class AnthropicChatModel {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 流式调用 Anthropic Messages API。
     *
     * @param config      激活的模型配置
     * @param systemPrompt 系统提示词
     * @param messages    对话消息列表
     * @param tools       可用工具定义列表
     * @param tokenConsumer  文本片段回调
     * @param toolCallConsumer 工具调用回调
     * @return 完整响应（stop_reason 等）
     */
    public AnthropicResponse streamChat(
            TerraModelConfig config,
            String systemPrompt,
            List<Map<String, Object>> messages,
            List<ToolDefinition> tools,
            Consumer<String> tokenConsumer,
            Consumer<ToolCallInfo> toolCallConsumer) {

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", config.getModelName());
            requestBody.put("max_tokens", config.getMaxTokens());
            requestBody.put("temperature", config.getTemperature().doubleValue());
            requestBody.put("stream", true);
            requestBody.put("system", systemPrompt);

            // messages
            ArrayNode messagesNode = requestBody.putArray("messages");
            for (Map<String, Object> msg : messages) {
                messagesNode.add(objectMapper.valueToTree(msg));
            }

            // tools
            if (tools != null && !tools.isEmpty()) {
                ArrayNode toolsNode = requestBody.putArray("tools");
                for (ToolDefinition tool : tools) {
                    ObjectNode toolNode = toolsNode.addObject();
                    toolNode.put("name", tool.getName());
                    toolNode.put("description", tool.getDescription());
                    toolNode.put("input_schema", tool.getParametersSchema() != null
                        ? objectMapper.valueToTree(tool.getParametersSchema())
                        : Map.of("type", "object", "properties", Map.of()));
                }
            }

            WebClient client = WebClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader("x-api-key", config.getApiKey())
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

            StringBuilder fullContent = new StringBuilder();
            StringBuilder stopReason = new StringBuilder();
            List<ToolCallInfo> toolCalls = new java.util.ArrayList<>();

            // SSE 流式解析
            String response = client.post()
                .uri("/v1/messages")
                .bodyValue(requestBody.toString())
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(event -> {
                    // 解析 SSE event
                    parseStreamEvent(event, tokenConsumer, toolCallConsumer,
                        fullContent, stopReason, toolCalls);
                })
                .blockLast();  // 阻塞直到流完成

            AnthropicResponse result = new AnthropicResponse();
            result.setContent(fullContent.toString());
            result.setStopReason(stopReason.toString());
            result.setToolCalls(toolCalls);
            return result;

        } catch (Exception e) {
            log.error("Anthropic API call failed", e);
            throw new ServiceException("模型调用失败: " + e.getMessage());
        }
    }

    private void parseStreamEvent(
            String event,
            Consumer<String> tokenConsumer,
            Consumer<ToolCallInfo> toolCallConsumer,
            StringBuilder fullContent,
            StringBuilder stopReason,
            List<ToolCallInfo> toolCalls) {
        try {
            JsonNode node = objectMapper.readTree(event);
            String type = node.path("type").asText();
            switch (type) {
                case "content_block_delta" -> {
                    JsonNode delta = node.path("delta");
                    if ("text_delta".equals(delta.path("type").asText())) {
                        String text = delta.path("text").asText();
                        fullContent.append(text);
                        tokenConsumer.accept(text);
                    }
                }
                case "message_delta" -> {
                    JsonNode delta = node.path("delta");
                    if (delta.has("stop_reason")) {
                        stopReason.setLength(0);
                        stopReason.append(delta.path("stop_reason").asText());
                    }
                }
                case "content_block_start" -> {
                    JsonNode contentBlock = node.path("content_block");
                    if ("tool_use".equals(contentBlock.path("type").asText())) {
                        // 工具调用开始 — 累积后续 input_json_delta
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse stream event: {}", event, e);
        }
    }

    /** Anthropic 响应封装 */
    @lombok.Data
    public static class AnthropicResponse {
        private String content;
        private String stopReason;
        private List<ToolCallInfo> toolCalls;
    }

    /** 工具调用信息 */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ToolCallInfo {
        private String id;
        private String name;
        private Map<String, Object> input;
    }
}
```

> 注意：Anthropic SSE 流式响应的工具调用解析比文本更复杂。`content_block_start` 标记工具调用开始，后续的 `input_json_delta` 需要拼接。完整实现需要在 `parseStreamEvent` 中累积 `input_json` 完整 JSON 后再回调 `toolCallConsumer`。

- [ ] **步骤 2：实现 TerraSseEmitter（SSE 封装）**

```java
package com.zwei.terra.agent.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * Terra SSE Emitter — 封装 SSE 事件推送。
 */
@Slf4j
public class TerraSseEmitter extends SseEmitter {

    public TerraSseEmitter(long timeout) {
        super(timeout);
    }

    public void sendToken(String content) {
        try {
            send(SseEmitter.event().name("token").data("{\"content\":\"" + escapeJson(content) + "\"}"));
        } catch (IOException e) {
            log.error("Failed to send token SSE", e);
        }
    }

    public void sendToolCall(String callId, String tool, String execSide, Object params) {
        try {
            String data = String.format("{\"callId\":\"%s\",\"tool\":\"%s\",\"execSide\":\"%s\"}",
                callId, tool, execSide);
            send(SseEmitter.event().name("tool_call").data(data));
        } catch (IOException e) {
            log.error("Failed to send tool_call SSE", e);
        }
    }

    public void sendToolResult(String callId, boolean success, Object result) {
        try {
            send(SseEmitter.event().name("tool_result").data(
                String.format("{\"callId\":\"%s\",\"success\":%s}", callId, success)));
        } catch (IOException e) {
            log.error("Failed to send tool_result SSE", e);
        }
    }

    public void sendDone(Long messageId, int tokensUsed) {
        try {
            send(SseEmitter.event().name("done").data(
                String.format("{\"messageId\":%s,\"tokensUsed\":%d}", messageId, tokensUsed)));
        } catch (IOException e) {
            log.error("Failed to send done SSE", e);
        }
    }

    public void sendError(String message) {
        try {
            send(SseEmitter.event().name("error").data(
                String.format("{\"message\":\"%s\"}", escapeJson(message))));
        } catch (IOException e) {
            log.error("Failed to send error SSE", e);
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
```

- [ ] **步骤 3：验证编译并 Commit**

```bash
cd server && mvn clean compile -pl zwei-terra-agent -am -DskipTests
git add server/zwei-terra/zwei-terra-agent/
git commit -m "feat(terra): AnthropicChatModel HTTP 客户端 + SSE 封装"
```

---

## 任务 9：实现 ChatService + ReactLoop + 对话 Controller

**文件：**
- 创建：`server/zwei-terra/zwei-terra-agent/src/main/java/com/zwei/terra/agent/chat/ChatService.java`
- 创建：`server/zwei-terra/zwei-terra-agent/src/main/java/com/zwei/terra/agent/controller/TerraChatController.java`

- [ ] **步骤 1：实现 ChatService（对话编排 + React Loop）**

```java
package com.zwei.terra.agent.chat;

import com.zwei.common.exception.ServiceException;
import com.zwei.terra.agent.config.TerraProperties;
import com.zwei.terra.agent.domain.*;
import com.zwei.terra.agent.mapper.*;
import com.zwei.terra.agent.service.ITerraPersonalityService;
import com.zwei.terra.agent.tool.ToolManager;
import com.zwei.terra.core.tool.ToolDefinition;
import com.zwei.terra.core.tool.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * 对话编排服务 — 管理 React Loop、工具调用、SSE 推流。
 */
@Service
@Slf4j
public class ChatService {

    @Autowired private ITerraPersonalityService personalityService;
    @Autowired private TerraModelConfigMapper modelConfigMapper;
    @Autowired private ToolManager toolManager;
    @Autowired private AnthropicChatModel chatModel;
    @Autowired private TerraProperties properties;
    @Autowired private TerraConversationMapper conversationMapper;
    @Autowired private TerraMessageMapper messageMapper;

    /** 前端工具回调等待中 */
    private final Map<String, CompletableFuture<ToolResult>> pendingFrontendTools = new ConcurrentHashMap<>();

    /**
     * 发送消息并返回 SSE 流。
     */
    public SseEmitter chat(Long conversationId, String userMessage, Long userId) {
        TerraSseEmitter emitter = new TerraSseEmitter(300_000L); // 5 分钟超时

        // 异步执行 React Loop
        CompletableFuture.runAsync(() -> {
            try {
                executeReactLoop(emitter, conversationId, userMessage, userId);
            } catch (Exception e) {
                log.error("React loop failed", e);
                emitter.sendError(e.getMessage());
                emitter.complete();
            }
        });

        return emitter;
    }

    private void executeReactLoop(TerraSseEmitter emitter, Long conversationId,
                                   String userMessage, Long userId) {
        // 1. 获取/创建会话
        TerraConversation conv = getOrCreateConversation(conversationId, userId);

        // 2. 保存用户消息
        saveMessage(conv.getId(), "user", userMessage, null, null);

        // 3. 检查模型配置
        TerraModelConfig modelConfig = modelConfigMapper.selectActive();
        if (modelConfig == null) {
            emitter.sendError("未激活模型配置，请在设置中配置并激活模型");
            emitter.complete();
            return;
        }

        // 4. 构建 system prompt
        String systemPrompt = personalityService.buildSystemPrompt();

        // 5. 加载历史消息
        List<TerraMessage> history = messageMapper.selectByConversationId(conv.getId(),
            properties.getChat().getMaxHistoryMessages());

        // 6. 获取可用工具
        List<ToolDefinition> tools = toolManager.getEnabledToolDefinitions();

        // 7. 构建 messages 列表
        List<Map<String, Object>> messages = buildMessages(history, userMessage);

        // 8. React Loop
        int maxRounds = properties.getChat().getMaxReactRounds();
        for (int round = 0; round < maxRounds; round++) {
            // 调用 LLM
            AnthropicChatModel.AnthropicResponse response = chatModel.streamChat(
                modelConfig, systemPrompt, messages, tools,
                emitter::sendToken,  // 文本片段 → SSE
                null  // tool call 在下面处理
            );

            if ("end_turn".equals(response.getStopReason())) {
                // 对话结束，保存 assistant 消息
                saveMessage(conv.getId(), "assistant", response.getContent(), null, null);
                emitter.sendDone(conv.getId(), 0);
                break;
            }

            if ("tool_use".equals(response.getStopReason()) && response.getToolCalls() != null) {
                // 处理工具调用
                for (AnthropicChatModel.ToolCallInfo toolCall : response.getToolCalls()) {
                    String callId = toolCall.getId();
                    String toolKey = toolCall.getName();
                    Map<String, Object> params = toolCall.getInput();

                    if (toolManager.isFrontendTool(toolKey)) {
                        // 前端工具：SSE 下发，等待回调
                        emitter.sendToolCall(callId, toolKey, "frontend", params);
                        ToolResult result = waitForFrontendTool(callId,
                            toolManager.getTimeoutSeconds(toolKey));
                        emitter.sendToolResult(callId, result.isSuccess(), result.getResult());
                        messages.add(buildToolResultMessage(callId, result));
                    } else {
                        // 后端工具：同步执行
                        emitter.sendToolCall(callId, toolKey, "backend", params);
                        ToolResult result = toolManager.execute(toolKey, params);
                        emitter.sendToolResult(callId, result.isSuccess(), result.getResult());
                        messages.add(buildToolResultMessage(callId, result));
                    }
                }
                // 继续 React Loop
                continue;
            }

            // 未知 stop_reason，退出
            break;
        }

        emitter.complete();
    }

    /** 前端工具回调入口 */
    public void resolveFrontendTool(String callId, boolean success, Object result) {
        CompletableFuture<ToolResult> future = pendingFrontendTools.remove(callId);
        if (future != null) {
            future.complete(success
                ? ToolResult.success(result)
                : ToolResult.failure(String.valueOf(result)));
        }
    }

    private ToolResult waitForFrontendTool(String callId, int timeoutSeconds) {
        CompletableFuture<ToolResult> future = new CompletableFuture<>();
        pendingFrontendTools.put(callId, future);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            pendingFrontendTools.remove(callId);
            return ToolResult.failure("frontend tool timeout");
        } catch (Exception e) {
            return ToolResult.failure(e.getMessage());
        }
    }

    // ... 辅助方法: getOrCreateConversation, saveMessage, buildMessages,
    //     buildToolResultMessage, getUsername 等
}
```

- [ ] **步骤 2：实现 TerraChatController + TerraConversationController**

```java
package com.zwei.terra.agent.controller;

import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.terra.agent.chat.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/terra")
public class TerraChatController extends BaseController {

    @Autowired
    private ChatService chatService;

    /**
     * 发送消息（SSE 流式响应）
     */
    @PostMapping("/chat")
    @PreAuthorize("@ss.hasPermi('terra:chat')")
    public SseEmitter chat(@RequestBody Map<String, Object> body) {
        Long conversationId = body.get("conversationId") != null
            ? Long.valueOf(body.get("conversationId").toString()) : null;
        String message = body.get("message").toString();
        return chatService.chat(conversationId, message, getUserId());
    }

    /**
     * 前端工具执行结果回调
     */
    @PostMapping("/chat/{conversationId}/tool-result")
    @PreAuthorize("@ss.hasPermi('terra:chat')")
    public AjaxResult toolResult(@PathVariable Long conversationId, @RequestBody Map<String, Object> body) {
        String callId = body.get("callId").toString();
        boolean success = Boolean.TRUE.equals(body.get("success"));
        Object result = body.get("result");
        chatService.resolveFrontendTool(callId, success, result);
        return success();
    }
}
```

`TerraConversationController` 标准实现：
- `GET /api/v1/terra/conversations` — 当前用户的会话列表
- `GET /api/v1/terra/conversations/{id}/messages` — 会话消息历史
- `POST /api/v1/terra/conversations` — 新建会话
- `DELETE /api/v1/terra/conversations/{id}` — 删除会话

- [ ] **步骤 3：添加 terra 配置到 application.yml**

在 `server/zwei-admin/src/main/resources/application.yml` 中添加:

```yaml
terra:
  skills:
    base-path: ${user.home}/terra/skills
  chat:
    max-react-rounds: 10
    default-timeout-seconds: 30
    max-history-messages: 20
```

- [ ] **步骤 4：创建预置技能目录和示例 SKILL.md**

创建 `${user.home}/terra/skills/preset/` 目录。
创建一个示例预置技能 `welcome/SKILL.md`:

```yaml
---
name: welcome
description: Terra 基础欢迎和引导技能
---

# 行为指令

当用户第一次打开对话时，热情地介绍自己的能力。
引导用户了解可以通过 terra 查询设备、查看告警、操作页面等功能。
```

- [ ] **步骤 5：验证编译并 Commit**

```bash
cd server && mvn clean compile -pl zwei-terra-agent -am -DskipTests
git add server/zwei-terra/ server/zwei-admin/src/main/resources/application.yml
git commit -m "feat(terra): ChatService React Loop + 对话 Controller + 配置"
```

---

## 任务 10：端到端编译验证 + 权限注册

**文件：**
- 修改：数据库 sys_menu 表（新增 terra 权限菜单）

- [ ] **步骤 1：全量编译验证**

```bash
cd server && mvn clean compile -DskipTests
```
预期：BUILD SUCCESS。如有编译错误，逐一修复。

- [ ] **步骤 2：注册权限菜单**

执行 SQL 注册 terra 权限:

```sql
-- terra 设置菜单（父级：系统设置）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, perms, icon, create_by, create_time)
VALUES ('Terra 设置', 2, 10, 'terra', NULL, 'M', '0', 'terra:settings', 'tool', 'admin', NOW());

-- terra 对话权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, perms, icon, create_by, create_time)
VALUES ('Terra 对话', 0, 0, '', NULL, 'F', '1', 'terra:chat', '#', 'admin', NOW());
```

- [ ] **步骤 3：打包验证**

```bash
cd server && mvn clean package -DskipTests -pl zwei-admin -am
```
预期：BUILD SUCCESS，生成 `zwei-admin/target/zwei-admin.jar`。

- [ ] **步骤 4：Commit**

```bash
git add db/upgrade/terra_v1.0.sql
git commit -m "feat(terra): 权限注册 + 全量编译验证通过"
```

---

## 自检

### 规格覆盖度

| 规格章节 | 对应任务 | 状态 |
|---------|---------|------|
| 2. 模块架构 | 任务 1 | 覆盖 |
| 3. 数据模型 (7 张表) | 任务 2 | 覆盖 |
| 2. terra-core 接口 | 任务 3 | 覆盖 |
| 3. Domain + Mapper | 任务 4 | 覆盖 |
| 5. 后端架构 — System Prompt 拼装 | 任务 5 | 覆盖 |
| 5. 后端架构 — React Loop | 任务 9 | 覆盖 |
| 5. 后端架构 — Tool Registry | 任务 7 | 覆盖 |
| 5. 后端架构 — AnthropicChatModel | 任务 8 | 覆盖 |
| 4. 技能系统 (目录管理) | 任务 6 | 覆盖 |
| 6. API 设计 | 任务 5, 6, 9 | 覆盖 |
| 9. 安全考虑 (API Key 加密) | 任务 5 | 覆盖 (Service 层脱敏) |
| 10. 配置项 | 任务 9 | 覆盖 |

### 遗漏

- API Key AES 加密/解密逻辑需在 TerraModelConfigServiceImpl 中完整实现（参考项目现有加密工具）
- `buildParameterSchema` 和 `mapParameters` 在 ToolManager 中需要完整实现（使用 Spring ParameterNameDiscoverer）
- Anthropic SSE 流式解析中 `input_json_delta` 的累积逻辑需要完善
- MyBatis mapper-locations 需要确认 zwei-admin 的全局配置包含 `classpath*:mapper/**/*.xml`

这些是实现细节，在对应任务的步骤中完成。
