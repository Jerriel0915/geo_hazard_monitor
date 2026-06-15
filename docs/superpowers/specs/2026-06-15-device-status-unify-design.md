# 设备状态统一为「正常 / 维修 / 停用」设计文档

> 将系统中设备业务状态相关的 **8 个文件 / 14 处文本**（含数据字典、SQL 注释、Java 注释、错误信息、CLAUDE.md、deviceIcon JSDoc、设备管理文档）统一为 **1-正常 / 2-维修 / 3-停用**。
> **数值层不变**（仍为 1/2/3），仅做显示层与文档层一致化；零数据迁移。

## 一、概述

### 1.1 背景

`device.status` 字段是设备业务状态（人工维护），当前在系统中存在**多份相互冲突的描述**：

| 位置 | 当前描述 | 实际行为 |
|---|---|---|
| `sys_dict_data` (device_status) | 1-正常 / 2-故障 / 3-离线 | 字典文本，**与代码不一致** |
| `db/...sql` device 表 COMMENT | 1-正常 / 2-故障 / 3-离线 | 元数据注释，**与代码不一致** |
| `Device.java` 字段 Javadoc | 1-正常 / 2-故障 / 3-停用 | 注释，**与 CASE 标签不一致** |
| `DeviceServiceImpl` case 3 错误信息 | "仅正常或故障状态的设备可以停用" | 实际行为是把 2 当作"维修" |
| `DeviceMapper.xml` SQL CASE | 1-正常 / 2-维修 / 3-停用 | **运行时实际显示**（API 返回给前端的 `statusName`） |
| `useDeviceCrud.ts` getStatusLabel | 1-正常 / 2-维修 / 3-停用 | 详情对话框显示 |
| `Device.vue` 搜索下拉 | label="故障" value=2 / label="维修" value=3 | **Bug**: 标签与值错位 |
| `deviceIcon.ts` JSDoc | 1-正常 / 2-故障 / 3-停用 | 注释，**与实现不一致** |

**实际生效的代码逻辑**（已统一为 1-正常/2-维修/3-停用）：
- `DeviceServiceImpl.maintenanceDevice` 状态机：1→2（报修→维修）、2→1（修复）、1或2→3（停用）、3→1（恢复）
- `DeviceMapper.xml` SQL CASE 返回的 `statusName`
- `useDeviceCrud.ts` 详情页 status 标签

只有**显示/文档/字典**三处仍保留旧的 1-正常/2-故障/3-离线 描述。

### 1.2 目标

- 修复 `Device.vue` 搜索下拉的 **label/value 错位 Bug**（用户搜「故障」实际查到的 status=2 设备是维修状态）
- 把数据字典、SQL 注释、Java 注释、错误信息、CLAUDE.md、deviceIcon JSDoc、设备管理文档共 **10 个文件 / 16 处文本** 统一为 **1-正常 / 2-维修 / 3-停用**
- 提供 `db/upgrade/v2.1-device-status-rename.sql` 升级脚本（已有部署可重放）
- **不动数值**（1/2/3）→ 零数据迁移，不影响任何 API 行为

### 1.3 范围外

- `device_online_status` 表的 0/1（在线/离线）— 这是独立的实时状态机，**不在本任务范围**
- `video_device.status` 的 0/1/2 — 视频设备独立状态机，**不在本任务范围**
- `device_sensor.status` 的 0/1（禁用/启用）— 传感器独立状态，**不在本任务范围**
- 引入 `DeviceStatusEnum` 重构 — 是独立演进任务，本任务**不打开**此重构口子

## 二、详细改动清单（8 个文件 / 14 处文本）

### 2.1 数据库

| # | 文件 | 行号 | 改动 |
|---|---|---|---|
| 1.1 | `db/geo_hazard_monitor_v2.0.sql` | 1773, 1775 | `sys_dict_data` 设备状态字典：35 行 `dict_label` `故障`→`维修`、`remark` `设备故障`→`设备维修中`；36 行 `离线`→`停用`、`remark` `设备离线`→`设备停用` |
| 1.2 | `db/geo_hazard_monitor_v2.0.sql` | 585 | `device` 表 `status` 列 COMMENT：`状态: 1-正常, 2-故障, 3-离线` → `状态: 1-正常, 2-维修, 3-停用` |
| 1.3 | `db/upgrade/v2.1-device-status-rename.sql` | **新建** | 升级脚本：`UPDATE sys_dict_data SET dict_label/remark WHERE dict_type='device_status' AND dict_value IN ('2','3')`（幂等） |

### 2.2 后端（Java）

| # | 文件 | 行号 | 改动 |
|---|---|---|---|
| 2.1 | `server/zwei-iot-device/.../domain/Device.java` | 97-99 | 字段 Javadoc：`1-正常, 2-故障, 3-停用` → `1-正常, 2-维修, 3-停用` |
| 2.2 | `server/zwei-iot-device/.../service/impl/DeviceServiceImpl.java` | 517 | case 3 注释 `故障(2)` → `维修(2)` |
| 2.3 | `server/zwei-iot-device/.../service/impl/DeviceServiceImpl.java` | 518 | case 3 错误信息 `仅正常或故障状态的设备可以停用` → `仅正常或维修状态的设备可以停用` |

### 2.3 前端（Vue）

| # | 文件 | 行号 | 改动 |
|---|---|---|---|
| 3.1 | `web/src/views/basic/Device.vue` | 25 | 搜索下拉错位修复：`<el-option label="故障" :value="2" />` → `<el-option label="维修" :value="2" />` |
| 3.2 | `web/src/views/basic/Device.vue` | 26 | `<el-option label="维修" :value="3" />` → `<el-option label="停用" :value="3" />` |
| 3.3 | `web/src/utils/deviceIcon.ts` | 8 | JSDoc `<b>red</b> — 故障（status=2）` → `<b>red</b> — 维修（status=2）` |
| 3.4 | `web/src/utils/deviceIcon.ts` | 9 | JSDoc `<b>repair</b> — 停用/维修中（status=3）` → `<b>repair</b> — 停用（status=3）` |
| 3.5 | `web/src/utils/deviceIcon.ts` | 22 | `@param status 业务状态：1=正常, 2=故障, 3=停用` → `1=正常, 2=维修, 3=停用` |
| 3.6 | `web/src/utils/deviceIcon.ts` | 72 | `<p>默认颜色档位为 green（正常状态），仅当设备明确为故障/停用/离线时才切换其他颜色。</p>` → `仅当设备明确为维修/停用时才切换其他颜色。`（在线状态已在 onlineStatus 中处理，移除此处「离线」表述避免混淆） |
| 3.7 | `web/src/views/basic/components/DeviceDetail.vue` | 212 | `getStatusLabel` map 错位修复：`{1:'正常', 2:'故障', 3:'维修'}` → `{1:'正常', 2:'维修', 3:'停用'}`（line 98 实际调用，维修日志显示） |
| 3.8 | `web/src/views/basic/components/HazardPointDetail.vue` | 65-70 | 字符串 enum `'NORMAL'/'FAULT'` 与 Integer 比较永远 false 的 bug 修复 → 改为 `=== 1 / === 2` 数字比较（标签与颜色映射同步改为「正常/维修/停用」） |

### 2.4 文档

| # | 文件 | 行号 | 改动 |
|---|---|---|---|
| 4.1 | `server/zwei-iot-device/CLAUDE.md` | 设备状态机表（line 122-129） | 1 (正常) → 2 (故障) → 1 (正常) → 改 2 (维修) |
| 4.2 | `server/zwei-iot-device/CLAUDE.md` | 数据模型 device 段（line 254） | `status: 1-正常 2-故障 3-离线` → `status: 1-正常 2-维修 3-停用` |
| 4.3 | `docs/设备管理.md` | 5 | `设备状态（正常/故障/维修）` → `设备状态（正常/维修/停用）` |

## 三、关键技术决策

### 3.1 字典定位方式：dict_type + dict_value

**决策**：升级脚本用 `WHERE dict_type='device_status' AND dict_value='2'/'3'` 定位行，**不依赖 dict_code 或 id**。

**理由**：
- `sys_dict_data` 表无 UNIQUE 约束在 `(dict_type, dict_value)` 上，但生产中此组合唯一（参照 db/CLAUDE.md 字典设计原则）
- 避免 id 漂移（35/36 是开发库当前 id，部署后可能不同）
- 对全量脚本和升级脚本使用相同条件，便于审计

### 3.2 数值层不动

**决策**：保持 `device.status` 字段为 `tinyint`，值仍为 1/2/3。

**理由**：
- 现有 1 条 `device` 记录 (status=1) 和 2 条 `device_status_log` 记录 (1→2 报修, 2→1 修复) **与新 label 完全兼容**——无需迁移
- API 行为不变，前端 status 过滤器值不变
- 减少误操作风险（无需考虑数值重映射）

### 3.3 升级脚本确定性

**决策**：`db/upgrade/v2.1-device-status-rename.sql` 用 `UPDATE ... WHERE` 条件定位，可重复执行（重复执行无副作用，label 已是目标值）。

**理由**：已有部署可重放；幂等。

## 四、不需要修改（已正确）

| 位置 | 当前内容 | 原因 |
|---|---|---|
| `DeviceMapper.xml` SQL CASE | 1-正常/2-维修/3-停用 | 运行时实际生效的 label，**保持** |
| `useDeviceCrud.ts` getStatusLabel | 1-正常/2-维修/3-停用 | 详情对话框显示，**保持** |
| `Device.vue` 维修弹窗 (line 800-806) | 按 status=1/2/3 控制可选操作 | 状态机逻辑，**保持** |
| `DeviceServiceImpl.maintenanceDevice` 状态机实现 | 1→2→1, 3 stop | 实际行为，**保持** |
| `deviceIcon.ts` 实现（`if (status === 2) return 'red'`） | red=2, repair=3 | 颜色档位，**保持** |
| `device_status_log` 测试数据 (1→2 报修) | 旧值=1, 新值=2 | 状态机的 1→2 = 正常→维修，**保持** |
| `uniapp/src/utils/device.ts` mock | '在线'/'离线'/'维修' | 字符串型 mock，**与业务状态字段无关** |

## 五、风险评估

| 风险 | 等级 | 缓解 |
|---|---|---|
| 字典升级误改其他字典类型 | 低 | WHERE 条件已限定 `dict_type='device_status'`，只动 2 行 |
| 搜索下拉修改后行为相反 | 低 | 同步修改 label 和 value（值不变，label 跟着实际值走） |
| 文档遗漏造成下次又写错 | 中 | 14 处文件清单已列出，逐项 diff review |
| 已部署环境遗漏升级脚本 | 低 | 升级脚本与全量脚本**同时**提交，db/CLAUDE.md 维护变更日志 |
| 第三方系统依赖 status=2 旧 label | 低 | 本系统为内部系统，无已知外部系统依赖设备状态 label |

## 六、验证计划

1. **SQL 验证**：
   ```sql
   SELECT dict_label, dict_value, remark FROM sys_dict_data
   WHERE dict_type='device_status' ORDER BY dict_value;
   -- 期望：1=正常, 2=维修, 3=停用
   ```

2. **后端验证**：
   - 启动后访问 `/api/v1/devices/page` 设备列表，断言 `statusName` 字段为「正常/维修/停用」
   - `DeviceServiceImpl` 单元测试 `maintenanceDevice` 通过（含 case 3 错误信息「仅正常或维修状态」断言）

3. **前端验证**：
   - 设备列表页搜索下拉：「维修」选项 value=2、「停用」选项 value=3
   - 设备详情页「设备状态」列显示「正常/维修/停用」（与数据库一致）
   - 设备维修弹窗：status=1 显示「报修/停用」按钮；status=2 显示「修复/停用」；status=3 显示「启用」

4. **数据库升级验证**（针对已有部署）：
   - 执行 `db/upgrade/v2.1-device-status-rename.sql` 后查询字典确认
   - 重复执行无副作用（幂等）

## 七、文件清单汇总（10 个文件 / 16 处文本）

| 类别 | 计数 | 文件 |
|---|---|---|
| SQL 全量 | 2 处 | `db/geo_hazard_monitor_v2.0.sql` |
| SQL 升级 | 1 个文件 | `db/upgrade/v2.1-device-status-rename.sql`（新建） |
| 后端 Java | 2 个文件 / 3 处 | `Device.java` / `DeviceServiceImpl.java` |
| 前端 | 4 个文件 / 8 处 | `Device.vue` / `deviceIcon.ts` / `DeviceDetail.vue` / `HazardPointDetail.vue` |
| 文档 | 2 个文件 / 3 处 | `zwei-iot-device/CLAUDE.md` / `docs/设备管理.md` |

> 已确认 `CLAUDE.md`（根）、`db/CLAUDE.md` 中不包含设备业务状态描述，无需修改。
> `DeviceDetail.vue:212` 与 `HazardPointDetail.vue:65-70` 是任务 6 代码审查时发现的范围外同类 Bug，已合并修复并入本计划。

## 八、变更记录

| 时间 | 变更 | 作者 |
|---|---|---|
| 2026-06-15 | 首次创建 — 设备业务状态统一为 1-正常/2-维修/3-停用 | brainstorming flow |
