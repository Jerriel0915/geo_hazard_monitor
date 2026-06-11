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
        <el-option label="故障" :value="2" />
        <el-option label="维修" :value="3" />
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
              <img v-if="getDeviceIconPath(row)" :src="getDeviceIconPath(row)" class="table-icon" alt="icon"/>
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
                <el-button type="primary" text size="small" @click="handleView(row)">查看</el-button>
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
                <img v-if="getDeviceIconPath(formData)" :src="getDeviceIconPath(formData)" class="device-icon-img"
                     alt="icon"/>
                <span v-else class="device-icon-placeholder">点击选择图标</span>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="设备状态" prop="status">
              <el-select v-model="formData.status" placeholder="请选择设备状态" :disabled="isView">
                <el-option label="正常" :value="1" />
                <el-option label="故障" :value="2" />
                <el-option label="停用" :value="3" />
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

    <!-- 设备详情弹窗 -->
    <el-dialog
        v-model="detailDialogVisible"
        :title="`设备详情 — ${currentRow?.name || ''}`"
        width="960px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <el-tabs v-model="detailTab">
        <el-tab-pane label="设备详情" name="info">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="设备编号">{{ currentRow?.code }}</el-descriptions-item>
            <el-descriptions-item label="设备名称">{{ currentRow?.name }}</el-descriptions-item>
            <el-descriptions-item label="设备SN">{{ currentRow?.sn || '-' }}</el-descriptions-item>
            <el-descriptions-item label="接入协议">{{ currentRow?.protocolType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="注册来源">{{ currentRow?.registerSource || '-' }}</el-descriptions-item>
            <el-descriptions-item label="接入账号">{{ currentRow?.authUsername || '-' }}</el-descriptions-item>
            <el-descriptions-item label="接入密码">
              <template v-if="currentRow?.authPassword">
                <span class="pwd-masked">{{ detailPwdVisible ? currentRow.authPassword : '••••••••' }}</span>
                <el-button size="small" text type="primary" @click="detailPwdVisible = !detailPwdVisible">
                  {{ detailPwdVisible ? '隐藏' : '查看' }}
                </el-button>
                <el-button size="small" text type="primary" @click="copyPwd(currentRow.authPassword)">复制</el-button>
              </template>
              <span v-else>-</span>
            </el-descriptions-item>
            <el-descriptions-item label="安装位置">
              {{ formatCoord(currentRow?.longitude, currentRow?.latitude) }}
              <el-button v-if="currentRow?.longitude != null" size="small" text type="primary"
                         @click="openViewMap(currentRow)">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                     stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="14" height="14">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                  <circle cx="12" cy="10" r="3"/>
                </svg>
                查看
              </el-button>
            </el-descriptions-item>
            <el-descriptions-item label="设备状态">
              <el-tag :type="getStatusType(currentRow?.status || 0)" size="small">{{ currentRow?.statusName }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="在线状态">
              <el-tag :type="currentRow?.onlineStatus === 1 ? 'success' : 'info'" size="small">
                {{ currentRow?.onlineStatus === 1 ? '在线' : '离线' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="最近上报时间">{{ currentRow?.lastReportTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ currentRow?.createTime || '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">传感器列表</el-divider>
          <el-table :data="sensorList" border size="small" v-loading="sensorLoading">
            <el-table-column label="图标" width="60" align="center">
              <template #default="{ row }">
                <img v-if="getDeviceIconPath(row)" :src="getDeviceIconPath(row)" class="table-icon" alt="icon"/>
                <span v-else class="empty-text">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="sensorCode" label="传感器编号" width="150" align="center"/>
            <el-table-column prop="sensorName" label="传感器名称" width="150" align="center"/>
            <el-table-column prop="sensorNo" label="主题编号" width="120" align="center"/>
            <el-table-column prop="monitorTypeName" label="监测类型" width="150" align="center"/>
            <el-table-column label="属性配置" min-width="250" align="center">
              <template #default="{ row }">
                <div v-for="attr in row.attrList" :key="attr.attrCode" class="attr-item">
                  {{ attr.attrName }}: {{ attr.initialValue }}{{ attr.unit }}
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="运维记录" name="operation">
          <div class="ops-section">
            <div class="ops-section__title">运行状态变更</div>
            <el-table :data="onlineLogs" border size="small" max-height="400">
              <el-table-column prop="eventTime" label="时间" width="170"/>
              <el-table-column prop="eventType" label="类型" width="80">
                <template #default="{row}">
                  <el-tag :type="row.eventType==='ONLINE'?'success':'danger'" size="small">{{
                      row.eventType
                    }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="clientId" label="Client ID" min-width="160"/>
              <el-table-column prop="clientIp" label="IP" width="140"/>
              <el-table-column prop="reason" label="原因" min-width="120"/>
            </el-table>
          </div>
          <div class="ops-section">
            <div class="ops-section__title">维修记录</div>
            <el-table :data="maintenanceLogs" border size="small" max-height="400">
              <el-table-column label="操作" width="80">
                <template #default="{row}">
                  <el-tag :type="row.newStatus === 1 ? 'success' : row.newStatus === 2 ? 'danger' : 'info'"
                          size="small">
                    {{ row.statusText }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态变化" width="110">
                <template #default="{row}">{{ getStatusLabel(row.oldStatus) }}→{{
                    getStatusLabel(row.newStatus)
                  }}
                </template>
              </el-table-column>
              <el-table-column prop="operatorName" label="操作人" width="90"/>
              <el-table-column prop="operatorPhone" label="电话" width="120"/>
              <el-table-column prop="operationDate" label="操作日期" width="160"/>
              <el-table-column prop="createTime" label="记录时间" width="160"/>
              <el-table-column prop="description" label="描述" min-width="120"/>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

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
        <el-table-column prop="sensorNo" label="主题编号" width="120" align="center" />
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
                  placeholder="请输入传感器编号"
                  :disabled="sensorFormMode === 'edit'"
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
            <el-form-item label="主题编号" prop="sensorNo">
              <el-input
                  v-model="sensorFormData.sensorNo"
                  :placeholder="sensorNoPlaceholder"
                  :disabled="sensorFormData.monitorTypeId == null"
                  @input="handleSensorNoInput"
              />
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
    <el-dialog v-model="deviceIconDialogVisible" title="选择设备图标" width="600px">
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

    <!-- 地图坐标选择弹窗 -->
    <el-dialog
        v-model="mapDialogVisible"
        title="在地图上选择安装位置"
        width="680px"
        :close-on-click-modal="false"
        destroy-on-close
        @opened="initMapPicker"
    >
      <div class="map-picker-container">
        <div ref="mapPickerRef" class="map-picker-inner"></div>
        <div class="map-picker-info">
          <span v-if="pickerLng != null && pickerLat != null" class="picker-coord">
            已选坐标：经度 {{ pickerLng!.toFixed(6) }}，纬度 {{ pickerLat!.toFixed(6) }}
          </span>
          <span v-else class="picker-coord picker-hint">点击地图选择坐标</span>
        </div>
      </div>
      <template #footer>
        <el-button v-if="mapViewOnly" @click="mapDialogVisible = false">关闭</el-button>
        <template v-else>
          <el-button @click="mapDialogVisible = false">取消</el-button>
          <el-button type="primary" :disabled="pickerLng == null" @click="confirmMapPicker">确定</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, onMounted, reactive, ref} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {Cpu, User} from '@element-plus/icons-vue'
import request from '@/utils/request'
import {showRequestErrorMessage} from '@/utils/errorHandler'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import {
  changeDeviceAuthStatus,
  copyDevice as copyDeviceApi,
  createDevice as createDeviceApi,
  deleteDevice as deleteDeviceApi,
  type DeviceAuthAccount,
  type DeviceItem,
  type DevicePageParams,
  getDeviceAuthAccount,
  getDeviceDetail,
  getDevicePage,
  resetDevicePassword,
  updateDevice as updateDeviceApi
} from '@/api/device'
import {getMonitorTypeListWithContents} from '@/api/monitorType'
import {
  createSensor,
  deleteSensor,
  deleteSensorAttribute,
  getDeviceSensors,
  getNextSensorNo,
  getSensorDetail,
  type SensorItem,
  updateSensor
} from '@/api/sensor'
import {getIconList} from '@/constants/monitorIcons'
import {getDeviceIconPath} from '@/utils/deviceIcon'

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
  sensorNo: string
  sensorName: string
  monitorTypeId: number | null
  monitorTypeName: string
  status: number
  attrList: SensorAttrItem[]
}

interface MonitorTypeItem {
  id: number
  name: string
  modelAttrs: {
    attrCode: string
    attrName: string
    rangeMin: number
    rangeMax: number
    unit: string
    icon?: string
  }[]
  /**
   * 监测类型下的监测内容列表（含 indicatorType，用于生成 sensorNo 占位）。
   * 由 loadMonitorTypeList 从 getMonitorTypeListWithContents() 透传保留。
   */
  contents?: {
    id: number
    code: string
    name: string
    indicatorType: string
    unit: string
    rangeMin?: number | null
    rangeMax?: number | null
    icon?: string
  }[]
}

const loading = ref(false)
const refreshing = ref(false)
const submitLoading = ref(false)
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
const tableData = ref<DeviceItem[]>([])
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
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const searchStatus = ref<number | ''>('')

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const isView = ref(false)
const formRef = ref()

const detailDialogVisible = ref(false)
const detailPwdVisible = ref(false)
const currentRow = ref<DeviceItem | null>(null)
const detailTab = ref('info')
const onlineLogs = ref<any[]>([])
const maintenanceLogs = ref<any[]>([])
const loadOpsLogs = async (deviceId: number) => {
  try {
    const [online, maint] = await Promise.all([
      request.get(`/devices/${deviceId}/online-logs`),
      request.get(`/devices/${deviceId}/maintenance-logs`)
    ])
    onlineLogs.value = online.data || []
    maintenanceLogs.value = maint.data || []
  } catch {
    onlineLogs.value = [];
    maintenanceLogs.value = []
  }
}
const copyPwd = async (pwd: string) => {
  try {
    await navigator.clipboard.writeText(pwd);
    ElMessage.success('密码已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

const formatCoord = (lng?: number | null, lat?: number | null) => {
  if (lng == null || lat == null) return '-'
  return `${lng.toFixed(6)}, ${lat.toFixed(6)}`
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
// sensorNo 自动占位相关：nextId 来自后端预测；manuallyEdited 标记用户是否手动改过
const nextSensorId = ref<number | null>(null)
const sensorNoManuallyEdited = ref(false)

const deviceIconDialogVisible = ref(false)

// 地图坐标选择
const mapDialogVisible = ref(false)
const mapViewOnly = ref(false)
const mapPickerRef = ref<HTMLDivElement | null>(null)
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
let mapPickerInstance: L.Map | null = null
let mapPickerMarker: L.Marker | null = null
const TIANDITU_KEY = '8dda07d4649c77efd0537a0ff0a1df13'

const openMapPicker = () => {
  onLocationBlur()
  mapViewOnly.value = false
  pickerLng.value = formData.longitude
  pickerLat.value = formData.latitude
  mapDialogVisible.value = true
}

const openViewMap = (row: DeviceItem) => {
  mapViewOnly.value = true
  pickerLng.value = row.longitude ?? null
  pickerLat.value = row.latitude ?? null
  mapDialogVisible.value = true
}

const initMapPicker = () => {
  nextTick(() => {
    if (!mapPickerRef.value) return
    // 销毁旧地图实例（dialog destroy-on-close 后 DOM 已重建，旧实例无效）
    if (mapPickerInstance) {
      mapPickerInstance.remove()
      mapPickerInstance = null
    }
    mapPickerMarker = null

    const center: [number, number] = pickerLat.value != null && pickerLng.value != null
      ? [pickerLat.value, pickerLng.value]
      : [30.65, 104.10]

    mapPickerInstance = L.map(mapPickerRef.value, {
      center,
      zoom: pickerLat.value != null ? 15 : 12,
      zoomControl: true
    })

    L.tileLayer(
      `https://t0.tianditu.gov.cn/img_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=img&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${TIANDITU_KEY}`,
      { maxZoom: 18, minZoom: 3 }
    ).addTo(mapPickerInstance)

    L.tileLayer(
      `https://t0.tianditu.gov.cn/cia_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=cia&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${TIANDITU_KEY}`,
      { maxZoom: 18, minZoom: 3 }
    ).addTo(mapPickerInstance)

    mapPickerInstance.on('click', (e: L.LeafletMouseEvent) => {
      if (mapViewOnly.value) return
      const { lat, lng } = e.latlng
      pickerLng.value = lng
      pickerLat.value = lat
      if (!mapPickerInstance) return
      if (mapPickerMarker) {
        mapPickerMarker.setLatLng([lat, lng])
      } else {
        mapPickerMarker = L.marker([lat, lng], {
          icon: L.icon({
            iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
            iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
            shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
            iconSize: [25, 41],
            iconAnchor: [12, 41]
          })
        }).addTo(mapPickerInstance)
      }
    })

    // 如果已有坐标，放一个标记
    if (pickerLat.value != null && pickerLng.value != null) {
      mapPickerMarker = L.marker([pickerLat.value, pickerLng.value], {
        icon: L.icon({
          iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
          iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
          shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
          iconSize: [25, 41],
          iconAnchor: [12, 41]
        })
      }).addTo(mapPickerInstance)
    }
  })
}

const confirmMapPicker = () => {
  formData.longitude = pickerLng.value
  formData.latitude = pickerLat.value
  syncFormToText()
  mapDialogVisible.value = false
}

const clearLocation = () => {
  formData.longitude = null
  formData.latitude = null
  locationText.value = ''
}

const sensorFormRef = ref()

const formData = reactive<{
  id?: number
  code: string
  name: string
  sn: string
  deviceType: number | null
  networkType: number | null
  protocolType: string
  vendorName: string
  icon: string
  iconPath: string
  longitude: number | null
  latitude: number | null
  status: number
  sensorList: SensorItem[]
}>({
  code: '',
  name: '',
  sn: '',
  deviceType: 0,
  networkType: 0,
  protocolType: 'MQTT',
  vendorName: '',
  icon: '',
  iconPath: '',
  longitude: null,
  latitude: null,
  status: 1,
  sensorList: []
})

const sensorFormData = reactive<SensorFormModel>({
  sensorCode: '',
  sensorNo: '',
  sensorName: '',
  monitorTypeId: null,
  monitorTypeName: '',
  status: 1,
  attrList: []
})

const formRules = {
  code: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }]
}

const sensorFormRules = {
  sensorCode: [{ required: true, message: '请输入传感器编号', trigger: 'blur' }],
  sensorName: [{ required: true, message: '请输入传感器名称', trigger: 'blur' }],
  monitorTypeId: [{ required: true, message: '请选择监测类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const getStatusType = (status: number) => {
  const types: Record<number, string> = { 1: 'success', 2: 'danger', 3: 'info' }
  return types[status] || 'default'
}

const getStatusLabel = (status: number) => {
  const labels: Record<number, string> = {1: '正常', 2: '故障', 3: '停用'}
  return labels[status] || '未知'
}

// 根据设备当前状态计算可选的操作类型
const availableOperationTypes = computed(() => {
  const status = maintenanceDeviceStatus.value
  const options: { label: string, value: number }[] = []
  if (status === 1) {
    options.push({label: '报修', value: 1}, {label: '停用', value: 3})
  } else if (status === 2) {
    options.push({label: '修复', value: 2}, {label: '停用', value: 3})
  } else if (status === 3) {
    options.push({label: '恢复', value: 4})
  }
  return options
})

// 格式化当前时间
const nowString = () => {
  const d = new Date()
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

// ==================== API 请求 ====================

// 分页查询设备
const loadTableData = async () => {
  loading.value = true
  try {
    const params: DevicePageParams = {
      pageNum: currentPage.value,
      pageSize: pageSize.value
    }
    if (searchKeyword.value) {
      params.code = searchKeyword.value
    }
    if (searchStatus.value !== '') params.status = searchStatus.value
    const data = await getDevicePage(params)
    tableData.value = data.rows || []
    total.value = data.total || 0
  } catch (error) {
    showRequestErrorMessage(error, '加载设备列表失败')
  } finally {
    loading.value = false
  }
}

// 获取设备详情
const fetchDetail = async (id: number) => {
  loading.value = true
  try {
    return await getDeviceDetail(id)
  } catch (error) {
    showRequestErrorMessage(error, '获取设备详情失败')
    return null
  } finally {
    loading.value = false
  }
}

// 新增设备
const createDevice = async () => {
  submitLoading.value = true
  try {
    const result = await createDeviceApi({
      code: formData.code,
      name: formData.name,
      sn: formData.sn || undefined,
      deviceType: formData.deviceType,
      networkType: formData.networkType,
      protocolType: formData.protocolType,
      vendorName: formData.vendorName || undefined,
      icon: formData.icon,
      iconPath: formData.iconPath,
      longitude: formData.longitude,
      latitude: formData.latitude,
      status: formData.status
    })
    ElMessage.success('新增成功')
    dialogVisible.value = false
    await loadTableData()
    const row = tableData.value.find(item => item.id === result.id)
    await openAuthDialog(row || {
      id: result.id,
      code: formData.code,
      name: formData.name,
      status: formData.status
    }, {
      deviceId: result.id,
      username: result.username,
      password: result.password,
      authStatus: 1
    })
  } catch (error: any) {
    showRequestErrorMessage(error, '新增设备失败')
  } finally {
    submitLoading.value = false
  }
}

// 修改设备
const updateDevice = async () => {
  submitLoading.value = true
  try {
    await updateDeviceApi(Number(formData.id), {
      name: formData.name,
      sn: formData.sn || undefined,
      deviceType: formData.deviceType,
      networkType: formData.networkType,
      protocolType: formData.protocolType,
      vendorName: formData.vendorName || undefined,
      icon: formData.icon,
      iconPath: formData.iconPath,
      longitude: formData.longitude,
      latitude: formData.latitude,
      status: formData.status
    })
    ElMessage.success('修改成功')
    dialogVisible.value = false
    await loadTableData()
  } catch (error: any) {
    showRequestErrorMessage(error, '修改设备失败')
  } finally {
    submitLoading.value = false
  }
}

// 删除设备
const deleteDevice = async (id: number) => {
  try {
    await deleteDeviceApi(id)
    ElMessage.success('删除成功')
    await loadTableData()
  } catch (error) {
    showRequestErrorMessage(error, '删除设备失败')
  }
}

// 复制设备
const copyDevice = async (id: number) => {
  try {
    await copyDeviceApi(id)
    ElMessage.success('复制成功')
    await loadTableData()
  } catch (error) {
    showRequestErrorMessage(error, '复制设备失败')
  }
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
          // 透传 contents 供 sensorNo 占位生成读取 indicatorType
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

// ==================== 事件处理方法 ====================

const handleSearch = () => {
  currentPage.value = 1
  loadTableData()
}

const handleReset = () => {
  searchKeyword.value = ''
  searchStatus.value = ''
  currentPage.value = 1
  loadTableData()
}

// 刷新页面
const handleRefresh = async () => {
  refreshing.value = true
  try {
    await loadTableData()
    ElMessage.success('刷新成功')
  } catch (error) {
    showRequestErrorMessage(error, '刷新失败')
  } finally {
    refreshing.value = false
  }
}

const handleSizeChange = () => {
  loadTableData()
}

const handlePageChange = () => {
  loadTableData()
}

const handleAdd = () => {
  dialogTitle.value = '新增设备'
  isEdit.value = false
  isView.value = false
  Object.assign(formData, {
    id: undefined,
    code: '',
    name: '',
    sn: '',
    deviceType: 0,
    networkType: 0,
    protocolType: 'MQTT',
    vendorName: '',
    icon: '',
    iconPath: '',
    longitude: null,
    latitude: null,
    status: 1,
    sensorList: []
  })
  syncFormToText()
  dialogVisible.value = true
}

const handleEdit = async (row: DeviceItem) => {
  dialogTitle.value = '编辑设备'
  isEdit.value = true
  isView.value = false
  Object.assign(formData, {
    id: row.id,
    code: row.code,
    name: row.name,
    sn: row.sn || '',
    deviceType: row.deviceType ?? 0,
    networkType: row.networkType ?? 0,
    protocolType: row.protocolType || 'MQTT',
    vendorName: row.vendorName || '',
    icon: row.icon || '',
    iconPath: row.iconPath || '',
    longitude: row.longitude ?? null,
    latitude: row.latitude ?? null,
    status: row.status,
    sensorList: []
  })
  syncFormToText()
  dialogVisible.value = true
}

const handleView = async (row: DeviceItem) => {
  detailPwdVisible.value = false
  currentRow.value = row
  const detail = await fetchDetail(Number(row.id))
  if (detail) {
    currentRow.value = detail
    sensorList.value = detail.sensors || []
  }
  loadOpsLogs(Number(row.id))
  detailDialogVisible.value = true
}

const handleMoreCommand = (command: string, row: DeviceItem) => {
  const map: Record<string, () => void> = {
    account: () => handleViewAuth(row),
    maintenance: () => handleMaintenance(row),
    sensors: () => handleConfigSensors(row),
    copy: () => handleCopy(row),
    delete: () => handleDelete(row)
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

const handleDelete = (row: DeviceItem) => {
  ElMessageBox.confirm(`确定要删除设备"${row.name}"吗?`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteDevice(Number(row.id))
  }).catch(() => {})
}

const handleExport = () => {
  ElMessage.info('正在导出...')
  setTimeout(() => {
    ElMessage.success('导出成功')
  }, 1000)
}

// 对当前页 tableData 做 code / sn 快速查重，命中后再依赖后端兜底做全局校验
const validateDeviceIdentity = () => {
  const code = formData.code?.trim()
  const sn = formData.sn?.trim()
  const excludeId = formData.id
  if (code) {
    const conflict = tableData.value.find((d) => d.id !== excludeId && d.code === code)
    if (conflict) {
      ElMessage.warning(`设备编号 ${code} 已被【${conflict.name}】占用`)
      return false
    }
  }
  if (sn) {
    const conflict = tableData.value.find((d) => d.id !== excludeId && d.sn === sn)
    if (conflict) {
      ElMessage.warning(`设备 SN ${sn} 已被【${conflict.name}】占用`)
      return false
    }
  }
  return true
}

const handleSubmit = () => {
  formRef.value.validate((valid: boolean) => {
    if (valid && validateDeviceIdentity()) {
      if (formData.id) {
        updateDevice()
      } else {
        createDevice()
      }
    }
  })
}

const handleCopy = (row: DeviceItem) => {
  ElMessageBox.confirm(`确定要复制设备"${row.name}"吗?`, '复制确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    copyDevice(Number(row.id))
  }).catch(() => {})
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
    sensorNo: '',
    sensorName: '',
    monitorTypeId: null,
    monitorTypeName: '',
    status: 1,
    attrList: []
  })
  // 重置"用户是否手动改过 sensorNo"标记；切换监测类型时据此决定是否重算占位
  sensorNoManuallyEdited.value = false
}

// input 提示文本
const sensorNoPlaceholder = computed(() => {
  if (sensorFormData.monitorTypeId == null) {
    return '请先选择监测类型'
  }
  if (!nextSensorId.value) {
    return '加载中…'
  }
  return `默认 {TYPE}_${nextSensorId.value}（本设备第 ${nextSensorId.value} 个传感器），可手动修改`
})

/**
 * 计算 sensorNo 占位值：{@code {indicator_type(大写)}_{nextId}}。
 * 取所选监测类型下第一个监测内容的 indicatorType（与 attrList[0] 一致）。
 */
const computeSensorNoPlaceholder = (): string => {
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

// 拉取指定设备的下一个预测传感器序号；失败时让用户手动填
const fetchNextSensorNo = async (deviceId?: number) => {
  const id = deviceId ?? currentSensorDevice.value?.id
  if (id == null) {
    nextSensorId.value = null
    return
  }
  try {
    const {nextNo} = await getNextSensorNo(Number(id))
    nextSensorId.value = nextNo
  } catch {
    nextSensorId.value = null
  }
}

// 用户手动修改 sensorNo：标记一下，切换监测类型时不再覆盖
const handleSensorNoInput = () => {
  sensorNoManuallyEdited.value = true
}

const handleAddSensor = async () => {
  sensorFormTitle.value = '新增传感器'
  sensorFormMode.value = 'add'
  resetSensorForm()
  sensorFormDialogVisible.value = true
  // 拉取当前设备的下一个预测序号（设备下未删除传感器数 +1）
  nextSensorId.value = null
  await fetchNextSensorNo()
  if (!sensorNoManuallyEdited.value) {
    sensorFormData.sensorNo = computeSensorNoPlaceholder()
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
      sensorNo: detail.sensorNo || '',
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
    // 若用户未手动改过 sensorNo，则根据新监测类型重算占位
    if (!sensorNoManuallyEdited.value) {
      row.sensorNo = computeSensorNoPlaceholder()
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

/**
 * 校验主题编号（sensorNo）在当前设备的已加载传感器列表中是否重复。
 * 镜像 validateSensorCode。空值不校验（由后端兜底生成）。
 */
const validateSensorNo = (): boolean => {
  if (sensorFormMode.value !== 'add') {
    return true
  }
  const no = sensorFormData.sensorNo?.trim()
  if (!no) {
    return true
  }
  const conflict = sensorTableData.value.find((s) => s.sensorNo === no)
  if (conflict) {
    ElMessage.warning(`主题编号 ${no} 已被【${conflict.sensorName}】占用`)
    return false
  }
  return true
}

const buildSensorPayload = () => ({
  sensorCode: sensorFormData.sensorCode.trim(),
  sensorNo: sensorFormData.sensorNo.trim() || undefined,
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
    if (!valid || !validateSensorAttrs() || !validateSensorCode() || !validateSensorNo()) {
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

onMounted(() => {
  loadTableData()
  loadMonitorTypeList()
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
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 15px;
  padding: 10px;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 10px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  min-width: 0;
  overflow: hidden;
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

/* 运维记录 - 上下线/维修 两块上下排列 */
.ops-section {
  margin-bottom: 18px;
}

.ops-section:last-child {
  margin-bottom: 0;
}

.ops-section__title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  padding: 6px 0;
  margin-bottom: 8px;
  border-left: 3px solid #1890ff;
  padding-left: 8px;
  background: #fafbfc;
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

</style>
