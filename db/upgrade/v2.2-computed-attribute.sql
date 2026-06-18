-- v2.2 计算属性(Computed Attribute)升级脚本
-- 为 monitor_content 增加字段类型(固有/计算)与计算脚本列

ALTER TABLE monitor_content
    ADD COLUMN field_type VARCHAR(16) NOT NULL DEFAULT 'inherent'
        COMMENT '字段类型: inherent-固有属性, computed-计算属性'
        AFTER indicator_type,
    ADD COLUMN calc_script MEDIUMTEXT NULL
        COMMENT '计算属性脚本(Groovy 代码块, 仅 field_type=computed 时必填)'
        AFTER field_type;

ALTER TABLE monitor_content
    ADD INDEX idx_monitor_content_field_type (monitor_type_id, field_type);

-- 新增权限: 计算脚本在线测试
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, perms,
                     menu_type, visible, status, create_by, create_time)
SELECT '监测内容脚本测试', menu_id, 5, '', '', 'basic:monitorContent:test',
       'F', '0', '0', 'admin', NOW()
FROM sys_menu
WHERE perms = 'basic:monitorContent:list'
LIMIT 1;
