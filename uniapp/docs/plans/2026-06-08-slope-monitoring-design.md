# 边坡监测预警小程序设计方案

## 概述

基于现有烟感监测小程序框架（uniapp + Vue 3 + Pinia + Wot Design Uni + ECharts），重构为边坡监测预警系统。保留框架和通用组件，替换页面内容和 tab 配置。

## 约束

- 主题色保持蓝色 #3068e4
- 页面内直接使用模拟数据，暂不对接 API
- 监测数据先做通用框架，不限定传感器类型
- 告警级别：红橙黄蓝四级

## 底部导航（5个Tab）

| Tab | 页面 | 说明 |
|-----|------|------|
| 事件大厅 | pages/index.vue | 告警事件列表与处理 |
| 隐患点 | pages/hazard.vue | 隐患点卡片列表 |
| 设备库 | pages/device.vue | 设备卡片列表 |
| 监测数据 | pages/chart.vue | ECharts数据可视化 |
| 个人中心 | pages/profile.vue | 保持现有 |

## 页面设计

### 事件大厅 (index.vue)

- 顶部：红橙黄蓝四级告警数量统计（4个统计卡片）
- 主体：告警事件列表（卡片形式，显示告警级别色条、隐患点名称、告警类型、时间）
- 下拉刷新 + 骨架屏加载态

### 告警详情 (alarm-detail.vue)

- 告警基本信息（级别、类型、时间、描述）
- 告警次数统计
- 分发日志时间线
- 关联设备列表（阈值预警=单设备，综合预警=该隐患点下所有设备）
- 处理按钮 + 反馈输入

### 隐患点 (hazard.vue)

- 顶部：隐患点总数统计
- 卡片列表：名称、等级、位置、设备数量、状态

### 隐患点详情 (hazard-detail.vue)

- 基本信息展示
- 关联设备列表

### 设备库 (device.vue)

- 搜索栏
- 卡片列表：设备名称、类型、连接状态、最后上报时间、所属隐患点

### 设备详情 (device-detail.vue)

- 设备配置信息
- 连接状态
- 最后上报数据时间

### 监测数据 (chart.vue)

- 设备选择器（支持多选）
- 时间范围切换
- ECharts图表（折线图/柱状图）
- 复用现有 echarts.vue 组件

### 个人中心 (profile.vue)

- 保持现有功能不变

## 文件改动清单

| 文件 | 操作 | 说明 |
|------|------|------|
| pages.json | 修改 | 5个tab配置+新页面路由 |
| manifest.json | 修改 | 更新应用名称 |
| pages/index.vue | 重写 | 事件大厅 |
| pages/hazard.vue | 新建 | 隐患点列表 |
| pages/hazard-detail.vue | 新建 | 隐患点详情 |
| pages/device.vue | 新建 | 设备库列表 |
| pages/device-detail.vue | 新建 | 设备详情 |
| pages/alarm-detail.vue | 重写 | 告警详情 |
| pages/chart.vue | 改造 | 多设备+数据图表 |
| utils/alarm.ts | 重写 | 告警模拟数据 |
| utils/hazard.ts | 新建 | 隐患点模拟数据 |
| utils/device.ts | 重写 | 设备模拟数据 |
| static/icons/ | 添加 | 新tab图标 |
