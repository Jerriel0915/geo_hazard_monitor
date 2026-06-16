# Uniapp 接口对接设计规格

> 创建于 2026-06-16
> 范围：uniapp 移动端对接后端真实接口，替换现有 mock 数据
> 工作区：使用 git worktree 隔离开发

## 1. 背景与目标

uniapp 移动端界面已开发完成，所有 `utils/*.ts` 当前返回**硬编码的 mock 数据**。本任务将它们替换为对后端 REST 接口的真实调用，并完成登录页验证码功能改造。

### 不在范围内
- `pages/index.vue`（事件大厅）—— 用户明确要求不对接
- `pages/alarm-detail.vue`（告警详情页）—— 不在范围内
- `utils/alarm.ts`、`utils/miniapp.ts` —— 保持现状

### BASE_URL
- 后端地址：`http://124.221.142.86/api/v1`
- 仅修改默认值，保留 DEV/PROD 对象结构

## 2. 接口契约（已确认）

### 2.1 认证接口

| 方法 | 路径 | 鉴权 | 用途 |
|---|---|---|---|
| GET | `/auth/captcha` | 匿名 | 获取图形验证码 |
| POST | `/auth/login` | 匿名 | 登录获取 token |
| GET | `/auth/getInfo` | Bearer | 获取当前用户信息 |
| POST | `/system/auth/logout` | Bearer | 登出 |

**`GET /auth/captcha` 响应：**
```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "captchaEnabled": true,
    "captchaKey": "uuid-string",
    "captchaImage": "base64-编码"
  }
}
```

**`POST /auth/login` 请求体：**
```json
{
  "username": "string",
  "password": "string",
  "code": "string",
  "uuid": "string",
  "rememberMe": false
}
```

**响应：**
```json
{
  "code": 200,
  "msg": "登陆成功",
  "data": { "token": "Bearer jwt-token", "expiresIn": 1200 }
}
```

**`GET /auth/getInfo` 响应：**
```json
{
  "code": 200,
  "data": {
    "user": {
      "userId": 1,
      "userName": "admin",
      "nickName": "管理员",
      "phonenumber": "13800138000",
      "avatar": "url",
      "deptId": 1,
      "email": "..."
    },
    "roles": ["admin"],
    "permissions": ["*:*:*"]
  }
}
```

### 2.2 隐患点接口

| 方法 | 路径 | 权限 |
|---|---|---|
| GET | `/hazard-points/page` | `iot:hazard-point:list` |
| GET | `/hazard-points/{id}` | `iot:hazard-point:query` |
| GET | `/hazard-points/{hpId}/bound-devices` | `iot:hazard-point:list` |

**列表查询参数：** `pageNum`、`pageSize`、`name`、`status`、`code`、`groupId`

**响应字段（HazardPointExportVO）：**
```
id, code, name, groupName, longitude, latitude, strike,
description, status(statusName), deviceCount,
createBy, createTime, updateBy, updateTime
```

**注意：** 后端**没有** `level`（风险等级）字段。

### 2.3 设备接口

| 方法 | 路径 | 权限 |
|---|---|---|
| GET | `/devices` | `basic:device:list`（不分页全量） |
| GET | `/devices/page` | `basic:device:list`（分页） |
| GET | `/devices/{id}` | `basic:device:query` |
| GET | `/devices/{deviceId}/sensors` | `basic:device:query` |

**列表查询参数：** `pageNum`、`pageSize`、`deviceCode`、`deviceName`、`status`、`onlineStatus`

**响应字段（DeviceExportVO）：**
```
id, code, name, sn, deviceTypeName, networkTypeName, protocolType,
vendorName, longitude, latitude, status(statusName),
onlineStatus(1=在线 / 0/null=离线), onlineStatusName,
sensorCount, lastReportTime, createTime
```

**注意：** 后端设备表**不直接包含** hazardName（通过 device_hazard_point 关联表维护）。

**`GET /devices/{deviceId}/sensors` 响应：** 传感器数组，每个传感器含 attrs 数组（attrCode、attrName、unit、rangeMin、rangeMax 等）。

### 2.4 监测数据接口

| 方法 | 路径 | 权限 |
|---|---|---|
| GET | `/monitor-data/latest` | `basic:device:query` |
| GET | `/monitor-data/page` | `basic:device:query` |
| GET | `/monitor-data/chart` | `basic:device:query` |

**`/monitor-data/chart` 查询参数（hazardPointId 必填）：**
```
hazardPointId, deviceId, sensorId, attrCode,
valueType(current/hour/24h/72h),
startTime, endTime
```

**响应（ChartDataVO 数组）：**
```
seriesName, deviceName, sensorName,
labels[], values[], unit, attrName,
maxValue, minValue, avgValue
```

## 3. 实施方案

### 3.1 `utils/api.ts` — request 工具
- BASE_URL 改为 `http://124.221.142.86/api/v1`
- 保留 DEV/PROD 对象，`currentEnv = ENV.PROD`
- 修复：401 跳转从 `redirectTo` 改为 `reLaunch`（避免页面栈问题）

### 3.2 `utils/auth.ts` — 认证 API
删除所有 mock，实现：

```ts
interface CaptchaInfo {
  captchaEnabled: boolean
  captchaKey: string
  captchaImage: string  // base64
}

interface LoginResult {
  token: string
  expiresIn?: number
}

interface UserInfo {
  userId: number
  userName: string
  nickName: string
  phonenumber: string
  avatar: string
  deptId?: number
  email?: string
}

export default {
  getCaptcha(): Promise<CaptchaInfo>
  login(username, password, code, uuid): Promise<LoginResult>
  getUserInfo(): Promise<{ user: UserInfo; roles: string[]; permissions: string[] }>
  logout(): Promise<void>
}
```

**Storage 约定（保留）：**
- `accessToken`：后端返回的 token 值（含 Bearer 前缀，Authorization 头格式 `Bearer ${token}`）
- `user`：JSON.stringify(UserInfo)
- **删除** `refreshToken` 概念（后端不提供）

### 3.3 `pages/login.vue` — 登录页改造
- 输入框 1：「账号」（type="text"，placeholder="请输入账号"）
- 输入框 2：「密码」（保留现有眼睛图标）
- 输入框 3（新增）：「验证码」4 位 + 右侧图片（点击刷新）
- 流程：
  1. `onMounted`：调 `getCaptcha` → 展示图片，保存 `captchaKey`
  2. 提交：`login(username, password, code, captchaKey)` → 拿 `token` → 存 `accessToken`
  3. 调 `getUserInfo` → 存 `user`
  4. `switchTab` 到 `/pages/index`
- 错误处理：登录失败刷新验证码

### 3.4 隐患点（`utils/hazard.ts` + `hazard.vue` + `hazard-detail.vue`）

**Hazard 接口字段调整：**
```ts
interface Hazard {
  id: number
  name: string
  code?: string
  longitude?: number
  latitude?: number
  location: string  // 派生：经纬度拼接
  status: string    // 后端 statusName
  deviceCount: number
  description?: string
  createTime: string
}
```

**API 实现：**
- `getAll()` → GET `/hazard-points/page?pageSize=100` → 映射为 Hazard[]
- `getById(id)` → GET `/hazard-points/{id}` → HazardWithDevices（devices 暂为空数组，单独拉）
- `getBoundDevices(id)` → GET `/hazard-points/{id}/bound-devices` → DeviceInfo[]

**UI 改动：**
- `hazard.vue` 列表卡片：**不显示 level**（现状已经不显示）
- `hazard-detail.vue`：**删除"风险等级"那一行**（line 24-29）和相关 `getLevelColor` 函数

### 3.5 设备库（`utils/device.ts` + `device.vue` + `device-detail.vue`）

**DeviceInfo 接口字段调整：**
```ts
interface DeviceInfo {
  id: number
  name: string          // 后端 name（替代 deviceName）
  code: string          // 后端 code（替代 deviceCode）
  deviceTypeName: string // 后端 deviceTypeName（替代 deviceType）
  status: string        // 后端 onlineStatusName："在线"/"离线"
  onlineStatus: number  // 后端 onlineStatus：1/0
  lastReportTime: string
  createTime?: string
}

interface SensorAttr {
  attrCode: string
  attrName: string
  unit?: string
  rangeMin?: number
  rangeMax?: number
}

interface DeviceSensor {
  id: number
  sensorNo?: string
  sensorName?: string
  attrs: SensorAttr[]
}
```

**API 实现：**
- `getAll()` → GET `/devices` → DeviceInfo[]
- `getById(id)` → GET `/devices/{id}` → DeviceInfo
- `getSensors(deviceId)` → GET `/devices/{deviceId}/sensors` → DeviceSensor[]

**UI 改动：**
- `device.vue`：卡片底部去掉「所属隐患点」一项，只保留「最近上报」
- `device-detail.vue`：去掉「所属隐患点」一行（line 50-53）
- `device-detail.vue`：监测参数从 `device.attributes` 改为 `getSensors` 拉取的 `attrs` 列表（无 currentValue，显示 attrName + unit）

**类型兼容（保留旧字段名）：** 为减少 .vue 改动量，可在 API 层做映射：
- `getAll/getById` 返回时同时填充 `deviceName/deviceCode/deviceType` 别名字段
- 但 `getTypeColor` 函数依赖 `deviceType`，可继续工作

### 3.6 监测数据（`utils/monitor.ts` 新建 + `chart.vue`）

**新建 utils/monitor.ts：**
```ts
interface ChartQuery {
  hazardPointId: number
  deviceId?: number
  sensorId?: number
  attrCode?: string
  valueType?: 'current' | 'hour' | '24h' | '72h'
  startTime: string
  endTime: string
}

interface ChartSeries {
  seriesName: string
  deviceName: string
  sensorName: string
  labels: string[]
  values: number[]
  unit: string
  attrName: string
  maxValue?: number
  minValue?: number
  avgValue?: number
}

export const monitorApi = {
  getLatest(hazardPointId: number): Promise<MonitorDataVO[]>
  getChart(query: ChartQuery): Promise<ChartSeries[]>
}
```

**chart.vue 改造：**
1. 顶部 FAB 弹窗内**新增隐患点下拉选择器**（默认显示「请选择隐患点」）
2. 选中隐患点 → 调 `hazardApi.getBoundDevices(hpId)` → 填充设备列表
3. 用户多选设备 → 关闭弹窗
4. 对每个已选设备：
   - 调 `deviceApi.getSensors(deviceId)` 拿传感器列表
   - 对每个传感器的每个 attrCode：
     - 调 `monitorApi.getChart({ hazardPointId, deviceId, sensorId, attrCode, startTime, endTime })`
     - 收集 ChartSeries
   - 一个设备生成一张图，多条线（每个 attrCode 一条）
5. 时间范围切换（24h / 7d / 30d）重新拉取

**UI 不变的部分：**
- 顶部设备标签横向滚动条
- 时间范围 tab
- 一个设备一张图的卡片布局
- 雨量类指标用柱状图，其他用折线图（保留现有判断逻辑：attrCode 含 `rain`）

### 3.7 `pages/profile.vue` — 个人中心
- 用户字段读取 storage（登录时已通过 getUserInfo 存入）：
  - `user.userName`、`user.nickName`、`user.phonenumber`
- 退出登录调 `authApi.logout()` → 清 storage → `reLaunch` 到 `/pages/login`

## 4. 类型映射策略

为减少 .vue 文件改动，API 层做字段映射：

| 业务字段 | 后端字段 | UI 兼容字段（保留） |
|---|---|---|
| 设备名 | `name` | `deviceName` 别名 |
| 设备编号 | `code` | `deviceCode` 别名 |
| 设备类型 | `deviceTypeName` | `deviceType` 别名 |
| 隐患点名 | `name` | `hazardName`（仅在告警上下文） |

## 5. 实施顺序

1. **基础：** `utils/api.ts` 修改 BASE_URL
2. **认证闭环：** `utils/auth.ts` + `pages/login.vue` + `pages/profile.vue`
3. **隐患点：** `utils/hazard.ts` + `pages/hazard.vue` + `pages/hazard-detail.vue`
4. **设备库：** `utils/device.ts` + `pages/device.vue` + `pages/device-detail.vue`
5. **监测数据：** `utils/monitor.ts` + `pages/chart.vue`（最复杂，最后做）

## 6. 风险与待验证

| 风险 | 缓解 |
|---|---|
| `/devices` 不分页接口可能未实现 | 实施时先试 `/devices`，失败回退 `/devices/page?pageSize=200` |
| `/monitor-data/chart` 必须传 hazardPointId | chart.vue 弹窗加隐患点下拉，强制先选 |
| 后端 `onlineStatusName` 是否真实返回 | 实施时验证；缺失则前端按 `onlineStatus===1?'在线':'离线'` 计算 |
| 设备详情的 sensors attrs 是否含 currentValue | device-detail.vue 不显示 currentValue，避免依赖 |
| 验证码图片在小程序 base64 显示 | 已用 `<image :src="'data:image/png;base64,'+captchaImage">` 兼容 |

## 7. 验证清单

实施完成后需验证：
- [ ] 登录：账号密码 + 验证码图片显示与校验
- [ ] 登出：调后端 logout 成功
- [ ] 隐患点列表加载、详情页加载、关联设备显示
- [ ] 设备库列表加载、详情页加载、监测参数显示
- [ ] 监测数据：选隐患点 → 选设备 → 看图表
- [ ] 个人中心：用户名/手机号显示正确
- [ ] 401 拦截：token 失效自动跳登录
