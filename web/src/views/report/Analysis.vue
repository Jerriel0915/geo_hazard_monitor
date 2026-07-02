<template>
  <div class="analysis-page">
    <!-- Mode Selection -->
    <div v-if="currentMode === ''" class="mode-selection">
      <h2 class="mode-title">数据分析</h2>
      <p class="mode-subtitle">请选择分析模式</p>
      <div class="mode-cards">
        <div class="mode-card" @click="currentMode = 'correlation'">
          <div class="mode-card-icon">📈</div>
          <div class="mode-card-title">关联分析</div>
          <div class="mode-card-desc">将多个传感器属性叠加在同一坐标系中，对比分析不同指标的变化趋势与关联关系</div>
        </div>
        <div class="mode-card" @click="currentMode = 'grid'">
          <div class="mode-card-icon">📊</div>
          <div class="mode-card-title">数据宫格</div>
          <div class="mode-card-desc">以九宫格模式同时查看多个传感器的独立数据图表，快速纵览整体监测态势</div>
        </div>
      </div>
    </div>

    <!-- Correlation Analysis Component -->
    <div v-if="currentMode === 'correlation'" class="analysis-mode-wrapper">
      <CorrelationAnalysis @back="currentMode = ''" />
    </div>

    <!-- Data Grid Component -->
    <div v-if="currentMode === 'grid'" class="analysis-mode-wrapper">
      <DataGrid @back="currentMode = ''" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import CorrelationAnalysis from './components/AnalysisCorrelation.vue'
import DataGrid from './components/AnalysisDataGrid.vue'

const currentMode = ref<string>('') // '' | 'correlation' | 'grid'
</script>

<style scoped>
/* Page shell */
.analysis-page {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

/* Mode selection */
.mode-selection {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}
.analysis-mode-wrapper {
  flex: 1;
  min-height: 0;
}
.mode-title {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}
.mode-subtitle {
  color: #909399;
  margin-bottom: 40px;
  font-size: 14px;
}
.mode-cards {
  display: flex;
  gap: 30px;
}
.mode-card {
  width: 300px;
  padding: 40px 30px;
  border: 2px solid #e8e8e8;
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}
.mode-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 20px rgba(64, 158, 255, 0.15);
  transform: translateY(-4px);
}
.mode-card-icon {
  font-size: 48px;
  margin-bottom: 16px;
}
.mode-card-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 12px;
}
.mode-card-desc {
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
}
</style>