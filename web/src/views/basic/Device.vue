<!--设备管理-->
<template>
  <div class="page">
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">设备管理</h2>
        <span class="header__subtitle">监测设备全生命周期管理与传感器配置</span>
      </div>
      <div class="header__right">
        <el-button type="primary" @click="handleAdd">+ 新增</el-button>
        <el-button @click="handleExport">导出</el-button>
      </div>
    </div>

    <div class="search">
      <el-input
          v-model="searchKeyword"
          placeholder="搜索编号或名称"
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
      />
      <el-select v-model="searchStatus" placeholder="设备状态" clearable>
        <el-option label="正常" :value="1" />
        <el-option label="维修" :value="2" />
        <el-option label="停用" :value="3" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div class="table-wrap">
      <div class="table-wrap__scroll">
        <el-table
            :data="tableData"
            border
            stripe
            v-loading="loading"
        >
          <el-table-column label="图标" width="80" align="center">
            <template #default="{ row }">
              <img v-if="getDeviceIconPathGreen(row)" :src="getDeviceIconPathGreen(row)" class="table-icon" alt="icon"/>
              <span v-else class="empty-text">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="code" label="编号" width="130" align="center" />
          <el-table-column prop="name" label="名称" min-width="160" align="center" />
          <el-table-column label="传感器数量" width="110" align="center">
            <template #default="{ row }">
              <el-tooltip
                  :content="row.sensorCount != null ? `查看 ${row.name} 的传感器配置` : `为 ${row.name} 配置传感器`"
                  placement="top"
              >
                <span
                    class="sensor-count-cell"
                    :class="{
                    'is-active': row.sensorCount != null && row.sensorCount > 0,
                    'is-zero': row.sensorCount === 0
                  }"
                    @click="handleOpenSensorsFromList(row)"
                >
                  <el-icon v-if="row.sensorCount != null && row.sensorCount > 0" class="cell-icon"><Cpu/></el-icon>
                  <span v-if="row.sensorCount != null">{{ row.sensorCount }}</span>
                  <span v-else>—</span>
                </span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column prop="sn" label="SN" min-width="160" align="center">
            <template #default="{ row }">
              <span>{{ row.sn || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="authUsername" label="接入账号" width="120" align="center">
            <template #default="{ row }">
              <el-tooltip
                  v-if="row.authUsername"
                  :content="`查看 ${row.name} 的接入账号`"
                  placement="top"
              >
                <span class="link-cell" @click="handleOpenAccountFromList(row)">
                  <el-icon class="link-icon"><User/></el-icon>
                  <span>{{ row.authUsername }}</span>
                </span>
              </el-tooltip>
              <span v-else class="empty-text">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="statusName" label="设备状态" width="100" align="center">
            <template #default="{ row }">
              <el-tooltip :content="`对 ${row.name} 进行运维操作`" placement="top">
                <span class="status-cell" @click="handleOpenMaintenanceFromList(row)">
                  <el-tag :type="getStatusType(row.status)" effect="plain">{{ row.statusName }}</el-tag>
                </span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="在线状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.onlineStatus === 1 ? 'success' : 'info'" effect="plain">
                {{ row.onlineStatus === 1 ? '在线' : '离线' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastReportTime" label="最近上报" min-width="170" align="center">
            <template #default="{ row }">
              <span v-if="row.lastReportTime">{{ row.lastReportTime }}</span>
              <span v-else class="empty-text">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" min-width="170" align="center" />
          <el-table-column label="操作" width="200" fixed="right" align="center">
            <template #default="{ row }">
              <div class="op-cell">
                <el-button type="primary" text size="small" @click="handleViewLocal(row)">查看</el-button>
                <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
                <el-dropdown trigger="hover" @command="(cmd: string) => handleMoreCommand(cmd, row)">
                  <el-button type="primary" text size="small">更多</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="account">账号</el-dropdown-item>
                      <el-dropdown-item command="maintenance">运维</el-dropdown-item>
                      <el-dropdown-item command="sensors">传感器</el-dropdown-item>
                      <el-dropdown-item command="copy">复制</el-dropdown-item>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="800px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="编号" prop="code">
              <el-input v-model="formData.code" placeholder="请输入设备编号" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="名称" prop="name">
              <el-input v-model="formData.name" placeholder="请输入设备名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="设备SN" prop="sn">
              <el-input v-model="formData.sn" placeholder="请输入设备SN" :disabled="isView" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="厂商名称" prop="vendorName">
              <el-input v-model="formData.vendorName" placeholder="请输入厂商名称" :disabled="isView" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="设备类型" prop="deviceType">
              <el-select v-model="formData.deviceType" placeholder="请选择设备类型" :disabled="isView">
                <el-option label="单参数" :value="0" />
                <el-option label="多参数" :value="1" />
                <el-option label="本地组网" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="网络类型" prop="networkType">
              <el-select v-model="formData.networkType" placeholder="请选择网络类型" :disabled="isView">
                <el-option label="蜂窝" :value="0" />
                <el-option label="NB-Iot" :value="1" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="接入协议" prop="protocolType">
              <el-select v-model="formData.protocolType" placeholder="请选择接入协议" :disabled="isView">
                <el-option label="MQTT" value="MQTT" />
                <el-option label="HTTP" value="HTTP" />
                <el-option label="COAP" value="COAP" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="图标" prop="icon">
              <div class="device-icon-selector" @click="!isView && handleSelectDeviceIcon()">
                <img v-if="formData.icon && getDeviceIconPathGreen(formData)" :src="getDeviceIconPathGreen(formData)" class="device-icon-img" alt="icon"/>
                <div v-else class="device-icon-placeholder">
                  <el-icon :size="20"><Plus /></el-icon>
                </div>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="关联隐患点">
              <el-select
                v-model="formData.boundHazardPointId"
                placeholder="选择隐患点"
                clearable
                filterable
                :disabled="isView"
                @change="onHpChange"
              >
                <el-option
                  v-for="hp in hazardPointList"
                  :key="hp.id"
                  :label="hp.name"
                  :value="Number(hp.id)"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="安装位置">
              <div class="install-location-wrap">
                <el-input
                    v-model="locationText"
                    size="small"
                    :disabled="isView"
                    class="location-input"
                    @blur="onLocationBlur"
                />
                <el-button
                    size="small"
                    :disabled="isView"
                    class="map-pick-btn"
                    @click="openMapPicker"
                    title="在地图上获取坐标"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                       stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="15" height="15">
                    <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                    <circle cx="12" cy="10" r="3"/>
                  </svg>
                </el-button>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer v-if="!isView">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
      <template #footer v-else>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
        v-model="authDialogVisible"
        :title="`设备账号[${currentAuthDevice?.name || ''}]`"
        width="640px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <el-descriptions :column="2" border v-if="authAccount">
        <el-descriptions-item label="设备编号">{{ currentAuthDevice?.code || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ currentAuthDevice?.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ authAccount.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="密码">{{ authAccount.password || '-' }}</el-descriptions-item>
        <el-descriptions-item label="账号状态">
          <el-tag :type="authAccount.authStatus === 1 ? 'success' : 'danger'" size="small">
            {{ authAccount.authStatus === 1 ? '有效' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ authAccount.registeredAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近鉴权">{{ authAccount.lastAuthTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="鉴权IP">{{ authAccount.lastAuthIp || '-' }}</el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="authDialogVisible = false">关闭</el-button>
        <el-button
          :type="authAccount?.authStatus === 1 ? 'warning' : 'success'"
          @click="handleToggleAuthStatus(currentAuthDevice)"
          :loading="authStatusLoading"
        >
          {{ authAccount?.authStatus === 1 ? '禁用账号' : '启用账号' }}
        </el-button>
        <el-button type="primary" @click="handleResetPassword" :loading="authResetLoading">重置密码</el-button>
      </template>
    </el-dialog>

    <!-- 维修状态弹窗 -->
    <el-dialog v-model="maintenanceDialogVisible" title="维修状态操作" width="520px" :close-on-click-modal="false"
               destroy-on-close>
      <el-form ref="maintenanceFormRef" :model="maintenanceForm" :rules="maintenanceFormRules" label-width="80px">
        <el-form-item label="设备">{{ maintenanceDeviceName }}</el-form-item>
        <el-form-item label="操作类型" prop="operationType">
          <el-select v-model="maintenanceForm.operationType" placeholder="请选择" style="width:100%">
            <el-option v-for="opt in availableOperationTypes" :key="opt.value" :label="opt.label" :value="opt.value"/>
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="操作人" prop="operatorName">
              <el-input v-model="maintenanceForm.operatorName" placeholder="姓名"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="operatorPhone">
              <el-input v-model="maintenanceForm.operatorPhone" placeholder="电话"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="操作日期" prop="operationDate">
          <el-date-picker v-model="maintenanceForm.operationDate" type="datetime" placeholder="选择日期时间"
                          value-format="YYYY-MM-DD HH:mm:ss" style="width:100%"/>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="maintenanceForm.description" type="textarea" :rows="3" placeholder="操作原因或备注"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="maintenanceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleMaintenanceSubmit" :loading="maintenanceLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 设备详情组件 -->
    <DeviceDetail
        v-model:visible="detailDialogVisible"
        :device="currentRow"
        @view-on-map="openViewMapFromDetail"
    />

    <!-- 传感器配置弹窗 -->
    <el-dialog
        v-model="sensorDialogVisible"
        :title="`传感器配置[${currentSensorDevice?.name || ''}]`"
        width="900px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <div class="device-info-bar">
        <span class="info-label">设备编号:</span>
        <span class="info-value">{{ currentSensorDevice?.code }}</span>
        <span class="info-label">设备名称:</span>
        <span class="info-value">{{ currentSensorDevice?.name }}</span>
      </div>
      <div class="sensor-toolbar">
        <el-button type="primary" size="small" @click="handleAddSensor">
          <span class="btn-icon">+</span> 添加传感器
        </el-button>
      </div>
      <el-table :data="sensorTableData" border size="small" v-loading="sensorLoading">
        <el-table-column prop="sensorCode" label="传感器编号" width="150" align="center" />
        <el-table-column prop="sensorName" label="传感器名称" width="150" align="center" />
        <el-table-column prop="monitorTypeName" label="监测类型" width="180" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="属性配置" min-width="320" align="center">
          <template #default="{ row }">
            <div v-if="row.attrList?.length" class="attr-config-list">
              <div v-for="attr in row.attrList" :key="attr.id || attr.attrCode" class="attr-config-item">
                <span class="attr-name">{{ attr.attrName }}:</span>
                <span>{{ attr.initialValue ?? 0 }}</span>
                <span class="attr-unit">{{ attr.unit }}</span>
              </div>
            </div>
            <span v-else class="empty-text">暂无属性</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button type="text" size="small" @click="handleEditSensor(row)">编辑</el-button>
            <el-button type="text" size="small" class="danger-text" @click="handleDeleteSensor(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="sensorDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
        v-model="sensorFormDialogVisible"
        :title="sensorFormTitle"
        width="820px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <el-form ref="sensorFormRef" :model="sensorFormData" :rules="sensorFormRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="传感器编号" prop="sensorCode">
              <el-input
                  v-model="sensorFormData.sensorCode"
                  :placeholder="sensorCodePlaceholder"
                  :disabled="sensorFormMode === 'edit'"
                  @input="handleSensorCodeInput"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="传感器名称" prop="sensorName">
              <el-input v-model="sensorFormData.sensorName" placeholder="请输入传感器名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="监测类型" prop="monitorTypeId">
              <el-select
                  v-model="sensorFormData.monitorTypeId"
                  placeholder="请选择监测类型"
                  :disabled="sensorFormMode === 'edit'"
                  @change="handleMonitorTypeChange(sensorFormData)"
              >
                <el-option v-for="mt in monitorTypeList" :key="mt.id" :label="mt.name" :value="mt.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="sensorFormData.status" placeholder="请选择状态">
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">
          <span class="divider-title">属性配置</span>
        </el-divider>

        <el-table :data="sensorFormData.attrList" border size="small">
          <el-table-column prop="attrCode" label="属性编码" width="150" align="center" />
          <el-table-column prop="attrName" label="属性名称" width="150" align="center" />
          <el-table-column prop="unit" label="单位" width="100" align="center" />
          <el-table-column label="初始值" width="140" align="center">
            <template #default="{ row }">
              <el-input-number
                  v-model="row.initialValue"
                  :min="row.rangeMin"
                  :max="row.rangeMax"
                  controls-position="right"
                  size="small"
                  style="width: 110px"
              />
            </template>
          </el-table-column>
          <el-table-column label="量程范围" width="180" align="center">
            <template #default="{ row }">
              {{ row.rangeMin }} ~ {{ row.rangeMax }}
            </template>
          </el-table-column>
          <el-table-column prop="unit" label="单位" width="80" align="center" />
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button type="danger" text size="small" @click="handleDeleteAttr($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>

      <template #footer>
        <el-button @click="sensorFormDialogVisible = false">取消</el-button>
        <el-button
            type="primary"
            :loading="sensorFormSubmitLoading"
            :disabled="sensorFormMode === 'add' && sensorFormData.monitorTypeId == null"
            @click="handleSensorSubmit"
        >确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 设备图标选择弹窗 -->
    <el-dialog v-model="deviceIconDialogVisible" title="选择设备图标" width="750px">
      <div class="icon-grid">
        <div
            v-for="item in deviceIconList"
            :key="item.code"
            class="icon-item"
            @click="handleDeviceIconSelect(item)"
        >
          <img :src="item.path" class="icon-select-img" :alt="item.name" />
          <span class="icon-name">{{ item.name }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="deviceIconDialogVisible = false">取消</el-button>
      </template>
    </el-dialog>

    <!-- 地图坐标选择弹窗(公共组件) -->
    <MapLocationPickerDialog
        v-model="mapDialogVisible"
        :initial-point="mapInitialPoint"
        :show-hp-overlay="true"
        :hazard-point-list="hazardPointList"
        :initial-hp-id="mapInitialHpId"
        :readonly="mapViewOnly"
        :title="mapViewOnly ? '查看安装位置' : '在地图上选择安装位置'"
        @confirm="onMapConfirm"
    />

    <!-- 复制设备弹窗 -->
    <el-dialog
        v-model="copyDialogVisible"
        title="复制设备"
        width="480px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <el-form ref="copyFormRef" :model="copyFormData" :rules="copyFormRules" label-width="80px">
        <el-form-item label="设备编号" prop="code">
          <el-input v-model="copyFormData.code" placeholder="请输入新设备编号" />
        </el-form-item>
        <el-form-item label="设备名称" prop="name">
          <el-input v-model="copyFormData.name" placeholder="请输入新设备名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="copyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCopyConfirm" :loading="copySubmitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'
import {Cpu, User, Plus} from '@element-plus/icons-vue'
import request from '@/utils/request'
import DeviceDetail from './components/DeviceDetail.vue'
import {showRequestErrorMessage} from '@/utils/errorHandler'
import MapLocationPickerDialog from '@/components/map/MapLocationPickerDialog.vue'
import {type LatLng} from '@/lib/boundaryCoords'
import {changeDeviceAuthStatus, type DeviceAuthAccount, getDeviceAuthAccount, resetDevicePassword} from '@/api/device'
import {getMonitorTypeListWithContents} from '@/api/monitorType'
import {getHazardPointPage} from '@/api/hazardPoint'
import {
  createSensor,
  deleteSensor,
  deleteSensorAttribute,
  getDeviceSensors,
  getNextSensorCode,
  getSensorDetail,
  type SensorItem,
  updateSensor
} from '@/api/sensor'
import {getIconList} from '@/constants/monitorIcons'
import {getDeviceIconPathGreen} from '@/utils/deviceIcon'
import {type DeviceItem, useDeviceCrud} from './composables/useDeviceCrud'

const {
  searchKeyword, searchStatus,
  loading, submitLoading, tableData, currentPage, pageSize, total,
  dialogVisible, dialogTitle, isEdit, isView, formRef, formData, formRules,
  detailDialogVisible, currentRow,
  getStatusType, nowString,
  loadTableData, fetchDetail,
  handleSearch, handleReset, handleSizeChange, handlePageChange,
  handleAdd, handleEdit, handleView, handleSubmit, handleDelete, handleCopyOpen, handleCopyConfirm, handleExport,
  copyDialogVisible, copyFormRef, copyFormData, copyFormRules, copySubmitLoading,
} = useDeviceCrud()

const route = useRoute()
const router = useRouter()

const deviceIconList = getIconList()

interface SensorAttrItem {
  id?: number
  monitorContentId?: number
  attrCode: string
  attrName: string
  initialValue: number
  unit: string
  rangeMin: number
  rangeMax: number
  icon?: string
}

interface SensorFormModel {
  id?: number
  sensorCode: string
  sensorName: string
  monitorTypeId: number | null
  monitorTypeName: string
  status: number
  attrList: SensorAttrItem[]
}

interface MonitorTypeItem {
  id: number
  name: string
  modelAttrs: { attrCode: string; attrName: string; rangeMin: number; rangeMax: number; unit: string; icon?: string }[]
  contents?: {
    id: number;
    code: string;
    name: string;
    indicatorType: string;
    unit: string;
    rangeMin?: number | null;
    rangeMax?: number | null;
    icon?: string
  }[]
}

// ── Remaining local state ──
const sensorLoading = ref(false)
const sensorFormSubmitLoading = ref(false)
const authResetLoading = ref(false)
const authStatusLoading = ref(false)
const maintenanceDialogVisible = ref(false)
const maintenanceLoading = ref(false)
const maintenanceFormRef = ref()
const maintenanceDeviceId = ref<number | null>(null)
const maintenanceDeviceName = ref('')
const maintenanceDeviceStatus = ref<number>(1)
const maintenanceForm = reactive({
  operationType: null as number | null,
  operatorName: '',
  operatorPhone: '',
  operationDate: '',
  description: ''
})
const maintenanceFormRules = {
  operationType: [{required: true, message: '请选择操作类型', trigger: 'change'}],
  operatorName: [{required: true, message: '请输入操作人', trigger: 'blur'}],
  operationDate: [{required: true, message: '请选择操作日期', trigger: 'change'}]
}
const sensorList = ref<SensorItem[]>([])
const monitorTypeList = ref<MonitorTypeItem[]>([])
const onlineLogs = ref<any[]>([])
const maintenanceLogs = ref<any[]>([])

const loadOpsLogs = async (deviceId: number) => {
  try {
    const [online, maint] = await Promise.all([request.get(`/devices/${deviceId}/online-logs`), request.get(`/devices/${deviceId}/maintenance-logs`)])
    onlineLogs.value = online.data || [];
    maintenanceLogs.value = maint.data || []
  } catch {
    onlineLogs.value = [];
    maintenanceLogs.value = []
  }
}

const authDialogVisible = ref(false)
const currentAuthDevice = ref<DeviceItem | null>(null)
const authAccount = ref<DeviceAuthAccount | null>(null)

const sensorDialogVisible = ref(false)
const currentSensorDevice = ref<DeviceItem | null>(null)
const sensorTableData = ref<SensorItem[]>([])
const sensorFormDialogVisible = ref(false)
const sensorFormTitle = ref('新增传感器')
const sensorFormMode = ref<'add' | 'edit'>('add')
// sensorCode 自动占位：nextId 来自后端预测；manuallyEdited 标记用户是否手动改过
const nextSensorId = ref<number | null>(null)
const sensorCodeManuallyEdited = ref(false)

const deviceIconDialogVisible = ref(false)

// 地图坐标选择
const mapDialogVisible = ref(false)
const mapViewOnly = ref(false)
const viewPoint = ref<LatLng | null>(null)  // "查看"模式下当前行的位置
// 弹窗打开时的初始点:编辑时从 formData 派生,查看时从 viewPoint 派生
const mapInitialPoint = computed<LatLng | null>(() => {
  if (mapViewOnly.value) return viewPoint.value
  const lng = formData.longitude
  const lat = formData.latitude
  return lng != null && lat != null ? {lng, lat} : null
})

// 隐患点列表(供地图选点弹窗叠加预览用 + 编辑表单选择)
const hazardPointList = ref<{ id: string, name: string }[]>([])
const onHpChange = () => {
  // 切换隐患点后，同步更新地图弹窗的初始 HP
}
const loadHazardPointList = async () => {
  try {
    const resp: any = await getHazardPointPage({pageNum: 1, pageSize: 1000})
    if (resp.code === 200) {
      const rows: any[] = resp.data?.rows || []
      hazardPointList.value = rows.map((hp) => ({id: String(hp.id), name: hp.name}))
    }
  } catch {
    // 静默:列表为空不影响弹窗打开
  }
}

// 地图选点弹窗的初始叠加 HP：由 openMapPicker / openViewMap 在打开前显式设置
const mapInitialHpId = ref('')
const pickerLng = ref<number | null>(null)
const pickerLat = ref<number | null>(null)

// 安装位置的输入框文本（格式：经度,纬度）
const locationText = ref('')

const syncFormToText = () => {
  if (formData.longitude != null && formData.latitude != null) {
    locationText.value = `${formData.longitude}, ${formData.latitude}`
  } else {
    locationText.value = ''
  }
}

// 编辑弹窗打开时回显安装位置
watch(dialogVisible, (val) => {
  if (val) syncFormToText()
})

const onLocationBlur = () => {
  const raw = locationText.value.trim()
  if (!raw) {
    formData.longitude = null
    formData.latitude = null
    locationText.value = ''
    return
  }
  // 支持逗号或空格分隔
  const parts = raw.split(/[,，\s]+/)
  if (parts.length >= 2) {
    const lng = parseFloat(parts[0])
    const lat = parseFloat(parts[1])
    if (!isNaN(lng) && !isNaN(lat)) {
      formData.longitude = lng
      formData.latitude = lat
      locationText.value = `${lng}, ${lat}`
      return
    }
  }
  // 无法解析，清空
  formData.longitude = null
  formData.latitude = null
  locationText.value = ''
}
const openMapPicker = () => {
  onLocationBlur()
  mapViewOnly.value = false
  viewPoint.value = null
  mapInitialHpId.value = formData.boundHazardPointId != null ? String(formData.boundHazardPointId) : ''
  mapDialogVisible.value = true
}

const openViewMap = (row: DeviceItem) => {
  mapViewOnly.value = true
  viewPoint.value = row.longitude != null && row.latitude != null
      ? {lng: row.longitude, lat: row.latitude}
    : null
  mapInitialHpId.value = row.boundHazardPointId != null ? String(row.boundHazardPointId) : ''
  mapDialogVisible.value = true
}

const onMapConfirm = (point: LatLng) => {
  formData.longitude = point.lng
  formData.latitude = point.lat
  syncFormToText()
}

const clearLocation = () => {
  formData.longitude = null
  formData.latitude = null
  locationText.value = ''
}

const sensorFormRef = ref()

const sensorFormData = reactive<SensorFormModel>({
  sensorCode: '',
  sensorName: '',
  monitorTypeId: null,
  monitorTypeName: '',
  status: 1,
  attrList: []
})

const sensorFormRules = {
  sensorCode: [{ required: true, message: '请输入传感器编号', trigger: 'blur' }],
  sensorName: [{ required: true, message: '请输入传感器名称', trigger: 'blur' }],
  monitorTypeId: [{ required: true, message: '请选择监测类型', trigger: 'change' }],
  status: [{required: true, message: '请选择状态', trigger: 'change'}],
}

// ── Maintenance dialog ──
const availableOperationTypes = computed(() => {
  const status = maintenanceDeviceStatus.value
  const options: { label: string, value: number }[] = []
  if (status === 1) {
    options.push({label: '报修', value: 1}, {label: '停用', value: 3})
  } else if (status === 2) {
    options.push({label: '修复', value: 2}, {label: '停用', value: 3})
  } else if (status === 3) {
    options.push({label: '启用', value: 4})
  }
  return options
})


const openViewMapFromDetail = (device: DeviceItem) => {
  openViewMap(device)
}

const openAuthDialog = async (device: DeviceItem, account?: DeviceAuthAccount) => {
  currentAuthDevice.value = device
  authAccount.value = account || await getDeviceAuthAccount(Number(device.id))
  authDialogVisible.value = true
}

// 获取监测类型列表（用于传感器配置）
// 使用批量接口一次加载所有类型及其内容，避免逐条拉取详情的 N+1 请求
const loadMonitorTypeList = async () => {
  try {
    const allTypes = await getMonitorTypeListWithContents()
    const details = (allTypes || [])
        .filter((item: any) => item.status !== 0)
        .map((item: any) => ({
          id: Number(item.id),
          name: item.name,
          modelAttrs: (item.contents || []).map((content: any) => ({
            attrCode: content.code,
            attrName: content.name,
            rangeMin: content.rangeMin ?? 0,
            rangeMax: content.rangeMax ?? 999999,
            unit: content.unit || '',
            icon: content.icon || ''
          })),
          // 透传 contents 供前端读取 indicatorType
          contents: (item.contents || []).map((content: any) => ({
            id: Number(content.id),
            code: content.code,
            name: content.name,
            indicatorType: content.indicatorType || '',
            unit: content.unit || '',
            rangeMin: content.rangeMin ?? null,
            rangeMax: content.rangeMax ?? null,
            icon: content.icon || ''
          }))
        } as MonitorTypeItem))
    monitorTypeList.value = details
  } catch (error) {
    showRequestErrorMessage(error, '获取监测类型失败')
  }
}

const handleViewLocal = async (row: DeviceItem) => {
  await handleView(row)
  sensorList.value = (currentRow.value as any)?.sensors || []
  loadOpsLogs(Number(row.id))
}

const handleMoreCommand = (command: string, row: DeviceItem) => {
  const map: Record<string, () => void> = {
    account: () => handleViewAuth(row),
    maintenance: () => handleMaintenance(row),
    sensors: () => handleConfigSensors(row),
    copy: () => handleCopyOpen(row),
    delete: () => handleDelete(row),
  }
  map[command]?.()
}

const handleMaintenance = (row: DeviceItem) => {
  maintenanceDeviceId.value = row.id!
  maintenanceDeviceName.value = row.name
  maintenanceDeviceStatus.value = row.status
  maintenanceForm.operationType = null
  maintenanceForm.operatorName = ''
  maintenanceForm.operatorPhone = ''
  maintenanceForm.operationDate = nowString()
  maintenanceForm.description = ''
  maintenanceDialogVisible.value = true
}

const handleMaintenanceSubmit = () => {
  maintenanceFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    if (!maintenanceDeviceId.value) return
    const typeLabel = availableOperationTypes.value.find((o: {
      value: number,
      label: string
    }) => o.value === maintenanceForm.operationType)?.label || '操作'
    try {
      await ElMessageBox.confirm(
          `确认对设备【${maintenanceDeviceName.value}】执行"${typeLabel}"操作？`,
          '操作确认',
          {confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'}
      )
    } catch {
      return // 用户取消
    }
    maintenanceLoading.value = true
    try {
      await request.post(`/devices/${maintenanceDeviceId.value}/maintenance`, {
        operationType: maintenanceForm.operationType,
        operatorName: maintenanceForm.operatorName,
        operatorPhone: maintenanceForm.operatorPhone || undefined,
        operationDate: maintenanceForm.operationDate,
        description: maintenanceForm.description || undefined
      })
      ElMessage.success('操作成功')
      maintenanceDialogVisible.value = false
      await loadTableData()
    } catch (e: any) {
      showRequestErrorMessage(e, '操作失败')
    } finally {
      maintenanceLoading.value = false
    }
  })
}

const handleViewAuth = async (row: DeviceItem) => {
  try {
    await openAuthDialog(row)
  } catch (error) {
    showRequestErrorMessage(error, '获取设备账号失败')
  }
}

const handleToggleAuthStatus = async (row?: DeviceItem | null) => {
  if (!row?.id) {
    return
  }
  const currentStatus = row.authStatus ?? authAccount.value?.authStatus ?? 1
  const nextStatus = currentStatus === 1 ? 2 : 1
  const actionText = nextStatus === 1 ? '启用' : '禁用'
  try {
    const { value } = await ElMessageBox.prompt(`请输入${actionText}原因`, `${actionText}账号`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: `例如：现场${actionText}账号`,
      inputValue: `现场${actionText}账号`
    })
    authStatusLoading.value = true
    const account = await changeDeviceAuthStatus(Number(row.id), nextStatus, value || undefined)
    if (currentAuthDevice.value?.id === row.id) {
      authAccount.value = account
      currentAuthDevice.value = {
        ...currentAuthDevice.value,
        authStatus: account.authStatus
      }
    }
    const tableRow = tableData.value.find(item => item.id === row.id)
    if (tableRow) {
      tableRow.authStatus = account.authStatus
    }
    ElMessage.success(`${actionText}成功`)
  } catch (error: any) {
    if (error === 'cancel' || error?.action === 'cancel' || error?.action === 'close') {
      return
    }
    showRequestErrorMessage(error, `${actionText}账号失败`)
  } finally {
    authStatusLoading.value = false
  }
}

const handleResetPassword = async () => {
  if (!currentAuthDevice.value?.id) {
    return
  }
  try {
    const { value } = await ElMessageBox.prompt('请输入重置原因', '重置密码', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：现场更换设备',
      inputValue: '现场运维重置'
    })
    authResetLoading.value = true
    const result = await resetDevicePassword(Number(currentAuthDevice.value.id), value || undefined)
    authAccount.value = {
      deviceId: Number(currentAuthDevice.value.id),
      username: result.username,
      password: result.password,
      authStatus: authAccount.value?.authStatus ?? 1,
      registeredAt: authAccount.value?.registeredAt,
      lastAuthTime: authAccount.value?.lastAuthTime,
      lastAuthIp: authAccount.value?.lastAuthIp
    }
    ElMessage.success('密码已重置')
    await loadTableData()
  } catch (error: any) {
    if (error === 'cancel' || error?.action === 'cancel' || error?.action === 'close') {
      return
    }
    showRequestErrorMessage(error, '重置密码失败')
  } finally {
    authResetLoading.value = false
  }
}

const handleConfigSensors = async (row: DeviceItem) => {
  currentSensorDevice.value = row
  await loadSensorTableData(Number(row.id))
  sensorDialogVisible.value = true
}

// 列表行内“传感器数量”单元格快捷入口：直接打开该设备的传感器配置弹窗
const handleOpenSensorsFromList = async (row: DeviceItem) => {
  if (!row.id) return
  await handleConfigSensors(row)
}

// 列表行内“接入账号”单元格快捷入口：直接打开该设备的账号弹窗
const handleOpenAccountFromList = async (row: DeviceItem) => {
  if (!row.id) return
  await handleViewAuth(row)
}

// 列表行内“设备状态”单元格快捷入口：直接打开该设备的运维弹窗
const handleOpenMaintenanceFromList = (row: DeviceItem) => {
  if (!row.id) return
  handleMaintenance(row)
}

const loadSensorTableData = async (deviceId: number) => {
  sensorLoading.value = true
  try {
    sensorTableData.value = await getDeviceSensors(deviceId)
  } catch (error) {
    showRequestErrorMessage(error, '获取传感器列表失败')
    sensorTableData.value = []
  } finally {
    sensorLoading.value = false
  }
}

const resetSensorForm = () => {
  Object.assign(sensorFormData, {
    id: undefined,
    sensorCode: '',
    sensorName: '',
    monitorTypeId: null,
    monitorTypeName: '',
    status: 1,
    attrList: []
  })
  // 重置"用户是否手动改过 sensorCode"标记；切换监测类型时据此决定是否重算占位
  sensorCodeManuallyEdited.value = false
}

// input 提示文本
const sensorCodePlaceholder = computed(() => {
  if (sensorFormData.monitorTypeId == null) {
    return '请先选择监测类型'
  }
  if (!nextSensorId.value) {
    return '加载中…'
  }
  return `默认 {TYPE}_${nextSensorId.value}（本设备第 ${nextSensorId.value} 个传感器），可手动修改`
})

/**
 * 计算 sensorCode 占位值：{@code {indicator_type(大写)}_{nextId}}。
 * 取所选监测类型下第一个监测内容的 indicatorType（与 attrList[0] 一致）。
 */
const computeSensorCodePlaceholder = (): string => {
  if (sensorFormData.monitorTypeId == null || nextSensorId.value == null) {
    return ''
  }
  const mt = monitorTypeList.value.find(m => m.id === sensorFormData.monitorTypeId)
  const firstContent = mt?.contents?.[0]
  const indicatorType = (firstContent?.indicatorType || '').trim().toUpperCase()
  if (!indicatorType) {
    return ''
  }
  return `${indicatorType}_${nextSensorId.value}`
}

// 用户手动修改 sensorCode：标记一下，切换监测类型时不再覆盖
const handleSensorCodeInput = () => {
  sensorCodeManuallyEdited.value = true
}

// 拉取指定设备的下一个预测传感器序号；失败时让用户手动填
const fetchNextSensorCode = async (deviceId?: number) => {
  const id = deviceId ?? currentSensorDevice.value?.id
  if (id == null) {
    nextSensorId.value = null
    return
  }
  try {
    const {nextNo} = await getNextSensorCode(Number(id))
    nextSensorId.value = nextNo
  } catch {
    nextSensorId.value = null
  }
}

const handleAddSensor = async () => {
  sensorFormTitle.value = '新增传感器'
  sensorFormMode.value = 'add'
  resetSensorForm()
  sensorFormDialogVisible.value = true
  // 拉取当前设备的下一个预测序号（设备下未删除传感器数 +1）
  nextSensorId.value = null
  await fetchNextSensorCode()
  if (!sensorCodeManuallyEdited.value) {
    sensorFormData.sensorCode = computeSensorCodePlaceholder()
  }
}

const handleEditSensor = async (row: SensorItem) => {
  sensorFormTitle.value = '编辑传感器'
  sensorFormMode.value = 'edit'
  resetSensorForm()
  try {
    const detail = await getSensorDetail(Number(row.id))
    Object.assign(sensorFormData, {
      id: detail.id,
      sensorCode: detail.sensorCode,
      sensorName: detail.sensorName,
      monitorTypeId: detail.monitorTypeId,
      monitorTypeName: detail.monitorTypeName || '',
      status: detail.status,
      attrList: (detail.attrList || []).map((attr) => ({
        id: attr.id,
        attrCode: attr.attrCode,
        attrName: attr.attrName,
        initialValue: Number(attr.initialValue ?? 0),
        unit: attr.unit || '',
        rangeMin: Number(attr.rangeMin ?? 0),
        rangeMax: Number(attr.rangeMax ?? 999999),
        icon: attr.icon || ''
      }))
    })
    sensorFormDialogVisible.value = true
  } catch (error) {
    showRequestErrorMessage(error, '获取传感器详情失败')
  }
}

const handleDeleteSensor = (row: SensorItem) => {
  ElMessageBox.confirm(`确定要删除传感器"${row.sensorName}"吗?`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteSensor(Number(row.id))
      ElMessage.success('删除成功')
      await loadSensorTableData(Number(currentSensorDevice.value?.id))
    } catch (error) {
      showRequestErrorMessage(error, '删除传感器失败')
    }
  }).catch(() => {})
}

const handleMonitorTypeChange = (row: SensorFormModel) => {
  const mt = monitorTypeList.value.find(item => item.id === row.monitorTypeId)
  if (mt) {
    row.monitorTypeName = mt.name
    row.attrList = mt.modelAttrs.map(attr => ({
      attrCode: attr.attrCode,
      attrName: attr.attrName,
      initialValue: 0,
      unit: attr.unit,
      rangeMin: attr.rangeMin,
      rangeMax: attr.rangeMax,
      icon: attr.icon
    }))
    // 若用户未手动改过 sensorCode，则根据新监测类型重算占位
    if (!sensorCodeManuallyEdited.value) {
      row.sensorCode = computeSensorCodePlaceholder()
    }
  }
}

const validateSensorAttrs = () => {
  if (!sensorFormData.attrList.length) {
    ElMessage.warning('属性列表不能为空')
    return false
  }

  const codeSet = new Set<string>()
  for (let index = 0; index < sensorFormData.attrList.length; index += 1) {
    const attr = sensorFormData.attrList[index]
    attr.attrCode = attr.attrCode.trim()
    attr.attrName = attr.attrName.trim()
    if (!attr.attrCode) {
      ElMessage.warning(`第 ${index + 1} 行属性编码不能为空`)
      return false
    }
    if (!attr.attrName) {
      ElMessage.warning(`第 ${index + 1} 行属性名称不能为空`)
      return false
    }
    if (codeSet.has(attr.attrCode)) {
      ElMessage.warning(`属性编码 ${attr.attrCode} 重复`)
      return false
    }
    codeSet.add(attr.attrCode)
    if (attr.rangeMin > attr.rangeMax) {
      ElMessage.warning(`属性 ${attr.attrName} 的最小值不能大于最大值`)
      return false
    }
  }
  return true
}

// 对当前设备已加载的 sensorTableData 做 sensorCode 快速查重，命中后再依赖后端兜底做全局校验
const validateSensorCode = () => {
  if (sensorFormMode.value !== 'add') {
    return true
  }
  const code = sensorFormData.sensorCode?.trim()
  if (!code) {
    return true
  }
  const conflict = sensorTableData.value.find((s) => s.sensorCode === code)
  if (conflict) {
    ElMessage.warning(`传感器编号 ${code} 已被【${conflict.sensorName}】占用`)
    return false
  }
  return true
}

const buildSensorPayload = () => ({
  sensorCode: sensorFormData.sensorCode.trim(),
  sensorName: sensorFormData.sensorName.trim(),
  monitorTypeId: Number(sensorFormData.monitorTypeId),
  status: sensorFormData.status,
  attrList: sensorFormData.attrList.map((attr) => ({
    id: attr.id,
    attrCode: attr.attrCode.trim(),
    attrName: attr.attrName.trim(),
    initialValue: attr.initialValue,
    unit: attr.unit || undefined,
    rangeMin: attr.rangeMin,
    rangeMax: attr.rangeMax,
    icon: attr.icon || undefined
  }))
})

const handleDeleteAttr = async (index: number) => {
  const attr = sensorFormData.attrList[index]
  if (attr.id) {
    try {
      await deleteSensorAttribute(sensorFormData.id!, attr.id)
    } catch { /* ignore, frontend already removed */ }
  }
  sensorFormData.attrList.splice(index, 1)
}

const handleSensorSubmit = () => {
  sensorFormRef.value.validate(async (valid: boolean) => {
    if (!valid || !validateSensorAttrs() || !validateSensorCode()) {
      return
    }

    sensorFormSubmitLoading.value = true
    try {
      const payload = buildSensorPayload()
      if (sensorFormMode.value === 'add') {
        await createSensor(Number(currentSensorDevice.value?.id), payload)
        ElMessage.success('新增成功')
      } else if (sensorFormData.id) {
        await updateSensor(sensorFormData.id, {
          sensorName: payload.sensorName,
          status: payload.status,
          attrList: payload.attrList
        })
        ElMessage.success('修改成功')
      }
      sensorFormDialogVisible.value = false
      await loadSensorTableData(Number(currentSensorDevice.value?.id))
    } catch (error: any) {
      showRequestErrorMessage(error, '保存传感器失败')
    } finally {
      sensorFormSubmitLoading.value = false
    }
  })
}

const handleSelectDeviceIcon = () => {
  deviceIconDialogVisible.value = true
}

const handleDeviceIconSelect = (item: { code: string; name: string; icon: string; path: string }) => {
  formData.icon = item.icon
  formData.iconPath = item.path
  deviceIconDialogVisible.value = false
}

onMounted(async () => {
  await loadTableData()
  loadMonitorTypeList()
  loadHazardPointList()
  // 通知中心跳转携带 ?deviceId= 时，自动打开该设备详情
  const deviceId = route.query.deviceId
  if (deviceId) {
    const id = Number(deviceId)
    // 清除 query，避免刷新后再次弹出
    router.replace({ path: route.path, query: {} })
    if (!Number.isNaN(id)) {
      try {
        const detail = await fetchDetail(id)
        if (detail) {
          currentRow.value = detail
          detailDialogVisible.value = true
        }
      } catch { /* 设备不存在或无权查看，静默忽略 */ }
    }
  }
})
</script>

<style scoped>
.table-icon {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.divider-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
}

.sensor-toolbar {
  margin-bottom: 15px;
}

.device-info-bar {
  background: #f5f7fa;
  padding: 10px 15px;
  border-radius: 4px;
  margin-bottom: 15px;
}

.info-label {
  color: #909399;
  margin-right: 6px;
}

.info-value {
  color: #303133;
  font-weight: bold;
  margin-right: 20px;
}

.device-icon-selector {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 42px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.device-icon-selector:hover {
  border-color: #1890ff;
  background: #e6f7ff;
}

.device-icon-img {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.device-icon-placeholder {
  color: #909399;
  font-size: 12px;
}

.attr-config-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.attr-config-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.attr-name {
  color: #606266;
  white-space: nowrap;
}

.attr-unit {
  color: #909399;
}

.pwd-masked {
  font-family: monospace;
  letter-spacing: 2px;
}
.attr-item {
  font-size: 13px;
  color: #606266;
  padding: 2px 0;
}

.icon-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 10px 5px;
  max-height: 380px;
  overflow-y: auto;
  justify-content: flex-start;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 85px;
  padding: 8px 2px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.icon-item:hover {
  border-color: #1890ff;
  background: #e6f7ff;
}

.icon-select-img {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.icon-name {
  font-size: 12px;
  color: #606266;
  margin-top: 6px;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-descriptions) {
  margin-bottom: 20px;
}

/* 安装位置 */
.install-location-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
}

.install-location-wrap :deep(.el-input) {
  width: 100%;
}

.map-pick-btn {
  flex-shrink: 0;
}

/* 地图坐标选择器 */
.map-picker-container {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.map-picker-inner {
  width: 100%;
  height: 380px;
  border-radius: 6px 6px 0 0;
  overflow: hidden;
  border: 1px solid #e8e8e8;
  border-bottom: none;
}

.map-picker-info {
  background: #f5f7fa;
  border: 1px solid #e8e8e8;
  border-radius: 0 0 6px 6px;
  padding: 10px 14px;
}

.picker-coord {
  font-size: 13px;
  color: #303133;
}

.picker-hint {
  color: #909399;
  font-style: italic;
}

/* 传感器数量单元格（列表行内可点击徽标） */
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

/* 接入账号单元格（蓝色文字链接样式） */
.link-cell {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #1890ff;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
  padding: 2px 6px;
  border-radius: 4px;
}

.link-cell:hover {
  color: #096dd9;
  background: #e6f7ff;
  text-decoration: underline;
}

.link-cell .link-icon {
  font-size: 13px;
}

/* 设备状态单元格（点击整行打开运维弹窗，hover 微微高亮） */
.status-cell {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all 0.2s;
}

.status-cell:hover {
  background: #f5f7fa;
  transform: scale(1.05);
}

.status-cell:hover :deep(.el-tag) {
  filter: brightness(0.95);
}

.device-icon-selector {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.device-icon-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.device-icon-img {
  width: 28px;
  height: 28px;
  object-fit: contain;
}
</style>
