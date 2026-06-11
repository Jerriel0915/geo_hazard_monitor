/**
 * 生成3D饼图配置项（echarts-gl surface）
 * 移植自 showroom/src/views/disaster/components/getPie3D.js
 */

interface PieDataItem {
  name: string
  value: number
  itemStyle?: { color?: string; opacity?: number }
  selected?: boolean
  hovered?: boolean
  label?: any
  proportion?: string
  startRatio?: number
  endRatio?: number
}

interface ParametricEquation {
  u: { min: number; max: number; step: number }
  v: { min: number; max: number; step: number }
  x: (u: number, v: number) => number
  y: (u: number, v: number) => number
  z: (u: number, v: number) => number
}

interface SeriesItem {
  name: string
  type: 'surface'
  parametric: boolean
  wireframe: { show: boolean }
  pieData: PieDataItem
  pieStatus: { selected: boolean; hovered: boolean; k: number }
  itemStyle?: { color?: string; opacity?: number }
  parametricEquation?: ParametricEquation
}

export function getPie3D(
  pieData: PieDataItem[],
  internalDiameterRatio?: number,
  height?: number
): SeriesItem[] {
  const series: SeriesItem[] = []
  let sumValue = 0
  let startValue = 0
  let endValue = 0

  const k =
    typeof internalDiameterRatio !== 'undefined'
      ? (1 - internalDiameterRatio) / (1 + internalDiameterRatio)
      : 1 / 8

  let total = 0
  for (let i = 0; i < pieData.length; i++) {
    pieData[i].value = Number(pieData[i].value)
    total += Number(pieData[i].value)
  }

  for (let i = 0; i < pieData.length; i++) {
    pieData[i].proportion = parseFloat(String(pieData[i].value / total)).toFixed(4)
  }

  for (let i = 0; i < pieData.length; i++) {
    sumValue += pieData[i].value
    const seriesItem: SeriesItem = {
      name: typeof pieData[i].name === 'undefined' ? `series${i}` : pieData[i].name,
      type: 'surface',
      parametric: true,
      wireframe: { show: false },
      pieData: pieData[i],
      pieStatus: {
        selected: !!pieData[i].selected,
        hovered: !!pieData[i].hovered,
        k: k
      }
    }

    if (typeof pieData[i].itemStyle !== 'undefined') {
      const itemStyle: { color?: string; opacity?: number } = {}
      if (typeof pieData[i].itemStyle!.color !== 'undefined') {
        itemStyle.color = pieData[i].itemStyle!.color
      }
      itemStyle.opacity =
        typeof pieData[i].itemStyle!.opacity !== 'undefined' ? pieData[i].itemStyle!.opacity : 0.5
      seriesItem.itemStyle = itemStyle
    }
    series.push(seriesItem)
  }

  for (let i = 0; i < series.length; i++) {
    endValue = startValue + series[i].pieData.value
    series[i].pieData.startRatio = startValue / sumValue
    series[i].pieData.endRatio = endValue / sumValue
    series[i].parametricEquation = getParametricEquation(
      series[i].pieData.startRatio!,
      series[i].pieData.endRatio!,
      series[i].pieStatus.selected,
      series[i].pieStatus.hovered,
      k,
      height ? Number(series[i].pieData.proportion) * height : 1
    )
    startValue = endValue
  }

  return series
}

export function getParametricEquation(
  startRatio: number,
  endRatio: number,
  isSelected: boolean,
  isHovered: boolean,
  k: number,
  height: number
): ParametricEquation {
  const midRatio = (startRatio + endRatio) / 2
  const startRadian = startRatio * Math.PI * 2
  const endRadian = endRatio * Math.PI * 2
  const midRadian = midRatio * Math.PI * 2

  if (startRatio === 0 && endRatio === 1) {
    isSelected = false
  }

  k = typeof k !== 'undefined' ? k : 1 / 3

  const offsetX = isSelected ? Math.cos(midRadian) * 0.2 : 0
  const offsetY = isSelected ? Math.sin(midRadian) * 0.2 : 0
  const hoverRate = isHovered ? 1.05 : 1

  return {
    u: { min: -Math.PI, max: Math.PI * 3, step: Math.PI / 32 },
    v: { min: 0, max: Math.PI * 2, step: Math.PI / 20 },
    x: function (u: number, v: number) {
      if (u < startRadian) {
        return offsetX + Math.cos(startRadian) * (1 + Math.cos(v) * k) * hoverRate
      }
      if (u > endRadian) {
        return offsetX + Math.cos(endRadian) * (1 + Math.cos(v) * k) * hoverRate
      }
      return offsetX + Math.cos(u) * (1 + Math.cos(v) * k) * hoverRate
    },
    y: function (u: number, v: number) {
      if (u < startRadian) {
        return offsetY + Math.sin(startRadian) * (1 + Math.cos(v) * k) * hoverRate
      }
      if (u > endRadian) {
        return offsetY + Math.sin(endRadian) * (1 + Math.cos(v) * k) * hoverRate
      }
      return offsetY + Math.sin(u) * (1 + Math.cos(v) * k) * hoverRate
    },
    z: function (u: number, v: number) {
      if (u < -Math.PI * 0.5) return Math.sin(u)
      if (u > Math.PI * 2.5) return Math.sin(u)
      return Math.sin(v) > 0 ? 1 * height : -1
    }
  }
}
