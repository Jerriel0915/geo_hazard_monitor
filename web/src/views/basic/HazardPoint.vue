<template>
  <div class="hazard-point-page">
    <div class="page-container">
      <div class="group-panel" :style="{ width: groupPanelWidth + 'px' }">
        <div class="panel-header">
          <span class="panel-title">分组列表</span>
          <div class="panel-actions">
            <el-button size="mini" @click="handleAddGroup">+ 新增</el-button>
          </div>
        </div>
        <div class="group-list" @scroll="handleGroupListScroll">
          <div
              v-for="group in displayGroupList"
              :key="group.id"
              :class="['group-item', { active: selectedGroupId === group.id }]"
              @click="handleSelectGroup(group)"
          >
            <span class="group-name">{{ group.name }}</span>
            <span class="group-count">({{ group.count }})</span>
            <div class="group-actions">
              <span class="action-btn" @click.stop="handleEditGroup(group)">✎</span>
              <span class="action-btn delete-btn" @click.stop="handleDeleteGroup(group)">✕</span>
            </div>
          </div>
          <div v-if="loadingGroups" class="loading-more">加载中...</div>
        </div>
      </div>

      <div class="resize-handle" @mousedown="startResize"></div>

      <div class="content-panel">
        <div class="page-header">
          <div class="header-left">
            <h2 class="page-title">隐患点管理</h2>
          </div>
          <div class="header-right">
            <el-button type="primary" @click="handleAdd">+ 新增</el-button>
            <el-button type="danger" @click="handleBatchDelete" :disabled="selectedRows.length === 0" plain>批量删除</el-button>
            <el-button @click="handleBatchPause" :disabled="selectedRows.length === 0" plain>停测</el-button>
            <el-button @click="handleBatchResume" :disabled="selectedRows.length === 0" plain>恢复</el-button>
            <el-button @click="handleBatchComplete" :disabled="selectedRows.length === 0" type="warning" plain>完结</el-button>
            <el-button @click="handleExportHazardPoints" plain>
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" style="margin-right:4px;vertical-align:-2px"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>导出
            </el-button>
          </div>
        </div>

        <div class="stats-bar">
          <span class="stat-item">隐患点 <strong>{{ statsTotal }}</strong></span>
          <span class="stat-sep">|</span>
          <span class="stat-item">监测中 <strong class="c-green">{{ statsMonitoring }}</strong></span>
          <span class="stat-sep">|</span>
          <span class="stat-item">关联设备 <strong class="c-amber">{{ statsDeviceTotal }}</strong></span>
          <span class="stat-sep">|</span>
          <span class="stat-item">分组 <strong class="c-purple">{{ statsGroupCount }}</strong></span>
        </div>

        <div class="search-bar">
  <el-select v-model="searchType" placeholder="搜索方式" class="search-type-select">
    <el-option label="按名称" value="name" />
    <el-option label="按编号" value="code" />
  </el-select>
  <el-input
    v-model="searchKeyword"
    :placeholder="searchType === 'name' ? '搜索名称' : '搜索编号'"
    class="search-input"
    clearable
    @clear="handleSearch"
    @keyup.enter="handleSearch"
  >
    <template #prefix>
      <span class="search-icon">🔍</span>
    </template>
  </el-input>
  <el-select v-model="searchStatus" placeholder="状态" clearable class="status-select">
    <el-option label="监测中" value="MONITORING" />
    <el-option label="停测中" value="PAUSED" />
    <el-option label="已完结" value="COMPLETED" />
  </el-select>
  <el-button type="primary" @click="handleSearch">搜索</el-button>
  <el-button @click="handleReset">重置</el-button>
  <el-button @click="handleRefresh" :loading="refreshing">刷新</el-button>
</div>

        <div class="table-container">
          <div class="table-scroll">
            <el-table
              :data="tableData"
              border
              stripe
              v-loading="loading"
              :header-cell-style="{ background: '#f8fafc', color: '#475569', fontWeight: 600 }"
              @selection-change="handleSelectionChange"
            >
            <el-table-column type="selection" width="55" align="center" />
            <el-table-column prop="code" label="编号" width="150" align="center" />
            <el-table-column prop="name" label="名称" min-width="200" align="center" />
            <el-table-column prop="statusName" label="状态" width="100" align="center">
              <template #default="{ row }">
                <span :class="['status-badge', `status-${row.status.toLowerCase()}`]">
                  {{ row.statusName }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="groupName" label="分组" width="120" align="center">
              <template #default="{ row }">
                <span>{{ row.groupName || '未分组' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="coordinates" label="中心坐标" width="180" align="center">
              <template #default="{ row }">
                <span v-if="row.longitude && row.latitude">{{ row.longitude }}, {{ row.latitude }}</span>
                <span v-else class="empty-text">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="deviceCount" label="设备数量" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="info" effect="plain">{{ row.deviceCount || 0 }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right" align="center">
              <template #default="{ row }">
                <div class="op-cell">
                  <el-button type="primary" text size="small" @click="handleView(row)">查看</el-button>
                  <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
                  <el-dropdown trigger="hover" @command="(cmd: string) => handleMoreCommand(cmd, row)">
                    <el-button type="primary" text size="small">更多</el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item v-if="row.status !== 'COMPLETED'" command="togglePause">
                          {{ row.status === 'PAUSED' ? '恢复' : '停测' }}
                        </el-dropdown-item>
                        <el-dropdown-item v-if="row.status !== 'COMPLETED'" command="complete">完结</el-dropdown-item>
                        <el-dropdown-item command="bindDevice">绑定设备</el-dropdown-item>
                        <el-dropdown-item command="alarmConfig">告警配置</el-dropdown-item>
                        <el-dropdown-item command="delete" divided>
                          <span style="color: #f56c6c">删除</span>
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </template>
            </el-table-column>
          </el-table>
          </div>

          <div class="pagination-container">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="total"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              prev-text="上一页"
              next-text="下一页"
              :disabled="total === 0"
              @size-change="handleSizeChange"
              @current-change="handlePageChange"
            />
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="groupDialogVisible"
      :title="groupDialogTitle"
      width="500px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="groupFormRef" :model="groupFormData" :rules="groupFormRules" label-width="100px">
        <el-form-item label="分组名称" prop="name">
          <el-input v-model="groupFormData.name" placeholder="请输入分组名称" />
        </el-form-item>
        <el-form-item label="分组描述" prop="description">
          <el-input v-model="groupFormData.description" type="textarea" :rows="3" placeholder="请输入分组描述（可选）" />
        </el-form-item>
        <el-form-item label="排序序号" prop="sortOrder">
          <el-input-number v-model="groupFormData.sortOrder" :min="0" :max="999" placeholder="排序序号" />
          <span class="form-hint">数字越小越靠前</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGroupSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="编号" prop="code">
              <el-input v-model="formData.code" placeholder="请输入隐患点编号" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="名称" prop="name">
              <el-input v-model="formData.name" placeholder="请输入隐患点名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="分组" prop="groupId">
              <div style="display: flex; align-items: center; width: 100%; gap: 5px;">
                <el-select v-model="formData.groupId" placeholder="未分组" style="width: 85%;">
                  <el-option v-for="g in groupOptions" :key="g.id" :value="g.id" :label="g.name">
                    <div style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
                      <span>{{ g.name }}</span>
                      <span v-if="g.id !== '1'" style="display: flex; gap: 5px;">
                        <span class="group-action-btn" @click.stop="handleEditGroupFromSelect(g)" title="修改">✎</span>
                        <span class="group-action-btn delete-btn" @click.stop="handleDeleteGroupFromSelect(g)" title="删除">×</span>
                      </span>
                    </div>
                  </el-option>
                </el-select>
                <el-button type="primary" size="small" @click="handleAddGroupFromSelect" title="新增分组">+</el-button>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="走向" prop="strike">
              <el-input-number v-model="formData.strike" :min="0" :max="360" placeholder="走向角度(度)" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="中心坐标" prop="coordinates">
          <div class="coordinate-input">
            <el-input-number v-model="formData.longitude" :precision="6" :step="0.000001" placeholder="经度" class="coord-input" />
            <span class="coord-separator">,</span>
            <el-input-number v-model="formData.latitude" :precision="6" :step="0.000001" placeholder="纬度" class="coord-input" />
            <el-button type="primary" size="small" @click="handleOpenMap">
              <span>📍</span> 地图设置
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="mapDialogVisible"
      title="绘制隐患点范围"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="map-container">
        <div id="hazard-point-map" ref="mapRef" style="width: 100%; height: 400px;"></div>
      </div>
      <div class="map-actions">
        <el-button-group>
          <el-button type="primary" size="small" @click="setDrawMode('point')">📍 设置中心点</el-button>
          <el-button size="small" @click="setDrawMode('polygon')">✏️ 绘制范围</el-button>
          <el-button size="small" @click="setDrawMode('strike')">➡️ 绘制走向</el-button>
          <el-button size="small" @click="clearDraw">🗑 清除</el-button>
        </el-button-group>
      </div>
      <div class="map-info">
        <div>中心坐标: {{ formData.longitude.toFixed(6) }}, {{ formData.latitude.toFixed(6) }}</div>
        <div v-if="polygonCoords.length > 0">范围顶点数: {{ polygonCoords.length }}</div>
        <div v-if="strikeCoords.length >= 2">走向角度: {{ strikeAngle }}°</div>
      </div>
      <template #footer>
        <el-button @click="mapDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleMapConfirm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detailDialogVisible"
      title="隐患点详情"
      width="1000px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="basic">
          <div class="basic-info-container">
            <div class="info-section">
              <h3 class="section-title">隐患点信息</h3>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="隐患点编号">{{ currentRow?.code }}</el-descriptions-item>
                <el-descriptions-item label="隐患点名称">{{ currentRow?.name }}</el-descriptions-item>
                <el-descriptions-item label="分组">{{ currentRow?.groupName || '未分组' }}</el-descriptions-item>
                <el-descriptions-item label="状态">
                  <el-tag :type="getStatusType(currentRow?.status || '')" size="small">{{ currentRow?.statusName }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="中心坐标" :span="2">
                  {{ currentRow?.longitude?.toFixed(6) }}, {{ currentRow?.latitude?.toFixed(6) }}
                </el-descriptions-item>
                <el-descriptions-item label="走向">{{ currentRow?.strike }}°</el-descriptions-item>
                <el-descriptions-item label="描述" :span="2">{{ currentRow?.description || '-' }}</el-descriptions-item>
              </el-descriptions>
            </div>

            <div class="map-section">
              <h3 class="section-title">隐患点区域展示</h3>
              <div id="detail-map" ref="detailMapRef" style="width: 100%; height: 300px;"></div>
            </div>

            <div class="system-info-section">
              <h3 class="section-title">系统信息</h3>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="创建人">{{ currentRow?.createBy || '-' }}</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ currentRow?.createTime || '-' }}</el-descriptions-item>
                <el-descriptions-item label="更新人">{{ currentRow?.updateBy || '-' }}</el-descriptions-item>
                <el-descriptions-item label="更新时间">{{ currentRow?.updateTime || '-' }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="绑定设备" name="devices">
          <el-table :data="boundDevices" border size="small">
            <el-table-column prop="deviceCode" label="设备编号" width="150" align="center" />
            <el-table-column prop="deviceName" label="设备名称" min-width="150" align="center" />
            <el-table-column prop="sensorNames" label="传感器" min-width="150" align="center">
              <template #default="{ row }">
                <span v-for="sensor in row.sensors" :key="sensor.id" class="sensor-tag">
                  {{ sensor.name }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="bindTime" label="绑定时间" width="180" align="center" />
            <el-table-column prop="deviceStatus" label="设备状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.deviceStatus === 'NORMAL' ? 'success' : row.deviceStatus === 'FAULT' ? 'danger' : 'warning'" size="small">
                  {{ row.deviceStatus === 'NORMAL' ? '正常' : row.deviceStatus === 'FAULT' ? '故障' : '离线' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="告警配置" name="alarmConfig">
          <div class="alarm-config-view">
            <div class="config-section">
              <h3 class="section-title">告警判据</h3>
              <el-table :data="alarmCriteriaList" border size="small">
                <el-table-column prop="name" label="判据名称" width="150" align="center" />
                <el-table-column prop="monitorTypeName" label="监测类型" width="150" align="center" />
                <el-table-column prop="expression" label="表达式" width="250" align="center" />
                <el-table-column prop="alarmLevel" label="告警等级" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag :type="getAlarmLevelType(row.alarmLevel)" size="small">{{ row.alarmLevelText }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="isEnabled" label="状态" width="80" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.isEnabled ? 'success' : 'info'" size="small">{{ row.isEnabled ? '启用' : '禁用' }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <div class="config-section">
              <h3 class="section-title">告警分发</h3>
              <el-table :data="dispatchRules" border size="small">
                <el-table-column prop="name" label="规则名称" width="150" align="center" />
                <el-table-column prop="type" label="类型" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.type === 'ALARM' ? 'warning' : 'info'" size="small">
                      {{ row.type === 'ALARM' ? '告警分发' : '状态通知' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="alarmLevel" label="告警等级" width="120" align="center">
                  <template #default="{ row }">
                    <span v-if="row.type === 'ALARM'">{{ row.alarmLevel }}</span>
                    <span v-else class="empty-text">-</span>
                  </template>
                </el-table-column>
                <el-table-column prop="recipientName" label="接收人" width="120" align="center" />
                <el-table-column prop="channel" label="通知渠道" width="150" align="center">
                  <template #default="{ row }">
                    <span v-for="ch in row.channel.split(',')" :key="ch" class="channel-tag">{{ getChannelLabel(ch) }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="isEnabled" label="状态" width="80" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.isEnabled ? 'success' : 'info'" size="small">{{ row.isEnabled ? '启用' : '禁用' }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="监测数据" name="monitorData">
          <div class="monitor-data-panel">
            <div class="data-filters">
              <el-select v-model="dataFilter.deviceId" placeholder="选择设备" clearable style="width: 150px"
                         @change="onDataDeviceChange">
                <el-option v-for="d in boundDevices" :key="d.deviceId" :label="d.deviceName" :value="d.deviceId" />
              </el-select>
              <el-select v-model="dataFilter.sensorId" placeholder="选择传感器" clearable style="width: 150px"
                         @change="onDataSensorChange">
                <el-option v-for="s in monitorSensors" :key="s.id" :label="s.name" :value="s.id"/>
              </el-select>
              <el-select v-model="dataFilter.attrCode" placeholder="选择指标" clearable style="width: 160px">
                <el-option v-for="a in monitorAttrs" :key="a.code" :label="a.label" :value="a.code"/>
              </el-select>
              <el-select v-model="dataFilter.valueType" placeholder="聚合粒度" style="width: 120px">
                <el-option label="原始值" value="current" />
                <el-option label="小时均值" value="hour" />
                <el-option label="日均值" value="24h" />
                <el-option label="3日均值" value="72h" />
              </el-select>
              <el-date-picker
                v-model="dataFilter.timeRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始"
                end-placeholder="结束"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 360px"
              />
              <el-button type="primary" size="small" @click="handleQueryData">查询</el-button>
            </div>

            <div class="data-toolbar">
              <el-button-group>
                <el-button :type="dataDisplayMode === 'chart' ? 'primary' : 'default'" size="small" @click="dataDisplayMode = 'chart'">图表展示</el-button>
                <el-button :type="dataDisplayMode === 'table' ? 'primary' : 'default'" size="small" @click="dataDisplayMode = 'table'">表格展示</el-button>
              </el-button-group>
              <div class="data-actions">
                <el-button size="small" @click="handleImportData">导入数据</el-button>
                <el-button size="small" @click="handleExportData">导出数据</el-button>
              </div>
            </div>

            <div class="data-content">
              <div v-if="dataDisplayMode === 'chart'" class="chart-container">
                <VueApexCharts
                  v-if="chartSeriesData.length > 0"
                  type="area"
                  height="100%"
                  :options="chartOptions"
                  :series="chartOptions.series"
                />
                <div v-if="chartSeriesData.length === 0" class="chart-empty-tip">暂无数据，选择条件后将自动加载近3天数据</div>
              </div>
              <div v-else class="table-container">
                <el-table :data="monitorDataList" border size="small">
                  <el-table-column prop="dataTime" label="时间" width="180" align="center" />
                  <el-table-column prop="deviceName" label="设备" width="150" align="center" />
                  <el-table-column prop="sensorName" label="传感器" width="120" align="center" />
                  <el-table-column prop="attrName" label="指标" width="100" align="center"/>
                  <el-table-column prop="value" label="数值" width="100" align="center" />
                  <el-table-column prop="unit" label="单位" width="80" align="center" />
                  <el-table-column prop="qualityText" label="质量" width="80" align="center" />
                </el-table>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="alarmConfigDialogVisible"
      :title="`告警配置[${currentRow?.name || ''}]`"
      width="1000px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="alarm-config-content">
        <div class="config-section">
          <h3 class="section-title">告警判据</h3>
          <div class="alarm-toolbar">
            <el-button type="primary" size="small" @click="handleAddAlarmCriteria">
              <span class="btn-icon">+</span> 添加判据
            </el-button>
          </div>
          <el-table :data="alarmCriteriaList" border size="small">
            <el-table-column prop="name" label="判据名称" width="150" align="center" />
            <el-table-column prop="monitorTypeName" label="监测类型" width="150" align="center" />
            <el-table-column prop="expression" label="表达式" width="250" align="center" />
            <el-table-column prop="alarmLevel" label="告警等级" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getAlarmLevelType(row.alarmLevel)" size="small">{{ row.alarmLevelText }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="isEnabled" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.isEnabled" @change="handleToggleAlarm(row)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-button type="text" size="small" @click="handleEditAlarm(row)">编辑</el-button>
                <el-button type="text" size="small" class="danger-text" @click="handleDeleteAlarm(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="config-section">
          <h3 class="section-title">告警分发</h3>
          <div class="dispatch-toolbar">
            <el-button type="primary" size="small" @click="handleAddDispatchRule">
              <span class="btn-icon">+</span> 添加规则
            </el-button>
          </div>
          <el-table :data="dispatchRules" border size="small">
            <el-table-column prop="type" label="类型" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="row.type === 'alarm' ? 'danger' : 'warning'" size="small">
                  {{ row.type === 'alarm' ? '监测告警' : '设备离线通知' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="告警等级/关联设备" min-width="200">
              <template #default="{ row }">
                <template v-if="row.type === 'alarm'">
                  <el-tag v-for="(lvl, idx) in row.level" :key="idx" :type="getAlarmLevelType(lvl)" size="small" style="margin-right: 4px;">{{ lvl }}</el-tag>
                </template>
                <template v-else-if="row.type === 'offline' && row.deviceNames && row.deviceNames.length > 0">
                  <el-tag v-for="(name, idx) in row.deviceNames" :key="idx" size="small" style="margin-right: 4px;">{{ name }}</el-tag>
                </template>
                <span v-else class="empty-text">-</span>
              </template>
            </el-table-column>
            <el-table-column label="通知人员" min-width="150">
              <template #default="{ row }">
                <el-tag v-for="p in row.persons" :key="p" size="small" style="margin-right: 4px;">{{ p }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="channels" label="通知渠道" width="150">
              <template #default="{ row }">
                <span v-for="(c, idx) in row.channels" :key="c">
                  {{ getChannelLabel(c) }}{{ idx < row.channels.length - 1 ? '、' : '' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.status" @change="handleToggleDispatchStatus(row)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-button type="text" size="small" @click="handleEditDispatchRule(row)">编辑</el-button>
                <el-button type="text" size="small" class="danger-text" @click="handleDeleteDispatchRule(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <template #footer>
        <el-button @click="alarmConfigDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="bindDeviceDialogVisible"
      :title="`绑定设备[${currentRow?.name || ''}]`"
      width="900px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="transfer-container">
        <div class="transfer-panel">
          <div class="panel-header">
            <span class="panel-title">待绑定设备</span>
            <div style="display: flex; gap: 8px;">
              <el-input
                  v-model="leftSearchText"
                  placeholder="搜索设备/传感器名称"
                  class="search-input"
                  clearable
                  size="small"
                  @clear="handleSearchUnboundDevices"
                  @keyup.enter="handleSearchUnboundDevices"
              />
              <el-button size="small" @click="handleSearchUnboundDevices">搜索</el-button>
            </div>
          </div>
          <div class="transfer-tree">
            <el-tree
              :data="leftDeviceTree"
              :props="{ label: 'label', children: 'children' }"
              show-checkbox
              node-key="id"
              :default-checked-keys="selectedLeftKeys"
              @check="handleLeftCheck"
              :filter-node-method="filterLeftNode"
            >
              <template #default="{ node, data }">
                <span class="tree-node">
                  <img v-if="data.icon" :src="data.icon" class="node-icon" />
                  <span>{{ node.label }}</span>
                  <span v-if="data.bindCount !== undefined" class="bind-count">({{ data.bindCount }})</span>
                  <el-tag v-if="data.status" :type="getStatusTagType(data.status)" size="mini" class="status-tag">{{ data.statusText }}</el-tag>
                </span>
              </template>
            </el-tree>
          </div>
        </div>

        <div class="transfer-actions">
          <el-button type="primary" size="small" @click="transferToRight">
            <span class="arrow-icon">→</span>
          </el-button>
          <el-button type="primary" size="small" @click="transferAllToRight">
            <span class="arrow-icon">⇒</span>
          </el-button>
          <el-button size="small" @click="transferToLeft">
            <span class="arrow-icon">←</span>
          </el-button>
          <el-button size="small" @click="transferAllToLeft">
            <span class="arrow-icon">⇐</span>
          </el-button>
        </div>

        <div class="transfer-panel">
          <div class="panel-header">
            <span class="panel-title">已绑定设备</span>
            <el-input
              v-model="rightSearchText"
              placeholder="搜索设备/传感器名称"
              class="search-input"
              clearable
              size="small"
            />
          </div>
          <div class="transfer-tree">
            <el-tree
              :data="rightDeviceTree"
              :props="{ label: 'label', children: 'children' }"
              show-checkbox
              node-key="id"
              :default-checked-keys="selectedRightKeys"
              @check="handleRightCheck"
              :filter-node-method="filterRightNode"
            >
              <template #default="{ node, data }">
                <span class="tree-node">
                  <img v-if="data.icon" :src="data.icon" class="node-icon" />
                  <span>{{ node.label }}</span>
                  <span v-if="data.bindCount !== undefined" class="bind-count">({{ data.bindCount }})</span>
                  <el-tag v-if="data.status" :type="getStatusTagType(data.status)" size="mini" class="status-tag">{{ data.statusText }}</el-tag>
                </span>
              </template>
            </el-tree>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="bindDeviceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBindDeviceSubmit" :loading="bindLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="alarmDialogVisible"
      :title="`${isEditAlarm ? '编辑告警判据' : '添加告警判据'}[${currentRow?.name || ''}]`"
      width="700px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="alarmFormRef" :model="alarmFormData" :rules="alarmFormRules" label-width="120px">
        <el-form-item label="判据名称" prop="name">
          <el-input v-model="alarmFormData.name" placeholder="请输入判据名称" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="设备" prop="deviceId">
              <el-select v-model="alarmFormData.deviceId" placeholder="请选择设备" @change="handleAlarmDeviceChange">
                <el-option v-for="d in boundDevices" :key="d.deviceId" :label="d.deviceName" :value="d.deviceId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="监测类型" prop="monitorTypeId">
              <el-select v-model="alarmFormData.monitorTypeId" placeholder="请选择监测类型" @change="handleMonitorTypeChange">
                <el-option v-for="mt in monitorTypeList" :key="mt.id" :label="mt.name" :value="mt.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="监测内容" prop="monitorContentCode">
              <el-select v-model="alarmFormData.monitorContentCode" placeholder="请选择监测内容" @change="handleMonitorContentChange">
                <el-option v-for="mc in filteredMonitorContent" :key="mc.value" :label="mc.label" :value="mc.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="指标单位" prop="unit">
              <el-input v-model="alarmFormData.unit" placeholder="自动填充" :disabled="true" />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="expression-builder">
          <div class="expression-section">
            <span class="section-title">蓝色预警</span>
            <div class="expression-row">
              <el-input v-model="alarmFormData.blueExpression" placeholder="请输入判断表达式" class="expr-input" />
              <el-input v-model="alarmFormData.blueDescription" placeholder="描述" class="desc-input" />
            </div>
          </div>
          <div class="expression-section">
            <span class="section-title">黄色预警</span>
            <div class="expression-row">
              <el-input v-model="alarmFormData.yellowExpression" placeholder="请输入判断表达式" class="expr-input" />
              <el-input v-model="alarmFormData.yellowDescription" placeholder="描述" class="desc-input" />
            </div>
          </div>
          <div class="expression-section">
            <span class="section-title">橙色预警</span>
            <div class="expression-row">
              <el-input v-model="alarmFormData.orangeExpression" placeholder="请输入判断表达式" class="expr-input" />
              <el-input v-model="alarmFormData.orangeDescription" placeholder="描述" class="desc-input" />
            </div>
          </div>
          <div class="expression-section">
            <span class="section-title">红色预警</span>
            <div class="expression-row">
              <el-input v-model="alarmFormData.redExpression" placeholder="请输入判断表达式" class="expr-input" />
              <el-input v-model="alarmFormData.redDescription" placeholder="描述" class="desc-input" />
            </div>
          </div>
        </div>
        <div class="expression-toolbar">
          <el-button-group size="small">
            <el-button @click="insertExpression('value')">值(value)</el-button>
            <el-button @click="insertExpression('>')">&gt;</el-button>
            <el-button @click="insertExpression('<')">&lt;</el-button>
            <el-button @click="insertExpression('>=')">&gt;=</el-button>
            <el-button @click="insertExpression('<=')">&lt;=</el-button>
            <el-button @click="insertExpression('==')">==</el-button>
            <el-button @click="insertExpression('&&')">&&</el-button>
            <el-button @click="insertExpression('||')">||</el-button>
            <el-button @click="insertExpression('(')">(</el-button>
            <el-button @click="insertExpression(')')">)</el-button>
          </el-button-group>
          <div class="expression-tips">
            <span>可用变量: value(当前值), hourChange(小时变化), dayChange(日变化), 支持数学运算和逻辑判断</span>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="alarmDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAlarmSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="dispatchDialogVisible"
      :title="`${isEditDispatch ? '编辑告警分发规则' : '添加告警分发规则'}[${currentRow?.name || ''}]`"
      width="550px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="dispatchFormRef" :model="dispatchFormData" :rules="dispatchFormRules" label-width="100px">
        <el-form-item label="隐患点">
          <el-select v-model="dispatchFormData.hazardPointId" disabled style="width: 100%">
            <el-option :value="currentRow?.id" :label="currentRow?.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="dispatchFormData.type">
            <el-radio label="alarm">监测告警</el-radio>
            <el-radio label="offline">设备离线通知</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="告警等级" prop="level" v-if="dispatchFormData.type === 'alarm'">
          <el-select v-model="dispatchFormData.level" multiple placeholder="请选择告警等级（支持多选）" style="width: 100%">
            <el-option label="四级(注意)" value="四级(注意)" />
            <el-option label="三级(警示)" value="三级(警示)" />
            <el-option label="二级(警戒)" value="二级(警戒)" />
            <el-option label="一级(警报)" value="一级(警报)" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联设备" prop="deviceIds" v-if="dispatchFormData.type === 'offline'">
          <el-select v-model="dispatchFormData.deviceIds" multiple placeholder="请选择设备（支持多选）" style="width: 100%">
            <el-option
              v-for="d in boundDevices"
              :key="d.deviceId"
              :label="`${d.deviceCode} - ${d.deviceName}`"
              :value="d.deviceId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="执行时间" v-if="dispatchFormData.type === 'offline'">
          <el-radio-group v-model="dispatchFormData.execType" class="exec-type-group">
            <el-radio label="realtime">实时执行</el-radio>
            <el-radio label="timed">定时</el-radio>
          </el-radio-group>
          <div v-if="dispatchFormData.execType === 'timed'" class="exec-time-config">
            <span class="exec-label">每</span>
            <el-input-number v-model="dispatchFormData.execFrequencyNum" :min="1" :max="99" style="width: 80px" />
            <el-select v-model="dispatchFormData.execFrequencyUnit" style="width: 100px">
              <el-option label="分钟" value="minute" />
              <el-option label="小时" value="hour" />
              <el-option label="天" value="day" />
              <el-option label="周" value="week" />
              <el-option label="月" value="month" />
              <el-option label="年" value="year" />
            </el-select>
            <span class="exec-label">在</span>
            <el-input v-model="dispatchFormData.execTimePoints" placeholder="多个时间点用逗号隔开" style="width: 150px" />
            <span class="exec-label">执行</span>
            <span class="form-hint">时间点示例：分钟填秒数(10,20)，小时填分钟数(10,50)，天填小时数(8,10)，周填星期(1-7)，月填日期(1,16)，年填天数(1,36)</span>
          </div>
        </el-form-item>
        <el-form-item label="通知人员" prop="persons">
          <el-select v-model="dispatchFormData.persons" multiple placeholder="请选择通知人员" style="width: 100%">
            <el-option v-for="u in userList" :key="u.id" :label="u.name" :value="u.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="通知渠道" prop="channels">
          <el-checkbox-group v-model="dispatchFormData.channels">
            <el-checkbox label="sms" border>短信</el-checkbox>
            <el-checkbox label="email" border>邮件</el-checkbox>
            <el-checkbox label="system" border>系统消息</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="dispatchFormData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="dispatchFormData.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dispatchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDispatchSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, onMounted, onUnmounted, reactive, ref, watch} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import VueApexCharts from 'vue3-apexcharts'
import {
  batchOperateHazardPoints,
  bindDevicesToHazardPoint,
  completeHazardPoint,
  createHazardPoint,
  createHazardPointGroup,
  deleteHazardPoint,
  deleteHazardPointGroup,
  deleteHazardPoints,
  exportHazardPoints,
  getBoundDevices,
  getHazardPointDetail,
  getHazardPointGroups,
  getHazardPointPage,
  getUnboundDevices,
  pauseHazardPoint,
  unbindDevicesFromHazardPoint,
  updateHazardPoint,
  updateHazardPointGroup
} from '@/api/hazardPoint'
import {getDeviceSensors} from '@/api/sensor'
import {getChartData, getLatestData, getMonitorDataPage} from '@/api/monitorData'
import type {ChartData, LatestDataItem, MonitorDataPageItem} from '@/api/monitorData'

interface HazardPointItem {
  id: string
  code: string
  name: string
  groupId?: string
  groupName: string
  status: string
  statusName: string
  statusColor?: string
  longitude?: number
  latitude?: number
  strike?: number
  description?: string
  deviceCount: number
  createTime?: string
  createBy?: string
  updateBy?: string
  updateTime?: string
}

interface GroupItem {
  id: string
  name: string
  code: string
  description: string
  sortOrder: number
  count: number
}

interface SensorItem {
  id: string
  name: string
  iconPath: string
}

interface BoundDevice {
  deviceId: string
  deviceCode: string
  deviceName: string
  bindTime: string
  deviceStatus: string
  sensors: SensorItem[]
}

interface AlarmCriteria {
  id: string
  name: string
  deviceId: string
  deviceName: string
  monitorTypeId: string
  monitorTypeName: string
  monitorContentCode: string
  monitorContentName: string
  expression: string
  alarmLevel: string
  alarmLevelText: string
  isEnabled: boolean
}

interface DispatchRule {
  id: string
  type: 'alarm' | 'offline'
  level: string[]
  deviceIds: string[]
  deviceNames?: string[]
  persons: string[]
  channels: string[]
  execTime: string
  status: number
  remark: string
}

interface TreeNode {
  id: string
  label: string
  icon?: string
  status?: string
  statusText?: string
  bindCount?: number
  children?: TreeNode[]
  [key: string]: any
}

const loading = ref(false)
const refreshing = ref(false)
const tableData = ref<HazardPointItem[]>([])
const groupList = ref<GroupItem[]>([])
const selectedGroupId = ref<string | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const searchStatus = ref('')
const groupPanelWidth = ref(200)
const activeTab = ref('basic')
const selectedRows = ref<HazardPointItem[]>([])
const searchType = ref('name')  // 默认按名称搜索
const bindLoading = ref(false)  //  绑定设备加载中

// 分组面板相关
const displayGroupList = ref<GroupItem[]>([])
const loadingGroups = ref(false)
const groupPageSize = ref(10)
const groupCurrentPage = ref(1)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref()

// 分组管理弹窗
const groupDialogVisible = ref(false)
const groupDialogTitle = ref('')
const isEditGroup = ref(false)
const groupFormRef = ref()
const groupFormData = reactive({
  id: '',
  name: '',
  code: '',
  description: '',
  sortOrder: 0
})

//#region 分组名称校验
const validateGroupName = (_rule: any, value: string, callback: any) => {
  if (!value) {
    callback()
    return
  }
  const exists = groupList.value.some(g => g.name === value && g.id !== groupFormData.id)
  if (exists) {
    callback(new Error('分组名称已存在'))
  } else {
    callback()
  }
}

const groupFormRules = {
  name: [
    { required: true, message: '请输入分组名称', trigger: 'blur' },
    { validator: validateGroupName, trigger: 'blur' }
  ]
}

const getRequestErrorInfo = (error: any, fallbackMessage = '网络请求失败') => {
  const status = error?.response?.status
  const backendMessage = error?.response?.data?.msg
  const message = backendMessage || error?.message || fallbackMessage
  return { status, message }
}

const showRequestErrorMessage = (error: any, fallbackMessage = '网络请求失败') => {
  const { status, message } = getRequestErrorInfo(error, fallbackMessage)
  if (status === 400) {
    ElMessage.warning(message)
    return
  }
  ElMessage.error(message)
}

const detailMapRef = ref<HTMLDivElement | null>(null)
let detailMapInstance: L.Map | null = null

const dataDisplayMode = ref('chart')
const monitorDataList = ref<MonitorDataPageItem[]>([])
const chartSeriesData = ref<ChartData[]>([])
const chartOptions = ref({
  series: [] as any[],
  chart: {} as any,
  xaxis: {} as any,
  yaxis: {} as any,
  stroke: {} as any,
  fill: {} as any,
  legend: {} as any,
  tooltip: {} as any,
  dataLabels: {} as any,
  grid: {} as any,
  colors: [] as string[],
  markers: {} as any,
})

const mapDialogVisible = ref(false)
const mapRef = ref<HTMLDivElement | null>(null)
let mapInstance: L.Map | null = null
let drawLayer: L.LayerGroup | null = null
let currentDrawMode = ref('point')
const polygonCoords = ref<L.LatLng[]>([])
const strikeCoords = ref<L.LatLng[]>([])
const strikeAngle = ref(0)

const detailDialogVisible = ref(false)
const currentRow = ref<HazardPointItem | null>(null)
const boundDevices = ref<BoundDevice[]>([])
const alarmCriteriaList = ref<AlarmCriteria[]>([])
const dispatchRules = ref<DispatchRule[]>([])
const monitorSensors = ref<{ id: number; name: string }[]>([])
const monitorAttrs = ref<{ code: string; label: string }[]>([])
const monitorSensorMap = ref<Map<number, any>>(new Map())

const dataFilter = reactive({
  deviceId: '' as string | number,
  sensorId: '' as string | number,
  attrCode: '',
  valueType: 'current',
  timeRange: null as [string, string] | null
})

const bindDeviceDialogVisible = ref(false)
const leftSearchText = ref('')
const rightSearchText = ref('')
const leftDeviceTree = ref<TreeNode[]>([])
const rightDeviceTree = ref<TreeNode[]>([])
const selectedLeftKeys = ref<string[]>([])
const selectedRightKeys = ref<string[]>([])

const alarmConfigDialogVisible = ref(false)
const alarmDialogVisible = ref(false)
const isEditAlarm = ref(false)
const alarmFormRef = ref()
const alarmFormData = reactive({
  id: '',
  name: '',
  deviceId: '',
  deviceName: '',
  monitorTypeId: '',
  monitorTypeName: '',
  monitorContentCode: '',
  monitorContentName: '',
  unit: '',
  blueExpression: '',
  blueDescription: '',
  yellowExpression: '',
  yellowDescription: '',
  orangeExpression: '',
  orangeDescription: '',
  redExpression: '',
  redDescription: ''
})
const alarmFormRules = {
  name: [{ required: true, message: '请输入判据名称', trigger: 'blur' }],
  deviceId: [{ required: true, message: '请选择设备', trigger: 'blur' }],
  monitorTypeId: [{ required: true, message: '请选择监测类型', trigger: 'blur' }],
  monitorContentCode: [{ required: true, message: '请选择监测内容', trigger: 'blur' }]
}

//#region 告警等级类型
const currentEditingAlarmLevel = ref('')


//#region 监测类型
const monitorTypeList = ref<{ id: string; name: string; code: string; contents: { value: string; label: string; unit: string }[] }[]>([
  { id: '1', name: '地表位移监测', code: 'DISPLACEMENT', contents: [
    { value: 'displacement_x', label: 'X方向位移', unit: 'mm' },
    { value: 'displacement_y', label: 'Y方向位移', unit: 'mm' },
    { value: 'displacement_z', label: 'Z方向位移', unit: 'mm' },
    { value: 'total_displacement', label: '总位移', unit: 'mm' }
  ]},
  { id: '2', name: '裂缝监测', code: 'CRACK', contents: [
    { value: 'crack_width', label: '裂缝宽度', unit: 'mm' },
    { value: 'crack_length', label: '裂缝长度', unit: 'm' },
    { value: 'crack_depth', label: '裂缝深度', unit: 'm' }
  ]},
  { id: '3', name: '雨量监测', code: 'RAINFALL', contents: [
    { value: 'rainfall_hour', label: '小时雨量', unit: 'mm' },
    { value: 'rainfall_day', label: '日雨量', unit: 'mm' },
    { value: 'rainfall_week', label: '周雨量', unit: 'mm' },
    { value: 'rainfall_month', label: '月雨量', unit: 'mm' }
  ]},
  { id: '4', name: '水位监测', code: 'WATER_LEVEL', contents: [
    { value: 'water_level', label: '水位', unit: 'm' },
    { value: 'water_temp', label: '水温', unit: '℃' },
    { value: 'water_pressure', label: '水压', unit: 'kPa' }
  ]},
  { id: '5', name: '地温监测', code: 'SOIL_TEMP', contents: [
    { value: 'soil_temp_10cm', label: '10cm地温', unit: '℃' },
    { value: 'soil_temp_30cm', label: '30cm地温', unit: '℃' },
    { value: 'soil_temp_50cm', label: '50cm地温', unit: '℃' }
  ]},
  { id: '6', name: '含水率监测', code: 'MOISTURE', contents: [
    { value: 'soil_moisture', label: '土壤含水率', unit: '%' },
    { value: 'volumetric_water', label: '体积含水率', unit: '%' }
  ]},
  { id: '7', name: '倾斜监测', code: 'INCLINATION', contents: [
    { value: 'inclination_x', label: 'X方向倾角', unit: '°' },
    { value: 'inclination_y', label: 'Y方向倾角', unit: '°' },
    { value: 'total_inclination', label: '总倾角', unit: '°' }
  ]},
  { id: '8', name: '应力应变监测', code: 'STRESS', contents: [
    { value: 'axial_stress', label: '轴向应力', unit: 'MPa' },
    { value: 'radial_stress', label: '径向应力', unit: 'MPa' },
    { value: 'strain', label: '应变', unit: 'με' }
  ]}
])

const filteredMonitorContent = computed(() => {
  if (!alarmFormData.monitorTypeId) {
    return []
  }
  const mt = monitorTypeList.value.find(t => t.id === alarmFormData.monitorTypeId)
  return mt ? mt.contents : []
})

const dispatchDialogVisible = ref(false)
const isEditDispatch = ref(false)
const dispatchFormRef = ref()
const dispatchFormData = reactive({
  id: '',
  hazardPointId: '',
  type: 'alarm' as 'alarm' | 'offline',
  level: [] as string[],
  deviceIds: [] as string[],
  persons: [] as string[],
  channels: ['system'] as string[],
  execTime: '',
  execType: 'realtime' as 'realtime' | 'timed',
  execFrequencyNum: 1,
  execFrequencyUnit: 'hour' as 'minute' | 'hour' | 'day' | 'week' | 'month' | 'year',
  execTimePoints: '',
  status: 1 as 0 | 1,
  remark: ''
})
const dispatchFormRules = {
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  level: [{ required: true, type: 'array', min: 1, message: '请选择告警等级', trigger: 'change' }],
  deviceIds: [{ required: true, type: 'array', min: 1, message: '请选择设备', trigger: 'change' }],
  persons: [{ required: true, type: 'array', min: 1, message: '请选择通知人员', trigger: 'change' }],
  channels: [{ required: true, type: 'array', min: 1, message: '请选择通知渠道', trigger: 'change' }]
}

const userList = ref<{ id: string; name: string; phone: string }[]>([
  { id: '1', name: '张三', phone: '13923755477' },
  { id: '2', name: '李四', phone: '13558981389' },
  { id: '3', name: '王强', phone: '13889771288' },
  { id: '4', name: '陈经理', phone: '13900001111' }
])

const groupOptions = computed(() => groupList.value.filter(g => g.id !== 'all'))

// 统计卡片数据
const statsTotal = computed(() => total.value)
const statsMonitoring = computed(() => tableData.value.filter(r => r.status === 'MONITORING').length)
const statsDeviceTotal = computed(() => tableData.value.reduce((sum, r) => sum + (r.deviceCount || 0), 0))
const statsGroupCount = computed(() => Math.max(0, groupList.value.length - 1)) // 排除"全部"

const formData = reactive({
  code: '',
  name: '',
  groupId: '',
  longitude: 104.06,
  latitude: 30.67,
  strike: 0,
  description: ''
})

const formRules = {
  code: [{ required: true, message: '请输入隐患点编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入隐患点名称', trigger: 'blur' }]
}

const getStatusValue = () => {
  if (!searchStatus.value) {
    return undefined
  }
  const statusMap: Record<string, number> = {
    MONITORING: 1,
    PAUSED: 2,
    COMPLETED: 3
  }
  return statusMap[searchStatus.value]
}

const buildHazardPointQueryParams = () => {
  const params: Record<string, any> = {
    pageNum: currentPage.value,
    pageSize: pageSize.value
  }

  if (searchKeyword.value) {
    if (searchType.value === 'name') {
      params.name = searchKeyword.value
    } else {
      params.code = searchKeyword.value
    }
  }

  const status = getStatusValue()
  if (status !== undefined) {
    params.status = status
  }

  if (selectedGroupId.value) {
    params.groupId = parseInt(selectedGroupId.value)
  }

  return params
}

const buildHazardPointPayload = () => ({
  code: formData.code,
  name: formData.name,
  groupId: formData.groupId ? Number(formData.groupId) : null,
  longitude: formData.longitude,
  latitude: formData.latitude,
  strike: formData.strike || 0,
  description: formData.description
})

const getStatusType = (status: string) => {
  const types: Record<string, string> = {
    'MONITORING': 'success',
    'PAUSED': 'warning',
    'COMPLETED': 'info'
  }
  return types[status] || 'default'
}

const getStatusTagType = (status: string) => {
  const types: Record<string, string> = {
    'NORMAL': 'success',
    'FAULT': 'danger',
    'OFFLINE': 'warning'
  }
  return types[status] || 'default'
}

const normalizeHazardPoint = (item: any): HazardPointItem => ({
  id: String(item.id),
  code: item.code || '',
  name: item.name || '',
  groupId: item.groupId ? String(item.groupId) : '',
  groupName: item.groupName || '',
  status: item.status === 1 ? 'MONITORING' : item.status === 2 ? 'PAUSED' : 'COMPLETED',
  statusName: item.statusName || '',
  longitude: item.longitude,
  latitude: item.latitude,
  strike: item.strike,
  description: item.description,
  deviceCount: item.deviceCount || 0,
  createTime: item.createTime,
  createBy: item.createBy,
  updateBy: item.updateBy,
  updateTime: item.updateTime
})

//#region 告警等级类型
const getAlarmLevelType = (level: string) => {
  const types: Record<string, string> = {
    '蓝色预警': 'primary',
    '黄色预警': 'warning',
    '橙色预警': 'warning',
    '红色预警': 'danger',
    '四级(注意)': 'primary',
    '三级(警示)': 'warning',
    '二级(警戒)': 'warning',
    '一级(警报)': 'danger'
  }
  return types[level] || 'default'
}

// 通知渠道标签
const getChannelLabel = (channel: string) => {
  const labels: Record<string, string> = {
    'SYSTEM': '系统消息',
    'SMS': '短信',
    'WECHAT': '微信',
    'EMAIL': '邮件',
    'system': '系统消息',
    'sms': '短信',
    'email': '邮件'
  }
  return labels[channel] || channel
}

const handleToggleDispatchStatus = (row: DispatchRule) => {
  ElMessage.success(`规则${row.status === 1 ? '启用' : '禁用'}成功`)
}

// ==================== 加载隐患点列表 ====================
// 用途：分页查询隐患点，支持编号/名称搜索、状态筛选、分组筛选
const loadTableData = async () => {
  loading.value = true
  try {
    const response: any = await getHazardPointPage(buildHazardPointQueryParams())

    if (response.code === 200) {
      const data = response.data
      tableData.value = data.rows.map((item: any) => normalizeHazardPoint(item))
      total.value = data.total
    } else {
      ElMessage.error(response.msg || '获取数据失败')
    }
  } catch (error) {
    console.error('请求失败:', error)
    ElMessage.error('网络请求失败')
  } finally {
    loading.value = false
  }
}

const fetchHazardPointDetail = async (id: string) => {
  const response: any = await getHazardPointDetail(id)
  if (response.code !== 200) {
    throw new Error(response.msg || '获取详情失败')
  }
  return normalizeHazardPoint(response.data)
}

const downloadBlobFile = (blob: Blob, fileName: string) => {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

const getExportFileName = (contentDisposition?: string) => {
  if (!contentDisposition) {
    return `hazard-points-${Date.now()}.xlsx`
  }
  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1])
  }
  const normalMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  if (normalMatch?.[1]) {
    return decodeURIComponent(normalMatch[1])
  }
  return `hazard-points-${Date.now()}.xlsx`
}

// ==================== 加载分组列表 ====================
// 用途：从后端获取分组列表，并添加"全部"选项
const loadGroupList = async () => {
  loadingGroups.value = true
  try {
    const response: any = await getHazardPointGroups()

    if (response.code === 200) {
      const groups = response.data.map((item: any) => ({
        id: String(item.id),
        name: item.name,
        code: item.code,
        description: item.description,
        sortOrder: item.sortOrder,
        count: item.count
      }))

      // 添加"全部"选项
      groupList.value = [
        {
          id: 'all',
          name: '全部',
          code: 'ALL',
          description: '所有隐患点',
          sortOrder: -1,
          count: total.value
        },
        ...groups
      ]

      loadGroupPage(1) // 加载第一页分组（用于左侧列表分页）
    } else {
      ElMessage.error(response.msg || '获取分组失败')
    }
  } catch (error) {
    console.error('获取分组失败:', error)
    showRequestErrorMessage(error, '获取分组失败')
  } finally {
    loadingGroups.value = false
  }
}

const loadGroupPage = (page: number) => {
  groupCurrentPage.value = page
  const start = (page - 1) * groupPageSize.value
  const end = start + groupPageSize.value
  displayGroupList.value = [...groupList.value].sort((a, b) => a.sortOrder - b.sortOrder).slice(start, end)
}

const handleGroupListScroll = (e: Event) => {
  const target = e.target as HTMLElement
  if (target.scrollTop + target.clientHeight >= target.scrollHeight - 10 && !loadingGroups.value) {
    loadingGroups.value = true
    setTimeout(() => {
      const nextPage = groupCurrentPage.value + 1
      const totalPages = Math.ceil(groupList.value.length / groupPageSize.value)
      if (nextPage <= totalPages) {
        const start = (nextPage - 1) * groupPageSize.value
        const end = start + groupPageSize.value
        const newGroups = [...groupList.value].sort((a, b) => a.sortOrder - b.sortOrder).slice(start, end)
        displayGroupList.value = [...displayGroupList.value, ...newGroups]
        groupCurrentPage.value = nextPage
      }
      loadingGroups.value = false
    }, 500)
  }
}

const handleAddGroup = () => {
  groupDialogTitle.value = '新增分组'
  isEditGroup.value = false
  Object.assign(groupFormData, {
    id: '',
    name: '',
    description: '',
    sortOrder: groupList.value.length
  })
  groupDialogVisible.value = true
}

const handleAddGroupFromSelect = () => {
  handleAddGroup()
}

const handleEditGroupFromSelect = (option: any) => {
  const group = groupList.value.find(g => g.id === option.id)
  if (!group || group.id === 'all' || group.id === '1') {
    ElMessage.warning('该分组不允许修改')
    return
  }
  groupDialogTitle.value = '修改分组'
  isEditGroup.value = true
  Object.assign(groupFormData, {
    id: group.id,
    name: group.name,
    description: group.description,
    sortOrder: group.sortOrder
  })
  groupDialogVisible.value = true
}

// ==================== 删除分组（从下拉选择框）====================
// 用途：复用 handleDeleteGroup 的逻辑
const handleDeleteGroupFromSelect = (option: any) => {
  const group = groupList.value.find(g => g.id === option.id)
  if (group) {
    handleDeleteGroup(group)
  }
}

// ==================== 打开编辑分组弹窗 ====================
// 用途：把选中分组的数据填入表单，打开编辑对话框
const handleEditGroup = (group: GroupItem) => {
  if (group.id === 'all') {
    ElMessage.warning('"全部"分组不允许修改')
    return
  }
  groupDialogTitle.value = '编辑分组'
  isEditGroup.value = true
  Object.assign(groupFormData, {
    id: group.id,
    code: group.code,
    name: group.name,
    description: group.description,
    sortOrder: group.sortOrder
  })
  groupDialogVisible.value = true
}

// ==================== 删除分组 ====================
// 用途：调用删除接口，删除选中的分组
const handleDeleteGroup = (group: GroupItem) => {
  if (group.id === 'all') {
    ElMessage.warning('"全部"分组不允许删除')
    return
  }

  if (group.count > 0) {
    ElMessage.warning(`分组"${group.name}"下仍绑定 ${group.count} 个隐患点，禁止删除`)
    return
  }

  ElMessageBox.confirm(`确定要删除分组"${group.name}"吗?`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    loading.value = true
    try {
      const res: any = await deleteHazardPointGroup(group.id)

      if (res.code === 200) {
        ElMessage.success('删除成功')
        loadGroupList()  // 刷新分组列表
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    } catch (error: any) {
      console.error('删除失败:', error)
      const { status } = getRequestErrorInfo(error, '删除失败')
      showRequestErrorMessage(error, '删除失败')
      if (status === 404) {
        loadGroupList()
      }
    } finally {
      loading.value = false
    }
  }).catch(() => {})
}

// ==================== 提交分组（新增/编辑） ====================
// 用途：调用新增或编辑分组接口
const handleGroupSubmit = async () => {
  groupFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      loading.value = true
      try {
        let res: any

        if (isEditGroup.value) {
          res = await updateHazardPointGroup(groupFormData.id, {
            name: groupFormData.name,
            description: groupFormData.description,
            sortOrder: groupFormData.sortOrder,
            status: 1
          })
        } else {
          const code = `G${Date.now()}`
          res = await createHazardPointGroup({
            code: code,
            name: groupFormData.name,
            description: groupFormData.description,
            sortOrder: groupFormData.sortOrder,
            status: 1
          })
        }

        if (res.code === 200) {
          ElMessage.success(isEditGroup.value ? '修改成功' : '新增成功')
          groupDialogVisible.value = false
          loadGroupList()  // 刷新分组列表
        } else {
          ElMessage.error(res.msg || '操作失败')
        }
      } catch (error) {
        console.error('提交失败:', error)
        showRequestErrorMessage(error, '操作失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const startResize = (e: MouseEvent) => {
  const startX = e.clientX
  const startWidth = groupPanelWidth.value

  const onMouseMove = (e: MouseEvent) => {
    const diff = e.clientX - startX
    groupPanelWidth.value = Math.max(150, Math.min(400, startWidth + diff))
  }

  const onMouseUp = () => {
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }

  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

// ==================== 选择分组 ====================
// 用途：点击左侧分组，触发筛选
const handleSelectGroup = (group: GroupItem) => {
  selectedGroupId.value = group.id === 'all' ? null : group.id
  handleSearch()
}

// ==================== 搜索隐患点 ====================
// 用途：根据分组ID和搜索关键词查询隐患点
const handleSearch = () => {
  currentPage.value = 1
  loadTableData()
}

// 重置搜索条件
const handleReset = () => {
  searchKeyword.value = ''
  searchStatus.value = ''
  searchType.value = 'name'
  selectedGroupId.value = null
  currentPage.value = 1
  loadTableData()
}

// 刷新页面（同时刷新隐患点列表和分组列表）
const handleRefresh = async () => {
  refreshing.value = true
  try {
    await Promise.all([loadTableData(), loadGroupList()])
    ElMessage.success('刷新成功')
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    refreshing.value = false
  }
}

// ==================== 分页 ====================
// 用途：根据当前页码和每页数量查询隐患点
const handleSizeChange = () => {
  loadTableData()
}

// ==================== 分页 ====================
// 用途：根据当前页码和每页数量查询隐患点
const handlePageChange = () => {
  loadTableData()
}

const handleSelectionChange = (val: HazardPointItem[]) => {
  selectedRows.value = val
}

// ==================== 打开新增弹窗 ====================
// 用途：清空表单，打开新增对话框
const handleAdd = () => {
  dialogTitle.value = '新增隐患点'
  isEdit.value = false
  Object.assign(formData, {
    code: '',
    name: '',
    groupId: '',
    longitude: 104.06,
    latitude: 30.67,
    strike: 0,
    description: ''
  })
  polygonCoords.value = []
  strikeCoords.value = []
  strikeAngle.value = 0
  dialogVisible.value = true
}

// ==================== 打开编辑弹窗 ====================
// 用途：把选中行的数据填入表单，打开编辑对话框
const handleEdit = (row: HazardPointItem) => {
  currentRow.value = row  // 保存当前行，用于修改接口
  dialogTitle.value = '编辑隐患点'
  isEdit.value = true
  Object.assign(formData, {
    code: row.code,
    name: row.name,
    groupId: row.groupId || '',
    longitude: row.longitude || 104.06,
    latitude: row.latitude || 30.67,
    strike: row.strike || 0,
    description: row.description || ''
  })
  dialogVisible.value = true
}

// ==================== 操作列下拉菜单路由 ====================
const handleRowCommand = (command: string, row: HazardPointItem) => {
  switch (command) {
    case 'togglePause': handleTogglePause(row); break
    case 'complete': handleComplete(row); break
    case 'delete': handleDelete(row); break
  }
}

const handleView = async (row: HazardPointItem) => {
  loading.value = true
  try {
    currentRow.value = await fetchHazardPointDetail(row.id)
    activeTab.value = 'basic'
    initBoundDevices(row.id)
    initAlarmCriteria(row.id)
    initDispatchRules(row.id)
    initLatestData(row.id)
    detailDialogVisible.value = true
    nextTick(() => {
      initDetailMap()
    })
  } catch (error) {
    console.error('获取详情失败:', error)
    ElMessage.error('获取详情失败')
  } finally {
    loading.value = false
  }
}

const initDetailMap = () => {
  if (!detailMapRef.value || !currentRow.value) return

  if (detailMapInstance) {
    detailMapInstance.remove()
  }

  const lat = currentRow.value.latitude || 30.67
  const lng = currentRow.value.longitude || 104.06

  detailMapInstance = L.map(detailMapRef.value).setView([lat, lng], 15)

  L.tileLayer('https://t0.tianditu.gov.cn/vec_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=vec&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=8dda07d4649c77efd0537a0ff0a1df13', {
    maxZoom: 18,
    attribution: '天地图'
  }).addTo(detailMapInstance)

  L.tileLayer('https://t0.tianditu.gov.cn/cva_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=cva&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=8dda07d4649c77efd0537a0ff0a1df13', {
    maxZoom: 18
  }).addTo(detailMapInstance)

  L.marker([lat, lng], {
    icon: L.divIcon({
      className: 'center-marker',
      html: `<div style="background:#f56c6c;color:#fff;padding:6px 12px;border-radius:8px;font-size:14px;font-weight:bold;">⚠ ${currentRow.value.name}</div>`,
      iconSize: [120, 40],
      iconAnchor: [60, 20]
    })
  }).addTo(detailMapInstance).bindPopup(`${currentRow.value.name}<br>坐标: ${lng.toFixed(6)}, ${lat.toFixed(6)}`).openPopup()

  if (currentRow.value.strike) {
    const strikeRad = (currentRow.value.strike * Math.PI) / 180
    const offset = 0.002
    const endLat = lat + Math.sin(strikeRad) * offset
    const endLng = lng + Math.cos(strikeRad) * offset
    L.polyline([[lat, lng], [endLat, endLng]], {
      color: '#1890ff',
      weight: 3
    }).addTo(detailMapInstance)
  }
}

// ==================== 删除隐患点 ====================
// 用途：调用删除接口，删除选中的隐患点
const handleMoreCommand = (command: string, row: any) => {
  const map: Record<string, () => void> = {
    togglePause: () => handleTogglePause(row),
    complete: () => handleComplete(row),
    bindDevice: () => handleBindDevice(row),
    alarmConfig: () => handleConfigAlarm(row),
    delete: () => handleDelete(row)
  }
  map[command]?.()
}
const handleDelete = async (row: any) => {
  ElMessageBox.confirm(`确定要删除隐患点"${row.name}"吗？`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    loading.value = true
    try {
      const res: any = await deleteHazardPoint(row.id)

      if (res.code === 200) {
        ElMessage.success('删除成功')
        loadTableData() // 刷新列表
        loadGroupList() // 刷新分组列表
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('网络请求失败')
    } finally {
      loading.value = false
    }
  }).catch(() => {})
}

const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要删除的隐患点')
    return
  }
  ElMessageBox.confirm(`确定要删除选中的 ${selectedRows.value.length} 个隐患点吗？`, '批量删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    loading.value = true
    try {
      const ids = selectedRows.value.map(row => parseInt(row.id))
      const res: any = await deleteHazardPoints(ids)
      if (res.code === 200) {
        ElMessage.success('批量删除成功')
        loadTableData()
        loadGroupList()
      } else {
        ElMessage.error(res.msg || '批量删除失败')
      }
    } catch (error) {
      console.error('批量删除失败:', error)
      ElMessage.error('网络请求失败')
    } finally {
      loading.value = false
    }
  }).catch(() => {})
}

const handleExportHazardPoints = async () => {
  try {
    const exportPayload: Record<string, any> = {}
    const selectedIds = selectedRows.value.map(row => parseInt(row.id))

    if (selectedIds.length > 0) {
      exportPayload.ids = selectedIds
    } else {
      const params = buildHazardPointQueryParams()
      exportPayload.code = params.code
      exportPayload.name = params.name
      exportPayload.groupId = params.groupId
      exportPayload.status = params.status
    }

    const response = await exportHazardPoints(exportPayload)
    const contentType = String(response.headers['content-type'] || '')
    if (contentType.includes('application/json')) {
      const text = await response.data.text()
      const result = JSON.parse(text)
      throw new Error(result.msg || '导出失败')
    }

    const fileName = getExportFileName(response.headers['content-disposition'])
    downloadBlobFile(response.data, fileName)
    ElMessage.success(selectedIds.length > 0 ? '已按选中隐患点导出' : '已按当前筛选条件导出')
  } catch (error: any) {
    console.error('导出失败:', error)
    ElMessage.error(error?.message || '导出失败')
  }
}

// ==================== 新增/编辑隐患点 ====================
// 用途：提交表单，调用新增或修改接口
const handleSubmit = async () => {
  formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      loading.value = true
      try {
        let res: any
        const payload = buildHazardPointPayload()

        if (isEdit.value && currentRow.value?.id) {
          res = await updateHazardPoint(currentRow.value.id, payload)
        } else {
          res = await createHazardPoint(payload)
        }

        if (res.code === 200) {
          ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
          dialogVisible.value = false
          loadTableData() // 刷新列表
          loadGroupList() // 刷新分组列表
        } else {
          ElMessage.error(res.msg || '操作失败')
        }
      } catch (error: any) {
        console.error('提交失败:', error)
        ElMessage.error(error?.response?.data?.msg || error?.message || '网络请求失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const handleTogglePause = async (row: HazardPointItem) => {
  const pause = row.status !== 'PAUSED'
  const actionText = pause ? '停测' : '恢复'
  ElMessageBox.confirm(`确定要${actionText}隐患点"${row.name}"吗？`, `${actionText}确认`, {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: pause ? 'warning' : 'info'
  }).then(async () => {
    loading.value = true
    try {
      const res: any = await pauseHazardPoint(row.id, pause)

      if (res.code === 200) {
        ElMessage.success(`${actionText}成功`)
        loadTableData()
      } else {
        ElMessage.error(res.msg || `${actionText}失败`)
      }
    } catch (error) {
      console.error(`${actionText}失败:`, error)
      ElMessage.error('网络请求失败')
    } finally {
      loading.value = false
    }
  }).catch(() => {})
}

const handleComplete = async (row: HazardPointItem) => {
  ElMessageBox.confirm(`确定要完结隐患点"${row.name}"吗？完结后将停止监测。`, '完结确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    loading.value = true
    try {
      const res: any = await completeHazardPoint(row.id)

      if (res.code === 200) {
        ElMessage.success('完结成功')
        loadTableData()
      } else {
        ElMessage.error(res.msg || '完结失败')
      }
    } catch (error) {
      console.error('完结失败:', error)
      ElMessage.error('网络请求失败')
    } finally {
      loading.value = false
    }
  }).catch(() => {})
}

const handleOpenMap = () => {
  mapDialogVisible.value = true
  nextTick(() => {
    initMap()
  })
}

const initMap = () => {
  if (!mapRef.value) return

  if (mapInstance) {
    mapInstance.remove()
  }

  mapInstance = L.map(mapRef.value).setView([formData.latitude, formData.longitude], 15)

  L.tileLayer('https://t0.tianditu.gov.cn/vec_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=vec&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=8dda07d4649c77efd0537a0ff0a1df13', {
    maxZoom: 18,
    attribution: '天地图'
  }).addTo(mapInstance)

  L.tileLayer('https://t0.tianditu.gov.cn/cva_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=cva&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=8dda07d4649c77efd0537a0ff0a1df13', {
    maxZoom: 18
  }).addTo(mapInstance)

  drawLayer = new L.LayerGroup().addTo(mapInstance)

  if (formData.longitude && formData.latitude) {
    L.marker([formData.latitude, formData.longitude], {
      icon: L.divIcon({
        className: 'center-marker',
        html: '<div style="background:#1890ff;color:#fff;padding:4px 8px;border-radius:50%;font-size:12px;width:30px;height:30px;display:flex;align-items:center;justify-content:center;">★</div>',
        iconSize: [30, 30],
        iconAnchor: [15, 15]
      })
    }).addTo(drawLayer)
  }

  mapInstance.on('click', (e: L.LeafletMouseEvent) => {
    if (currentDrawMode.value === 'point') {
      formData.longitude = e.latlng.lng
      formData.latitude = e.latlng.lat
      if (drawLayer) {
        drawLayer.clearLayers()
        L.marker([e.latlng.lat, e.latlng.lng], {
          icon: L.divIcon({
            className: 'center-marker',
            html: '<div style="background:#1890ff;color:#fff;padding:4px 8px;border-radius:50%;font-size:12px;width:30px;height:30px;display:flex;align-items:center;justify-content:center;">★</div>',
            iconSize: [30, 30],
            iconAnchor: [15, 15]
          })
        }).addTo(drawLayer)
      }
    } else if (currentDrawMode.value === 'polygon') {
      polygonCoords.value.push(e.latlng)
      if (drawLayer) {
        drawLayer.clearLayers()
        if (polygonCoords.value.length > 1) {
          L.polyline([...polygonCoords.value], { color: '#1890ff', dashArray: '5,5' }).addTo(drawLayer)
        }
        polygonCoords.value.forEach((coord, i) => {
          L.marker([coord.lat, coord.lng], {
            icon: L.divIcon({
              className: 'vertex-marker',
              html: `<div style="background:#67C23A;color:#fff;padding:2px 6px;border-radius:4px;font-size:10px;">${i + 1}</div>`,
              iconSize: [24, 20],
              iconAnchor: [12, 10]
            })
          }).addTo(drawLayer!)
        })
      }
    } else if (currentDrawMode.value === 'strike') {
      strikeCoords.value.push(e.latlng)
      if (drawLayer) {
        drawLayer.clearLayers()
        if (strikeCoords.value.length >= 2) {
          const p1 = strikeCoords.value[0]
          const p2 = strikeCoords.value[1]
          L.polyline([p1, p2], { color: '#f56c6c', weight: 3 }).addTo(drawLayer)
          const angle = Math.atan2(p2.lat - p1.lat, p2.lng - p1.lng) * (180 / Math.PI)
          strikeAngle.value = Math.round((angle + 360) % 360)
          formData.strike = strikeAngle.value
        }
        strikeCoords.value.forEach((coord, i) => {
          L.marker([coord.lat, coord.lng], {
            icon: L.divIcon({
              className: 'strike-marker',
              html: `<div style="background:#f56c6c;color:#fff;padding:2px 6px;border-radius:4px;font-size:10px;">${i === 0 ? '起' : '终'}</div>`,
              iconSize: [24, 20],
              iconAnchor: [12, 10]
            })
          }).addTo(drawLayer!)
        })
      }
    }
  })
}

const setDrawMode = (mode: string) => {
  currentDrawMode.value = mode
  if (mode === 'polygon') {
    polygonCoords.value = []
  } else if (mode === 'strike') {
    strikeCoords.value = []
    strikeAngle.value = 0
  }
}

const clearDraw = () => {
  if (drawLayer) {
    drawLayer.clearLayers()
  }
  polygonCoords.value = []
  strikeCoords.value = []
  strikeAngle.value = 0
}

const handleMapConfirm = () => {
  mapDialogVisible.value = false
  if (mapInstance) {
    mapInstance.remove()
    mapInstance = null
  }
  ElMessage.success('隐患点范围设置成功')
}

//初始化绑定设备
const initBoundDevices = async (hazardPointId: string) => {
  try {
    const response: any = await getBoundDevices(hazardPointId)
    if (response.code === 200) {
      boundDevices.value = response.data.map((item: any) => ({
        deviceId: String(item.deviceId || item.id),
        deviceCode: item.deviceCode,
        deviceName: item.deviceName,
        bindTime: item.bindTime,
        deviceStatus: item.deviceStatus === 1 ? 'NORMAL' : item.deviceStatus === 2 ? 'FAULT' : 'OFFLINE',
        sensors: item.sensors || []
      }))
    } else {
      boundDevices.value = []
    }
  } catch (error) {
    console.error('获取绑定设备失败:', error)
    boundDevices.value = []
  }
}

//设备绑定相关函数
const loadUnboundDevices = async (keyword?: string) => {
  if (!currentRow.value) return []
  try {
    const response: any = await getUnboundDevices(currentRow.value.id, keyword)
    if (response.code === 200) {
      return response.data.map((item: any) => ({
        id: String(item.id),
        label: item.label,
        bindCount: item.bindCount,
        status: String(item.status), // 转为字符串
        iconPath: item.iconPath,
        children: item.children?.map((child: any) => ({
          id: String(child.id),
          label: child.label,
          iconPath: child.iconPath,
          status: String(child.status) // 转为字符串
        })) || []
      }))
    }
    return []
  } catch (error) {
    console.error('获取未绑定设备失败:', error)
    return []
  }
}

const handleSearchUnboundDevices = async () => {
  if (!currentRow.value) return
  const devices = await loadUnboundDevices(leftSearchText.value)
  leftDeviceTree.value = devices
}

const refreshDeviceLists = async () => {
  if (!currentRow.value) return

  await initBoundDevices(currentRow.value.id)

  const unboundDevices = await loadUnboundDevices()
  leftDeviceTree.value = unboundDevices

  rightDeviceTree.value = boundDevices.value.map(device => ({
    id: device.deviceId,
    label: `${device.deviceCode} - ${device.deviceName}`,
    iconPath: '/jc-icon/green/device_green.png',
    status: String(device.deviceStatus === 'NORMAL' ? 1 : device.deviceStatus === 'FAULT' ? 2 : 3), // 转为字符串
    children: device.sensors.map(sensor => ({
      id: sensor.id,
      label: sensor.name,
      iconPath: sensor.iconPath
    }))
  }))
}

const initAlarmCriteria = (hazardPointId: string) => {
  // TODO: 接入后端告警判据查询接口
  alarmCriteriaList.value = []
}

const initDispatchRules = (hazardPointId: string) => {
  // TODO: 接入后端告警分发规则查询接口
  dispatchRules.value = []
}

const handleBindDevice = async (row: HazardPointItem) => {
  currentRow.value = row
  bindDeviceDialogVisible.value = true

  await refreshDeviceLists()

  selectedLeftKeys.value = []
  selectedRightKeys.value = []
}

const filterLeftNode = (value: string, data: any) => {
  if (!value) return true
  return data.label.toLowerCase().includes(value.toLowerCase())
}

const filterRightNode = (value: string, data: any) => {
  if (!value) return true
  return data.label.toLowerCase().includes(value.toLowerCase())
}

const handleLeftCheck = (data: any, checkedInfo: any) => {
  const node = data as TreeNode
  if (checkedInfo.checked) {
    selectedLeftKeys.value.push(node.id)
  } else {
    const index = selectedLeftKeys.value.indexOf(node.id)
    if (index > -1) selectedLeftKeys.value.splice(index, 1)
  }
}

const handleRightCheck = (data: any, checkedInfo: any) => {
  const node = data as TreeNode
  if (checkedInfo.checked) {
    selectedRightKeys.value.push(node.id)
  } else {
    const index = selectedRightKeys.value.indexOf(node.id)
    if (index > -1) selectedRightKeys.value.splice(index, 1)
  }
}

const transferToRight = async () => {
  if (selectedLeftKeys.value.length === 0) {
    ElMessage.warning('请选择要绑定的设备')
    return
  }

  const deviceIds = selectedLeftKeys.value.map(id => parseInt(id))

  bindLoading.value = true
  try {
    const response: any = await bindDevicesToHazardPoint(currentRow.value!.id, { deviceIds })
    if (response.code === 200) {
      ElMessage.success('绑定成功')
      await refreshDeviceLists()
      selectedLeftKeys.value = []
      selectedRightKeys.value = []
    } else {
      ElMessage.error(response.msg || '绑定失败')
    }
  } catch (error) {
    console.error('绑定失败:', error)
    ElMessage.error('绑定失败')
  } finally {
    bindLoading.value = false
  }
}

const transferToLeft = async () => {
  if (selectedRightKeys.value.length === 0) {
    ElMessage.warning('请选择要解绑的设备')
    return
  }

  const deviceIds = selectedRightKeys.value.map(id => parseInt(id))

  bindLoading.value = true
  try {
    const response: any = await unbindDevicesFromHazardPoint(currentRow.value!.id, deviceIds)
    if (response.code === 200) {
      ElMessage.success('解绑成功')
      await refreshDeviceLists()
      selectedLeftKeys.value = []
      selectedRightKeys.value = []
    } else {
      ElMessage.error(response.msg || '解绑失败')
    }
  } catch (error) {
    console.error('解绑失败:', error)
    ElMessage.error('解绑失败')
  } finally {
    bindLoading.value = false
  }
}

const transferAllToRight = async () => {
  const allDeviceIds = leftDeviceTree.value.map(node => parseInt(node.id))
  if (allDeviceIds.length === 0) {
    ElMessage.warning('没有可绑定的设备')
    return
  }

  bindLoading.value = true
  try {
    const response: any = await bindDevicesToHazardPoint(currentRow.value!.id, { deviceIds: allDeviceIds })
    if (response.code === 200) {
      ElMessage.success('全部绑定成功')
      await refreshDeviceLists()
      selectedLeftKeys.value = []
      selectedRightKeys.value = []
    } else {
      ElMessage.error(response.msg || '绑定失败')
    }
  } catch (error) {
    console.error('绑定失败:', error)
    ElMessage.error('绑定失败')
  } finally {
    bindLoading.value = false
  }
}

const transferAllToLeft = async () => {
  const allDeviceIds = rightDeviceTree.value.map(node => parseInt(node.id))
  if (allDeviceIds.length === 0) {
    ElMessage.warning('没有可解绑的设备')
    return
  }

  bindLoading.value = true
  try {
    const response: any = await unbindDevicesFromHazardPoint(currentRow.value!.id, allDeviceIds)
    if (response.code === 200) {
      ElMessage.success('全部解绑成功')
      await refreshDeviceLists()
      selectedLeftKeys.value = []
      selectedRightKeys.value = []
    } else {
      ElMessage.error(response.msg || '解绑失败')
    }
  } catch (error) {
    console.error('解绑失败:', error)
    ElMessage.error('解绑失败')
  } finally {
    bindLoading.value = false
  }
}

const handleBindDeviceSubmit = () => {
  bindDeviceDialogVisible.value = false
  loadTableData()
}

const handleConfigAlarm = (row: HazardPointItem) => {
  currentRow.value = row
  initAlarmCriteria(row.id)
  initDispatchRules(row.id)
  alarmConfigDialogVisible.value = true
}

const handleAddAlarmCriteria = () => {
  isEditAlarm.value = false
  Object.assign(alarmFormData, {
    id: '',
    name: '',
    deviceId: '',
    deviceName: '',
    monitorContentCode: '',
    monitorContentName: '',
    blueExpression: '',
    blueDescription: '',
    yellowExpression: '',
    yellowDescription: '',
    orangeExpression: '',
    orangeDescription: '',
    redExpression: '',
    redDescription: ''
  })
  alarmDialogVisible.value = true
}

const handleEditAlarm = (row: AlarmCriteria) => {
  isEditAlarm.value = true
  Object.assign(alarmFormData, {
    id: row.id,
    name: row.name,
    deviceId: row.deviceId,
    deviceName: row.deviceName,
    monitorTypeId: row.monitorTypeId,
    monitorTypeName: row.monitorTypeName,
    monitorContentCode: row.monitorContentCode,
    monitorContentName: row.monitorContentName,
    unit: ''
  })
  alarmDialogVisible.value = true
}

const handleAlarmDeviceChange = (val: string) => {
  const device = boundDevices.value.find(d => d.deviceId === val)
  if (device) {
    alarmFormData.deviceName = device.deviceName
  }
}

const handleMonitorTypeChange = (val: string) => {
  const mt = monitorTypeList.value.find(t => t.id === val)
  if (mt) {
    alarmFormData.monitorTypeName = mt.name
    alarmFormData.monitorContentCode = ''
    alarmFormData.monitorContentName = ''
    alarmFormData.unit = ''
  }
}

const handleMonitorContentChange = (val: string) => {
  const mt = monitorTypeList.value.find(t => t.id === alarmFormData.monitorTypeId)
  if (mt) {
    const content = mt.contents.find(c => c.value === val)
    if (content) {
      alarmFormData.monitorContentName = content.label
      alarmFormData.unit = content.unit
    }
  }
}

const insertExpression = (text: string) => {
  if (currentEditingAlarmLevel.value) {
    const field = currentEditingAlarmLevel.value + 'Expression'
    alarmFormData[field as keyof typeof alarmFormData] += text
  }
}

const handleAlarmSubmit = () => {
  alarmFormRef.value.validate((valid: boolean) => {
    if (valid) {
      ElMessage.success(isEditAlarm.value ? '判据修改成功' : '判据添加成功')
      alarmDialogVisible.value = false
      if (currentRow.value) {
        initAlarmCriteria(currentRow.value.id)
      }
    }
  })
}

const handleToggleAlarm = (row: AlarmCriteria) => {
  ElMessage.success(`判据${row.isEnabled ? '启用' : '停用'}成功`)
}

const handleDeleteAlarm = (row: AlarmCriteria) => {
  ElMessageBox.confirm(`确定要删除判据"${row.name}"吗?`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('删除成功')
    if (currentRow.value) {
      initAlarmCriteria(currentRow.value.id)
    }
  }).catch(() => {})
}

const handleAddDispatchRule = () => {
  isEditDispatch.value = false
  Object.assign(dispatchFormData, {
    id: '',
    hazardPointId: currentRow.value?.id || '',
    type: 'alarm',
    level: [],
    deviceIds: [],
    persons: [],
    channels: ['system'],
    execTime: '',
    status: 1,
    remark: ''
  })
  dispatchDialogVisible.value = true
}

const handleEditDispatchRule = (row: DispatchRule) => {
  isEditDispatch.value = true
  const execTime = row.execTime || ''
  let execType: 'realtime' | 'timed' = 'realtime'
  let execFrequencyNum = 1
  let execFrequencyUnit: 'minute' | 'hour' | 'day' | 'week' | 'month' | 'year' = 'hour'
  let execTimePoints = ''

  if (execTime) {
    const parts = execTime.split('|')
    if (parts.length === 2) {
      execType = 'timed'
      execFrequencyUnit = parts[0] as 'minute' | 'hour' | 'day' | 'week' | 'month' | 'year'
      execTimePoints = parts[1]
    }
  }

  Object.assign(dispatchFormData, {
    id: row.id,
    hazardPointId: currentRow.value?.id || '',
    type: row.type,
    level: row.level || [],
    deviceIds: row.deviceIds || [],
    persons: row.persons || [],
    channels: row.channels || ['system'],
    execTime: execTime,
    execType,
    execFrequencyNum,
    execFrequencyUnit,
    execTimePoints,
    status: row.status || 1,
    remark: row.remark || ''
  })
  dispatchDialogVisible.value = true
}

const handleDispatchSubmit = () => {
  dispatchFormRef.value.validate((valid: boolean) => {
    if (valid) {
      let execTimeValue = ''
      if (dispatchFormData.execType === 'timed' && dispatchFormData.execTimePoints) {
        execTimeValue = `${dispatchFormData.execFrequencyUnit}|${dispatchFormData.execTimePoints}`
      }
      dispatchFormData.execTime = execTimeValue

      ElMessage.success(isEditDispatch.value ? '规则修改成功' : '规则添加成功')
      dispatchDialogVisible.value = false
      if (currentRow.value) {
        initDispatchRules(currentRow.value.id)
      }
    }
  })
}

const handleDeleteDispatchRule = (row: DispatchRule) => {
  const ruleDesc = row.remark || (row.type === 'alarm' ? '监测告警规则' : '设备离线通知规则')
  ElMessageBox.confirm(`确定要删除规则"${ruleDesc}"吗?`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('删除成功')
    if (currentRow.value) {
      initDispatchRules(currentRow.value.id)
    }
  }).catch(() => {})
}

const latestDataList = ref<LatestDataItem[]>([])

const initLatestData = async (hazardPointId: string) => {
  try {
    latestDataList.value = await getLatestData(Number(hazardPointId))
  } catch {
    latestDataList.value = []
  }
}

// 设备选择 → 加载传感器列表

const onDataDeviceChange = async (deviceId: string | number) => {
  dataFilter.sensorId = ''
  dataFilter.attrCode = ''
  monitorSensors.value = []
  monitorAttrs.value = []
  if (!deviceId) return
  try {
    const sensors = await getDeviceSensors(Number(deviceId))
    const map = new Map(monitorSensorMap.value)
    for (const s of sensors) {
      if (s.id != null) {
        map.set(s.id, s)
        monitorSensors.value.push({id: s.id, name: s.sensorName})
      }
    }
    monitorSensorMap.value = map
  } catch { /* ignore */
  }
}

// 传感器选择 → 加载属性指标
const onDataSensorChange = (sensorId: string | number) => {
  dataFilter.attrCode = ''
  if (!sensorId) {
    monitorAttrs.value = [];
    return
  }
  const sensor = monitorSensorMap.value.get(Number(sensorId))
  monitorAttrs.value = (sensor?.attrList || []).map((a: any) => ({
    code: a.attrCode,
    label: `${a.attrName || a.attrCode}${a.unit ? ` (${a.unit})` : ''}`
  }))
}

// ==================== ApexCharts 图表配置 ====================
const CHART_COLORS = [
  '#5470C6', '#91CC75', '#FAC858', '#EE6666', '#73C0DE',
  '#3BA272', '#FC8452', '#9A60B4', '#EA7CCC', '#909399'
]

const buildChartOptions = () => {
  const seriesData = chartSeriesData.value
  if (seriesData.length === 0) return

  const allLabels = new Set<string>()
  for (const s of seriesData) for (const l of s.labels) allLabels.add(l)
  const xCategories = Array.from(allLabels).sort()

  chartOptions.value = {
    chart: {
      type: 'area' as const,
      height: '100%',
      fontFamily: 'inherit',
      toolbar: {
        tools: {
          download: true,
          selection: true,
          zoom: true,
          zoomin: true,
          zoomout: true,
          pan: true,
          reset: true
        }
      },
      zoom: { enabled: true, type: 'x' as const },
      animations: { enabled: true, easing: 'easeinout' as const, speed: 800 }
    },
    colors: CHART_COLORS,
    dataLabels: { enabled: false },
    stroke: { curve: 'smooth' as const, width: 2 },
    fill: {
      type: 'gradient',
      gradient: { shadeIntensity: 1, opacityFrom: 0.2, opacityTo: 0.02, stops: [0, 100] }
    },
    markers: {
      size: 0,
      hover: { size: 5 }
    },
    grid: {
      borderColor: '#e7e7e7',
      strokeDashArray: 4,
      padding: { top: 10, right: 10, bottom: 5, left: 10 }
    },
    legend: {
      position: 'top' as const,
      horizontalAlign: 'center' as const,
      fontSize: '13px',
      fontWeight: 500,
      markers: { width: 12, height: 12, radius: 6, offsetX: -4 },
      itemMargin: { horizontal: 16, vertical: 4 },
      offsetY: -4
    },
    xaxis: {
      type: 'category' as const,
      categories: xCategories,
      labels: {
        rotate: -30,
        style: { fontSize: '11px', colors: '#666' }
      },
      tickAmount: Math.min(xCategories.length, 10),
      tooltip: { enabled: false }
    },
    yaxis: {
      title: {
        text: seriesData[0]?.unit || '',
        style: { fontSize: '12px', color: '#888' }
      },
      labels: {
        formatter: (val: number) => val != null ? Number(val.toFixed(2)).toString() : ''
      }
    },
    tooltip: {
      shared: true,
      intersect: false
    },
    series: seriesData.map((s) => {
      const points = s.labels.map((l, i) => ({ x: l, y: s.values[i] }))
      return { name: s.seriesName, data: points }
    })
  }
}

const handleQueryData = async () => {
  if (!currentRow.value) {
    ElMessage.warning('请先选择隐患点');
    return
  }
  const baseParams = {
    hazardPointId: Number(currentRow.value.id),
    valueType: dataFilter.valueType || undefined,
    startTime: dataFilter.timeRange?.[0] || undefined,
    endTime: dataFilter.timeRange?.[1] || undefined
  }
  if (dataFilter.deviceId) Object.assign(baseParams, {deviceId: Number(dataFilter.deviceId)})
  if (dataFilter.sensorId) Object.assign(baseParams, {sensorId: Number(dataFilter.sensorId)})
  if (dataFilter.attrCode) Object.assign(baseParams, {attrCode: dataFilter.attrCode})

  if (dataDisplayMode.value === 'chart') {
    await queryChart(baseParams)
  } else {
    await queryPage(baseParams)
  }
}

const queryChart = async (baseParams: Record<string, unknown>) => {
  if (!baseParams.startTime || !baseParams.endTime) {
    ElMessage.warning('图表模式需要选择时间范围')
    return
  }
  try {
    const series = await getChartData({
      hazardPointId: baseParams.hazardPointId as number,
      deviceId: baseParams.deviceId as number | undefined,
      sensorId: baseParams.sensorId as number | undefined,
      attrCode: baseParams.attrCode as string | undefined,
      valueType: baseParams.valueType as string | undefined,
      startTime: baseParams.startTime as string,
      endTime: baseParams.endTime as string
    })
    chartSeriesData.value = series
    ElMessage.success(`加载 ${series.length} 条曲线，共 ${series[0]?.labels.length || 0} 个数据点`)
    await nextTick()
    buildChartOptions()
  } catch {
    ElMessage.error('获取图表数据失败')
  }
}

const queryPage = async (baseParams: Record<string, unknown>) => {
  try {
    const res = await getMonitorDataPage({
      hazardPointId: baseParams.hazardPointId as number,
      deviceId: baseParams.deviceId as number | undefined,
      sensorId: baseParams.sensorId as number | undefined,
      attrCode: baseParams.attrCode as string | undefined,
      valueType: baseParams.valueType as string | undefined,
      startTime: baseParams.startTime as string | undefined,
      endTime: baseParams.endTime as string | undefined,
      pageNum: 1,
      pageSize: 100
    })
    monitorDataList.value = res.rows || []
    ElMessage.success(`加载 ${monitorDataList.value.length} 条数据`)
  } catch {
    ElMessage.error('获取监测数据失败')
  }
}

const handleImportData = () => {
  ElMessage.info('导入功能开发中，敬请期待')
}

const handleExportData = () => {
  ElMessage.info('导出功能开发中，敬请期待')
}

const handleBatchPause = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要停测的隐患点')
    return
  }
  ElMessageBox.confirm('确定要暂停选中的隐患点监测吗？', '批量停测确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const ids = selectedRows.value.map(row => parseInt(row.id))
      const res: any = await batchOperateHazardPoints(ids, 'pause')
      if (res.code === 200) {
        ElMessage.success('批量停测成功')
        loadTableData()
      } else {
        ElMessage.error(res.msg || '批量停测失败')
      }
    } catch (error) {
      console.error('批量停测失败:', error)
      ElMessage.error('网络请求失败')
    }
  }).catch(() => {})
}

const handleBatchResume = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要恢复的隐患点')
    return
  }
  ElMessageBox.confirm('确定要恢复选中的隐患点监测吗？', '批量恢复确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(async () => {
    try {
      const ids = selectedRows.value.map(row => parseInt(row.id))
      const res: any = await batchOperateHazardPoints(ids, 'resume')
      if (res.code === 200) {
        ElMessage.success('批量恢复成功')
        loadTableData()
      } else {
        ElMessage.error(res.msg || '批量恢复失败')
      }
    } catch (error) {
      console.error('批量恢复失败:', error)
      ElMessage.error('网络请求失败')
    }
  }).catch(() => {})
}

const handleBatchComplete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要完结的隐患点')
    return
  }
  ElMessageBox.confirm('确定要完结选中的隐患点吗？完结后将停止监测。', '批量完结确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const ids = selectedRows.value.map(row => parseInt(row.id))
      const res: any = await batchOperateHazardPoints(ids, 'complete')
      if (res.code === 200) {
        ElMessage.success('批量完结成功')
        loadTableData()
      } else {
        ElMessage.error(res.msg || '批量完结失败')
      }
    } catch (error) {
      console.error('批量完结失败:', error)
      ElMessage.error('网络请求失败')
    }
  }).catch(() => {})
}

watch(dataDisplayMode, (mode) => {
  if (mode === 'chart') {
    nextTick(() => buildChartOptions())
  }
})

watch(activeTab, (tab) => {
  if (tab === 'monitorData') {
    if (!dataFilter.timeRange) {
      const end = new Date()
      const start = new Date(end.getTime() - 3 * 24 * 60 * 60 * 1000)
      const fmt = (d: Date) => {
        const pad = (n: number) => String(n).padStart(2, '0')
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
      }
      dataFilter.timeRange = [fmt(start), fmt(end)]
    }
    if (dataDisplayMode.value === 'chart') {
      nextTick(() => buildChartOptions())
    }
  }
})

watch(dataFilter, () => {
  if (dataFilter.deviceId && dataFilter.sensorId && dataDisplayMode.value === 'chart') {
    handleQueryData()
  }
})

// 关闭详情弹窗时重置状态
watch(detailDialogVisible, (visible) => {
  if (!visible) {
    monitorDataList.value = []
    chartSeriesData.value = []
    dataFilter.deviceId = ''
    dataFilter.sensorId = ''
    dataFilter.attrCode = ''
    monitorSensors.value = []
    monitorAttrs.value = []
  }
})

onMounted(() => {
  loadTableData()
  loadGroupList()
})

onUnmounted(() => {
})
</script>

<style scoped>
/* ========== 全局 ========== */
.hazard-point-page {
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  border-radius: 8px;
  height: 100%;
  display: flex;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.page-container {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.group-panel-toggle {
  width: 20px;
  background: #f5f7fa;
  border: 1px solid #e8e8e8;
  border-right: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 4px 0 0 4px;
  transition: all 0.3s;
}

.group-panel-toggle:hover {
  background: #e8e8e8;
}

.toggle-icon {
  font-size: 12px;
  color: #606266;
}

/* ========== 左侧分组面板 ========== */
.group-panel {
  background: #fafafa;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.panel-header {
  padding: 15px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-actions {
  display: flex;
  gap: 6px;
}

.panel-title {
  font-size: 12px;
  color: #909399;
  padding: 2px 6px;
  border-radius: 3px;
}

.action-btn:hover {
  background: #e8e8e8;
  color: #606266;
}

.group-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px;
}

.group-item {
  padding: 9px 12px;
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
  font-size: 13px;
  color: #374151;
  transition: all 0.15s;
  position: relative;
}

.group-item::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #cbd5e1;
  flex-shrink: 0;
  transition: all 0.15s;
}

.group-item:hover {
  background: #f8fafc;
}

.group-item.active {
  background: rgba(59, 130, 246, 0.06);
  color: #3b82f6;
  font-weight: 500;
}

.group-item.active::before {
  background: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
}

.group-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-count {
  font-size: 11px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 1px 7px;
  border-radius: 10px;
  font-weight: 500;
  flex-shrink: 0;
}

.group-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.15s;
}

.group-item:hover .group-actions {
  opacity: 1;
}

.action-btn {
  font-size: 11px;
  color: #94a3b8;
  padding: 2px 5px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.15s;
}

.group-item:hover {
  background: #e6f7ff;
}

.group-item.active {
  background: #bae7ff;
  color: #1890ff;
}

.action-btn.delete-btn:hover {
  background: #fef2f2;
  color: #ef4444;
}

.loading-more {
  text-align: center;
  padding: 10px;
  color: #94a3b8;
  font-size: 12px;
}

/* ========== 分隔手柄 ========== */
.resize-handle {
  width: 6px;
  height: 100%;
  cursor: col-resize;
  background: transparent;
  transition: background 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.resize-handle:hover {
  background: #1890ff;
}

.resize-handle:hover::after {
  content: '';
  width: 2px;
  height: 40px;
  background: #1890ff;
  border-radius: 1px;
}

/* ========== 内容面板 ========== */
.content-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding-left: 20px;
  min-width: 0;
}

/* ========== 页头 ========== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
  letter-spacing: -0.3px;
}

.header-right {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

/* ========== 统计条 ========== */
.stats-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 14px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  margin-bottom: 12px;
  font-size: 12px;
  color: #64748b;
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
}

.stat-item strong {
  font-size: 15px;
  font-weight: 700;
  color: #1f2937;
  margin-left: 3px;
}

.stat-item .c-green { color: #10b981; }
.stat-item .c-amber { color: #f59e0b; }
.stat-item .c-purple { color: #6366f1; }

.stat-sep {
  color: #e2e8f0;
  font-size: 14px;
}

/* ========== 搜索栏 ========== */
.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  align-items: center;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.search-input {
  width: 250px;
}

.search-icon {
  font-size: 14px;
}

.status-select {
  width: 120px;
}

.search-type-select {
  width: 100px;
}

/* ========== 表格 ========== */
.table-container {
  flex: 1;
  min-height: 0;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.table-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.table-scroll :deep(.el-table) {
  /* 表格填满滚动区 */
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  padding: 10px 16px;
  background: #fff;
  border-top: 1px solid #f1f5f9;
  flex-shrink: 0;
}

.empty-text {
  color: #94a3b8;
}

/* ========== 状态标签 ========== */
.status-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
  text-align: center;
  min-width: 56px;
  letter-spacing: 0.2px;
}

.status-monitoring {
  background: rgba(16, 185, 129, 0.08);
  color: #059669;
}

.status-paused {
  background: rgba(245, 158, 11, 0.08);
  color: #d97706;
}

.status-completed {
  background: rgba(100, 116, 139, 0.08);
  color: #64748b;
}

/* ========== 操作列 ========== */
.action-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  white-space: nowrap;
}

.action-more {
  font-size: 13px !important;
}

.dropdown-arrow {
  font-size: 10px;
  margin-left: 2px;
}

.drop-danger {
  color: #ef4444 !important;
}

.drop-danger:hover {
  background-color: #fef2f2 !important;
  color: #ef4444 !important;
}

/* ========== 表格全局微调 ========== */
:deep(.el-table) {
  --el-table-border-color: #f1f5f9;
  font-size: 13px;
}

:deep(.el-table th.el-table__cell) {
  background: #f8fafc !important;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  border-bottom: 2px solid #e2e8f0;
}

:deep(.el-table tr) {
  transition: background 0.15s;
}

:deep(.el-table .el-table__body tr:hover > td) {
  background: #f8fafc;
}

:deep(.el-tag) {
  border: none;
  font-weight: 500;
}

:deep(.el-button) {
  font-weight: 500;
  letter-spacing: 0.2px;
}

:deep(.el-pagination) {
  font-size: 13px;
}

:deep(.el-pagination .btn-prev),
:deep(.el-pagination .btn-next) {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

:deep(.el-pagination .el-pager li) {
  border-radius: 6px;
}

:deep(.el-pagination .el-pager li.is-active) {
  background: #3b82f6;
}

:deep(.el-select .el-input__wrapper) {
  border-radius: 6px;
}

:deep(.el-input__wrapper) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

/* ========== 对话框内表单 ========== */
.form-hint {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}

.group-select-wrapper {
  display: flex;
  align-items: center;
  gap: 4px;
}

.add-group-btn {
  width: 32px;
  height: 32px;
  padding: 0;
}

.group-action-btn {
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 14px;
  color: #10b981;
  transition: all 0.15s;
}

.group-action-btn:hover {
  background: rgba(16, 185, 129, 0.08);
}

.group-action-btn.delete-btn {
  color: #ef4444;
}

.group-action-btn.delete-btn:hover {
  background: #fef2f2;
}

/* ========== 坐标输入 ========== */
.coordinate-input {
  display: flex;
  align-items: center;
  gap: 8px;
}

.coord-input {
  width: 150px;
}

.coord-separator {
  margin: 0 4px;
  color: #94a3b8;
}

/* ========== 地图 ========== */
.map-container {
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  overflow: hidden;
}

.map-actions {
  margin: 15px 0;
}

.map-info {
  padding: 10px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 13px;
  color: #475569;
}

/* ========== 传感器标签 ========== */
.sensor-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-right: 6px;
  margin-bottom: 4px;
  padding: 2px 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  font-size: 11px;
  color: #475569;
}

/* ========== 穿梭框 ========== */
.transfer-container {
  display: flex;
  gap: 16px;
  height: 400px;
}

.transfer-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
}

.transfer-panel .panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-bottom: 1px solid #f1f5f9;
}

.transfer-panel .panel-title {
  font-size: 13px;
  font-weight: 600;
}

.transfer-panel .search-input {
  width: 180px;
}

.transfer-tree {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.transfer-actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.arrow-icon {
  font-size: 16px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.node-icon {
  width: 18px;
  height: 18px;
}

.bind-count {
  font-size: 11px;
  color: #94a3b8;
}

.status-tag {
  margin-left: auto;
}

/* ========== 表达编辑器 ========== */
.expression-builder {
  margin-top: 15px;
}

.expression-section {
  margin-bottom: 15px;
}

.expression-row {
  display: flex;
  gap: 10px;
}

.expr-input {
  flex: 2;
}

.desc-input {
  flex: 1;
}

.expression-toolbar {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #e8e8e8;
}

.expression-tips {
  margin-top: 10px;
  font-size: 12px;
  color: #94a3b8;
}

/* ========== 通知渠道标签 ========== */
.channel-tag {
  display: inline-block;
  padding: 2px 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  font-size: 11px;
  margin-right: 4px;
  color: #475569;
}

/* ========== 告警配置 ========== */
.alarm-config-content {
  padding: 8px;
}

.config-section {
  margin-bottom: 24px;
}

.config-section:last-child {
  margin-bottom: 0;
}

.alarm-toolbar,
.dispatch-toolbar {
  margin-bottom: 12px;
}

.alarm-config-view {
  padding: 8px;
}

/* ========== 分区标题 ========== */
.section-title {
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
  display: block;
  font-size: 13px;
}

.config-section .section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 2px solid #1890ff;
}

.divider-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

/* ========== 详情弹窗 ========== */
.basic-info-container {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.info-section,
.map-section,
.system-info-section {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
}

.info-section .section-title,
.map-section .section-title,
.system-info-section .section-title {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e8e8e8;
}

.map-section #detail-map {
  border-radius: 4px;
  border: 1px solid #e8e8e8;
}

/* ========== 监测数据面板 ========== */
.monitor-data-panel {
  padding: 8px 0;
}

.data-filters {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  padding: 14px 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.data-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.data-actions {
  display: flex;
  gap: 8px;
}

.data-content {
  height: 400px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}

.chart-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.chart-inner {
  width: 100%;
  height: 100%;
}

.chart-empty-tip {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #94a3b8;
  font-size: 13px;
  pointer-events: none;
}

.data-content .table-container {
  height: 100%;
  overflow-y: auto;
}

/* ========== Element Plus 深层覆盖 ========== */
:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-form-item__label) {
  color: #475569;
  font-size: 13px;
  font-weight: 500;
}

:deep(.el-descriptions) {
  margin-bottom: 18px;
}

:deep(.el-descriptions__label) {
  color: #64748b;
  font-size: 12px;
}

:deep(.el-descriptions__content) {
  color: #1f2937;
  font-size: 13px;
}

:deep(.el-dialog) {
  border-radius: 12px;
}

:deep(.el-dialog__header) {
  padding: 20px 24px 16px;
  border-bottom: 1px solid #f1f5f9;
}

:deep(.el-dialog__title) {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

:deep(.el-dialog__body) {
  padding: 20px 24px;
}

:deep(.el-dialog__footer) {
  padding: 14px 24px 20px;
  border-top: 1px solid #f1f5f9;
}

:deep(.el-tabs__item) {
  font-size: 13px;
  color: #64748b;
}

:deep(.el-tabs__item.is-active) {
  color: #3b82f6;
  font-weight: 600;
}

:deep(.el-tabs__active-bar) {
  background: #3b82f6;
}

/* ========== 告警表单相关 ========== */
.exec-type-group {
  display: flex;
  gap: 30px;
  margin-bottom: 12px;
}

.exec-time-config {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.exec-label {
  font-size: 13px;
  color: #475569;
}

.danger-text {
  color: #ef4444 !important;
}

.danger-text:hover {
  color: #dc2626 !important;
}

.data-placeholder {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  border: 1px dashed #e2e8f0;
  border-radius: 8px;
  color: #94a3b8;
}

:deep(.el-tree-node__content) {
  height: 32px;
  font-size: 13px;
}
</style>
