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

-- ============================================================
-- 权限注册 — sys_menu
-- ============================================================

-- Terra 设置菜单（系统设置目录下，parent_id=2 是"系统设置"目录菜单）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, perms, icon, create_by, create_time)
VALUES ('Terra 设置', 2, 10, 'terra', NULL, 'M', '0', 'terra:settings', 'tool', 'admin', NOW());

-- Terra 对话权限（功能按钮，不显示在菜单）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, perms, icon, create_by, create_time)
VALUES ('Terra 对话', 0, 0, '', NULL, 'F', '1', 'terra:chat', '#', 'admin', NOW());
