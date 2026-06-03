<template>
  <el-dialog 
    :model-value="modelValue" 
    @update:model-value="emit('update:modelValue', $event)"
    :title="dialogTitle" 
    width="90%" 
    max-width="1000px"
    :close-on-click-modal="false"
  >
    <div class="feedback-container" v-if="data">
      <!-- 头部信息 -->
      <div class="feedback-header">
        <div class="header-left">
          <div class="alarm-device">
            <span class="label">设备：</span>
            <span class="value">{{ data.deviceName || '未知设备' }}</span>
          </div>
          <div class="alarm-location">
            <span class="label">隐患点：</span>
            <span class="value">{{ data.hazardPointName }}</span>
          </div>
        </div>
        <div class="header-center">
          <div class="alarm-level-badge" :class="getAlarmLevelType(data.alarmLevel)">
            {{ getAlarmLevelText(data.alarmLevel) }}级(警报)
          </div>
          <div class="alarm-desc">{{ data.alarmContent }}</div>
        </div>
        <div class="header-right">
          <div class="timer-info">
            <span class="icon"><Clock /></span>
            <span>{{ formatDuration(data.firstAlarmTime) }}</span>
          </div>
          <el-button type="danger" size="small" @click="emit('close-alarm', data)">
            <el-icon><CircleClose /></el-icon>
            销警
          </el-button>
        </div>
      </div>

      <!-- 主体内容 -->
      <div class="feedback-body">
        <!-- 左侧：事件生命周期 -->
        <div class="side-panel left-panel">
          <div class="panel-title">
            <span class="icon-text">🔄</span>
            事件生命周期
          </div>
          <div class="lifecycle">
            <div class="lifecycle-item">
              <div class="lifecycle-node device-node">
                <span class="node-icon">📦</span>
              </div>
              <div class="lifecycle-line"></div>
              <div class="lifecycle-label">设备</div>
            </div>
            <div class="lifecycle-item">
              <div class="lifecycle-node gateway-node">
                <span class="node-icon">🔌</span>
              </div>
              <div class="lifecycle-line"></div>
              <div class="lifecycle-label">接入网关</div>
            </div>
            <div class="lifecycle-item">
              <div class="lifecycle-node storage-node">
                <span class="node-icon">💾</span>
              </div>
              <div class="lifecycle-line"></div>
              <div class="lifecycle-label">数据存储</div>
            </div>
            <div class="lifecycle-item active">
              <div class="lifecycle-node alarm-node">
                <el-icon><Bell /></el-icon>
              </div>
              <div class="lifecycle-line"></div>
              <div class="lifecycle-label">警报级</div>
            </div>
            <div class="lifecycle-item">
              <div class="lifecycle-node situation-node">
                <span class="node-icon">👁️</span>
                <span class="badge">0</span>
              </div>
              <div class="lifecycle-line"></div>
              <div class="lifecycle-label">情况核查</div>
            </div>
            <div class="lifecycle-item">
              <div class="lifecycle-node verify-node">
                <span class="node-icon">✓</span>
                <span class="badge">0</span>
              </div>
              <div class="lifecycle-line"></div>
              <div class="lifecycle-label">核查情况</div>
            </div>
            <div class="lifecycle-item">
              <div class="lifecycle-node close-node">
                <span class="node-icon">✕</span>
              </div>
              <div class="lifecycle-label">关闭事件</div>
            </div>
          </div>
        </div>

        <!-- 中间：告警资料和数据依据 -->
        <div class="main-panel">
          <!-- 告警资料 -->
          <div class="info-section">
            <div class="section-title">
              <span class="icon-text">📄</span>
              告警资料
            </div>
            <div class="info-grid">
              <div class="info-item">
                <label>初次告警</label>
                <span>{{ data.firstAlarmTime }}</span>
              </div>
              <div class="info-item">
                <label>最后告警</label>
                <span>{{ data.lastAlarmTime }}</span>
              </div>
              <div class="info-item">
                <label>最高告警等级</label>
                <span class="level-tag" :class="getAlarmLevelType(data.alarmLevel)">
                  {{ getAlarmLevelText(data.alarmLevel) }}级(警报)
                </span>
              </div>
              <div class="info-item">
                <label>最新告警等级</label>
                <span class="level-tag" :class="getAlarmLevelType(data.alarmLevel)">
                  {{ getAlarmLevelText(data.alarmLevel) }}级(警报)
                </span>
              </div>
              <div class="info-item">
                <label>告警次数</label>
                <span>{{ data.alarmCount }}</span>
              </div>
              <div class="info-item">
                <label>所属项目</label>
                <span>{{ data.projectName || '-' }}</span>
              </div>
              <div class="info-item">
                <label>项目类型</label>
                <span>{{ data.projectType || '自动化监测' }}</span>
              </div>
              <div class="info-item">
                <label>行政区划</label>
                <span>{{ data.district || '-' }}</span>
              </div>
              <div class="info-item">
                <label>监测对象</label>
                <span>{{ data.hazardPointName }}</span>
              </div>
              <div class="info-item">
                <label>监测点</label>
                <span>{{ data.monitorPoint || '-' }}</span>
              </div>
              <div class="info-item">
                <label>告警类型</label>
                <span>{{ getAlarmTypeText(data.alarmType) }}</span>
              </div>
              <div class="info-item">
                <label>设备名称</label>
                <span>{{ data.deviceName || '-' }}</span>
              </div>
            </div>
            <div class="alarm-desc-section">
              <label>告警描述</label>
              <p>{{ data.alarmContent }}</p>
            </div>
          </div>

          <!-- 数据依据 -->
          <div class="data-section">
            <div class="section-title">
              <span class="icon-text">📊</span>
              数据依据
            </div>
            <div class="sensor-selector">
              <el-form :inline="true" :model="sensorForm" size="small">
                <el-form-item label="传感器">
                  <el-select v-model="sensorForm.sensor" placeholder="请选择" style="width: 140px">
                    <el-option label="GNSS-07" value="gnss" />
                    <el-option label="裂缝计-01" value="crack" />
                    <el-option label="雨量计-02" value="rain" />
                  </el-select>
                </el-form-item>
                <el-form-item label="指标">
                  <el-select v-model="sensorForm.index" placeholder="请选择" style="width: 160px">
                    <el-option label="24小时位移速率" value="hour24Speed" />
                    <el-option label="累计位移" value="totalDisplacement" />
                    <el-option label="降雨量" value="rainfall" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="handleSearchData">
                    <el-icon><Search /></el-icon>
                    搜索
                  </el-button>
                  <el-button @click="resetSensorForm">重置</el-button>
                </el-form-item>
              </el-form>
            </div>
            <div class="chart-container">
              <div class="chart-placeholder">
                <span class="chart-icon">📈</span>
                <p>监测数据趋势图</p>
                <p class="hint">时间范围：2025 - 2025</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：时间轴 -->
        <div class="side-panel right-panel">
          <div class="panel-title">
            <span class="icon-text">⏱️</span>
            时间线
          </div>
          <div class="timeline-content">
            <div class="timeline-empty">
              <span class="empty-icon">📋</span>
              <p>暂无时间线数据</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部操作面板 -->
      <div class="feedback-footer">
        <div class="response-info">
          <span class="label">响应人员：</span>
          <span class="value">{{ data.responderName || '未响应' }}</span>
          <span class="label ml-4">响应时间：</span>
          <span class="value">{{ data.responseTime || '-' }}</span>
        </div>
        <div class="action-buttons">
          <el-button type="warning" size="small" @click="emit('false-alarm', data)">
            <span class="btn-icon">⚠️</span>
            标记误报
          </el-button>
          <el-button type="danger" size="small" @click="emit('close-alarm', data)">
            <el-icon><CircleClose /></el-icon>
            销警
          </el-button>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button size="small" @click="emit('update:modelValue', false); emit('close')">关闭</el-button>
      <el-button type="primary" size="small" @click="emit('submit')">提交反馈</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Clock, Bell, CircleClose, Search } from '@element-plus/icons-vue'

const props = defineProps<{
  modelValue: boolean
  data: any
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'close'): void
  (e: 'submit'): void
  (e: 'false-alarm', data: any): void
  (e: 'close-alarm', data: any): void
}>()

// 传感器表单
const sensorForm = reactive({
  sensor: '',
  index: ''
})

// 弹窗标题
const dialogTitle = computed(() => {
  if (!props.data) return '告警反馈'
  return `${props.data.hazardPointName}[${props.data.firstAlarmTime}]`
})

// 格式化持续时间
const formatDuration = (startTime: string) => {
  if (!startTime) return '0小时0分0秒'
  const start = new Date(startTime).getTime()
  const now = new Date().getTime()
  const diff = now - start
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  const seconds = Math.floor((diff % (1000 * 60)) / 1000)
  return `${hours}小时${minutes}分${seconds}秒`
}

// 获取告警等级类型
const getAlarmLevelType = (level: string) => {
  const map: Record<string, string> = {
    '1': 'danger',
    '2': 'warning',
    '3': 'success',
    '4': 'info'
  }
  return map[level] || 'info'
}

// 获取告警等级文本
const getAlarmLevelText = (level: string) => {
  const map: Record<string, string> = {
    '1': '一级',
    '2': '二级',
    '3': '三级',
    '4': '四级'
  }
  return map[level] || level
}

// 获取告警类型文本
const getAlarmTypeText = (type: string) => {
  const map: Record<string, string> = {
    'threshold': '阈值预警',
    'comprehensive': '综合预警'
  }
  return map[type] || type
}

// 搜索数据
const handleSearchData = () => {
  ElMessage.info('搜索功能已触发')
}

// 重置传感器表单
const resetSensorForm = () => {
  sensorForm.sensor = ''
  sensorForm.index = ''
}
</script>

<style scoped>
.feedback-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.feedback-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-left .label {
  color: #909399;
  font-size: 11px;
}

.header-left .value {
  font-weight: 500;
  font-size: 13px;
  color: #303133;
}

.header-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.alarm-level-badge {
  padding: 4px 16px;
  border-radius: 12px;
  font-weight: bold;
  font-size: 13px;
  color: #fff;
}

.alarm-level-badge.danger {
  background: #f56c6c;
}

.alarm-level-badge.warning {
  background: #e6a23c;
}

.alarm-level-badge.success {
  background: #67c23a;
}

.alarm-level-badge.info {
  background: #409eff;
}

.alarm-desc {
  font-size: 12px;
  color: #606266;
  max-width: 350px;
  text-align: center;
}

.header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.timer-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.timer-info .icon {
  font-size: 14px;
}

.feedback-body {
  display: flex;
  gap: 10px;
}

.side-panel {
  width: 160px;
  background: #fafafa;
  border-radius: 6px;
  padding: 10px;
  border: 1px solid #ebeef5;
}

.main-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid #ebeef5;
  font-size: 13px;
}

.panel-title .icon-text {
  font-size: 14px;
}

/* 事件生命周期 */
.lifecycle {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.lifecycle-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 0;
}

.lifecycle-item.active .lifecycle-node {
  transform: scale(1.1);
  box-shadow: 0 0 12px rgba(64, 158, 255, 0.4);
}

.lifecycle-node {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  position: relative;
  transition: all 0.3s ease;
}

.lifecycle-node .node-icon {
  font-size: 14px;
}

.lifecycle-node .badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #67c23a;
  color: #fff;
  font-size: 9px;
  padding: 1px 4px;
  border-radius: 8px;
}

.device-node {
  background: #909399;
}

.gateway-node {
  background: #409eff;
}

.storage-node {
  background: #409eff;
}

.alarm-node {
  background: #f56c6c;
}

.situation-node {
  background: #909399;
}

.verify-node {
  background: #909399;
}

.close-node {
  background: #909399;
}

.lifecycle-line {
  width: 2px;
  height: 16px;
  background: #dcdfe6;
  margin: 2px 0;
}

.lifecycle-label {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
  text-align: center;
}

/* 告警资料 */
.info-section {
  background: #fff;
  border-radius: 6px;
  padding: 10px;
  border: 1px solid #ebeef5;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 10px;
  font-size: 13px;
}

.section-title .icon-text {
  font-size: 14px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-item label {
  font-size: 11px;
  color: #909399;
}

.info-item span {
  font-size: 12px;
  color: #303133;
}

.level-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 500;
  color: #fff;
}

.level-tag.danger {
  background: #f56c6c;
}

.level-tag.warning {
  background: #e6a23c;
}

.level-tag.success {
  background: #67c23a;
}

.level-tag.info {
  background: #409eff;
}

.alarm-desc-section {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #ebeef5;
}

.alarm-desc-section label {
  display: block;
  font-size: 11px;
  color: #909399;
  margin-bottom: 4px;
}

.alarm-desc-section p {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
  margin: 0;
}

/* 数据依据 */
.data-section {
  background: #fff;
  border-radius: 6px;
  padding: 10px;
  border: 1px solid #ebeef5;
}

.sensor-selector {
  margin-bottom: 10px;
}

.chart-container {
  height: 200px;
  background: #fafafa;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chart-placeholder {
  text-align: center;
  color: #909399;
}

.chart-placeholder .chart-icon {
  font-size: 36px;
  margin-bottom: 8px;
}

.chart-placeholder p {
  margin: 0 0 4px 0;
  font-size: 12px;
}

.chart-placeholder .hint {
  font-size: 11px;
  color: #c0c4cc;
}

/* 右侧面板 */
.right-panel {
  width: 180px;
}

.timeline-content {
  height: 120px;
  background: #fff;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #ebeef5;
}

.timeline-empty {
  text-align: center;
  color: #909399;
}

.timeline-empty .empty-icon {
  font-size: 24px;
  margin-bottom: 6px;
}

.timeline-empty p {
  margin: 0;
  font-size: 11px;
}

/* 底部操作面板 */
.feedback-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}

.response-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.response-info .label {
  color: #909399;
  font-size: 12px;
}

.response-info .value {
  color: #303133;
  font-weight: 500;
  font-size: 12px;
}

.response-info .ml-4 {
  margin-left: 12px;
}

.feedback-footer .action-buttons {
  display: flex;
  gap: 6px;
}

.btn-icon {
  margin-right: 4px;
}

/* 响应式 */
@media (max-width: 1024px) {
  .info-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .side-panel {
    width: 120px;
  }
  
  .right-panel {
    width: 140px;
  }
}
</style>