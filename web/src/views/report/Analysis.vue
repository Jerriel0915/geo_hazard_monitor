<template>
  <div class="analysis-page">
    <!-- Mode Selection -->
    <Transition name="mode-fade">
      <div v-if="currentMode === ''" class="mode-selection">
        <div class="mode-header-area">
          <h2 class="mode-title">数据分析</h2>
          <p class="mode-subtitle">选择分析模式，探索监测数据中的规律与趋势</p>
        </div>
        <div class="mode-cards">
          <div class="mode-card" @click="currentMode = 'correlation'">
            <div class="mode-card-icon">
              <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="8" y="44" width="6" height="12" rx="1" fill="#5470c6" opacity="0.6"/>
                <rect x="18" y="32" width="6" height="24" rx="1" fill="#5470c6" opacity="0.75"/>
                <rect x="28" y="20" width="6" height="36" rx="1" fill="#5470c6" opacity="0.9"/>
                <rect x="38" y="28" width="6" height="28" rx="1" fill="#91cc75" opacity="0.75"/>
                <rect x="48" y="12" width="6" height="44" rx="1" fill="#91cc75" opacity="0.9"/>
                <polyline points="11,38 21,26 31,14 41,22 51,6" stroke="#ee6666" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <div class="mode-card-title">关联分析</div>
            <div class="mode-card-desc">将多个传感器属性叠加在同一坐标系中，对比分析不同指标的变化趋势与关联关系</div>
            <div class="mode-card-tag">多维度对比</div>
          </div>
          <div class="mode-card" @click="currentMode = 'grid'">
            <div class="mode-card-icon">
              <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="4" y="4" width="26" height="26" rx="2" fill="#91cc75" opacity="0.5"/>
                <rect x="34" y="4" width="26" height="26" rx="2" fill="#5470c6" opacity="0.4"/>
                <rect x="4" y="34" width="26" height="26" rx="2" fill="#fac858" opacity="0.5"/>
                <rect x="34" y="34" width="26" height="26" rx="2" fill="#ee6666" opacity="0.35"/>
                <line x1="17" y1="10" x2="17" y2="24" stroke="#91cc75" stroke-width="2.5" stroke-linecap="round"/>
                <polyline points="10,20 17,12 24,20" stroke="#91cc75" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                <rect x="37" y="14" width="20" height="10" rx="1" stroke="#5470c6" stroke-width="1.5" fill="none"/>
                <polyline points="10,46 15,40 19,44 24,36" stroke="#fac858" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M38 48 L44 48 L41 42 Z" stroke="#ee6666" stroke-width="1.5" fill="none" stroke-linejoin="round"/>
                <circle cx="52" cy="46" r="6" stroke="#ee6666" stroke-width="1.5" fill="none"/>
              </svg>
            </div>
            <div class="mode-card-title">数据宫格</div>
            <div class="mode-card-desc">以九宫格模式同时查看多个传感器的独立数据图表，快速纵览整体监测态势</div>
            <div class="mode-card-tag">全局纵览</div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Correlation Analysis Component -->
    <Transition name="mode-fade" mode="out-in">
      <div v-if="currentMode === 'correlation'" class="analysis-mode-wrapper" key="correlation">
        <CorrelationAnalysis @back="currentMode = ''" />
      </div>
      <div v-else-if="currentMode === 'grid'" class="analysis-mode-wrapper" key="grid">
        <DataGrid @back="currentMode = ''" />
      </div>
    </Transition>
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
  gap: 32px;
}
.mode-header-area {
  text-align: center;
}
.analysis-mode-wrapper {
  flex: 1;
  min-height: 0;
}
.mode-title {
  font-size: 26px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 10px;
}
.mode-subtitle {
  color: #6b7280;
  font-size: 14px;
  line-height: 1.5;
}
.mode-cards {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  justify-content: center;
}
.mode-card {
  width: 320px;
  padding: 36px 28px 28px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  text-align: center;
  cursor: pointer;
  background: #fff;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}
.mode-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 16px;
  opacity: 0;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.04) 0%, rgba(64, 158, 255, 0.01) 100%);
  transition: opacity 0.3s;
}
.mode-card:hover {
  border-color: #409eff;
  box-shadow: 0 8px 30px rgba(64, 158, 255, 0.12);
  transform: translateY(-4px);
}
.mode-card:hover::before {
  opacity: 1;
}
.mode-card:active {
  transform: translateY(-2px);
}
.mode-card-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
}
.mode-card-icon svg {
  width: 100%;
  height: 100%;
}
.mode-card-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 10px;
}
.mode-card-desc {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.7;
  margin-bottom: 16px;
}
.mode-card-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 100px;
  font-size: 11px;
  color: #409eff;
  background: rgba(64, 158, 255, 0.08);
  font-weight: 500;
}

/* Transition */
.mode-fade-enter-active,
.mode-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.mode-fade-enter-from {
  opacity: 0;
  transform: translateY(12px);
}
.mode-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
