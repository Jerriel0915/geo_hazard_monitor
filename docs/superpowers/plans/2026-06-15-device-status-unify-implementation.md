# 设备状态统一为「正常 / 维修 / 停用」实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 将系统中设备业务状态相关的 8 个文件 / 14 处文本统一为 **1-正常 / 2-维修 / 3-停用**，并修复 `Device.vue` 搜索下拉的 label/value 错位 Bug。数值 1/2/3 不变，零数据迁移。

**架构：** 一致性同步。10 个文件 16 处文本改动 + 1 个新增升级脚本。后端 `DeviceServiceImpl` case 3 错误信息变更配套单测；前端 `Device.vue` 下拉修复用人工 UI 验证。

**技术栈：** Java 17, Spring Boot, MyBatis, MySQL 8.0, Vue 3, TypeScript

---

## 文件结构

**修改文件（10 个）：**
- `db/geo_hazard_monitor_v2.0.sql` — 字典 + 设备表注释（2 处）
- `server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/Device.java` — 字段 Javadoc
- `server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/impl/DeviceServiceImpl.java` — case 3 注释 + 错误信息（2 处）
- `server/zwei-iot-device/src/test/java/com/zwei/iot/device/service/impl/DeviceServiceImplTest.java` — 新增单测
- `web/src/views/basic/Device.vue` — 搜索下拉（2 处）
- `web/src/utils/deviceIcon.ts` — JSDoc（4 处）
- `web/src/views/basic/components/DeviceDetail.vue` — `getStatusLabel` map 错位（任务 6 审查发现）
- `web/src/views/basic/components/HazardPointDetail.vue` — 字符串 vs Integer 比较类型 bug（任务 6 审查发现）
- `server/zwei-iot-device/CLAUDE.md` — 状态机表 + 数据模型（2 处）
- `docs/设备管理.md` — 设备状态描述

**新建文件（1 个）：**
- `db/upgrade/v2.1-device-status-rename.sql` — 升级脚本（幂等 UPDATE）

> `DeviceDetail.vue` 与 `HazardPointDetail.vue` 是任务 6 代码审查时发现的范围外同类 Bug，已合并修复并入本计划。

---

## 任务 1：数据库 — 更新 sys_dict_data 字典 + 设备表 COMMENT（全量脚本）

**文件：**
- 修改：`db/geo_hazard_monitor_v2.0.sql:1771-1776`
- 修改：`db/geo_hazard_monitor_v2.0.sql:585`

- [ ] **步骤 1：修改字典 35 行的 dict_label 与 remark（line 1773）**

修改前：
```sql
(35, 2, '故障', '2', 'device_status', '', 'danger', 'N', '0', 'admin', '2026-05-08 22:05:59', '', NULL, '设备故障'),
```

修改后：
```sql
(35, 2, '维修', '2', 'device_status', '', 'danger', 'N', '0', 'admin', '2026-05-08 22:05:59', '', NULL, '设备维修中'),
```

- [ ] **步骤 2：修改字典 36 行的 dict_label 与 remark（line 1775）**

修改前：
```sql
(36, 3, '离线', '3', 'device_status', '', 'warning', 'N', '0', 'admin', '2026-05-08 22:05:59', '', NULL, '设备离线'),
```

修改后：
```sql
(36, 3, '停用', '3', 'device_status', '', 'warning', 'N', '0', 'admin', '2026-05-08 22:05:59', '', NULL, '设备停用'),
```

- [ ] **步骤 3：修改 device 表 status 列 COMMENT（line 585）**

修改前：
```sql
`status`           tinyint               DEFAULT '1' COMMENT '状态: 1-正常, 2-故障, 3-离线',
```

修改后：
```sql
`status`           tinyint               DEFAULT '1' COMMENT '状态: 1-正常, 2-维修, 3-停用',
```

- [ ] **步骤 4：Commit**

```bash
git add db/geo_hazard_monitor_v2.0.sql
git commit -m "fix(db): unify device business status to 1-正常/2-维修/3-停用 (full script)"
```

---

## 任务 2：数据库 — 创建 v2.1 升级脚本

**文件：**
- 创建：`db/upgrade/v2.1-device-status-rename.sql`

- [ ] **步骤 1：创建目录与文件**

```bash
mkdir -p db/upgrade
```

创建 `db/upgrade/v2.1-device-status-rename.sql`：

```sql
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
```

- [ ] **步骤 2：验证脚本可执行（语法 + 幂等）**

```bash
# 在 MySQL 8.0 客户端中执行 (test/dev 环境)
mysql -u root -p geo_hazard_monitor < db/upgrade/v2.1-device-status-rename.sql

# 验证结果
mysql -u root -p geo_hazard_monitor -e \
  "SELECT dict_label, dict_value, remark FROM sys_dict_data WHERE dict_type='device_status' ORDER BY dict_value;"

# 预期输出:
# 1 | 正常 | 设备正常
# 2 | 维修 | 设备维修中
# 3 | 停用 | 设备停用

# 再次执行验证幂等
mysql -u root -p geo_hazard_monitor < db/upgrade/v2.1-device-status-rename.sql
# 预期: 无错误，UPDATE 影响 0 行
```

- [ ] **步骤 3：Commit**

```bash
git add db/upgrade/v2.1-device-status-rename.sql
git commit -m "feat(db): add v2.1.0 upgrade script for device status rename (idempotent)"
```

---

## 任务 3：后端 — 编写 DeviceServiceImpl case 3 错误信息变更测试

**文件：**
- 测试：`server/zwei-iot-device/src/test/java/com/zwei/iot/device/service/impl/DeviceServiceImplTest.java`

- [ ] **步骤 1：在测试类末尾新增两个测试方法**

在 `DeviceServiceImplTest.java` 的 `}` 闭合前（即最后一个测试方法之后）追加：

```java
    @Test
    @DisplayName("报修后停用应成功 (status 1→3)")
    void maintenanceDevice_normalToDisabled_shouldSucceed() {
        Device current = new Device();
        current.setId(1L);
        current.setCode("dev-001");
        current.setStatus(1);

        when(deviceMapper.selectDeviceById(1L)).thenReturn(current);

        String result = service.maintenanceDevice(1L, 3, "Test", "13800000000",
                "2026-06-15 10:00:00", "现场停用", "admin");

        assertEquals("停用", result);
        verify(deviceMapper).updateDevice(any(Device.class));
        verify(deviceStatusLogService).saveMaintenanceLog(
                eq(1L), eq("dev-001"), eq(1), eq(3), eq("停用"),
                eq("Test"), eq("13800000000"), any(), eq("现场停用"), eq("admin"));
    }

    @Test
    @DisplayName("维修后停用应成功 (status 2→3)")
    void maintenanceDevice_maintenanceToDisabled_shouldSucceed() {
        Device current = new Device();
        current.setId(2L);
        current.setCode("dev-002");
        current.setStatus(2);

        when(deviceMapper.selectDeviceById(2L)).thenReturn(current);

        String result = service.maintenanceDevice(2L, 3, "Test", "13800000000",
                "2026-06-15 10:00:00", "维修失败停用", "admin");

        assertEquals("停用", result);
        verify(deviceMapper).updateDevice(any(Device.class));
        verify(deviceStatusLogService).saveMaintenanceLog(
                eq(2L), eq("dev-002"), eq(2), eq(3), eq("停用"),
                eq("Test"), eq("13800000000"), any(), eq("维修失败停用"), eq("admin"));
    }

    @Test
    @DisplayName("停用后停用应抛 ServiceException 提示「仅正常或维修状态的设备可以停用」")
    void maintenanceDevice_disabledToDisabled_shouldThrow() {
        Device current = new Device();
        current.setId(3L);
        current.setCode("dev-003");
        current.setStatus(3);

        when(deviceMapper.selectDeviceById(3L)).thenReturn(current);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.maintenanceDevice(3L, 3, "Test", "13800000000",
                        "2026-06-15 10:00:00", "重复停用", "admin"));

        assertEquals("仅正常或维修状态的设备可以停用", ex.getMessage());
    }
```

- [ ] **步骤 2：添加 import（如果文件顶部没有）**

确认文件顶部 import 中已有：
```java
import com.zwei.common.exception.ServiceException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
```

如果没有，添加它们。检查当前 import 列表（位于 `DeviceServiceImplTest.java` 第 1-22 行），按需插入。

- [ ] **步骤 3：运行测试验证失败（断言旧错误信息）**

**重要**：此时**先不**修改 `DeviceServiceImpl`，先运行测试应当失败 — 失败信息会显示 "仅正常或故障状态的设备可以停用"（旧文案）。

```bash
cd server
mvn test -pl zwei-iot-device -Dtest=DeviceServiceImplTest#maintenanceDevice_disabledToDisabled_shouldThrow
```

预期：FAIL — `expected: <仅正常或维修状态的设备可以停用> but was: <仅正常或故障状态的设备可以停用>`

- [ ] **步骤 4：Commit（RED）**

```bash
git add server/zwei-iot-device/src/test/java/com/zwei/iot/device/service/impl/DeviceServiceImplTest.java
git commit -m "test(device): add maintenanceDevice case 3 tests (RED — assert 维修 wording)"
```

---

## 任务 4：后端 — 修正 DeviceServiceImpl case 3 注释 + 错误信息

**文件：**
- 修改：`server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/impl/DeviceServiceImpl.java:517-518`

- [ ] **步骤 1：修改 case 3 注释（line 517）**

修改前：
```java
            case 3 -> { // 停用：允许从 正常(1) 或 故障(2) 转入 停用(3)
                if (oldStatus != 1 && oldStatus != 2) throw new ServiceException("仅正常或故障状态的设备可以停用");
                yield 3;
            }
```

修改后：
```java
            case 3 -> { // 停用：允许从 正常(1) 或 维修(2) 转入 停用(3)
                if (oldStatus != 1 && oldStatus != 2) throw new ServiceException("仅正常或维修状态的设备可以停用");
                yield 3;
            }
```

- [ ] **步骤 2：运行单测验证通过（GREEN）**

```bash
cd server
mvn test -pl zwei-iot-device -Dtest=DeviceServiceImplTest
```

预期：全部测试 PASS（3 个新增 + 原有 6 个）。

- [ ] **步骤 3：Commit（GREEN）**

```bash
git add server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/impl/DeviceServiceImpl.java
git commit -m "fix(device): rename 故障→维修 in case 3 comment and error message"
```

---

## 任务 5：后端 — 修正 Device.java 字段 Javadoc

**文件：**
- 修改：`server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/Device.java:97-99`

- [ ] **步骤 1：修改 status 字段 Javadoc**

修改前：
```java
    /**
     * 业务状态（人工维护）: 1-正常, 2-故障, 3-停用
     */
    private Integer status;
```

修改后：
```java
    /**
     * 业务状态（人工维护）: 1-正常, 2-维修, 3-停用
     */
    private Integer status;
```

- [ ] **步骤 2：运行模块编译验证（无破坏）**

```bash
cd server
mvn compile -pl zwei-iot-device -am
```

预期：BUILD SUCCESS。

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/Device.java
git commit -m "docs(device): fix Device.status Javadoc to match unified wording"
```

---

## 任务 6：前端 — 修复 Device.vue 搜索下拉 label/value 错位 Bug

**文件：**
- 修改：`web/src/views/basic/Device.vue:25-26`

- [ ] **步骤 1：手动验证 Bug 复现（修改前）**

启动 dev server，访问 `/basic/device` 页面：
```bash
cd web
npm run dev
```

在搜索栏「设备状态」下拉中：选择「故障」选项 → 实际 value=2（维修状态的设备）；选择「维修」选项 → 实际 value=3（停用状态的设备）。**确认 Bug 存在**。

- [ ] **步骤 2：修改 Device.vue 搜索下拉**

修改前（line 25-26）：
```html
        <el-option label="正常" :value="1" />
        <el-option label="故障" :value="2" />
        <el-option label="维修" :value="3" />
```

修改后：
```html
        <el-option label="正常" :value="1" />
        <el-option label="维修" :value="2" />
        <el-option label="停用" :value="3" />
```

- [ ] **步骤 3：手动验证 Bug 修复（修改后）**

刷新浏览器（或 Vite 自动 HMR）：

| 操作 | 预期 |
|---|---|
| 选择「正常」 | 后端查 status=1（正常）设备 |
| 选择「维修」 | 后端查 status=2（维修）设备 |
| 选择「停用」 | 后端查 status=3（停用）设备 |
| 数据库查 `device.status=2` | 显示「维修」label（与字典一致） |

- [ ] **步骤 4：运行前端类型检查**

```bash
cd web
npm run build
```

预期：vue-tsc 通过 + vite build 成功。

- [ ] **步骤 5：Commit**

```bash
git add web/src/views/basic/Device.vue
git commit -m "fix(web): repair Device.vue status filter label/value mismatch (故障↔维修 swapped)"
```

---

## 任务 7：前端 — 修正 deviceIcon.ts JSDoc（4 处）

**文件：**
- 修改：`web/src/utils/deviceIcon.ts:8,9,22,72`

- [ ] **步骤 1：修改 JSDoc 第 8 行（red → 维修）**

修改前（line 8）：
```typescript
 *   <li><b>red</b> — 故障（status=2）</li>
```

修改后：
```typescript
 *   <li><b>red</b> — 维修（status=2）</li>
```

- [ ] **步骤 2：修改 JSDoc 第 9 行（停用/维修中 → 停用）**

修改前（line 9）：
```typescript
 *   <li><b>repair</b> — 停用/维修中（status=3）</li>
```

修改后：
```typescript
 *   <li><b>repair</b> — 停用（status=3）</li>
```

- [ ] **步骤 3：修改 @param 描述（line 22）**

修改前：
```typescript
 * @param status       业务状态：1=正常, 2=故障, 3=停用
```

修改后：
```typescript
 * @param status       业务状态：1=正常, 2=维修, 3=停用
```

- [ ] **步骤 4：修改函数注释（line 72）**

修改前：
```html
 * <p>默认颜色档位为 green（正常状态），仅当设备明确为故障/停用/离线时才切换其他颜色。</p>
```

修改后：
```html
 * <p>默认颜色档位为 green（正常状态），仅当设备明确为维修/停用时才切换其他颜色。</p>
```

> 注释：此处移除「离线」表述是因为离线由 `onlineStatus` 字段处理，不属于 `status` 业务状态的语义。

- [ ] **步骤 5：运行 TypeScript 检查**

```bash
cd web
npm run build
```

预期：vue-tsc 通过 + vite build 成功（注释改动不影响类型）。

- [ ] **步骤 6：Commit**

```bash
git add web/src/utils/deviceIcon.ts
git commit -m "docs(web): align deviceIcon.ts JSDoc with unified 维修/停用 wording"
```

---

## 任务 8：文档 — 修正 zwei-iot-device/CLAUDE.md（2 处）

**文件：**
- 修改：`server/zwei-iot-device/CLAUDE.md:122-129, 254`

- [ ] **步骤 1：修改设备状态机表（line 122-129）**

修改前：
```markdown
| operationType | 含义 | oldStatus → newStatus     |
|---------------|----|---------------------------|
| 1             | 报修 | 1 (正常) → 2 (故障)           |
| 2             | 修复 | 2 (故障) → 1 (正常)           |
| 3             | 停用 | 1 (正常) \| 2 (故障) → 3 (停用) |
| 4             | 恢复 | 3 (停用) → 1 (正常)           |
```

修改后：
```markdown
| operationType | 含义 | oldStatus → newStatus     |
|---------------|----|---------------------------|
| 1             | 报修 | 1 (正常) → 2 (维修)           |
| 2             | 修复 | 2 (维修) → 1 (正常)           |
| 3             | 停用 | 1 (正常) \| 2 (维修) → 3 (停用) |
| 4             | 恢复 | 3 (停用) → 1 (正常)           |
```

- [ ] **步骤 2：修改数据模型 device 段（line 254）**

修改前：
```markdown
- `device` — 设备主表 (id / code UNIQUE / sn / name / deviceType / networkType / protocolType: MQTT|HTTP|COAP /
  registerSource: MANUAL|API|IMPORT / authUsername UNIQUE char(6) / authPassword varchar(32) 明文 / authStatus / icon /
  iconPath / status: 1-正常 2-故障 3-离线 / lastReportTime / lastAuthTime / lastAuthIp / longitude / latitude)
```

修改后：
```markdown
- `device` — 设备主表 (id / code UNIQUE / sn / name / deviceType / networkType / protocolType: MQTT|HTTP|COAP /
  registerSource: MANUAL|API|IMPORT / authUsername UNIQUE char(6) / authPassword varchar(32) 明文 / authStatus / icon /
  iconPath / status: 1-正常 2-维修 3-停用 / lastReportTime / lastAuthTime / lastAuthIp / longitude / latitude)
```

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-device/CLAUDE.md
git commit -m "docs(device): unify status wording in module CLAUDE.md (state machine + data model)"
```

---

## 任务 9：文档 — 修正 docs/设备管理.md

**文件：**
- 修改：`docs/设备管理.md:5`

- [ ] **步骤 1：修改设备状态描述**

修改前：
```markdown
编号、设备名称（默认同编号）、所属隐患点（可以为空和多个）、设备状态（正常/故障/维修）、运行状态、最近上报时间、安装时间、创建部门、创建人员、创建时间、修改部门、修改人员、修改时间、删除、删除时间；
```

修改后：
```markdown
编号、设备名称（默认同编号）、所属隐患点（可以为空和多个）、设备状态（正常/维修/停用）、运行状态、最近上报时间、安装时间、创建部门、创建人员、创建时间、修改部门、修改人员、修改时间、删除、删除时间；
```

- [ ] **步骤 2：Commit**

```bash
git add docs/设备管理.md
git commit -m "docs: fix device management status list to 正常/维修/停用"
```

---

## 任务 10：端到端验证

- [ ] **步骤 1：数据库验证（执行升级脚本）**

```bash
mysql -u root -p geo_hazard_monitor < db/upgrade/v2.1-device-status-rename.sql

mysql -u root -p geo_hazard_monitor -e \
  "SELECT dict_label, dict_value, remark FROM sys_dict_data WHERE dict_type='device_status' ORDER BY dict_value;"
```

预期：1=正常, 2=维修, 3=停用。

- [ ] **步骤 2：后端构建 + 单测**

```bash
cd server
mvn clean test -pl zwei-iot-device -am
```

预期：BUILD SUCCESS + 全部测试 PASS（9 个，含新增 3 个）。

- [ ] **步骤 3：前端构建**

```bash
cd web
npm run build
```

预期：vue-tsc 通过 + vite build 成功。

- [ ] **步骤 4：API 端到端冒烟（启动后端 + 前端）**

```bash
# Terminal 1
cd server
mvn spring-boot:run -pl zwei-admin

# Terminal 2
cd web
npm run dev
```

浏览器访问 `http://localhost:5173/basic/device`：

| 检查项 | 预期 |
|---|---|
| 设备列表 | statusName 显示 正常/维修/停用（与 DB 一致） |
| 搜索下拉 | 「正常/维修/停用」三个选项，value 分别为 1/2/3 |
| 设备详情 status | 显示 statusName 来自后端 SQL CASE |
| 维修弹窗 | status=1 显示「报修/停用」；status=2 显示「修复/停用」；status=3 显示「启用」 |

- [ ] **步骤 5：Commit 验证报告（如需要）**

如果验证中发现问题，修复后按对应任务 commit。无需单独为验证结果 commit。

---

## 任务依赖图

```
[1. SQL 全量]    ─┐
[2. SQL 升级]    ─┤
                  ├─→ [10. 端到端验证]
[3. 单测 RED]    ─→ [4. 单测 GREEN]   ─┤
[5. Device.java] ──────────────────────┤
[6. Device.vue]  ──────────────────────┤
[7. deviceIcon]  ──────────────────────┤
[8. CLAUDE.md]   ──────────────────────┤
[9. 设备管理.md]  ──────────────────────┘
```

并行建议：任务 5/6/7/8/9 完全独立，可并发执行（子代理推荐模式）。
任务 3 → 4 串行（TDD red→green）。
任务 1/2 与其他任务可并发。

---

## 风险与回滚

| 风险 | 回滚 |
|---|---|
| 字典更新误改 | 升级脚本用 `dict_type='device_status'` 限定，WHERE 严格，回滚只需反向 UPDATE |
| Device.vue 修改后筛选反向 | 修改前已确认 Bug 存在；修改后通过手动验证确认正确 |
| 文档遗漏 | 9 处文件清单已列出，逐项 diff review；遗漏处可单独 commit 修复 |
| Maven 构建失败 | 单测改动只新增方法，不修改签名，编译兼容性已确认 |
| 字典同步失败影响前端 | 字典缓存由前端按需加载，启动时拉取；下次重启生效 |

---

## 变更记录

| 时间 | 变更 | 作者 |
|---|---|---|
| 2026-06-15 | 首次创建 — 设备业务状态统一为 1-正常/2-维修/3-停用实现计划 | writing-plans skill |
