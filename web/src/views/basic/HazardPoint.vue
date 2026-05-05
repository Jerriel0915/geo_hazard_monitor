<template>
  <div class="hazard-point-page">
    <div class="page-container">
      <div class="group-panel" :style="{ width: groupPanelWidth + 'px' }">
        <div class="panel-header">
          <span class="panel-title">分组列表</span>
        </div>
        <div class="group-list">
          <div
            v-for="group in groupList"
            :key="group.id"
            :class="['group-item', { active: selectedGroupId === group.id }]"
            @click="handleSelectGroup(group)"
          >
            <span class="group-name">{{ group.name }}</span>
            <span class="group-count">({{ group.count }})</span>
          </div>
        </div>
      </div>

      <div class="resize-handle" @mousedown="startResize"></div>

      <div class="content-panel">
        <div class="page-header">
          <div class="header-left">
            <h2 class="page-title">隐患点管理</h2>
          </div>
          <div class="header-right">
            <el-button type="primary" @click="handleAdd">
              <span class="btn-icon">+</span> 新增
            </el-button>
            <el-button @click="handleBatchPause" :disabled="selectedRows.length === 0">
              <span class="btn-icon">⏸</span> 停测
            </el-button>
            <el-button @click="handleBatchResume" :disabled="selectedRows.length === 0">
              <span class="btn-icon">▶</span> 恢复
            </el-button>
            <el-button @click="handleBatchComplete" :disabled="selectedRows.length === 0" type="warning">
              <span class="btn-icon">✓</span> 完结
            </el-button>
            <el-button @click="handleExport">
              <span class="btn-icon">↓</span> 导出
            </el-button>
          </div>
        </div>

        <div class="search-bar">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索编号或名称"
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
        </div>

        <div class="table-container">
          <el-table
            :data="tableData"
            border
            stripe
            v-loading="loading"
            :header-cell-style="{ background: '#f5f7fa', color: '#303133', fontWeight: 'bold' }"
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
            <el-table-column label="操作" width="280" fixed="right" align="center">
              <template #default="{ row }">
                <el-button type="text" size="small" @click="handleView(row)">查看</el-button>
                <el-button type="text" size="small" @click="handleEdit(row)">编辑</el-button>
                <el-button type="text" size="small" @click="handleBindDevice(row)">绑定设备</el-button>
                <el-button type="text" size="small" @click="handleConfigAlarm(row)">告警配置</el-button>
                <el-button type="text" size="small" class="danger-text" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

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
              <el-select v-model="formData.groupId" placeholder="未分组">
                <el-option v-for="g in groupOptions" :key="g.id" :label="g.name" :value="g.id" />
              </el-select>
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
                <el-descriptions-item label="创建人">{{ currentRow?.creator || '-' }}</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ currentRow?.createTime || '-' }}</el-descriptions-item>
                <el-descriptions-item label="更新人">{{ currentRow?.updater || '-' }}</el-descriptions-item>
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
                  <img :src="sensor.iconPath" class="sensor-icon" />{{ sensor.name }}
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
              <h3 class="section-title">通知分发</h3>
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
              <el-select v-model="dataFilter.deviceId" placeholder="选择设备" clearable style="width: 150px">
                <el-option v-for="d in boundDevices" :key="d.deviceId" :label="d.deviceName" :value="d.deviceId" />
              </el-select>
              <el-select v-model="dataFilter.sensorId" placeholder="选择传感器" clearable style="width: 150px">
                <el-option label="节点1" value="node1" />
                <el-option label="节点2" value="node2" />
                <el-option label="节点3" value="node3" />
                <el-option label="电量" value="battery" />
              </el-select>
              <el-select v-model="dataFilter.valueType" placeholder="值类型" clearable style="width: 150px">
                <el-option label="采集值" value="current" />
                <el-option label="小时变化" value="hour" />
                <el-option label="24小时变化" value="day" />
                <el-option label="72小时变化" value="week" />
              </el-select>
              <el-select v-model="dataFilter.direction" placeholder="方向" clearable style="width: 100px">
                <el-option label="X" value="x" />
                <el-option label="Y" value="y" />
                <el-option label="Z" value="z" />
              </el-select>
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
                <div class="chart-placeholder">
                  <span>ECHARTS图表展示区域</span>
                </div>
              </div>
              <div v-else class="table-container">
                <el-table :data="monitorDataList" border size="small">
                  <el-table-column prop="time" label="时间" width="180" align="center" />
                  <el-table-column prop="deviceName" label="设备" width="150" align="center" />
                  <el-table-column prop="sensorName" label="传感器" width="120" align="center" />
                  <el-table-column prop="value" label="数值" width="100" align="center" />
                  <el-table-column prop="unit" label="单位" width="80" align="center" />
                  <el-table-column prop="direction" label="方向" width="80" align="center" />
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
      title="告警配置"
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
          <h3 class="section-title">通知分发</h3>
          <div class="dispatch-toolbar">
            <el-button type="primary" size="small" @click="handleAddDispatchRule">
              <span class="btn-icon">+</span> 添加规则
            </el-button>
          </div>
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
                <el-switch v-model="row.isEnabled" />
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
      title="绑定设备"
      width="900px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="transfer-container">
        <div class="transfer-panel">
          <div class="panel-header">
            <span class="panel-title">待绑定设备</span>
            <el-input
              v-model="leftSearchText"
              placeholder="搜索设备/传感器名称"
              class="search-input"
              clearable
              size="small"
            />
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
        <el-button type="primary" @click="handleBindDeviceSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="alarmDialogVisible"
      :title="isEditAlarm ? '编辑告警判据' : '添加告警判据'"
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
      :title="isEditDispatch ? '编辑通知规则' : '添加通知规则'"
      width="500px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="dispatchFormRef" :model="dispatchFormData" :rules="dispatchFormRules" label-width="100px">
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="dispatchFormData.name" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="dispatchFormData.type" placeholder="请选择类型">
            <el-option label="告警分发" value="ALARM" />
            <el-option label="状态通知" value="STATUS" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="dispatchFormData.type === 'ALARM'" label="告警等级" prop="alarmLevel">
          <el-select v-model="dispatchFormData.alarmLevel" placeholder="请选择告警等级">
            <el-option label="一级(蓝色)" value="一级(蓝色)" />
            <el-option label="二级(黄色)" value="二级(黄色)" />
            <el-option label="三级(橙色)" value="三级(橙色)" />
            <el-option label="四级(红色)" value="四级(红色)" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="dispatchFormData.type === 'STATUS'" label="设备" prop="deviceId">
          <el-select v-model="dispatchFormData.deviceId" placeholder="请选择设备">
            <el-option label="全部" value="ALL" />
            <el-option v-for="d in boundDevices" :key="d.deviceId" :label="d.deviceName" :value="d.deviceId" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="dispatchFormData.type === 'STATUS'" label="时间设置" prop="timeSetting">
          <el-select v-model="dispatchFormData.timeSetting" placeholder="请选择时间类型">
            <el-option label="每天定时" value="DAILY" />
            <el-option label="设备离线" value="OFFLINE" />
          </el-select>
          <el-input v-if="dispatchFormData.timeSetting === 'DAILY'" v-model="dispatchFormData.timeValue" placeholder="例如: 9:30,16:00" style="margin-top: 10px; width: 100%" />
        </el-form-item>
        <el-form-item label="接收人" prop="recipientIds">
          <el-select v-model="dispatchFormData.recipientIds" multiple placeholder="请选择接收人" style="width: 100%">
            <el-option v-for="u in userList" :key="u.id" :label="u.name + '(' + u.phone + ')'" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="通知渠道" prop="channel">
          <el-checkbox-group v-model="dispatchFormData.channel">
            <el-checkbox label="SYSTEM" border>系统消息</el-checkbox>
            <el-checkbox label="SMS" border>短信通知</el-checkbox>
            <el-checkbox label="WECHAT" border>微信通知</el-checkbox>
            <el-checkbox label="EMAIL" border>电子邮件</el-checkbox>
          </el-checkbox-group>
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
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox, ElTree } from 'element-plus'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

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
  creator?: string
  updater?: string
  updateTime?: string
}

interface GroupItem {
  id: string
  name: string
  code: string
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
  name: string
  type: string
  alarmLevel: string
  deviceId?: string
  recipientName: string
  recipientIds: string[]
  channel: string
  timeSetting?: string
  timeValue?: string
  isEnabled: boolean
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

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref()

const detailMapRef = ref<HTMLDivElement | null>(null)
let detailMapInstance: L.Map | null = null

const dataDisplayMode = ref('chart')
const monitorDataList = ref<{ time: string; deviceName: string; sensorName: string; value: string; unit: string; direction: string }[]>([])

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
const dataFilter = reactive({
  deviceId: '',
  sensorId: '',
  valueType: 'current',
  direction: ''
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
const currentEditingAlarmLevel = ref('')

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
  name: '',
  type: 'ALARM',
  alarmLevel: '',
  deviceId: '',
  recipientIds: [] as string[],
  channel: [] as string[],
  timeSetting: '',
  timeValue: ''
})
const dispatchFormRules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'blur' }],
  recipientIds: [{ required: true, message: '请选择接收人', trigger: 'blur' }]
}

const userList = ref<{ id: string; name: string; phone: string }[]>([
  { id: '1', name: '张三', phone: '13923755477' },
  { id: '2', name: '李四', phone: '13558981389' },
  { id: '3', name: '王强', phone: '13889771288' },
  { id: '4', name: '陈经理', phone: '13900001111' }
])

const groupOptions = computed(() => groupList.value.filter(g => g.id !== 'all'))

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

const getAlarmLevelType = (level: string) => {
  const types: Record<string, string> = {
    '蓝色预警': 'primary',
    '黄色预警': 'warning',
    '橙色预警': 'warning',
    '红色预警': 'danger'
  }
  return types[level] || 'default'
}

const getChannelLabel = (channel: string) => {
  const labels: Record<string, string> = {
    'SYSTEM': '系统消息',
    'SMS': '短信',
    'WECHAT': '微信',
    'EMAIL': '邮件'
  }
  return labels[channel] || channel
}

const initTableData = () => {
  loading.value = true
  setTimeout(() => {
    tableData.value = [
      {
        id: '1',
        code: 'HP001',
        name: '龙潭寺滑坡隐患点',
        groupId: '1',
        groupName: '未分组',
        status: 'MONITORING',
        statusName: '监测中',
        statusColor: '#67C23A',
        longitude: 104.156789,
        latitude: 30.678901,
        strike: 45,
        description: '该区域存在滑坡风险，需要重点监测',
        deviceCount: 2,
        createTime: '2024-01-15 10:30:00',
        creator: '张三',
        updater: '李四',
        updateTime: '2024-01-20 14:00:00'
      },
      {
        id: '2',
        code: 'HP002',
        name: '青城山崩塌隐患点',
        groupId: '2',
        groupName: '高风险区',
        status: 'MONITORING',
        statusName: '监测中',
        statusColor: '#67C23A',
        longitude: 103.589234,
        latitude: 30.891234,
        strike: 90,
        description: '岩石崩塌风险较高',
        deviceCount: 1,
        createTime: '2024-01-16 14:20:00',
        creator: '李四',
        updater: '张三',
        updateTime: '2024-01-18 10:30:00'
      },
      {
        id: '3',
        code: 'HP003',
        name: '瓦屋山泥石流隐患点',
        groupId: '',
        groupName: '',
        status: 'MONITORING',
        statusName: '监测中',
        statusColor: '#67C23A',
        longitude: 102.891234,
        latitude: 29.589234,
        description: '雨季可能出现泥石流',
        deviceCount: 1,
        createTime: '2024-01-17 09:15:00'
      },
      {
        id: '4',
        code: 'HP004',
        name: '峨眉山边坡隐患点',
        groupId: '4',
        groupName: '低风险区',
        status: 'PAUSED',
        statusName: '停测中',
        statusColor: '#E6A23C',
        longitude: 103.334567,
        latitude: 29.556789,
        strike: 180,
        description: '边坡稳定性较差',
        deviceCount: 0,
        createTime: '2024-01-18 11:00:00'
      },
      {
        id: '5',
        code: 'HP005',
        name: '都江堰裂缝隐患点',
        groupId: '3',
        groupName: '中风险区',
        status: 'COMPLETED',
        statusName: '已完结',
        statusColor: '#909399',
        longitude: 103.654321,
        latitude: 30.987654,
        description: '已完成治理，监测结束',
        deviceCount: 0,
        createTime: '2024-01-19 10:00:00'
      }
    ]
    total.value = tableData.value.length
    loading.value = false
  }, 500)
}

const initGroupList = () => {
  groupList.value = [
    { id: 'all', name: '全部', code: 'ALL', count: 5 },
    { id: '1', name: '未分组', code: 'DEFAULT', count: 1 },
    { id: '2', name: '高风险区', code: 'HIGH_RISK', count: 1 },
    { id: '3', name: '中风险区', code: 'MEDIUM_RISK', count: 1 },
    { id: '4', name: '低风险区', code: 'LOW_RISK', count: 2 }
  ]
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

const handleSelectGroup = (group: GroupItem) => {
  selectedGroupId.value = group.id === 'all' ? null : group.id
  handleSearch()
}

const handleSearch = () => {
  currentPage.value = 1
  initTableData()
}

const handleSizeChange = () => {
  initTableData()
}

const handlePageChange = () => {
  initTableData()
}

const handleSelectionChange = (val: HazardPointItem[]) => {
  selectedRows.value = val
}

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

const handleEdit = (row: HazardPointItem) => {
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
  polygonCoords.value = []
  strikeCoords.value = []
  strikeAngle.value = 0
  dialogVisible.value = true
}

const handleView = (row: HazardPointItem) => {
  currentRow.value = row
  activeTab.value = 'basic'
  initBoundDevices(row.id)
  initAlarmCriteria(row.id)
  initDispatchRules(row.id)
  detailDialogVisible.value = true
  nextTick(() => {
    initDetailMap()
  })
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
      color: '#409eff',
      weight: 3
    }).addTo(detailMapInstance)
  }
}

const handleDelete = (row: HazardPointItem) => {
  ElMessageBox.confirm(`确定要删除隐患点"${row.name}"吗?`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    const index = tableData.value.findIndex(item => item.id === row.id)
    if (index > -1) {
      tableData.value.splice(index, 1)
      total.value--
    }
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const handleExport = () => {
  ElMessage.info('正在导出...')
  setTimeout(() => {
    ElMessage.success('导出成功')
  }, 1000)
}

const handleSubmit = () => {
  formRef.value.validate((valid: boolean) => {
    if (valid) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      initTableData()
    }
  })
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
        html: '<div style="background:#409eff;color:#fff;padding:4px 8px;border-radius:50%;font-size:12px;width:30px;height:30px;display:flex;align-items:center;justify-content:center;">★</div>',
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
            html: '<div style="background:#409eff;color:#fff;padding:4px 8px;border-radius:50%;font-size:12px;width:30px;height:30px;display:flex;align-items:center;justify-content:center;">★</div>',
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
          L.polyline([...polygonCoords.value], { color: '#409eff', dashArray: '5,5' }).addTo(drawLayer)
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

const initBoundDevices = (hazardPointId: string) => {
  if (hazardPointId === '1') {
    boundDevices.value = [
      {
        deviceId: '1',
        deviceCode: 'DEV001',
        deviceName: '雨量监测站-01',
        bindTime: '2024-01-15 10:00:00',
        deviceStatus: 'NORMAL',
        sensors: [
          { id: 's1', name: '雨量传感器', iconPath: '/jc-icon/green/rain_green.png' },
          { id: 's2', name: '温湿度传感器', iconPath: '/jc-icon/green/temp_green.png' }
        ]
      },
      {
        deviceId: '2',
        deviceCode: 'DEV002',
        deviceName: '位移监测站-01',
        bindTime: '2024-01-16 11:00:00',
        deviceStatus: 'NORMAL',
        sensors: [
          { id: 's3', name: '位移传感器X', iconPath: '/jc-icon/green/displacement_green.png' },
          { id: 's4', name: '位移传感器Y', iconPath: '/jc-icon/green/displacement_green.png' }
        ]
      }
    ]
  } else if (hazardPointId === '2') {
    boundDevices.value = [
      {
        deviceId: '3',
        deviceCode: 'DEV003',
        deviceName: '温湿度监测站-01',
        bindTime: '2024-01-17 09:00:00',
        deviceStatus: 'FAULT',
        sensors: [
          { id: 's5', name: '温度传感器', iconPath: '/jc-icon/green/temp_green.png' },
          { id: 's6', name: '湿度传感器', iconPath: '/jc-icon/green/humidity_green.png' }
        ]
      }
    ]
  } else {
    boundDevices.value = []
  }
}

const initAlarmCriteria = (hazardPointId: string) => {
  if (hazardPointId === '1') {
    alarmCriteriaList.value = [
      { id: '1', name: '水位雷达判据', deviceId: '1', deviceName: '雨量监测站-01', monitorTypeId: '4', monitorTypeName: '水位监测', monitorContentCode: 'water_level', monitorContentName: '水位', expression: '水位(m) >= 808.5', alarmLevel: '黄色预警', alarmLevelText: '黄色预警', isEnabled: true },
      { id: '2', name: '雨量告警判据', deviceId: '1', deviceName: '雨量监测站-01', monitorTypeId: '3', monitorTypeName: '雨量监测', monitorContentCode: 'rainfall_day', monitorContentName: '日雨量', expression: '雨量(mm) >= 100', alarmLevel: '橙色预警', alarmLevelText: '橙色预警', isEnabled: true },
      { id: '3', name: '位移变化告警', deviceId: '2', deviceName: '位移监测站-01', monitorTypeId: '1', monitorTypeName: '地表位移监测', monitorContentCode: 'total_displacement', monitorContentName: '总位移', expression: '位移(mm) >= 808.8', alarmLevel: '红色预警', alarmLevelText: '红色预警', isEnabled: true }
    ]
  } else {
    alarmCriteriaList.value = []
  }
}

const initDispatchRules = (hazardPointId: string) => {
  if (hazardPointId === '1') {
    dispatchRules.value = [
      { id: '1', name: '重大告警通知', type: 'ALARM', alarmLevel: '三级(橙色),四级(红色)', recipientName: '张三,李四', recipientIds: ['1', '2'], channel: 'SMS,WECHAT', isEnabled: true },
      { id: '2', name: '一般告警通知', type: 'ALARM', alarmLevel: '一级(蓝色),二级(黄色)', recipientName: '王强', recipientIds: ['3'], channel: 'SYSTEM', isEnabled: true },
      { id: '3', name: '设备离线通知', type: 'STATUS', alarmLevel: '', recipientName: '陈经理', recipientIds: ['4'], channel: 'SMS,EMAIL', timeSetting: 'OFFLINE', isEnabled: true }
    ]
  } else {
    dispatchRules.value = []
  }
}

const handleBindDevice = (row: HazardPointItem) => {
  currentRow.value = row
  initBoundDevices(row.id)
  
  const allDevices: TreeNode[] = [
    { id: 'd1', label: '雨量监测站-01', icon: '/jc-icon/green/device_green.png', status: 'NORMAL', statusText: '正常', bindCount: 2, children: [
      { id: 'd1-s1', label: '雨量传感器', icon: '/jc-icon/green/rain_green.png', status: 'NORMAL', statusText: '正常' },
      { id: 'd1-s2', label: '温湿度传感器', icon: '/jc-icon/green/temp_green.png', status: 'NORMAL', statusText: '正常' }
    ]},
    { id: 'd2', label: '位移监测站-01', icon: '/jc-icon/green/device_green.png', status: 'NORMAL', statusText: '正常', bindCount: 1, children: [
      { id: 'd2-s1', label: '位移传感器X', icon: '/jc-icon/green/displacement_green.png', status: 'NORMAL', statusText: '正常' },
      { id: 'd2-s2', label: '位移传感器Y', icon: '/jc-icon/green/displacement_green.png', status: 'NORMAL', statusText: '正常' }
    ]},
    { id: 'd3', label: '温湿度监测站-01', icon: '/jc-icon/green/device_green.png', status: 'FAULT', statusText: '故障', bindCount: 1, children: [
      { id: 'd3-s1', label: '温度传感器', icon: '/jc-icon/green/temp_green.png', status: 'NORMAL', statusText: '正常' },
      { id: 'd3-s2', label: '湿度传感器', icon: '/jc-icon/green/humidity_green.png', status: 'FAULT', statusText: '故障' }
    ]},
    { id: 'd4', label: '综合监测站-01', icon: '/jc-icon/green/device_green.png', status: 'OFFLINE', statusText: '离线', bindCount: 0, children: [
      { id: 'd4-s1', label: '倾斜传感器', icon: '/jc-icon/green/inclination_green.png', status: 'OFFLINE', statusText: '离线' }
    ]}
  ]

  const boundIds = boundDevices.value.map(d => 'd' + d.deviceId)
  
  leftDeviceTree.value = allDevices.filter(d => !boundIds.includes(d.id))
  rightDeviceTree.value = allDevices.filter(d => boundIds.includes(d.id))
  
  selectedLeftKeys.value = []
  selectedRightKeys.value = []
  
  bindDeviceDialogVisible.value = true
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

const transferToRight = () => {
  selectedLeftKeys.value.forEach(key => {
    const nodeIndex = leftDeviceTree.value.findIndex(n => n.id === key)
    if (nodeIndex > -1) {
      const node = leftDeviceTree.value.splice(nodeIndex, 1)[0]
      rightDeviceTree.value.push(node)
    }
  })
  selectedLeftKeys.value = []
}

const transferAllToRight = () => {
  rightDeviceTree.value.push(...leftDeviceTree.value)
  leftDeviceTree.value = []
  selectedLeftKeys.value = []
}

const transferToLeft = () => {
  selectedRightKeys.value.forEach(key => {
    const nodeIndex = rightDeviceTree.value.findIndex(n => n.id === key)
    if (nodeIndex > -1) {
      const node = rightDeviceTree.value.splice(nodeIndex, 1)[0]
      leftDeviceTree.value.push(node)
    }
  })
  selectedRightKeys.value = []
}

const transferAllToLeft = () => {
  leftDeviceTree.value.push(...rightDeviceTree.value)
  rightDeviceTree.value = []
  selectedRightKeys.value = []
}

const handleBindDeviceSubmit = () => {
  ElMessage.success('设备绑定成功')
  bindDeviceDialogVisible.value = false
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
    name: '',
    type: 'ALARM',
    alarmLevel: '',
    deviceId: '',
    recipientIds: [],
    channel: [],
    timeSetting: '',
    timeValue: ''
  })
  dispatchDialogVisible.value = true
}

const handleEditDispatchRule = (row: DispatchRule) => {
  isEditDispatch.value = true
  Object.assign(dispatchFormData, {
    id: row.id,
    name: row.name,
    type: row.type,
    alarmLevel: row.alarmLevel,
    deviceId: row.deviceId || '',
    recipientIds: row.recipientIds,
    channel: row.channel.split(','),
    timeSetting: row.timeSetting || '',
    timeValue: row.timeValue || ''
  })
  dispatchDialogVisible.value = true
}

const handleDispatchSubmit = () => {
  dispatchFormRef.value.validate((valid: boolean) => {
    if (valid) {
      ElMessage.success(isEditDispatch.value ? '规则修改成功' : '规则添加成功')
      dispatchDialogVisible.value = false
      if (currentRow.value) {
        initDispatchRules(currentRow.value.id)
      }
    }
  })
}

const handleDeleteDispatchRule = (row: DispatchRule) => {
  ElMessageBox.confirm(`确定要删除规则"${row.name}"吗?`, '删除确认', {
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

const handleQueryData = () => {
  ElMessage.info('正在加载监测数据...')
  setTimeout(() => {
    monitorDataList.value = [
      { time: '2024-01-20 08:00:00', deviceName: '雨量监测站-01', sensorName: '雨量传感器', value: '12.5', unit: 'mm', direction: 'X' },
      { time: '2024-01-20 08:15:00', deviceName: '雨量监测站-01', sensorName: '雨量传感器', value: '15.3', unit: 'mm', direction: 'X' },
      { time: '2024-01-20 08:30:00', deviceName: '雨量监测站-01', sensorName: '雨量传感器', value: '18.7', unit: 'mm', direction: 'X' },
      { time: '2024-01-20 08:45:00', deviceName: '雨量监测站-01', sensorName: '雨量传感器', value: '22.1', unit: 'mm', direction: 'X' },
      { time: '2024-01-20 09:00:00', deviceName: '雨量监测站-01', sensorName: '雨量传感器', value: '25.6', unit: 'mm', direction: 'X' },
      { time: '2024-01-20 09:15:00', deviceName: '位移监测站-01', sensorName: '位移传感器X', value: '0.5', unit: 'mm', direction: 'X' },
      { time: '2024-01-20 09:30:00', deviceName: '位移监测站-01', sensorName: '位移传感器Y', value: '0.3', unit: 'mm', direction: 'Y' },
      { time: '2024-01-20 09:45:00', deviceName: '位移监测站-01', sensorName: '位移传感器X', value: '0.7', unit: 'mm', direction: 'X' },
      { time: '2024-01-20 10:00:00', deviceName: '位移监测站-01', sensorName: '位移传感器Y', value: '0.4', unit: 'mm', direction: 'Y' },
      { time: '2024-01-20 10:15:00', deviceName: '雨量监测站-01', sensorName: '温湿度传感器', value: '25.3', unit: '℃', direction: '-' }
    ]
    ElMessage.success('监测数据加载成功')
  }, 800)
}

const handleImportData = () => {
  ElMessage.info('正在导入监测数据...')
  setTimeout(() => {
    ElMessage.success('监测数据导入成功')
  }, 1000)
}

const handleExportData = () => {
  ElMessage.info('正在导出监测数据...')
  setTimeout(() => {
    ElMessage.success('监测数据导出成功')
  }, 1000)
}

const handleBatchPause = () => {
  ElMessageBox.confirm('确定要暂停选中的隐患点监测吗？', '批量停测确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    selectedRows.value.forEach(row => {
      row.status = 'PAUSED'
      row.statusName = '停测中'
    })
    ElMessage.success('批量停测成功')
    selectedRows.value = []
  }).catch(() => {})
}

const handleBatchResume = () => {
  ElMessageBox.confirm('确定要恢复选中的隐患点监测吗？', '批量恢复确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    selectedRows.value.forEach(row => {
      row.status = 'MONITORING'
      row.statusName = '监测中'
    })
    ElMessage.success('批量恢复成功')
    selectedRows.value = []
  }).catch(() => {})
}

const handleBatchComplete = () => {
  ElMessageBox.confirm('确定要完结选中的隐患点吗？完结后将停止监测。', '批量完结确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    selectedRows.value.forEach(row => {
      row.status = 'COMPLETED'
      row.statusName = '已完结'
    })
    ElMessage.success('批量完结成功')
    selectedRows.value = []
  }).catch(() => {})
}

onMounted(() => {
  initTableData()
  initGroupList()
})
</script>

<style scoped>
.hazard-point-page {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  min-height: calc(100% - 40px);
}

.page-container {
  display: flex;
  height: calc(100vh - 180px);
}

.group-panel {
  background: #fafafa;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 15px;
  border-bottom: 1px solid #e4e7ed;
}

.panel-title {
  font-weight: bold;
  color: #303133;
}

.group-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.group-item {
  padding: 12px 15px;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.group-item:hover {
  background: #f0f7ff;
}

.group-item.active {
  background: #e6f0ff;
  color: #409eff;
}

.group-name {
  font-size: 14px;
}

.group-count {
  font-size: 12px;
  color: #909399;
}

.resize-handle {
  width: 5px;
  cursor: col-resize;
  background: transparent;
}

.resize-handle:hover {
  background: #409eff;
}

.content-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding-left: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  margin: 0;
}

.header-right {
  display: flex;
  gap: 10px;
}

.btn-icon {
  margin-right: 4px;
}

.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  align-items: center;
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

.table-container {
  flex: 1;
  background: #fff;
}

.empty-text {
  color: #909399;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.danger-text {
  color: #f56c6c !important;
}

.danger-text:hover {
  color: #f56c6c !important;
}

.divider-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
}

.alarm-toolbar,
.dispatch-toolbar {
  margin-bottom: 15px;
}

.monitor-data-panel {
  padding: 10px 0;
}

.data-filters {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.data-placeholder {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  color: #909399;
}

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
  color: #909399;
}

.map-container {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}

.map-actions {
  margin: 15px 0;
}

.map-info {
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
}

.sensor-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-right: 8px;
  margin-bottom: 4px;
  padding: 2px 8px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
}

.sensor-icon {
  width: 16px;
  height: 16px;
}

.transfer-container {
  display: flex;
  gap: 20px;
  height: 400px;
}

.transfer-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.transfer-panel .panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.transfer-panel .panel-title {
  font-size: 14px;
  font-weight: bold;
}

.transfer-panel .search-input {
  width: 180px;
}

.transfer-tree {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.transfer-actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
}

.arrow-icon {
  font-size: 18px;
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
  font-size: 12px;
  color: #909399;
}

.status-tag {
  margin-left: auto;
}

.expression-builder {
  margin-top: 15px;
}

.expression-section {
  margin-bottom: 15px;
}

.section-title {
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
  display: block;
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
  border-top: 1px solid #ebeef5;
}

.expression-tips {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.channel-tag {
  display: inline-block;
  padding: 2px 8px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  margin-right: 4px;
}

.alarm-config-content {
  padding: 10px;
}

.config-section {
  margin-bottom: 25px;
}

.config-section:last-child {
  margin-bottom: 0;
}

.config-section .section-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 2px solid #409eff;
}

.basic-info-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.info-section,
.map-section,
.system-info-section {
  background: #fafafa;
  border-radius: 8px;
  padding: 15px;
}

.info-section .section-title,
.map-section .section-title,
.system-info-section .section-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 15px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e4e7ed;
}

.map-section {
  padding: 15px;
}

.map-section #detail-map {
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.data-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.data-actions {
  display: flex;
  gap: 10px;
}

.data-content {
  height: 400px;
}

.chart-container {
  width: 100%;
  height: 100%;
}

.chart-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  color: #909399;
}

.data-content .table-container {
  height: 100%;
  overflow-y: auto;
}

.alarm-config-view {
  padding: 10px;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-descriptions) {
  margin-bottom: 20px;
}

.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  text-align: center;
  min-width: 60px;
}

.status-monitoring {
  background-color: #e8f5e9;
  color: #2e7d32;
  border: 1px solid #c8e6c9;
}

.status-paused {
  background-color: #fff3e0;
  color: #e65100;
  border: 1px solid #ffe0b2;
}

.status-completed {
  background-color: #f5f5f5;
  color: #757575;
  border: 1px solid #e0e0e0;
}
</style>