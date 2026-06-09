-- V3.0: 告警判据重构 — conditions_json + 8 个分级字段 → level_config JSON
-- 每个告警等级独立定义自己的多条件组合，支持函数主语 (AVG/MAX/MIN/SUM)

ALTER TABLE alarm_criteria
    DROP COLUMN conditions_json,
    DROP COLUMN logic_operator,
    DROP COLUMN blue_expression,
    DROP COLUMN blue_description,
    DROP COLUMN yellow_expression,
    DROP COLUMN yellow_description,
    DROP COLUMN orange_expression,
    DROP COLUMN orange_description,
    DROP COLUMN red_expression,
    DROP COLUMN red_description,
    ADD COLUMN level_config JSON NOT NULL COMMENT '四级告警条件配置。格式: {"blue":{"logicOperator":"AND","conditions":[...],"description":"..."},"yellow":{...},"orange":{...},"red":{...}}'
        AFTER monitor_content_code;

-- level_config 中单个 condition 的结构:
-- {"subject":"water_level","subjectType":"CONTENT","operator":"GT","threshold":8.0,"unit":"m"}
-- subjectType=FUNCTION 时: {"subject":"hourly_avg","subjectType":"FUNCTION","function":"AVG","functionParams":{"period":"1h","sourceSubject":"rainfall"},"operator":"GT","threshold":50.0,"unit":"mm"}
