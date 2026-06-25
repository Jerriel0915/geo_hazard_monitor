-- ============================================================
-- 修正 monitor_type 种子数据的 icon 指向
-- 日期: 2026-06-25
-- 说明:
--   全量脚本 geo_hazard_monitor_v2.0.sql 中的 monitor_type.icon 值与
--   监测类型名称语义不符 (如 "雨量监测" 指向 wj/弯矩图标),
--   导致设备详情-传感器列表图标显示异常。
--   本脚本将种子数据 JCLX001-008 的 icon 修正为与名称匹配的 jc-icon 文件。
--   通过 UI 新建的监测类型不受影响 (图标选择器来源正确)。
--   注意: 仅修正 code 匹配的记录, 不影响其他行。
-- ============================================================

UPDATE `monitor_type` SET `icon` = '/jc-icon/green/jy_green.png'  WHERE `code` = 'JCLX001' AND `del_flag` = 0; -- 雨量监测   wj→jy
UPDATE `monitor_type` SET `icon` = '/jc-icon/green/bsw_green.png' WHERE `code` = 'JCLX002' AND `del_flag` = 0; -- 位移监测   jsd→bsw
UPDATE `monitor_type` SET `icon` = '/jc-icon/green/wd_green.png'  WHERE `code` = 'JCLX003' AND `del_flag` = 0; -- 温湿度监测 ky→wd
UPDATE `monitor_type` SET `icon` = '/jc-icon/green/dw_green.png'  WHERE `code` = 'JCLX004' AND `del_flag` = 0; -- 水位监测   sg→dw
UPDATE `monitor_type` SET `icon` = '/jc-icon/green/lf_green.png'  WHERE `code` = 'JCLX005' AND `del_flag` = 0; -- 裂缝监测   jsd→lf
UPDATE `monitor_type` SET `icon` = '/jc-icon/green/qj_green.png'  WHERE `code` = 'JCLX006' AND `del_flag` = 0; -- 倾斜监测   nw→qj
UPDATE `monitor_type` SET `icon` = '/jc-icon/green/wd_green.png'  WHERE `code` = 'JCLX007' AND `del_flag` = 0; -- 地温监测   gnss→wd
UPDATE `monitor_type` SET `icon` = '/jc-icon/green/th_green.png'  WHERE `code` = 'JCLX008' AND `del_flag` = 0; -- 含水率监测 lf→th
