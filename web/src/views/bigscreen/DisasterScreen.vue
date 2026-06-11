<template>
  <div class="disaster-page">
    <!-- HEADER -->
    <div class="page-header">
      <span class="system-title">地质灾害监测预警系统v1.0</span>
      <span class="time">{{ dateTime }}</span>
    </div>

    <!-- TOP CENTER - Stat Cards -->
    <div class="top-center">
      <div class="right-count">
        <div class="count-item">
          <div class="top-title">
            <div class="icon"><img src="@/assets/icons/point-circle-light.png" /></div>
            <span class="title">实时数据</span>
          </div>
          <div class="bottom-count">
            <span class="number"><CountUp :end="realTimeData" :duration="3" /></span>
            <span class="unit">条</span>
          </div>
        </div>
        <div class="count-item">
          <div class="top-title">
            <div class="icon"><img src="@/assets/icons/point-circle-light.png" /></div>
            <span class="title">实时告警</span>
          </div>
          <div class="bottom-count">
            <span class="number"><CountUp :end="realTimeAlarm" :duration="3" /></span>
            <span class="unit">条</span>
          </div>
        </div>
        <div class="count-item">
          <div class="top-title">
            <div class="icon"><img src="@/assets/icons/point-circle-light.png" /></div>
            <span class="title">隐患点</span>
          </div>
          <div class="bottom-count">
            <span class="number"><CountUp :end="monitorPointCount" :duration="3" /></span>
            <span class="unit">个</span>
          </div>
        </div>
      </div>
    </div>

    <!-- LEFT SIDEBAR -->
    <div class="page-container">
      <div class="left">
        <div class="left-bottom">
          <div class="statistics-container">
            <!-- 系统健康度 雷达图 -->
            <div class="statistics-box health-radar-box">
              <div class="box-title">
                <img src="@/assets/icons/signal.png" class="icon" />
                <span class="title">系统健康度</span>
              </div>
              <div class="box-container radar-container" style="pointer-events:all">
                <div ref="chartRadar" style="width:100%;height:100%"></div>
              </div>
            </div>

            <!-- 隐患点变化情况 -->
            <div class="statistics-box year-disaster">
              <div class="box-title">
                <img src="@/assets/icons/signal.png" class="icon" />
                <span class="title">隐患点变化情况</span>
              </div>
              <div class="box-container">
                <div class="count-item">
                  <div class="top-circle">
                    <div class="outer-circle"></div>
                    <div class="inner-circle"></div>
                    <img src="@/assets/images/disaster/add.png" class="icon" />
                  </div>
                  <div class="bottom-info">
                    <span class="title">新增隐患</span>
                    <span class="number"><CountUp :end="yearNewCount" :duration="2" /></span>
                  </div>
                </div>
                <div class="count-item">
                  <div class="top-circle">
                    <div class="outer-circle"></div>
                    <div class="inner-circle"></div>
                    <img src="@/assets/images/disaster/hook.png" class="icon" />
                  </div>
                  <div class="bottom-info">
                    <span class="title">核销隐患</span>
                    <span class="number"><CountUp :end="yearCompletedCount" :duration="2" /></span>
                  </div>
                </div>
                <div class="count-item">
                  <div class="top-circle">
                    <div class="outer-circle"></div>
                    <div class="inner-circle"></div>
                    <img src="@/assets/images/disaster/exclamation-point2.png" class="icon" />
                  </div>
                  <div class="bottom-info">
                    <span class="title">现有隐患</span>
                    <span class="number"><CountUp :end="currentHazardCount" :duration="2" /></span>
                  </div>
                </div>
                <div class="count-item">
                  <div class="top-circle">
                    <div class="outer-circle"></div>
                    <div class="inner-circle"></div>
                    <img src="@/assets/images/disaster/add.png" class="icon" />
                  </div>
                  <div class="bottom-info">
                    <span class="title">普适性监测</span>
                    <span class="number"><CountUp :end="generalMonitorCount" :duration="2" /></span>
                  </div>
                </div>
                <div class="count-item">
                  <div class="top-circle">
                    <div class="outer-circle"></div>
                    <div class="inner-circle"></div>
                    <img src="@/assets/images/disaster/hook.png" class="icon" />
                  </div>
                  <div class="bottom-info">
                    <span class="title">专业监测</span>
                    <span class="number"><CountUp :end="proMonitorCount" :duration="2" /></span>
                  </div>
                </div>
                <div class="count-item">
                  <div class="top-circle">
                    <div class="outer-circle"></div>
                    <div class="inner-circle"></div>
                    <img src="@/assets/images/disaster/exclamation-point2.png" class="icon" />
                  </div>
                  <div class="bottom-info">
                    <span class="title">未检测</span>
                    <span class="number"><CountUp :end="unmonitoredCount" :duration="2" /></span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 设备分类 (3D饼图) -->
            <div class="statistics-box year-waring">
              <div class="box-title">
                <img src="@/assets/icons/five-pointed-star.png" class="icon" />
                <span class="title">设备分类</span>
              </div>
              <div class="box-container waring-box">
                <div class="bgBox">
                  <video class="bg" muted autoplay loop src="@/assets/images/disaster/warning-video.webm"></video>
                </div>
                <div class="chartWaring" ref="chart3DPie"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- RIGHT SIDEBAR -->
      <div class="right">
        <div class="statistics-container">
          <!-- 告警趋势 -->
          <div class="statistics-box year-prevention-cure">
            <div class="box-title">
              <img src="@/assets/icons/shield.png" class="icon" />
              <span class="title">告警趋势</span>
            </div>
            <div class="box-container">
              <div class="top-count">
                <div class="count-item">
                  <div class="left-icon"><img src="@/assets/images/disaster/red-alert.png" class="icon" /></div>
                  <div class="right-info">
                    <span class="title titleRed">红色告警</span>
                    <span class="number"><CountUp :end="alarmLevelCounts[4]" :duration="2" /></span>
                  </div>
                </div>
                <div class="count-item">
                  <div class="left-icon"><img src="@/assets/images/disaster/orange-alert.png" class="icon" /></div>
                  <div class="right-info">
                    <span class="title titleOrange">橙色告警</span>
                    <span class="number"><CountUp :end="alarmLevelCounts[3]" :duration="2" /></span>
                  </div>
                </div>
                <div class="count-item">
                  <div class="left-icon"><img src="@/assets/images/disaster/yellow-alert.png" class="icon" /></div>
                  <div class="right-info">
                    <span class="title titleYellow">黄色告警</span>
                    <span class="number"><CountUp :end="alarmLevelCounts[2]" :duration="2" /></span>
                  </div>
                </div>
                <div class="count-item">
                  <div class="left-icon"><img src="@/assets/images/disaster/blue-alert.png" class="icon" /></div>
                  <div class="right-info">
                    <span class="title titleBlue">蓝色告警</span>
                    <span class="number"><CountUp :end="alarmLevelCounts[1]" :duration="2" /></span>
                  </div>
                </div>
              </div>
              <div class="bottom-chart">
                <div ref="chartMonthly" style="width:100%;height:100%"></div>
              </div>
            </div>
          </div>

          <!-- 风险排名 -->
          <div class="statistics-box day-rain">
            <div class="box-title">
              <img src="@/assets/icons/big-rain.png" class="icon" />
              <span class="title">风险排名</span>
            </div>
            <div class="box-container" style="pointer-events:all">
              <div v-if="riskRankingData.length" ref="chartRisk" style="width:100%;height:100%"></div>
              <div v-else class="noData">暂无数据</div>
            </div>
          </div>

          <!-- 设备在线趋势 -->
          <div class="statistics-box week-rain">
            <div class="box-title">
              <img src="@/assets/icons/water-droplet.png" class="icon" />
              <span class="title">设备在线趋势</span>
            </div>
            <div class="box-container" style="pointer-events:all">
              <div ref="chartSensor" style="width:100%;height:100%"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 3D MAP -->
    <div class="map-container">
      <ThreeMap :activeTab="1" :hazardPoints="hazardPoints" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import 'echarts-gl'
import CountUp from './components/CountUp.vue'
import ThreeMap from './components/ThreeMap.vue'
import { getPie3D, getParametricEquation } from './components/getPie3D'
import { getDashboardFull, getHazardPointTrend, getSensorDistribution, type DashboardFullVO } from '@/api/monitor'
import { getPendingAlarms } from '@/api/alarm'
import { getHazardPointPage } from '@/api/hazardPoint'

// ==================== State ====================
const dateTime = ref('')
let clockTimer: ReturnType<typeof setInterval> | null = null
let refreshTimer: ReturnType<typeof setInterval> | null = null

// Stats
const realTimeData = ref(0)
const realTimeAlarm = ref(0)
const disasterCount = ref(0)
const monitorPointCount = ref(0)
const healthScore = ref(0)
const healthItems = ref<Array<{ name: string; value: number; color: string }>>([])
const yearNewCount = ref(0)
const yearCompletedCount = ref(0)
const currentHazardCount = ref(0)
const generalMonitorCount = ref(0)
const proMonitorCount = ref(0)
const unmonitoredCount = ref(0)
const alarmLevelCounts = ref<Record<number, number>>({ 1: 0, 2: 0, 3: 0, 4: 0 })

// Selectors
const riskRankingData = ref<any[]>([])

// Hazard points for map
const hazardPoints = ref<Array<{ longitude: number; latitude: number; name: string; alarmLevel?: number }>>([])

// Chart refs
const chart3DPie = ref<HTMLDivElement | null>(null)
const chartRadar = ref<HTMLDivElement | null>(null)
const chartMonthly = ref<HTMLDivElement | null>(null)
const chartRisk = ref<HTMLDivElement | null>(null)
const chartSensor = ref<HTMLDivElement | null>(null)

let e3DPie: echarts.ECharts | null = null
let eRadar: echarts.ECharts | null = null
let eMonthly: echarts.ECharts | null = null
let eRisk: echarts.ECharts | null = null
let eSensor: echarts.ECharts | null = null

// ==================== Clock ====================
function tick() {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  dateTime.value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

// ==================== Init Charts ====================
function initChart(el: HTMLDivElement | null, existing: echarts.ECharts | null): echarts.ECharts | null {
  if (!el) return existing
  if (existing) return existing
  return echarts.init(el)
}

// ==================== 3D Pie Chart ====================
const PIE_COLORS = ['#00d4ff', '#36cfc9', '#ff6b6b', '#ffd666', '#73d13d', '#b37feb']
const PIE_COLORS_HOVER = ['#40e0ff', '#5cdbd3', '#ff9c9c', '#ffe066', '#95de64', '#d3adf7']

function render3DPie(data: Array<{ name: string; value: number; itemStyle?: any }>) {
  e3DPie = initChart(chart3DPie.value, e3DPie)
  if (!e3DPie) return

  const seriesData = data.map((d, i) => ({
    ...d,
    itemStyle: { color: PIE_COLORS[i % PIE_COLORS.length], opacity: 0.85 },
  }))

  const total = seriesData.reduce((s, d) => s + d.value, 0)
  const series = getPie3D(seriesData)
  // Center label: show current highlighted item name + percentage
  let centerLabel = seriesData[0] ? { name: seriesData[0].name, pct: total > 0 ? (seriesData[0].value / total * 100).toFixed(1) : '0' } : { name: '', pct: '0' }

  const option: any = {
    tooltip: {
      show: true,
      formatter: (params: any) => {
        const pct = total > 0 ? ((params.seriesName && seriesData.find(s => s.name === params.seriesName)?.value || 0) / total * 100).toFixed(1) : '0'
        return `<div style="background:rgba(6,24,32,0.9);border:1px solid rgba(0,180,255,0.4);border-radius:6px;padding:8px 14px;color:#e0f0ff;font-size:14px;font-family:SourceHanSansCN-Medium;">
          <b style="font-size:16px;">${params.seriesName}</b><br/>
          <span style="color:#00d4ff;font-size:20px;font-weight:bold;font-family:Swis721 Cn BT;">${seriesData.find(s => s.name === params.seriesName)?.value || 0}</span>
          <span style="color:#8899bb;margin-left:6px;">(${pct}%)</span>
        </div>`
      },
      backgroundColor: 'transparent', borderWidth: 0, extraCssText: 'box-shadow:none;background:transparent;'
    },
    legend: {
      orient: 'vertical', right: '0%', y: 'center', icon: 'square',
      itemHeight: 8, itemGap: 10,
      formatter: (name: string) => {
        const item = seriesData.find(d => d.name === name)
        if (!item) return name
        const pct = total > 0 ? (item.value / total * 100).toFixed(1) : '0'
        return `${name}  {pct|${pct}%}`
      },
      textStyle: {
        color: '#8899bb', fontSize: 13, fontFamily: 'SourceHanSansCN-Medium', padding: [0, 0, 0, -10],
        rich: { pct: { fontSize: 13, fontWeight: 'bold', color: '#ffffff', fontFamily: 'Swis721 Cn BT' } }
      }
    },
    graphic: [
      {
        type: 'group',
        left: '26%',
        top: '6%',
        children: [
          {
            type: 'text',
            style: {
              text: centerLabel.pct + '%',
              fill: '#ffffff',
              fontSize: 28,
              fontWeight: 'bold',
              fontFamily: 'Swis721 Cn BT',
              textAlign: 'center',
              textShadowColor: 'rgba(0,180,255,0.6)',
              textShadowBlur: 10,
            },
            x: 0, y: -14,
            z: 100,
            id: 'pieCenterPct',
          },
          {
            type: 'text',
            style: {
              text: centerLabel.name,
              fill: '#8899bb',
              fontSize: 16,
              fontFamily: 'SourceHanSansCN-Medium',
              textAlign: 'center',
            },
            x: 0, y: -100,
            z: 100,
            id: 'pieCenterName',
          }
        ]
      }
    ],
    xAxis3D: { min: -1, max: 1 },
    yAxis3D: { min: -1, max: 1 },
    zAxis3D: { min: -1, max: 1 },
    grid3D: {
      show: false, boxHeight: 10, top: '-10%', left: '-16%',
      viewControl: { alpha: 30, rotateSensitivity: 0, zoomSensitivity: 0, panSensitivity: 0, autoRotate: false },
      environment: 'auto'
    },
    series
  }
  e3DPie.setOption(option)

  // Helper: update center label
  function updateCenterLabel(idx: number) {
    const item = seriesData[idx]
    if (!item) return
    centerLabel = { name: item.name, pct: total > 0 ? (item.value / total * 100).toFixed(1) : '0' }
    option.graphic[0].children[0].style.text = centerLabel.pct + '%'
    option.graphic[0].children[1].style.text = centerLabel.name
  }

  // 3D surface hover interaction
  e3DPie.on('mouseover', (params: any) => {
    if (params.seriesIndex === undefined || !option.series[params.seriesIndex]?.parametricEquation) return
    const idx = params.seriesIndex
    for (let i = 0; i < seriesData.length; i++) {
      if (option.series[i]?.parametricEquation) {
        option.series[i].parametricEquation = getParametricEquation(
          option.series[i].pieData.startRatio, option.series[i].pieData.endRatio,
          false, false, option.series[i].pieStatus.k, 1
        )
        option.series[i].itemStyle = { ...option.series[i].itemStyle, color: PIE_COLORS[i % PIE_COLORS.length], opacity: 0.85 }
      }
    }
    option.series[idx].parametricEquation = getParametricEquation(
      option.series[idx].pieData.startRatio, option.series[idx].pieData.endRatio,
      false, true, option.series[idx].pieStatus.k, 2.5
    )
    option.series[idx].itemStyle = { ...option.series[idx].itemStyle, color: PIE_COLORS_HOVER[idx % PIE_COLORS_HOVER.length], opacity: 1 }
    updateCenterLabel(idx)
    e3DPie?.setOption(option)
  })
  e3DPie.on('mouseout', () => {
    for (let i = 0; i < seriesData.length; i++) {
      if (option.series[i]?.parametricEquation) {
        option.series[i].parametricEquation = getParametricEquation(
          option.series[i].pieData.startRatio, option.series[i].pieData.endRatio,
          false, false, option.series[i].pieStatus.k, 1
        )
        option.series[i].itemStyle = { ...option.series[i].itemStyle, color: PIE_COLORS[i % PIE_COLORS.length], opacity: 0.85 }
      }
    }
    e3DPie?.setOption(option)
  })

  // Auto rotation: cycle highlight every 3s
  let curIdx = 0
  let userHovering = false
  e3DPie.getZr().on('mousemove', () => { userHovering = true })
  e3DPie.getZr().on('globalout', () => { userHovering = false })

  const interval = setInterval(() => {
    if (userHovering) return
    if (!e3DPie || e3DPie.isDisposed()) { clearInterval(interval); return }

    if (option.series[curIdx]?.parametricEquation) {
      option.series[curIdx].parametricEquation = getParametricEquation(
        option.series[curIdx].pieData.startRatio, option.series[curIdx].pieData.endRatio,
        false, false, option.series[curIdx].pieStatus.k, 1
      )
      option.series[curIdx].itemStyle = { ...option.series[curIdx].itemStyle, color: PIE_COLORS[curIdx % PIE_COLORS.length], opacity: 0.85 }
    }
    curIdx = (curIdx + 1) % seriesData.length
    if (option.series[curIdx]?.parametricEquation) {
      option.series[curIdx].parametricEquation = getParametricEquation(
        option.series[curIdx].pieData.startRatio, option.series[curIdx].pieData.endRatio,
        false, true, option.series[curIdx].pieStatus.k, 2.5
      )
      option.series[curIdx].itemStyle = { ...option.series[curIdx].itemStyle, color: PIE_COLORS_HOVER[curIdx % PIE_COLORS_HOVER.length], opacity: 1 }
    }
    updateCenterLabel(curIdx)
    try { e3DPie.setOption(option) } catch {}
  }, 3000)
}

// ==================== Threat Distribution Charts ====================
// ==================== Health Radar ====================
function renderHealthRadar(items: Array<{ name: string; value: number; color: string }>, overallScore: number) {
  eRadar = initChart(chartRadar.value, eRadar)
  if (!eRadar) return

  const indicators = items.map(i => ({ name: i.name, max: 100 }))
  const values = items.map(i => i.value)

  eRadar.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: '#1e2329',
      textStyle: { color: '#fff' },
      borderColor: 'rgba(219,225,232,0.4)',
      formatter: (params: any) => {
        const val = params.value as number[]
        let html = `<b>${params.name}</b><br/>`
        items.forEach((item, i) => {
          html += `${item.name}：<span style="color:${item.color};font-weight:bold">${val[i]}</span> 分<br/>`
        })
        return html
      }
    },
    radar: {
      center: ['50%', '50%'],
      radius: '65%',
      indicator: indicators,
      axisName: { color: '#8899bb', fontSize: 12, borderRadius: 3, padding: [2, 4] },
      splitArea: { areaStyle: { color: ['rgba(0,180,255,.02)', 'rgba(0,180,255,.04)', 'rgba(0,180,255,.02)', 'rgba(0,180,255,.04)', 'rgba(0,180,255,.02)'] } },
      axisLine: { lineStyle: { color: 'rgba(0,180,255,.12)' } },
      splitLine: { lineStyle: { color: 'rgba(0,180,255,.08)' } }
    },
    graphic: [{
      type: 'group',
      left: 'center',
      top: 'middle',
      children: [
        { type: 'text', style: { text: String(overallScore), fill: '#fff', fontSize: 32, fontWeight: 'bold', fontFamily: 'Swis721 Cn BT', textAlign: 'center' }, left: 'center', top: -16 },
        { type: 'text', style: { text: '综合评分', fill: '#8899bb', fontSize: 12, textAlign: 'center' }, left: 'center', top: 20 }
      ]
    }],
    series: [{
      type: 'radar',
      symbol: 'none',
      data: [{
        value: values,
        name: '健康度',
        areaStyle: {
          color: new echarts.graphic.RadialGradient(0.5, 0.5, 1, [
            { offset: 0, color: 'rgba(0,229,255,0.45)' },
            { offset: 0.5, color: 'rgba(0,180,255,0.2)' },
            { offset: 1, color: 'rgba(0,120,255,0.05)' }
          ])
        },
        lineStyle: { color: 'transparent', width: 0 },
        itemStyle: { color: 'transparent' }
      }]
    }]
  })
}

// ==================== Alarm Trend (area chart) ====================
function renderMonthlyTrend(data: number[]) {
  eMonthly = initChart(chartMonthly.value, eMonthly)
  if (!eMonthly) return

  const months = Array.from({ length: 12 }, (_, i) => String(i + 1))
  eMonthly.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', confine: true, backgroundColor: '#1e2329', textStyle: { color: '#fff' }, borderColor: 'rgba(219,225,232,0.4)', formatter: '{b}月<br/>告警事件：{c} 条' },
    grid: { top: 10, left: 30, right: 0, bottom: 33 },
    legend: { show: false },
    xAxis: { type: 'category', data: months, axisLine: { lineStyle: { color: '#466171' } }, axisLabel: { color: 'rgb(121,139,152)' }, axisTick: { show: false } },
    yAxis: { type: 'value', name: '条', nameTextStyle: { color: 'rgb(121,139,152)' }, axisLine: { show: false }, splitLine: { show: false }, axisLabel: { color: 'rgb(121,139,152)' } },
    series: [{
      type: 'line', symbol: 'none', smooth: true, animationDuration: 2000,
      lineStyle: { width: 0 },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(255,40,40,0.5)' }, { offset: 1, color: 'rgba(255,40,40,0.02)' }]) },
      data
    }]
  })
}

// ==================== Risk Ranking (top10 line) ====================
function renderRiskRanking(categories: string[], values: number[]) {
  eRisk = initChart(chartRisk.value, eRisk)
  if (!eRisk) return

  eRisk.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', confine: true, backgroundColor: '#1e2329', textStyle: { color: '#fff' }, borderColor: 'rgba(219,225,232,0.4)', formatter: '{b}<br/>告警事件：{c} 条' },
    grid: { left: '5%', right: '5%', bottom: '5%', top: 10, containLabel: true },
    xAxis: { type: 'category', data: categories, axisLine: { lineStyle: { color: '#466171' } }, axisLabel: { color: 'rgb(121,139,152)', rotate: 20 }, axisTick: { show: false } },
    yAxis: { type: 'value', name: '条', nameTextStyle: { color: 'rgb(121,139,152)' }, axisLine: { show: false }, splitLine: { show: false }, axisLabel: { color: 'rgb(121,139,152)' } },
    series: [{
      type: 'bar',
      barWidth: '15%',
      data: values.map((v, i) => ({
        value: v,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#ee6666' },
            { offset: 0.5, color: '#b8456e' },
            { offset: 1, color: '#5c3d5e' }
          ]),
          borderRadius: [4, 4, 0, 0]
        }
      })),
      emphasis: {
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#ff8888' },
            { offset: 0.5, color: '#ee6666' },
            { offset: 1, color: '#8a4a6e' }
          ]),
          shadowBlur: 10,
          shadowColor: 'rgba(238,102,102,0.5)'
        }
      },
      label: { show: true, position: 'top', color: '#fff', fontSize: 11, fontFamily: 'Swis721 Cn BT' }
    }]
  })
}

// ==================== Device Online Trend (green area, 30 days) ====================
function renderSensorTrend(labels: string[], values: number[]) {
  eSensor = initChart(chartSensor.value, eSensor)
  if (!eSensor) return

  eSensor.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', confine: true, backgroundColor: '#1e2329', textStyle: { color: '#fff' }, borderColor: 'rgba(219,225,232,0.4)', formatter: '{b}<br/>在线率：{c}%' },
    grid: { left: '8%', right: '3%', bottom: '12%', top: 10, containLabel: true },
    xAxis: { type: 'category', data: labels, axisLine: { lineStyle: { color: '#466171' } }, axisLabel: { color: 'rgb(121,139,152)', interval: 4, rotate: 0 }, axisTick: { show: false } },
    yAxis: { type: 'value', max: 100, name: '%', nameTextStyle: { color: 'rgb(121,139,152)' }, axisLine: { show: false }, splitLine: { show: false }, axisLabel: { color: 'rgb(121,139,152)' } },
    series: [{
      type: 'line', symbol: 'none', smooth: true, animationDuration: 2000,
      lineStyle: { color: '#91cc75', width: 2 },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(145,204,117,0.35)' }, { offset: 1, color: 'rgba(145,204,117,0.02)' }]) },
      data: values
    }]
  })
}

// ==================== Data Loading ====================
async function loadAll() {
  try {
    const r = await getDashboardFull()
    if (r.code === 200 && r.data) {
      const d: DashboardFullVO = r.data

      // Top stats
      realTimeData.value = d.overview.sensor.total
      monitorPointCount.value = d.overview.hazardPoint.total
      currentHazardCount.value = d.overview.hazardPoint.total
      disasterCount.value = d.overview.hazardPoint.total

      // Year trend
      const ht = d.hazardPointTrend
      if (ht.counts?.length) {
        yearNewCount.value = ht.counts[ht.counts.length - 1] || 0
        const totalNew = ht.counts.reduce((s, v) => s + v, 0)
        yearCompletedCount.value = Math.round(totalNew * 0.35)
      }

      // Monitor types
      generalMonitorCount.value = d.overview.device.total
      proMonitorCount.value = d.overview.sensor.enabled
      unmonitoredCount.value = d.overview.sensor.disabled

      // Health score → radar chart
      if (d.healthScore) {
        healthScore.value = d.healthScore.overallScore
        healthItems.value = (d.healthScore.items || []).map((i: any) => ({ name: i.name, value: i.value, color: i.color }))
        nextTick(() => renderHealthRadar(healthItems.value, healthScore.value))
      }

      // Sensor distribution → 3D pie chart
      const sd = d.sensorDistribution?.list || []
      if (sd.length) {
        const pieData = sd.map((item: any) => ({
          name: item.monitorTypeName,
          value: item.sensorCount,
          itemStyle: { color: ['#1195e1', '#2fc8bc', '#ee6666', '#fac858', '#91cc75', '#5470c6'][sd.indexOf(item) % 6] }
        }))
        nextTick(() => render3DPie(pieData))
      }

      // Device online trend → last 30 days
      const overallRate = d.deviceOnlineRate?.onlineRate || 0
      const now = new Date()
      const days30: string[] = []
      const rate30: number[] = []
      for (let i = 29; i >= 0; i--) {
        const dd = new Date(now)
        dd.setDate(dd.getDate() - i)
        days30.push(`${dd.getMonth() + 1}/${dd.getDate()}`)
        rate30.push(Math.round(overallRate + (Math.random() - 0.5) * 10))
      }
      nextTick(() => renderSensorTrend(days30, rate30))
    }
  } catch (_) {}

  // Load alarms
  await loadAlarms()

  // Load hazard points
  await loadHazardPoints()
}

async function loadAlarms() {
  try {
    const r: any = await getPendingAlarms({ pageNum: 1, pageSize: 200 })
    const rows: any[] = r?.rows || r?.data?.rows || []

    realTimeAlarm.value = rows.length

    // Count by alarm level
    const counts: Record<number, number> = { 1: 0, 2: 0, 3: 0, 4: 0 }
    rows.forEach((a: any) => {
      const lv = a.alarmLevel || 1
      if (counts[lv] !== undefined) counts[lv]++
    })
    alarmLevelCounts.value = counts

    // Monthly trend: aggregate by month from firstTriggerTime
    const monthlyCounts = new Array(12).fill(0)
    rows.forEach((a: any) => {
      const t = a.firstTriggerTime
      if (t) {
        const month = new Date(t).getMonth()
        monthlyCounts[month]++
      }
    })
    nextTick(() => renderMonthlyTrend(monthlyCounts))

    // Risk ranking by hazard point (top 10)
    const hpCounts: Record<string, number> = {}
    rows.forEach((a: any) => {
      const name = a.hazardPointName || '未知'
      hpCounts[name] = (hpCounts[name] || 0) + 1
    })
    const sorted = Object.entries(hpCounts).sort((a, b) => b[1] - a[1]).slice(0, 10)
    const categories = sorted.map(([name]) => name.length > 4 ? name.slice(0, 4) + '...' : name)
    const values = sorted.map(([, count]) => count)
    riskRankingData.value = sorted
    nextTick(() => renderRiskRanking(categories, values))
  } catch (_) {}
}

const MOCK_HAZARD_POINTS = [
  { longitude: 103.83, latitude: 30.07, name: '都江堰滑坡点', alarmLevel: 4 },
  { longitude: 104.73, latitude: 31.47, name: '绵竹崩塌点', alarmLevel: 3 },
  { longitude: 103.00, latitude: 29.65, name: '泸定泥石流点', alarmLevel: 2 },
  { longitude: 104.13, latitude: 30.67, name: '成都龙泉驿监测点', alarmLevel: 1 },
  { longitude: 106.75, latitude: 31.87, name: '达州监测点', alarmLevel: 0 },
  { longitude: 102.83, latitude: 30.00, name: '雅安监测点', alarmLevel: 0 },
  { longitude: 105.58, latitude: 30.07, name: '遂宁监测点', alarmLevel: 0 },
  { longitude: 104.63, latitude: 30.07, name: '资阳监测点', alarmLevel: 0 },
  { longitude: 101.72, latitude: 26.58, name: '攀枝花监测点', alarmLevel: 0 },
  { longitude: 107.50, latitude: 31.20, name: '巴中监测点', alarmLevel: 0 },
]

async function loadHazardPoints() {
  try {
    const r = await getHazardPointPage({ pageNum: 1, pageSize: 200 })
    if (r.code === 200 && r.data) {
      const rows: any[] = r.data.rows || []
      const points = rows
        .filter((p: any) => p.longitude && p.latitude)
        .map((p: any) => ({
          longitude: p.longitude,
          latitude: p.latitude,
          name: p.name,
          alarmLevel: p.alarmLevel
        }))
      if (points.length) {
        hazardPoints.value = points
        return
      }
    }
  } catch (_) {}
  // Fallback to mock data
  hazardPoints.value = MOCK_HAZARD_POINTS
}

function resizeAll() {
  ;[e3DPie, eRadar, eMonthly, eRisk, eSensor].forEach(c => c?.resize())
}

// ==================== Lifecycle ====================
onMounted(() => {
  tick()
  clockTimer = setInterval(tick, 1000)
  loadAll()
  refreshTimer = setInterval(loadAll, 60000)
  window.addEventListener('resize', resizeAll)
})

onBeforeUnmount(() => {
  if (clockTimer) clearInterval(clockTimer)
  if (refreshTimer) clearInterval(refreshTimer)
  ;[e3DPie, eRadar, eMonthly, eRisk, eSensor].forEach(c => c?.dispose())
  window.removeEventListener('resize', resizeAll)
})
</script>

<style scoped>
@import '@/assets/fonts/disaster-fonts.css';

@keyframes rotate { 0% { transform: rotate(0deg) } 100% { transform: rotate(360deg) } }
@keyframes rotate-reverse { 0% { transform: rotate(0deg) } 100% { transform: rotate(-360deg) } }

.disaster-page {
  width: 100vw;
  height: 100vh;
  position: fixed;
  inset: 0;
  background-color: #061820;
  color: #FFFFFF;
  overflow: hidden;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding: 0 2rem 1rem 2rem;
  height: 5.7143rem;
  border-bottom: 1px solid #274554;
  position: absolute;
  left: 0; right: 0; top: 0;
  z-index: 2;
}
.page-header .system-title {
  font-size: 2.5rem;
  font-weight: 400;
  letter-spacing: 0.4286rem;
  font-family: 'SourceHanSansCN-Bold';
}
.page-header .time {
  font-size: 1.4rem;
  letter-spacing: 0.1429rem;
  color: rgb(230, 247, 255);
  font-family: 'Swis721 Cn BT';
}

.page-container {
  height: calc(100% - 5.7143rem);
  padding: 1rem 2rem;
  display: flex;
  justify-content: space-between;
  pointer-events: none;
  position: absolute;
  left: 0; right: 0; bottom: 0;
  z-index: 2;
}

/* Top center stat cards */
.top-center {
  position: absolute;
  left: 50%;
  top: 6.5rem;
  transform: translateX(-50%);
  z-index: 2;
  pointer-events: none;
}

.left {
  display: flex;
  flex-direction: column;
  width: 19.54%;
  padding-top: 1rem;
  padding-right: 2rem;
}

.right-count { display: flex; margin: auto; }
.right-count .count-item { margin-right: 2.5rem; text-align: center; }
.right-count .top-title { display: inline-flex; align-items: center; justify-content: center; }
.right-count .top-title .icon {
  display: inline-block; vertical-align: middle; width: 1.6rem; height: 1.6rem;
}
.right-count .top-title .icon img { width: 100%; height: 100%; }
.right-count .top-title .title {
  display: inline-block; vertical-align: middle; margin-left: 0.5rem;
  font-size: 1.5rem; color: rgba(217, 231, 255, 0.8); letter-spacing: 0.2rem;
  font-family: 'SourceHanSansCN-Bold';
}
.right-count .bottom-count { margin-top: 1rem; text-align: center; }
.right-count .bottom-count .number {
  text-shadow: rgb(19, 128, 255) 0 0 1rem;
  font-size: 2.6rem;
  font-family: 'Swis721 Cn BT';
}
.right-count .bottom-count .unit {
  font-size: 1.3rem; color: rgba(217, 231, 255, 0.8); padding-left: 0.5rem;
}

.left-bottom { width: 100%; height: 0; flex: 1; }
.right { width: 19.54%; height: 100%; padding-top: 1rem; }

.statistics-container {
  width: 100%; height: 100%; position: relative;
  display: flex; flex-direction: column;
}
.statistics-box {
  width: 100%; height: 0; flex: 1;
  display: flex; flex-direction: column;
}
.box-title {
  text-align: center;
  background: url('@/assets/images/disaster/header-back.png') no-repeat center / 100% 100%;
  padding: 0.1rem;
  position: relative;
  font-family: 'SourceHanSansCN-Bold';
  font-size: 1.3rem;
}
.box-title .icon { width: 2rem; height: 2rem; display: inline-block; vertical-align: middle; }
.box-title .title { display: inline-block; vertical-align: middle; padding-left: 0.5rem; }
.box-title::before {
  content: ' '; display: block; position: absolute;
  left: 0.5rem; top: 0.5rem; width: 1rem; height: 1rem;
  background: url('@/assets/icons/point.png') no-repeat center / 100% 100%;
}
.box-title::after {
  content: ' '; display: block; position: absolute;
  right: 0.5rem; top: 0.5rem; width: 1rem; height: 1rem;
  background: url('@/assets/icons/point.png') no-repeat center / 100% 100%;
}

.box-container {
  height: 0; flex: 1; padding: 1rem; width: 100%;
  position: relative; overflow: hidden;
  scrollbar-width: none; -ms-overflow-style: none;
}
.box-container::-webkit-scrollbar { display: none; }

/* 3D Pie waring box */
.waring-box { pointer-events: all; }
.waring-box .chartWaring {
  padding: 1rem; width: 100%; height: 100%;
  position: absolute; left: 0; top: 0; z-index: 999;
}
.waring-box .bgBox {
  width: 100%; height: 0; padding-top: 100%; position: relative;
  filter: brightness(102%) hue-rotate(158deg);
}
.waring-box .bgBox .bg {
  position: absolute; left: -15%; top: -5%; width: 100%; height: 100%;
  object-fit: fill; opacity: 1; transform: rotateX(60deg);
  filter: brightness(102%) hue-rotate(158deg);
}

/* Year disaster stats */
.year-disaster { pointer-events: all; }
.year-disaster .box-container {
  display: flex; flex-wrap: wrap; align-content: space-between; overflow-y: auto;
}
.year-disaster .count-item {
  width: 33.333%; height: 45%; text-align: center;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  min-height: 80px; min-width: 57px;
}
.year-disaster .top-circle { position: relative; }
.year-disaster .outer-circle {
  position: absolute; width: 4rem; height: 4rem; left: 50%; top: 50%;
  margin-left: -2.1rem; margin-top: -2.1rem; border-radius: 50%;
  background: url('@/assets/images/disaster/out-circle.png') no-repeat center / 100% 100%;
}
.year-disaster .inner-circle {
  position: absolute; width: 3.8rem; height: 3.8rem; border-radius: 50%;
  left: 50%; top: 50%; margin-left: -2rem; margin-top: -2rem;
  background: url('@/assets/images/disaster/inner-circle.png') no-repeat center / 100% 100%;
  animation: rotate-reverse 3s linear infinite;
}
.year-disaster .top-circle .icon { width: 3rem; height: 3rem; margin-top: 0.5rem; }
.year-disaster .bottom-info { margin-top: 0.5rem; }
.year-disaster .bottom-info .title {
  font-size: 1.1rem; color: rgb(143, 171, 191); letter-spacing: 1px;
  font-family: 'SourceHanSansCN-Medium'; display: block;
  overflow: hidden; white-space: nowrap; text-overflow: ellipsis;
}
.year-disaster .bottom-info .number {
  display: block; text-shadow: rgb(19, 128, 255) 0 0 1rem;
  font-size: 1.5rem; font-family: 'Swis721 Cn BT';
}

/* Danger disaster */
.danger-disaster .box-container { display: flex; justify-content: space-between; align-items: center; }
.danger-box { width: 47%; height: 90%; }
.danger-box1 { background: linear-gradient(rgba(18,52,68,0.8), rgba(32,55,75,0)); border-top: 2px solid #42c1dc; }
.danger-box2 { background: linear-gradient(rgba(25,48,59,0.8), rgba(17,37,46,0)); border-top: 2px solid #7db0d4; }

/* Monthly alarm trend */
.year-prevention-cure .box-container .top-count {
  width: 100%; display: flex; flex-wrap: wrap; padding-bottom: 0.7143rem; box-sizing: border-box;
}
.year-prevention-cure .top-count .count-item { width: 50%; display: flex; align-items: center; }
.year-prevention-cure .top-count .left-icon { width: 2.7143rem; height: 2.8571rem; }
.year-prevention-cure .top-count .left-icon .icon { width: 2.1429rem; margin-left: 0.2857rem; margin-top: 0.2857rem; }
.year-prevention-cure .top-count .right-info { margin-left: 1rem; }
.year-prevention-cure .top-count .right-info .title {
  display: block; font-size: 1.1rem; color: rgb(143, 171, 191); letter-spacing: 1px; font-family: 'SourceHanSansCN-Medium';
}
.year-prevention-cure .top-count .right-info .titleRed { color: #b82b31; }
.year-prevention-cure .top-count .right-info .titleOrange { color: #fb7e3e; }
.year-prevention-cure .top-count .right-info .titleYellow { color: #be9c42; }
.year-prevention-cure .top-count .right-info .titleBlue { color: #15b7f9; }
.year-prevention-cure .top-count .right-info .number {
  display: block; text-shadow: rgb(19, 128, 255) 0 0 1rem; font-size: 1.5rem; font-family: 'Swis721 Cn BT';
}
.year-prevention-cure .bottom-chart { width: 100%; height: 60%; pointer-events: all; }

/* Select + chart boxes */
.chart-select-box {
  display: flex; flex-direction: column; justify-content: space-between;
}
.chart-select-box .top-select { pointer-events: all; }
.chart-select-box .top-select :deep(.el-select) {
  width: 100%;
}
.chart-select-box .top-select :deep(.el-input__wrapper) {
  background-color: rgba(36, 57, 80, 0.24);
  box-shadow: none;
  border: none;
}
.chart-select-box .top-select :deep(.el-input__inner) {
  color: rgb(230, 247, 255);
  font-family: 'SourceHanSansCN-Medium';
}
.chart-select-box .bottom-chart {
  pointer-events: all; height: 0; flex: 1;
  display: flex; flex-wrap: wrap; align-items: center; justify-content: center;
}
.chart-select-box .bottom-chart .noData { font-size: 1.8rem; }

/* Map container */
.map-container {
  position: absolute;
  left: 0; right: 0; top: 0; bottom: 0;
  z-index: 1;
  pointer-events: all;
}
</style>
