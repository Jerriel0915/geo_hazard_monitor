<template>
  <div class="page hazard-point-page">
    <!-- 标题栏 -->
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">隐患点管理</h2>
        <span class="header__subtitle">地质灾害隐患点基础信息维护</span>
      </div>
      <div class="header__right">
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

    <!-- 主体：左侧分组 + 右侧表格 -->
    <div class="page-body">
      <div class="group-panel" :style="{ width: groupPanelWidth + 'px' }">
        <div class="group-panel__header">
          <span class="group-panel__title">分组列表</span>
          <el-button size="small" @click="handleAddGroup">+ 新增</el-button>
        </div>
        <div class="group-panel__search">
          <el-input v-model="groupFilterName" placeholder="搜索分组名称" clearable size="small"
                    @input="loadGroupPage(1)" @clear="loadGroupPage(1)"/>
        </div>
        <div class="group-panel__list" @scroll="handleGroupListScroll">
          <div
            v-for="group in displayGroupList"
            :key="group.id"
            :class="['group-item', { 'group-item--active': selectedGroupId === group.id, 'group-item--all': group.id === 'all' }]"
            @click="handleSelectGroup(group)"
          >
            <span class="group-item__name">{{ group.name }}</span>
            <span class="group-item__count">({{ group.count }})</span>
            <div class="group-item__actions">
              <span class="group-item__action" @click.stop="handleEditGroup(group)"><el-icon :size="11"><Edit/></el-icon></span>
              <span class="group-item__action group-item__action--delete" @click.stop="handleDeleteGroup(group)"><svg
                  xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                  stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="11" height="11"><line x1="18"
                                                                                                               y1="6"
                                                                                                               x2="6"
                                                                                                               y2="18"/><line
                  x1="6" y1="6" x2="18" y2="18"/></svg></span>
            </div>
          </div>
          <div v-if="loadingGroups" class="loading-more">加载中...</div>
        </div>
      </div>

      <div class="resize-handle" @mousedown="startResize"></div>

      <div class="content-panel">
        <div class="search">
          <el-select v-model="searchType" placeholder="搜索方式" class="search__select">
            <el-option label="按名称" value="name" />
            <el-option label="按编号" value="code" />
          </el-select>
          <el-input
            v-model="searchKeyword"
            :placeholder="searchType === 'name' ? '搜索名称' : '搜索编号'"
            class="search__input"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search/></el-icon>
            </template>
          </el-input>
          <el-select v-model="searchStatus" placeholder="状态" clearable class="search__select">
            <el-option label="监测中" value="MONITORING" />
            <el-option label="停测中" value="PAUSED" />
            <el-option label="已完结" value="COMPLETED" />
          </el-select>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>

        <div class="table-wrap">
          <div class="table-wrap__scroll">
            <el-table
              :data="sort.sorted(tableData)"
              border
              stripe
              v-loading="loading"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="55" align="center" />
              <el-table-column prop="code" label="编号" width="100" align="center">
                <template #header>
                  <TableSortHeader label="编号" :order="sortInfo.order && sortInfo.field === 'code' ? sortInfo.order : ''" @toggle="sort.toggle('code')" />
                </template>
              </el-table-column>
              <el-table-column prop="name" label="名称" min-width="140" align="center">
                <template #header>
                  <TableSortHeader label="名称" :order="sortInfo.order && sortInfo.field === 'name' ? sortInfo.order : ''" @toggle="sort.toggle('name')" />
                </template>
              </el-table-column>
              <el-table-column prop="statusName" label="状态" width="100" align="center">
                <template #default="{ row }">
                  <span :class="['status-badge', `status--${row.status.toLowerCase()}`]">
                    {{ row.statusName }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="分组" width="110" align="center">
                <template #header>
                  <TableSortHeader label="分组" :order="sortInfo.order && sortInfo.field === 'groupName' ? sortInfo.order : ''" @toggle="sort.toggle('groupName')" />
                </template>
                <template #default="{ row }">
                  <span>{{ row.groupName || '未分组' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="coordinates" label="中心坐标" min-width="160" align="center">
                <template #default="{ row }">
                  <span v-if="row.longitude && row.latitude">{{ row.longitude }}, {{ row.latitude }}</span>
                  <span v-else class="empty-text">-</span>
                </template>
              </el-table-column>
              <el-table-column label="设备数量" width="110" align="center">
                <template #default="{ row }">
                  <el-tooltip
                      :content="(row.deviceCount ?? 0) > 0 ? `管理 ${row.name} 的 ${row.deviceCount} 台绑定设备` : `为 ${row.name} 绑定设备`"
                      placement="top"
                  >
                    <span
                        class="sensor-count-cell"
                        :class="{
                        'is-active': (row.deviceCount ?? 0) > 0,
                        'is-zero': row.deviceCount === 0
                      }"
                        @click="handleBindDevice(row)"
                    >
                      <el-icon v-if="(row.deviceCount ?? 0) > 0" class="cell-icon"><Connection/></el-icon>
                      <span>{{ row.deviceCount || 0 }}</span>
                    </span>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column label="操作" min-width="200" fixed="right" align="center">
                <template #default="{ row }">
                  <div class="op-cell">
                    <el-button type="primary" text size="small" @click="handleViewAndOpen(row)">查看</el-button>
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

          <div class="table-wrap__pagination">
            <div class="pagination-stats">
              <span>隐患点 <strong>{{ statsTotal }}</strong></span>
              <span class="pagination-stats__sep">|</span>
              <span>监测中 <strong class="c-green">{{ statsMonitoring }}</strong></span>
              <span class="pagination-stats__sep">|</span>
              <span>关联设备 <strong class="c-amber">{{ statsDeviceTotal }}</strong></span>
              <span class="pagination-stats__sep">|</span>
              <span>分组 <strong class="c-purple">{{ statsGroupCount }}</strong></span>
            </div>
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
                        <span class="group-action-btn" @click.stop="handleEditGroupFromSelect(g)" title="修改"><el-icon
                            :size="14"><Edit/></el-icon></span>
                        <span class="group-action-btn delete-btn" @click.stop="handleDeleteGroupFromSelect(g)"
                              title="删除"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
                                                stroke="currentColor" stroke-width="2" stroke-linecap="round"
                                                stroke-linejoin="round" width="14" height="14"><line x1="18" y1="6"
                                                                                                     x2="6" y2="18"/><line
                            x1="6" y1="6" x2="18" y2="18"/></svg></span>
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
            <el-input-number v-model="formData.longitude" placeholder="经度" class="coord-input"/>
            <span class="coord-separator">,</span>
            <el-input-number v-model="formData.latitude" placeholder="纬度" class="coord-input"/>
            <el-button type="primary" size="small" @click="handleOpenMap">
              <el-icon>
                <Location/>
              </el-icon>
              地图设置
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
      width="900px"
      :close-on-click-modal="false"
      :before-close="beforeMapClose"
      destroy-on-close
    >
      <MapBoundaryEditor
        ref="mapEditorRef"
        :initial-value="formData.boundaryCoords"
        :initial-center="mapInitialCenter"
        height="500px"
        @done="onMapDone"
        @cancel="mapDialogVisible = false"
      />
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
              <MapBoundaryPreview
                  v-if="currentRow"
                  :initial-value="parsedBoundary"
                  :initial-center="previewCenter"
                  height="300px"
              />
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
          <div class="table-wrap">
            <div class="table-wrap__scroll">
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
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="告警配置" name="alarmConfig">
          <div class="alarm-config-view">
            <div class="config-section">
              <h3 class="section-title">告警判据</h3>
              <div class="table-wrap">
                <div class="table-wrap__scroll">
                  <el-table :data="alarmCriteriaList" border size="small">
                    <el-table-column prop="name" label="判据名称" width="150" align="center" />
                    <el-table-column prop="monitorTypeName" label="监测类型" width="150" align="center" />
                    <el-table-column prop="expression" label="表达式" min-width="250" align="center" />
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
              </div>
            </div>

            <div class="config-section">
              <h3 class="section-title">告警分发</h3>
              <div class="table-wrap">
                <div class="table-wrap__scroll">
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
                    <el-table-column prop="channel" label="通知渠道" min-width="150" align="center">
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
              <div v-else class="table-wrap">
                <div class="table-wrap__scroll">
                  <el-table :data="monitorDataList" border size="small">
                    <el-table-column prop="dataTime" label="时间" min-width="180" align="center" />
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
          <div class="table-wrap">
            <div class="table-wrap__scroll">
              <el-table :data="alarmCriteriaList" border size="small">
                <el-table-column prop="name" label="判据名称" width="140" align="center" />
                <el-table-column prop="monitorTypeName" label="监测类型" width="140" align="center" />
                <el-table-column prop="expression" label="表达式" min-width="240" align="center" />
                <el-table-column prop="alarmLevel" label="告警等级" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag :type="getAlarmLevelType(row.alarmLevel)" size="small">{{ row.alarmLevelText }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="isEnabled" label="状态" width="80" align="center">
                  <template #default="{ row }">
                    <el-switch v-model="row.isEnabled" @change="handleToggleAlarm(row)" size="small" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120" align="center">
                  <template #default="{ row }">
                    <div class="op-cell">
                      <el-button type="primary" text size="small" @click="handleEditAlarm(row)">编辑</el-button>
                      <el-button type="danger" text size="small" @click="handleDeleteAlarm(row)">删除</el-button>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </div>

        <div class="config-section">
          <h3 class="section-title">告警分发</h3>
          <div class="dispatch-toolbar">
            <el-button type="primary" size="small" @click="handleAddDispatchRule">
              <span class="btn-icon">+</span> 添加规则
            </el-button>
          </div>
          <div class="table-wrap">
            <div class="table-wrap__scroll">
              <el-table :data="dispatchRules" border size="small">
                <el-table-column prop="type" label="类型" width="120" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.type === 'alarm' ? 'danger' : 'warning'" size="small">
                      {{ row.type === 'alarm' ? '监测告警' : '设备离线通知' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="告警等级/关联设备" min-width="190">
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
                <el-table-column label="通知人员" min-width="140">
                  <template #default="{ row }">
                    <el-tag v-for="p in row.persons" :key="p" size="small" style="margin-right: 4px;">{{ p }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="channels" label="通知渠道" min-width="140">
                  <template #default="{ row }">
                    <span v-for="(c, idx) in row.channels" :key="c">
                      {{ getChannelLabel(c) }}{{ idx < row.channels.length - 1 ? '、' : '' }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="80" align="center">
                  <template #default="{ row }">
                    <el-switch v-model="row.status" @change="handleToggleDispatchStatus(row)" size="small" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120" align="center">
                  <template #default="{ row }">
                    <div class="op-cell">
                      <el-button type="primary" text size="small" @click="handleEditDispatchRule(row)">编辑</el-button>
                      <el-button type="danger" text size="small" @click="handleDeleteDispatchRule(row)">删除</el-button>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
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
          <div class="transfer-tree device-tree">
            <el-tree
                ref="leftTreeRef"
              :data="leftDeviceTree"
                :props="{ label: 'label', children: 'children', disabled: 'disabled' }"
                node-key="key"
              :filter-node-method="filterLeftNode"
            >
              <template #default="{ node, data }">
                <span class="tree-node">
                  <span class="tree-node__check">
                    <el-checkbox
                        v-if="!data.disabled"
                        :model-value="isLeftNodeChecked(data)"
                        @update:model-value="(val: boolean) => toggleLeftNode(data, val)"
                        @click.stop
                    />
                  </span>
                  <img v-if="data.icon" :src="data.icon" class="node-icon" />
                  <span>{{ node.label }}</span>
                  <span v-if="data.children?.length" class="bind-count">({{ data.children.length }}传感器)</span>
                  <el-tag v-if="data.status" :type="getStatusTagType(data.status)" size="mini" class="status-tag">{{ data.statusText }}</el-tag>
                </span>
              </template>
            </el-tree>
          </div>
        </div>

        <div class="transfer-actions">
          <el-tooltip content="将左侧选中的设备加入待绑定列表" placement="left">
            <el-button
                type="primary"
                size="small"
                :disabled="bindLoading"
                @click="transferToRight"
            >
              <el-icon>
                <ArrowRight/>
              </el-icon>
              <span class="btn-label">选中</span>
            </el-button>
          </el-tooltip>
          <el-tooltip content="将左侧所有设备加入待绑定列表" placement="left">
            <el-button
                type="primary"
                size="small"
                plain
                :disabled="bindLoading"
                @click="transferAllToRight"
            >
              <el-icon>
                <DArrowRight/>
              </el-icon>
              <span class="btn-label">全部</span>
            </el-button>
          </el-tooltip>

          <div class="transfer-divider"/>

          <el-tooltip content="将右侧选中的设备移出待绑定列表" placement="right">
            <el-button
                type="warning"
                size="small"
                :disabled="bindLoading"
                @click="transferToLeft"
            >
              <span class="btn-label">选中</span>
              <el-icon>
                <ArrowLeft/>
              </el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="将右侧所有设备移出待绑定列表" placement="right">
            <el-button
                type="warning"
                size="small"
                plain
                :disabled="bindLoading"
                @click="transferAllToLeft"
            >
              <span class="btn-label">全部</span>
              <el-icon>
                <DArrowLeft/>
              </el-icon>
            </el-button>
          </el-tooltip>
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
          <div class="transfer-tree device-tree">
            <el-tree
                ref="rightTreeRef"
              :data="rightDeviceTree"
                :props="{ label: 'label', children: 'children', disabled: 'disabled' }"
                node-key="key"
              :filter-node-method="filterRightNode"
            >
              <template #default="{ node, data }">
                <span class="tree-node">
                  <span class="tree-node__check">
                    <el-checkbox
                        v-if="!data.disabled"
                        :model-value="isRightNodeChecked(data)"
                        @update:model-value="(val: boolean) => toggleRightNode(data, val)"
                        @click.stop
                    />
                  </span>
                  <img v-if="data.icon" :src="data.icon" class="node-icon" />
                  <span>{{ node.label }}</span>
                  <span v-if="data.children?.length" class="bind-count">({{ data.children.length }}传感器)</span>
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
import {computed, onMounted, onUnmounted, ref, type Ref, watch} from 'vue'
import {ElMessage} from 'element-plus'
import TableSortHeader from '@/components/TableSortHeader.vue'
import {useTableSort} from '@/composables/useTableSort'
import {
  ArrowLeft,
  ArrowRight,
  Connection,
  DArrowLeft,
  DArrowRight,
  Edit,
  Location,
  Search
} from '@element-plus/icons-vue'
import MapBoundaryEditor from '@/components/map/MapBoundaryEditor.vue'
import MapBoundaryPreview from '@/components/map/MapBoundaryPreview.vue'
import {type BoundaryCoords, deserialize, type LatLng} from '@/lib/boundaryCoords'
import VueApexCharts from 'vue3-apexcharts'
import {
  getAlarmLevelType,
  getChannelLabel,
  getStatusTagType,
  getStatusType,
  type HazardPointItem,
  useHazardPointCrud,
} from './composables/useHazardPointCrud'
import {type GroupItem, useHazardPointGroups} from './composables/useHazardPointGroups'
import {type DispatchRule, useHazardPointAlarm} from './composables/useHazardPointAlarm'
import {type BoundDevice, useHazardPointDeviceBind} from './composables/useHazardPointDeviceBind'
import {useHazardPointMonitor} from './composables/useHazardPointMonitor'

// ── Local refs shared between composables ──
const boundDevices = ref<BoundDevice[]>([])

// ── CRUD composable ──
const selectedGroupId = ref<string | null>(null)

const {
  searchKeyword,
  searchStatus,
  searchType,
  loading,
  refreshing,
  tableData,
  selectedRows,
  currentPage,
  pageSize,
  total,
  dialogVisible,
  dialogTitle: crudDialogTitle,
  isEdit,
  formRef,
  formData,
  formRules,
  currentRow,
  statsTotal,
  statsMonitoring,
  statsDeviceTotal,
  statsGroupCount: _crudStatsGroupCount,
  loadTableData,
  fetchDetail: fetchHazardPointDetail,
  handleSearch,
  handleReset,
  handleRefresh,
  handleSizeChange,
  handlePageChange,
  handleSelectionChange,
  handleAdd,
  handleEdit,
  handleSubmit,
  handleView,
  handleMoreCommand,
  handleDelete,
  handleTogglePause,
  handleComplete,
  handleBatchDelete,
  handleBatchPause,
  handleBatchResume,
  handleBatchComplete,
  handleExport: rawHandleExport,
} = useHazardPointCrud({
  groupId: selectedGroupId,
  onRefreshGroups: () => loadGroupList(),
  onExtraCommand: (command: string, row: HazardPointItem) => {
    if (command === 'bindDevice') {
      handleBindDevice(row);
      return true
    }
    if (command === 'alarmConfig') {
      handleConfigAlarm(row);
      return true
    }
    return false
  },
})

// 模板中引用 dialogTitle 时用 crudDialogTitle 别名
const dialogTitle = crudDialogTitle

// 导出按钮在模板中绑定
const handleExportHazardPoints = rawHandleExport

// ── Groups composable ──
const {
  groupList,
  displayGroupList,
  loadingGroups,
  groupPanelWidth,
  groupDialogVisible,
  groupDialogTitle: groupDlgTitle,
  isEditGroup,
  groupFormRef,
  groupFormData,
  groupFormRules,
  groupOptions,
  statsGroupCount: groupStatsGroupCount,
  groupFilterName,
  loadGroupList,
  loadGroupPage,
  handleGroupListScroll,
  startResize,
  handleAddGroup,
  handleAddGroupFromSelect,
  handleEditGroupFromSelect,
  handleDeleteGroupFromSelect,
  handleEditGroup,
  handleDeleteGroup,
  handleGroupSubmit,
} = useHazardPointGroups({
  total, // from CRUD composable
})

// 表格排序
const sort = useTableSort()
const sortInfo = sort.sortInfo

// Alias for template
const groupDialogTitle = groupDlgTitle

const statsGroupCount = groupStatsGroupCount

// ── Remaining local state ──
const activeTab = ref('basic')

// ── Detail map preview data (传入 MapBoundaryPreview) ──
const parsedBoundary = computed(() => {
  if (!currentRow.value) return null
  return deserialize((currentRow.value as any).boundaryCoords)
})

const previewCenter = computed<LatLng | null>(() => {
  const r = currentRow.value
  if (!r || r.latitude == null || r.longitude == null) return null
  return {lat: r.latitude, lng: r.longitude}
})

// ── Monitor data composable ──
const {
  dataDisplayMode,
  monitorDataList,
  chartSeriesData,
  chartOptions,
  latestDataList,
  monitorSensors,
  monitorAttrs,
  dataFilter,
  initLatestData,
  onDataDeviceChange,
  onDataSensorChange,
  handleQueryData,
  handleImportData,
  handleExportData,
} = useHazardPointMonitor({
  currentRow,
  activeTab,
})

const mapDialogVisible = ref(false)
const mapEditorRef = ref<InstanceType<typeof MapBoundaryEditor> | null>(null)
const mapInitialCenter = computed<LatLng>(() => ({
  lat: formData.latitude,
  lng: formData.longitude
}))

const detailDialogVisible = ref(false)

// ── Device bind composable ──
const {
  bindDeviceDialogVisible,
  bindLoading,
  leftSearchText,
  rightSearchText,
  leftDeviceTree,
  rightDeviceTree,
  leftTreeRef,
  rightTreeRef,
  filterLeftNode,
  filterRightNode,
  isLeftNodeChecked,
  isRightNodeChecked,
  toggleLeftNode,
  toggleRightNode,
  handleSearchUnboundDevices,
  initBoundDevices,
  handleBindDevice,
  transferToRight,
  transferToLeft,
  transferAllToRight,
  transferAllToLeft,
  handleBindDeviceSubmit,
} = useHazardPointDeviceBind({
  currentRow,
  boundDevices,
  onSaved: () => loadTableData(),
})

// ── Alarm composable ──
const {
  alarmConfigDialogVisible,
  alarmDialogVisible,
  isEditAlarm,
  alarmFormRef,
  alarmFormData,
  alarmFormRules,
  alarmCriteriaList,
  currentEditingAlarmLevel,
  monitorTypeList,
  filteredMonitorContent,
  dispatchRules,
  dispatchDialogVisible,
  isEditDispatch,
  dispatchFormRef,
  dispatchFormData,
  dispatchFormRules,
  userList,
  initAlarmCriteria,
  initDispatchRules,
  handleConfigAlarm,
  handleAddAlarmCriteria,
  handleEditAlarm,
  handleAlarmDeviceChange,
  handleMonitorTypeChange,
  handleMonitorContentChange,
  insertExpression,
  handleAlarmSubmit,
  handleToggleAlarm,
  handleDeleteAlarm,
  handleAddDispatchRule,
  handleEditDispatchRule,
  handleDispatchSubmit,
  handleDeleteDispatchRule,
} = useHazardPointAlarm({
  currentRow,
  boundDevices: boundDevices as Ref<{ deviceId: string; deviceName: string }[]>,
})

const handleToggleDispatchStatus = (row: DispatchRule) => {
  ElMessage.success(`规则${row.status === 1 ? '启用' : '禁用'}成功`)
}

// ==================== 选择分组 ====================
// 用途：点击左侧分组，触发筛选
const handleSelectGroup = (group: GroupItem) => {
  selectedGroupId.value = group.id === 'all' ? null : group.id
  handleSearch()
}

// 点击操作列"查看"时加载详情并打开详情弹窗（额外处理：加载关联数据+地图）
const handleViewAndOpen = async (row: HazardPointItem) => {
  await handleView(row)
  activeTab.value = 'basic'
  initBoundDevices(row.id)
  initAlarmCriteria(row.id)
  initDispatchRules(row.id)
  initLatestData(row.id)
  detailDialogVisible.value = true
  // 地图由 <MapBoundaryPreview> 组件自管理生命周期,无需手动初始化
}

const handleOpenMap = () => {
  mapDialogVisible.value = true
}

const beforeMapClose = (done: () => void) => {
  // 无取消按钮，直接关闭；若有 dirty 检测需求可在此处加
  done()
}

const onMapDone = (value: BoundaryCoords, center: LatLng | null) => {
  formData.boundaryCoords = value
  if (center) {
    formData.longitude = center.lng
    formData.latitude = center.lat
  }
  mapDialogVisible.value = false
}

// ── Old Leaflet map code replaced by <MapBoundaryEditor> component ──

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
/* ========== 页面覆盖 ========== */
.hazard-point-page {
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  height: 100%;
}

/* ========== 主体：左分组 + 右内容 ========== */
.page-body {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* ========== 左侧分组面板 ========== */
.group-panel {
  background: #fafafa;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
  border-radius: 8px 0 0 8px;
}

.group-panel__header {
  padding: 12px 15px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.group-panel__title {
  font-size: 12px;
  color: #909399;
}

.group-panel__search {
  padding: 8px 12px;
  border-bottom: 1px solid #e8e8e8;
}

.group-panel__list {
  flex: 1;
  overflow-y: auto;
  padding: 6px;
}

/* ---------- 分组项 ---------- */
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
  background: #e6f7ff;
}

.group-item--active {
  background: #bae7ff;
  color: #1890ff;
  font-weight: 500;
}

.group-item--active::before {
  background: #1890ff;
  box-shadow: 0 0 0 3px rgba(24, 144, 255, 0.2);
}

.group-item--all .group-item__name {
  font-size: 14px;
  font-weight: 700;
}

.group-item--all::before {
  width: 8px;
  height: 8px;
}

.group-item__name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-item__count {
  font-size: 11px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 1px 7px;
  border-radius: 10px;
  font-weight: 500;
  flex-shrink: 0;
  transition: opacity 0.15s;
}

.group-item:hover .group-item__count {
  opacity: 0;
  pointer-events: none;
}

.group-item__actions {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  gap: 4px;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.15s;
}

.group-item:hover .group-item__actions {
  opacity: 1;
  pointer-events: auto;
}

.group-item__action {
  font-size: 11px;
  color: #94a3b8;
  padding: 2px 5px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.15s;
  background: #f1f5f9;
}

.group-item__action:hover {
  color: #1890ff;
  background: #e6f7ff;
}

.group-item__action--delete:hover {
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
  position: relative;
}

.resize-handle::after {
  content: '';
  width: 2px;
  height: 40px;
  background: #b0b0b0;
  border-radius: 1px;
  transition: background 0.2s, height 0.2s;
}

.resize-handle:hover {
  background: rgba(24, 144, 255, 0.08);
}

.resize-handle:hover::after {
  background: #1890ff;
  height: 56px;
}

/* ========== 右侧内容面板 ========== */
.content-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding-left: 0;
  min-width: 0;
}

/* 分页栏左侧统计数据 */
.pagination-stats {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #64748b;
  margin-right: auto;
}

.pagination-stats strong {
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
  margin-left: 2px;
}

.pagination-stats__sep {
  color: #e2e8f0;
}

.c-green { color: #10b981; }
.c-amber { color: #f59e0b; }
.c-purple { color: #6366f1; }

/* ========== 统计条 ========== */
/* ========== 分页激活态 ========== */
:deep(.el-pagination .el-pager li.is-active) {
  background: #3b82f6;
}
:deep(.el-input__wrapper) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

/* ========== 对话框内表单 ========== */
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
  align-items: center;
  gap: 8px;
}

.transfer-actions .el-button {
  width: 90px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.transfer-actions .btn-label {
  font-size: 12px;
}

.transfer-divider {
  width: 24px;
  height: 1px;
  background: #e4e7ed;
  margin: 2px 0;
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

/* 设备数量单元格（列表行内可点击徽标，与设备管理页保持一致） */
.sensor-count-cell {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 32px;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  color: #909399;
  background: #f4f4f5;
  border: 1px solid #e9e9eb;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.sensor-count-cell:hover {
  background: #ecf5ff;
  border-color: #1890ff;
  color: #1890ff;
}

.sensor-count-cell.is-active {
  color: #1890ff;
  background: #e6f7ff;
  border-color: #91d5ff;
}

.sensor-count-cell.is-active:hover {
  background: #1890ff;
  color: #fff;
  border-color: #1890ff;
}

.sensor-count-cell.is-zero {
  color: #909399;
}

.sensor-count-cell .cell-icon {
  font-size: 12px;
}
</style>

<style>
/* 设备绑定弹窗 — 传感器节点不再渲染复选框,只渲染纯文字
   .tree-node__check 占位保持设备行/传感器行的文字纵向对齐 */
.device-tree .tree-node__check {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  flex-shrink: 0;
}
</style>
