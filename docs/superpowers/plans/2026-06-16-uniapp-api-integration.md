# Uniapp 接口对接实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 将 uniapp 移动端 `utils/*.ts` 中的 mock 数据替换为对后端真实 REST 接口的调用，并完成登录页验证码功能。

**架构：** API 层（utils/*.ts）封装请求和字段映射，页面层（pages/*.vue）改成异步调用。统一 BASE_URL 为 `http://124.221.142.86/api/v1`，请求头自动携带 `Authorization: Bearer ${token}`。

**技术栈：** uniapp + Vue 3 + TypeScript + uni.request（无 axios）+ Pinia

**验证方式：** 本项目无单元测试框架。每个任务用 `npm run type-check` + `npm run lint` 验证，配合手动检查清单（H5 dev 模式下打开页面验证）。

**规格文档：** `docs/superpowers/specs/2026-06-16-uniapp-api-integration-design.md`

**工作目录：** 所有路径相对于 `uniapp/`

---

## 文件结构

### 修改的文件
| 文件 | 职责 |
|---|---|
| `src/utils/api.ts` | 通用请求工具（BASE_URL、token、错误处理） |
| `src/utils/auth.ts` | 认证 API：验证码、登录、用户信息、登出 |
| `src/utils/hazard.ts` | 隐患点 API：列表、详情、绑定设备 |
| `src/utils/device.ts` | 设备 API：列表、详情、传感器列表 |
| `src/pages/login.vue` | 登录页：账号输入 + 验证码图片 + 异步登录 |
| `src/pages/profile.vue` | 个人中心：用户字段映射 + 退出登录对接 |
| `src/pages/hazard.vue` | 隐患点列表页：异步加载 |
| `src/pages/hazard-detail.vue` | 隐患点详情页：删除风险等级 + 异步加载 |
| `src/pages/device.vue` | 设备库列表页：去掉所属隐患点 + 异步加载 |
| `src/pages/device-detail.vue` | 设备详情页：去掉所属隐患点 + getSensors + 异步加载 |
| `src/pages/chart.vue` | 监测数据页：加隐患点下拉 + 走 monitor-data/chart 接口 |

### 新建的文件
| 文件 | 职责 |
|---|---|
| `src/utils/monitor.ts` | 监测数据 API：最新值、图表数据 |

### 不动的文件
- `src/pages/index.vue`（事件大厅）—— 用户明确要求不对接
- `src/pages/alarm-detail.vue`、`src/pages/alarm-handle.vue`
- `src/utils/alarm.ts`、`src/utils/miniapp.ts`、`src/utils/dashboard.ts`、`src/utils/system.ts`、`src/utils/container.ts`、`src/utils/polling.ts`、`src/utils/appVersion.ts`、`src/utils/downloadManager.ts`
- `src/utils/auth.ts` 中的 `wechatLogin`、`refreshToken`、`checkToken` 函数删除（无对应后端实现）

---

## 任务 1：request 工具基础（BASE_URL + 401 跳转修复）

**文件：**
- 修改：`src/utils/api.ts`

- [ ] **步骤 1：修改 BASE_URL 和 401 跳转**

将 `src/utils/api.ts:7-24` 的 ENV 配置和 BASE_URL 修改为：

```ts
// 环境配置
const ENV = {
  // 开发环境
  DEV: {
    BASE_URL: 'http://124.221.142.86/api/v1',
    MQTT_URL: 'ws://124.221.142.86:8083/mqtt',
  },
  // 生产环境
  PROD: {
    BASE_URL: 'http://124.221.142.86/api/v1',
    MQTT_URL: 'ws://124.221.142.86:8083/mqtt',
  }
}

// 当前环境
const currentEnv = ENV.PROD

const BASE_URL = currentEnv.BASE_URL
const MQTT_URL = currentEnv.MQTT_URL
```

将 `src/utils/api.ts:86-88` 的 401 跳转改为：

```ts
          // 如果不是静默模式，跳转登录
          if (!options.silent) {
            uni.reLaunch({ url: '/pages/login' })
          }
```

- [ ] **步骤 2：type-check 和 lint 验证**

运行：`cd uniapp && npm run type-check`
预期：PASS（无错误）

运行：`cd uniapp && npm run lint -- src/utils/api.ts`
预期：PASS

- [ ] **步骤 3：Commit**

```bash
cd uniapp
git add src/utils/api.ts
git commit -m "fix(uniapp): 修正 request 工具 BASE_URL 并改用 reLaunch 处理 401"
```

---

## 任务 2：auth.ts + login.vue（验证码改造）

**文件：**
- 修改：`src/utils/auth.ts`（完全重写）
- 修改：`src/pages/login.vue`（增加验证码 UI、改账号输入、异步登录）

- [ ] **步骤 1：完全重写 `src/utils/auth.ts`**

替换整个文件为：

```ts
/**
 * 认证相关 API
 * @author linx
 */
import http from '@/utils/api'

export interface CaptchaInfo {
  captchaEnabled: boolean
  captchaKey: string
  captchaImage: string
}

export interface LoginResult {
  token: string
  expiresIn?: number
}

export interface UserInfo {
  userId: number
  userName: string
  nickName: string
  phonenumber: string
  avatar: string
  deptId?: number
  email?: string
}

export interface GetInfoResult {
  user: UserInfo
  roles: string[]
  permissions: string[]
}

const authApi = {
  /**
   * 获取图形验证码
   */
  getCaptcha(): Promise<CaptchaInfo> {
    return http.get('/auth/captcha', {}, { silent: true }) as Promise<CaptchaInfo>
  },

  /**
   * 账号密码登录
   */
  login(username: string, password: string, code: string, uuid: string): Promise<LoginResult> {
    return http.post('/auth/login', {
      username,
      password,
      code,
      uuid,
      rememberMe: false
    }) as Promise<LoginResult>
  },

  /**
   * 获取当前登录用户信息
   */
  getUserInfo(): Promise<GetInfoResult> {
    return http.get('/auth/getInfo') as Promise<GetInfoResult>
  },

  /**
   * 登出
   */
  logout(): Promise<void> {
    return http.post('/system/auth/logout', {}) as Promise<void>
  }
}

export default authApi
```

- [ ] **步骤 2：修改 `src/pages/login.vue` 的 template**

将 `src/pages/login.vue:16-48` 的 form 部分替换为：

```html
      <view class="login-form">
        <view class="form-item">
          <view class="input-icon-wrap">
            <zui-svg-icon icon="phone" width="36rpx" />
          </view>
          <input
            v-model="username"
            type="text"
            placeholder="请输入账号"
            class="input"
            maxlength="32"
          />
        </view>

        <view class="form-item">
          <view class="input-icon-wrap">
            <zui-svg-icon icon="lock" width="36rpx" />
          </view>
          <input
            v-model="password"
            :password="!showPassword"
            placeholder="请输入密码"
            class="input"
          />
          <view class="input-suffix" @click="showPassword = !showPassword">
            <zui-svg-icon :icon="showPassword ? 'eye-off' : 'eye'" width="32rpx" />
          </view>
        </view>

        <view class="form-item">
          <view class="input-icon-wrap">
            <zui-svg-icon icon="lock" width="36rpx" />
          </view>
          <input
            v-model="captchaCode"
            type="text"
            placeholder="请输入验证码"
            class="input"
            maxlength="6"
          />
          <view class="captcha-image" @click="refreshCaptcha">
            <image
              v-if="captchaImage"
              :src="captchaImage"
              mode="scaleToFill"
              class="captcha-img"
            />
            <text v-else class="captcha-loading">加载中</text>
          </view>
        </view>

        <button class="login-btn" @click="handleLogin" :loading="loading">
          <text v-if="!loading">登 录</text>
        </button>
      </view>
```

- [ ] **步骤 3：修改 `src/pages/login.vue` 的 script**

将 `src/pages/login.vue:57-109` 的整个 `<script setup>` 块替换为：

```ts
<script setup lang="ts">
import { useSafeArea } from '@/composables/useSafeArea'
import authApi from '@/utils/auth'
import { onMounted, ref } from 'vue'

const { statusBarHeight } = useSafeArea()

const username = ref('')
const password = ref('')
const captchaCode = ref('')
const captchaKey = ref('')
const captchaImage = ref('')
const loading = ref(false)
const showPassword = ref(false)
const systemTitle = ref('边坡监测预警系统')
const copyright = ref('© 2025 交通边坡监测预警系统 版权所有')
const versionName = ref('1.0.0')

onMounted(async () => {
  const accessToken = uni.getStorageSync('accessToken')
  if (accessToken) {
    uni.switchTab({ url: '/pages/index' })
    return
  }
  await refreshCaptcha()
})

const refreshCaptcha = async () => {
  try {
    const data = await authApi.getCaptcha()
    if (data.captchaEnabled) {
      captchaKey.value = data.captchaKey
      // 后端返回的是纯 base64 或带 data:image 前缀，统一处理
      captchaImage.value = data.captchaImage.startsWith('data:')
        ? data.captchaImage
        : `data:image/png;base64,${data.captchaImage}`
    } else {
      // 后端关闭验证码，隐藏输入框由 v-if 控制（保持简单：保留输入但提示）
      captchaImage.value = ''
    }
  } catch (error) {
    console.error('获取验证码失败:', error)
  }
}

const handleLogin = async () => {
  if (!username.value) {
    uni.showToast({ title: '请输入账号', icon: 'none' })
    return
  }
  if (!password.value) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return
  }
  if (!captchaCode.value) {
    uni.showToast({ title: '请输入验证码', icon: 'none' })
    return
  }

  loading.value = true
  try {
    const loginResult = await authApi.login(
      username.value,
      password.value,
      captchaCode.value,
      captchaKey.value
    )

    // 存储 token（保留 accessToken 键名，兼容 api.ts 的 getToken）
    uni.setStorageSync('accessToken', loginResult.token)

    // 拉取用户信息
    const info = await authApi.getUserInfo()
    const user = {
      id: info.user.userId,
      username: info.user.userName,
      nickname: info.user.nickName,
      phone: info.user.phonenumber,
      avatar: info.user.avatar || ''
    }
    uni.setStorageSync('user', JSON.stringify(user))

    uni.showToast({ title: '登录成功', icon: 'success' })

    setTimeout(() => {
      uni.switchTab({ url: '/pages/index' })
    }, 800)
  } catch (error: any) {
    console.error('登录失败:', error)
    uni.showToast({ title: error.message || '登录失败', icon: 'none' })
    // 登录失败刷新验证码
    captchaCode.value = ''
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>
```

- [ ] **步骤 4：在 `src/pages/login.vue` 的 `<style>` 块末尾追加验证码图片样式**

在 `.input-suffix` 样式块后追加：

```scss
.captcha-image {
  width: 160rpx;
  height: 60rpx;
  margin-left: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  border-radius: 8rpx;
  overflow: hidden;
}

.captcha-img {
  width: 160rpx;
  height: 60rpx;
}

.captcha-loading {
  font-size: 22rpx;
  color: #9ca3af;
}
```

- [ ] **步骤 5：type-check 和 lint 验证**

运行：`cd uniapp && npm run type-check`
预期：PASS

运行：`cd uniapp && npm run lint -- src/utils/auth.ts src/pages/login.vue`
预期：PASS

- [ ] **步骤 6：手动验证（H5 dev）**

运行：`cd uniapp && npm run dev:h5`
- 打开 `http://localhost:5173/#/pages/login`
- 检查：账号输入框、密码输入框、验证码输入框 + 验证码图片显示
- 点击验证码图片：能刷新
- 输入错误验证码：提示错误并刷新图片
- 输入正确凭据：登录成功跳转事件大厅

- [ ] **步骤 7：Commit**

```bash
cd uniapp
git add src/utils/auth.ts src/pages/login.vue
git commit -m "feat(uniapp): 登录页对接后端接口并增加图形验证码"
```

---

## 任务 3：profile.vue（退出登录对接）

**文件：**
- 修改：`src/pages/profile.vue`

- [ ] **步骤 1：修改 `profile.vue` 的 `handleLogout` 函数**

将 `src/pages/profile.vue:285-299` 的 `handleLogout` 替换为：

```ts
const handleLogout = async () => {
  const res = await new Promise<UniApp.ShowModalRes>((resolve) => {
    uni.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: resolve as any
    })
  })
  if (!res.confirm) return

  try {
    await authApi.logout()
  } catch (error) {
    console.error('登出接口失败（忽略，继续清理本地）:', error)
  }

  uni.removeStorageSync('accessToken')
  uni.removeStorageSync('user')
  uni.removeStorageSync('alarmSubscribed')
  uni.reLaunch({ url: '/pages/login' })
}
```

- [ ] **步骤 2：修改 `profile.vue` 的 import**

将 `src/pages/profile.vue:89` 的 import 修改为：

```ts
import authApi from '@/utils/auth'
```

（保留下面原有的 miniappApi import，仅新增 authApi）

实际操作：在 `src/pages/profile.vue:88-92` 的 import 块中追加一行：

```ts
import authApi from '@/utils/auth'
import miniappApi from '@/utils/miniapp'
```

- [ ] **步骤 3：type-check 和 lint 验证**

运行：`cd uniapp && npm run type-check`
预期：PASS

运行：`cd uniapp && npm run lint -- src/pages/profile.vue`
预期：PASS

- [ ] **步骤 4：Commit**

```bash
cd uniapp
git add src/pages/profile.vue
git commit -m "feat(uniapp): 个人中心退出登录对接后端 logout 接口"
```

---

## 任务 4：hazard.ts + hazard.vue + hazard-detail.vue

**文件：**
- 修改：`src/utils/hazard.ts`（完全重写）
- 修改：`src/pages/hazard.vue`（异步加载）
- 修改：`src/pages/hazard-detail.vue`（删除风险等级 + 异步加载）

- [ ] **步骤 1：完全重写 `src/utils/hazard.ts`**

替换整个文件为：

```ts
// src/utils/hazard.ts
import http from '@/utils/api'

export interface Hazard {
  id: number
  name: string
  code?: string
  longitude?: number
  latitude?: number
  location: string
  status: string
  deviceCount: number
  description?: string
  createTime: string
}

export interface HazardDetail extends Hazard {
  groupName?: string
  strike?: number
  updateBy?: string
  updateTime?: string
}

export interface HazardWithDevices extends HazardDetail {
  devices: any[]
}

interface HazardRawItem {
  id: number
  code?: string
  name: string
  groupName?: string
  longitude?: number
  latitude?: number
  strike?: number
  description?: string
  status?: number
  statusName?: string
  deviceCount?: number
  createTime?: string
  updateBy?: string
  updateTime?: string
}

const formatLocation = (item: HazardRawItem): string => {
  if (item.longitude != null && item.latitude != null) {
    return `${Number(item.longitude).toFixed(6)}, ${Number(item.latitude).toFixed(6)}`
  }
  return '-'
}

const mapHazard = (item: HazardRawItem): Hazard => ({
  id: item.id,
  name: item.name,
  code: item.code,
  longitude: item.longitude,
  latitude: item.latitude,
  location: formatLocation(item),
  status: item.statusName || (item.status === 1 ? '监测中' : '已停测'),
  deviceCount: item.deviceCount || 0,
  description: item.description || '',
  createTime: item.createTime || ''
})

export const hazardApi = {
  async getAll(): Promise<Hazard[]> {
    const res = await http.get('/hazard-points/page', {
      pageNum: 1,
      pageSize: 200
    })
    const list = (res as any)?.rows || (res as any[]) || []
    return list.map(mapHazard)
  },

  async getById(id: number): Promise<HazardWithDevices | undefined> {
    try {
      const res = await http.get(`/hazard-points/${id}`)
      const item = res as HazardRawItem
      const base = mapHazard(item)
      return {
        ...base,
        groupName: item.groupName,
        strike: item.strike,
        updateBy: item.updateBy,
        updateTime: item.updateTime,
        devices: []
      }
    } catch (error) {
      console.error('获取隐患点详情失败:', error)
      return undefined
    }
  },

  async getBoundDevices(hazardPointId: number): Promise<any[]> {
    try {
      const res = await http.get(`/hazard-points/${hazardPointId}/bound-devices`)
      const list = (res as any)?.rows || (res as any[]) || []
      return list
    } catch (error) {
      console.error('获取绑定设备失败:', error)
      return []
    }
  }
}

export default hazardApi
```

- [ ] **步骤 2：修改 `src/pages/hazard.vue` 的 `loadData`**

将 `src/pages/hazard.vue:88-97` 的 `loadData` 替换为：

```ts
const loadData = async () => {
  try {
    hazards.value = await hazardApi.getAll()
  } catch (error) {
    console.error('加载隐患点数据失败:', error)
  } finally {
    loading.value = false
    isRefreshing.value = false
  }
}
```

- [ ] **步骤 3：修改 `src/pages/hazard-detail.vue` 删除风险等级**

将 `src/pages/hazard-detail.vue:24-29` 的「风险等级」info-row 块删除：

删除这部分：
```html
          <view class="info-row">
            <text class="info-label">风险等级</text>
            <view class="level-tag" :style="{ background: getLevelColor(hazard.level) }">
              {{ hazard.level || '-' }}
            </view>
          </view>
```

- [ ] **步骤 4：修改 `src/pages/hazard-detail.vue` 的 script**

将 `src/pages/hazard-detail.vue:88-137` 整个 `<script setup>` 块替换为：

```ts
<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useSafeArea } from '@/composables/useSafeArea'
import { hazardApi } from '@/utils/hazard'
import type { Hazard } from '@/utils/hazard'

const { statusBarHeight } = useSafeArea()

const hazardId = ref<number>(0)
const hazard = ref<Partial<Hazard>>({})
const devices = ref<any[]>([])
const loading = ref(true)

onLoad(async (options) => {
  if (options?.id) {
    hazardId.value = Number(options.id)
    await loadData()
  }
})

const loadData = async () => {
  loading.value = true
  try {
    const detail = await hazardApi.getById(hazardId.value)
    if (detail) {
      hazard.value = detail
      devices.value = await hazardApi.getBoundDevices(hazardId.value)
    } else {
      uni.showToast({ title: '隐患点不存在', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 1500)
    }
  } catch (error) {
    console.error('加载隐患点详情失败:', error)
  } finally {
    loading.value = false
  }
}

const goToDeviceDetail = (item: any) => {
  uni.navigateTo({ url: `/pages/device-detail?id=${item.id}` })
}

const goBack = () => {
  uni.navigateBack()
}
</script>
```

- [ ] **步骤 5：删除 hazard-detail.vue 中已不使用的 device 类型导入和 deviceApi 引用**

由于 script 已重写，原来的 `deviceApi`、`DeviceInfo` 引用已删除，模板里的 device 字段需要适配后端响应：

修改 `src/pages/hazard-detail.vue:62-75` 模板中的设备显示部分，把 `device.deviceName` 改为 `device.name`、`device.deviceType` 改为 `device.deviceTypeName`、`device.status` 改为 `device.onlineStatusName`：

```html
          <view
            v-for="device in devices"
            :key="device.id"
            class="device-item"
            @click="goToDeviceDetail(device)"
          >
            <view class="device-left">
              <view class="device-main">
                <text class="device-name">{{ device.name || device.deviceName }}</text>
                <view class="device-type-tag">{{ device.deviceTypeName || device.deviceType || '-' }}</view>
              </view>
              <text class="device-time">最近上报：{{ device.lastReportTime || '-' }}</text>
            </view>
            <view class="device-status">
              <view
                class="status-dot"
                :class="(device.onlineStatusName || device.status) === '在线' ? 'online' : (device.onlineStatusName || device.status) === '故障' ? 'fault' : 'offline'"
              ></view>
              <text class="status-text">{{ device.onlineStatusName || device.status || '-' }}</text>
            </view>
          </view>
```

- [ ] **步骤 6：type-check 和 lint 验证**

运行：`cd uniapp && npm run type-check`
预期：PASS

运行：`cd uniapp && npm run lint -- src/utils/hazard.ts src/pages/hazard.vue src/pages/hazard-detail.vue`
预期：PASS

- [ ] **步骤 7：手动验证（H5 dev）**

- 进入隐患点 tab：列表加载显示
- 点击隐患点：详情页加载，显示基本信息和关联设备列表
- 检查详情页**没有**风险等级一行

- [ ] **步骤 8：Commit**

```bash
cd uniapp
git add src/utils/hazard.ts src/pages/hazard.vue src/pages/hazard-detail.vue
git commit -m "feat(uniapp): 隐患点列表/详情对接后端接口"
```

---

## 任务 5：device.ts + device.vue + device-detail.vue

**文件：**
- 修改：`src/utils/device.ts`（完全重写，含字段映射）
- 修改：`src/pages/device.vue`（去掉所属隐患点 + 异步加载）
- 修改：`src/pages/device-detail.vue`（去掉所属隐患点 + getSensors + 异步加载）

- [ ] **步骤 1：完全重写 `src/utils/device.ts`**

替换整个文件为：

```ts
// src/utils/device.ts
import http from '@/utils/api'

export interface DeviceInfo {
  id: number
  name: string
  code: string
  deviceTypeName: string
  status: string
  onlineStatus: number
  lastReportTime: string
  createTime?: string
  // 兼容字段（保留供现有 .vue 使用）
  deviceName: string
  deviceCode: string
  deviceType: string
}

export interface SensorAttr {
  attrCode: string
  attrName: string
  unit?: string
  rangeMin?: number | null
  rangeMax?: number | null
}

export interface DeviceSensor {
  id: number
  sensorNo?: string
  sensorName?: string
  monitorTypeName?: string
  attrs: SensorAttr[]
}

interface DeviceRawItem {
  id: number
  code?: string
  name?: string
  sn?: string
  deviceTypeName?: string
  status?: number
  statusName?: string
  onlineStatus?: number
  onlineStatusName?: string
  lastReportTime?: string
  createTime?: string
}

const mapDevice = (item: DeviceRawItem): DeviceInfo => {
  const name = item.name || ''
  const code = item.code || ''
  const deviceTypeName = item.deviceTypeName || ''
  const onlineStatus = item.onlineStatus ?? 0
  const status = item.onlineStatusName || (onlineStatus === 1 ? '在线' : '离线')
  return {
    id: item.id,
    name,
    code,
    deviceTypeName,
    status,
    onlineStatus,
    lastReportTime: item.lastReportTime || '',
    createTime: item.createTime,
    // 兼容字段
    deviceName: name,
    deviceCode: code,
    deviceType: deviceTypeName
  }
}

export const deviceApi = {
  async getAll(): Promise<DeviceInfo[]> {
    try {
      // 优先尝试不分页接口
      const res = await http.get('/devices')
      const list = (res as any)?.rows || (res as any[]) || []
      return list.map(mapDevice)
    } catch (error) {
      console.error('获取设备列表失败，回退分页:', error)
      const res = await http.get('/devices/page', { pageNum: 1, pageSize: 200 })
      const list = (res as any)?.rows || []
      return list.map(mapDevice)
    }
  },

  async getById(id: number): Promise<DeviceInfo | undefined> {
    try {
      const res = await http.get(`/devices/${id}`)
      return mapDevice(res as DeviceRawItem)
    } catch (error) {
      console.error('获取设备详情失败:', error)
      return undefined
    }
  },

  async getSensors(deviceId: number): Promise<DeviceSensor[]> {
    try {
      const res = await http.get(`/devices/${deviceId}/sensors`)
      const list = (res as any)?.rows || (res as any[]) || []
      // 兼容后端可能直接返回 attrs 字段或需要再查
      return list.map((s: any) => ({
        id: s.id,
        sensorNo: s.sensorNo,
        sensorName: s.sensorName,
        monitorTypeName: s.monitorTypeName,
        attrs: Array.isArray(s.attrs) ? s.attrs : (Array.isArray(s.attrList) ? s.attrList : [])
      }))
    } catch (error) {
      console.error('获取传感器列表失败:', error)
      return []
    }
  }
}

export default deviceApi
```

- [ ] **步骤 2：修改 `src/pages/device.vue` 的 `loadDevices`**

将 `src/pages/device.vue:155-157` 的 `loadDevices` 替换为：

```ts
const loadDevices = async () => {
  try {
    devices.value = await deviceApi.getAll()
  } catch (error) {
    console.error('加载设备列表失败:', error)
  } finally {
    setTimeout(() => { isRefreshing.value = false }, 400)
  }
}
```

- [ ] **步骤 3：修改 `src/pages/device.vue` 模板，去掉所属隐患点**

将 `src/pages/device.vue:91-100` 的 `card-bottom` 块替换为：

```html
          <view class="card-bottom">
            <view class="card-info-item">
              <text class="info-label">最近上报</text>
              <text class="info-value">{{ formatTime(item.lastReportTime) }}</text>
            </view>
          </view>
```

- [ ] **步骤 4：修改 `src/pages/device-detail.vue` 模板，去掉所属隐患点**

将 `src/pages/device-detail.vue:50-53` 的「所属隐患点」info-row 块删除：

删除这部分：
```html
          <view class="info-row">
            <text class="info-label">所属隐患点</text>
            <text class="info-value hazard-name">{{ device?.hazardName || '-' }}</text>
          </view>
```

同时将 `src/pages/device-detail.vue:55-57` 的「安装日期」info-row 也删除（后端设备表无 installDate 字段）：

```html
          <view class="info-row">
            <text class="info-label">安装日期</text>
            <text class="info-value">{{ device?.installDate || '-' }}</text>
          </view>
```

- [ ] **步骤 5：修改 `src/pages/device-detail.vue` 监测参数显示**

将 `src/pages/device-detail.vue:61-82` 的「监测参数」section 替换为：

```html
      <!-- 监测参数 -->
      <view class="section">
        <text class="section-title">监测参数</text>
        <view v-if="sensorAttrs.length > 0" class="attr-list">
          <view
            v-for="attr in sensorAttrs"
            :key="attr.attrCode"
            class="attr-card"
          >
            <view class="attr-left">
              <text class="attr-name">{{ attr.attrName }}</text>
            </view>
            <view class="attr-right">
              <text class="attr-unit">{{ attr.unit || '-' }}</text>
            </view>
          </view>
        </view>
        <view v-else-if="!sensorsLoading" class="empty-attrs">
          <text class="empty-attrs-text">暂无监测参数</text>
        </view>
      </view>
```

- [ ] **步骤 6：修改 `src/pages/device-detail.vue` 的 script**

将 `src/pages/device-detail.vue:97-151` 整个 `<script setup>` 替换为：

```ts
<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { deviceApi } from '@/utils/device'
import type { DeviceInfo, SensorAttr } from '@/utils/device'
import { useSafeArea } from '@/composables/useSafeArea'

const { statusBarHeight } = useSafeArea()

const deviceId = ref(0)
const device = ref<DeviceInfo | undefined>(undefined)
const sensorAttrs = ref<SensorAttr[]>([])
const sensorsLoading = ref(true)

onLoad(async (options) => {
  if (options?.id) {
    deviceId.value = Number(options.id)
    await loadDevice()
  }
})

const loadDevice = async () => {
  try {
    device.value = await deviceApi.getById(deviceId.value)
    if (!device.value) {
      uni.showToast({ title: '设备不存在', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 1500)
      return
    }
    // 拉取传感器及其属性
    const sensors = await deviceApi.getSensors(deviceId.value)
    const attrs: SensorAttr[] = []
    sensors.forEach(s => {
      s.attrs.forEach(a => attrs.push(a))
    })
    sensorAttrs.value = attrs
  } catch (error) {
    console.error('加载设备详情失败:', error)
  } finally {
    sensorsLoading.value = false
  }
}

const goBack = () => {
  uni.navigateBack()
}

const goToChart = () => {
  uni.navigateTo({ url: `/pages/chart?deviceId=${deviceId.value}` })
}

const getTypeColor = (type: string): string => {
  const colorMap: Record<string, string> = {
    'GNSS': '#3068e4',
    '雨量计': '#1890ff',
    '测斜仪': '#722ed1',
    '裂缝计': '#fa8c16',
    '水位计': '#13c2c2',
  }
  return colorMap[type] || '#3068e4'
}

const getStatusClass = (status: string): string => {
  switch (status) {
    case '在线': return 'online'
    case '离线': return 'offline'
    case '故障': return 'fault'
    default: return 'offline'
  }
}
</script>
```

- [ ] **步骤 7：type-check 和 lint 验证**

运行：`cd uniapp && npm run type-check`
预期：PASS

运行：`cd uniapp && npm run lint -- src/utils/device.ts src/pages/device.vue src/pages/device-detail.vue`
预期：PASS

- [ ] **步骤 8：手动验证（H5 dev）**

- 进入设备库 tab：列表加载，卡片**没有**所属隐患点
- 点击设备：详情页加载，**没有**所属隐患点和安装日期
- 监测参数列表显示从 sensors 接口拿到的属性

- [ ] **步骤 9：Commit**

```bash
cd uniapp
git add src/utils/device.ts src/pages/device.vue src/pages/device-detail.vue
git commit -m "feat(uniapp): 设备库列表/详情对接后端接口并拉取传感器参数"
```

---

## 任务 6：monitor.ts + chart.vue（监测数据）

**文件：**
- 创建：`src/utils/monitor.ts`
- 修改：`src/pages/chart.vue`（加隐患点下拉 + 走真实 chart 接口）

- [ ] **步骤 1：创建 `src/utils/monitor.ts`**

```ts
// src/utils/monitor.ts
import http from '@/utils/api'

export interface ChartQuery {
  hazardPointId: number
  deviceId?: number
  sensorId?: number
  attrCode?: string
  valueType?: 'current' | 'hour' | '24h' | '72h'
  startTime: string
  endTime: string
}

export interface ChartSeries {
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

export interface LatestMonitorData {
  hazardPointId: number
  hazardPointName: string
  deviceId: number
  deviceName: string
  sensorId: number
  sensorName: string
  attrCode: string
  attrName: string
  value: number | null
  unit: string
  dataTime: string
  quality: number
  qualityText: string
}

export const monitorApi = {
  async getLatest(hazardPointId: number): Promise<LatestMonitorData[]> {
    try {
      const res = await http.get('/monitor-data/latest', { hazardPointId })
      const list = (res as any)?.rows || (res as any[]) || []
      return list
    } catch (error) {
      console.error('获取最新监测数据失败:', error)
      return []
    }
  },

  async getChart(query: ChartQuery): Promise<ChartSeries[]> {
    try {
      const res = await http.get('/monitor-data/chart', {
        hazardPointId: query.hazardPointId,
        deviceId: query.deviceId,
        sensorId: query.sensorId,
        attrCode: query.attrCode,
        valueType: query.valueType || 'current',
        startTime: query.startTime,
        endTime: query.endTime
      })
      const list = (res as any)?.rows || (res as any[]) || []
      return list
    } catch (error) {
      console.error('获取图表数据失败:', error)
      return []
    }
  }
}

export default monitorApi
```

- [ ] **步骤 2：修改 `src/pages/chart.vue` 的 template（设备选择弹窗加隐患点下拉）**

将 `src/pages/chart.vue:103-126` 的 `picker-mask` 块替换为：

```html
    <!-- 设备选择弹窗 -->
    <view v-if="showDevicePicker" class="picker-mask" @click="showDevicePicker = false">
      <view class="picker-panel" @click.stop>
        <view class="picker-header">
          <text class="picker-title">选择设备</text>
          <text class="picker-close" @click="showDevicePicker = false">×</text>
        </view>

        <!-- 隐患点选择 -->
        <view class="hazard-filter">
          <text class="filter-label">隐患点：</text>
          <picker
            mode="selector"
            :range="hazardNames"
            :value="selectedHazardIndex"
            @change="onHazardChange"
          >
            <view class="filter-picker">
              <text class="filter-text">{{ hazardNames[selectedHazardIndex] || '请选择' }}</text>
              <text class="filter-arrow">▼</text>
            </view>
          </picker>
        </view>

        <scroll-view class="picker-list" scroll-y>
          <view v-if="availableDevices.length === 0" class="picker-empty">
            <text class="picker-empty-text">{{ selectedHazardId ? '该隐患点暂无设备' : '请先选择隐患点' }}</text>
          </view>
          <view
            v-for="device in availableDevices"
            :key="device.id"
            class="picker-item"
            :class="{ selected: isSelected(device.id) }"
            @click="toggleDevice(device)"
          >
            <view class="picker-device-info">
              <text class="picker-device-name">{{ device.name || device.deviceName }}</text>
              <text class="picker-device-type">{{ device.deviceTypeName || device.deviceType || '-' }}</text>
            </view>
            <view class="picker-check" v-if="isSelected(device.id)">✓</view>
          </view>
        </scroll-view>
      </view>
    </view>
```

- [ ] **步骤 3：完全重写 `src/pages/chart.vue` 的 `<script setup>`**

将 `src/pages/chart.vue:130-390` 整个 `<script setup>` 块替换为：

```ts
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useSafeArea } from '@/composables/useSafeArea'
import * as echartsLib from '@/components/echarts.esm.min.js'
import EchartsComponent from '@/components/echarts.vue'
import { deviceApi } from '@/utils/device'
import type { DeviceInfo, DeviceSensor } from '@/utils/device'
import { hazardApi } from '@/utils/hazard'
import type { Hazard } from '@/utils/hazard'
import { monitorApi } from '@/utils/monitor'
import type { ChartSeries } from '@/utils/monitor'

const { statusBarHeight } = useSafeArea()

const timeTabs = [
  { label: '24小时', value: '24h' },
  { label: '7天', value: '7d' },
  { label: '30天', value: '30d' }
]

const loading = ref(false)
const allHazards = ref<Hazard[]>([])
const allDevices = ref<DeviceInfo[]>([])
const selectedDevices = ref<DeviceInfo[]>([])
const activeTimeTab = ref('24h')
const showDevicePicker = ref(false)
const selectedHazardIndex = ref(0)

interface ChartGroup {
  deviceId: number
  deviceName: string
  deviceType: string
  option: any
}

const chartGroups = ref<ChartGroup[]>([])

const hazardNames = computed(() => allHazards.value.map(h => h.name))

const selectedHazardId = computed(() => {
  const h = allHazards.value[selectedHazardIndex.value]
  return h?.id || 0
})

const availableDevices = computed(() => {
  if (!selectedHazardId.value) return []
  // 从已加载的 allDevices 中筛选该隐患点的设备
  return allDevices.value
})

const isSelected = (id: number) => selectedDevices.value.some(d => d.id === id)

onMounted(async () => {
  // 加载隐患点列表
  try {
    allHazards.value = await hazardApi.getAll()
  } catch (error) {
    console.error('加载隐患点失败:', error)
  }

  // 检查 URL 参数，预选设备
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1] as any
  const options = currentPage?.options || currentPage?.$page?.options || {}
  if (options?.deviceId) {
    try {
      const device = await deviceApi.getById(Number(options.deviceId))
      if (device) {
        selectedDevices.value = [device]
        await loadAllCharts()
      }
    } catch (error) {
      console.error('预加载设备失败:', error)
    }
  }
})

const onHazardChange = async (e: any) => {
  selectedHazardIndex.value = e.detail.value
  if (selectedHazardId.value) {
    try {
      // 拉取该隐患点下的设备
      const list = await hazardApi.getBoundDevices(selectedHazardId.value)
      allDevices.value = list.map((d: any) => ({
        id: d.id,
        name: d.name || d.deviceName || '',
        code: d.code || d.deviceCode || '',
        deviceTypeName: d.deviceTypeName || d.deviceType || '',
        status: d.onlineStatusName || d.status || '',
        onlineStatus: d.onlineStatus ?? 0,
        lastReportTime: d.lastReportTime || '',
        deviceName: d.name || d.deviceName || '',
        deviceCode: d.code || d.deviceCode || '',
        deviceType: d.deviceTypeName || d.deviceType || ''
      })) as DeviceInfo[]
    } catch (error) {
      console.error('加载设备列表失败:', error)
      allDevices.value = []
    }
  } else {
    allDevices.value = []
  }
}

const toggleDevice = (device: DeviceInfo) => {
  const idx = selectedDevices.value.findIndex(d => d.id === device.id)
  if (idx >= 0) {
    selectedDevices.value.splice(idx, 1)
  } else {
    selectedDevices.value.push(device)
  }
  loadAllCharts()
}

const removeDevice = (id: number) => {
  selectedDevices.value = selectedDevices.value.filter(d => d.id !== id)
  chartGroups.value = chartGroups.value.filter(g => g.deviceId !== id)
}

const changeTimeTab = (value: string) => {
  activeTimeTab.value = value
  loadAllCharts()
}

const getTimeRange = () => {
  let hours = 24
  switch (activeTimeTab.value) {
    case '7d': hours = 168; break
    case '30d': hours = 720; break
  }
  const endTime = new Date()
  const startTime = new Date(endTime.getTime() - hours * 3600000)
  const fmt = (d: Date) => {
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  }
  return { startTime: fmt(startTime), endTime: fmt(endTime) }
}

const loadAllCharts = async () => {
  if (selectedDevices.value.length === 0 || !selectedHazardId.value) {
    chartGroups.value = []
    return
  }

  loading.value = true
  try {
    const groups: ChartGroup[] = []
    const { startTime, endTime } = getTimeRange()
    const hazardPointId = selectedHazardId.value

    for (const device of selectedDevices.value) {
      // 拉取该设备的传感器列表
      let sensors: DeviceSensor[] = []
      try {
        sensors = await deviceApi.getSensors(device.id)
      } catch (error) {
        console.error(`获取设备 ${device.id} 传感器失败:`, error)
      }

      // 对每个传感器的每个 attrCode 拉取图表数据
      const allSeries: ChartSeries[] = []
      for (const sensor of sensors) {
        for (const attr of sensor.attrs) {
          try {
            const seriesList = await monitorApi.getChart({
              hazardPointId,
              deviceId: device.id,
              sensorId: sensor.id,
              attrCode: attr.attrCode,
              valueType: 'current',
              startTime,
              endTime
            })
            allSeries.push(...seriesList)
          } catch (error) {
            console.error(`获取图表数据失败 device=${device.id} attr=${attr.attrCode}:`, error)
          }
        }
      }

      const option = buildOption(allSeries)
      groups.push({
        deviceId: device.id,
        deviceName: device.name || device.deviceName,
        deviceType: device.deviceTypeName || device.deviceType,
        option
      })
    }

    chartGroups.value = groups
  } finally {
    loading.value = false
  }
}

const buildOption = (series: ChartSeries[]): any => {
  if (series.length === 0) return null

  // 合并所有 labels 作为 x 轴
  const allLabels = new Set<string>()
  series.forEach(s => s.labels.forEach(l => allLabels.add(l)))
  const categories = Array.from(allLabels)

  const seriesColors = ['#3068e4', '#52c41a', '#fa8c16', '#722ed1', '#13c2c2', '#eb2f96']
  const seriesList: any[] = series.map((s, i) => {
    const color = seriesColors[i % seriesColors.length]
    const valueMap = new Map<string, number>()
    s.labels.forEach((l, idx) => valueMap.set(l, s.values[idx]))
    const data = categories.map(c => valueMap.get(c) ?? null)
    const isRain = /rain/i.test(s.attrCode) || /雨量/.test(s.attrName)

    if (isRain) {
      return {
        name: s.seriesName || s.attrName,
        type: 'bar',
        data,
        yAxisIndex: 0,
        itemStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color },
              { offset: 1, color: hexToRgba(color, 0.4) }
            ]
          },
          borderRadius: [4, 4, 0, 0]
        },
        barWidth: '30%'
      }
    }
    return {
      name: s.seriesName || s.attrName,
      type: 'line',
      data,
      smooth: true,
      symbol: 'circle',
      symbolSize: 4,
      showSymbol: categories.length <= 24,
      lineStyle: { color, width: 2.5 },
      itemStyle: { color: '#fff', borderColor: color, borderWidth: 2 },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: hexToRgba(color, 0.2) },
            { offset: 1, color: hexToRgba(color, 0.02) }
          ]
        }
      }
    }
  })

  const hasBarAndLine = seriesList.some((s: any) => s.type === 'bar') && seriesList.some((s: any) => s.type === 'line')

  return {
    animation: true,
    legend: {
      data: seriesList.map((s: any) => s.name),
      bottom: 0,
      textStyle: { color: '#6b7280', fontSize: 10 },
      itemWidth: 16,
      itemHeight: 10
    },
    grid: { left: '5%', right: hasBarAndLine ? '8%' : '5%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: categories,
      boundaryGap: hasBarAndLine,
      axisLabel: { color: '#9ca3af', fontSize: 9, margin: 8 },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false },
      splitLine: { show: false }
    },
    yAxis: hasBarAndLine ? [
      {
        type: 'value',
        axisLabel: { color: '#9ca3af', fontSize: 9 },
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } }
      },
      {
        type: 'value',
        axisLabel: { color: '#9ca3af', fontSize: 9 },
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { show: false }
      }
    ] : {
      type: 'value',
      axisLabel: { color: '#9ca3af', fontSize: 9 },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } }
    },
    series: seriesList,
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0,0,0,0.8)',
      borderColor: 'rgba(0,0,0,0.8)',
      textStyle: { color: '#fff', fontSize: 11 }
    }
  }
}

const initChart = (canvas: any, width: number, height: number, deviceId: number) => {
  const group = chartGroups.value.find(g => g.deviceId === deviceId)
  if (!group?.option) return null

  const chart = echartsLib.init(canvas, null, { width, height })
  canvas.setChart(chart)
  chart.setOption(group.option)
  return chart
}

const hexToRgba = (hex: string, alpha: number) => {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}
</script>
```

- [ ] **步骤 4：在 `src/pages/chart.vue` 的 `<style>` 末尾追加隐患点筛选样式**

```scss
/* 隐患点筛选 */
.hazard-filter {
  display: flex;
  align-items: center;
  padding: 16rpx 32rpx;
  border-bottom: 1rpx solid #f0f0f0;
  gap: 16rpx;
}

.filter-label {
  font-size: 26rpx;
  color: #6b7280;
  flex-shrink: 0;
}

.filter-picker {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12rpx 20rpx;
  background: #f7f8fc;
  border-radius: 8rpx;
}

.filter-text {
  font-size: 26rpx;
  color: #1a1a2e;
}

.filter-arrow {
  font-size: 18rpx;
  color: #9ca3af;
}

.picker-empty {
  padding: 80rpx 0;
  text-align: center;
}

.picker-empty-text {
  font-size: 26rpx;
  color: #9ca3af;
}
```

- [ ] **步骤 5：type-check 和 lint 验证**

运行：`cd uniapp && npm run type-check`
预期：PASS

运行：`cd uniapp && npm run lint -- src/utils/monitor.ts src/pages/chart.vue`
预期：PASS

- [ ] **步骤 6：手动验证（H5 dev）**

- 进入监测数据 tab：默认无设备，提示添加
- 点击 FAB 弹出设备选择：先选隐患点（picker），自动加载该隐患点的设备
- 多选设备：每个设备显示一张图，每条曲线是一个 attrCode 的数据
- 切换时间范围（24h/7d/30d）：图表重新加载

- [ ] **步骤 7：Commit**

```bash
cd uniapp
git add src/utils/monitor.ts src/pages/chart.vue
git commit -m "feat(uniapp): 监测数据对接后端 chart 接口并新增隐患点筛选"
```

---

## 全部任务完成后

- [ ] **步骤：全量 type-check + lint**

运行：`cd uniapp && npm run type-check`
预期：PASS

运行：`cd uniapp && npm run lint`
预期：PASS（warning 可忽略）

- [ ] **步骤：H5 端到端手动测试**

按规格文档第 7 节"验证清单"完整跑一遍。

- [ ] **步骤：合并回主分支**

由用户决定：直接 merge / 提 PR / cherry-pick。
