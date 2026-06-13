/**
 * Tree-shaken ECharts — 按需引入 chart types / components / renderer。
 *
 * 6.1.0 的 ESM 入口在 `echarts/core`，chart 在 `echarts/charts`，
 * component 在 `echarts/components`，renderer 在 `echarts/renderers`。
 *
 * echarts-gl 是副作用导入（`import 'echarts-gl'`），仅在
 * bigscreen/DisasterScreen.vue 中使用，不在本文件引入。
 */

import * as echarts from 'echarts/core'

// --- Charts 按项目实际使用清单 ---
import {BarChart, LineChart, PieChart, RadarChart} from 'echarts/charts'

// --- Components ---
import {
    DataZoomComponent,
    GridComponent,
    LegendComponent,
    MarkPointComponent,
    TitleComponent,
    ToolboxComponent,
    TooltipComponent,
} from 'echarts/components'

// --- Renderer ---
import {CanvasRenderer} from 'echarts/renderers'

echarts.use([
    BarChart,
    LineChart,
    PieChart,
    RadarChart,
    GridComponent,
    TooltipComponent,
    LegendComponent,
    TitleComponent,
    DataZoomComponent,
    MarkPointComponent,
    ToolboxComponent,
    CanvasRenderer,
])

export default echarts
