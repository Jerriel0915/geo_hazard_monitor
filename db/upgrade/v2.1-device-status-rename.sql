-- =============================================================================
-- v2.1 — 设备业务状态统一为 1-正常 / 2-维修 / 3-停用
-- =============================================================================
-- 背景: 此前 sys_dict_data 中 device_status 字典的 label 仍为旧文案
--       1-正常 / 2-故障 / 3-离线，与代码实际行为 (1-正常/2-维修/3-停用) 不一致。
--       数值 1/2/3 不变，仅更新字典 label/remark，零数据迁移。
-- 幂等: 重复执行无副作用（label 已是目标值时 UPDATE 0 行）。
-- =============================================================================

UPDATE sys_dict_data
SET dict_label = '维修', remark = '设备维修中'
WHERE dict_type = 'device_status' AND dict_value = '2';

UPDATE sys_dict_data
SET dict_label = '停用', remark = '设备停用'
WHERE dict_type = 'device_status' AND dict_value = '3';
