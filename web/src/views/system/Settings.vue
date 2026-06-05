<template>
  <div class="page-content">
    <div class="page-title">系统设置</div>
    <div class="page-body">
      <el-tabs v-model="activeTab" type="border-card">
        <!-- 系统参数 -->
        <el-tab-pane label="系统参数" name="params">
          <div class="tab-content params-content">
            <div class="params-sidebar">
              <div
                v-for="cat in paramCategories"
                :key="cat.key"
                class="category-item"
                :class="{ active: currentCategory === cat.key }"
                @click="scrollToCategory(cat.key)"
              >
                {{ cat.label }}
              </div>
            </div>
            <div class="params-main">
              <el-form
                ref="paramsFormRef"
                :model="paramsFormData"
                label-width="200px"
                class="params-form"
              >
                <div
                  v-for="cat in paramCategories"
                  :key="cat.key"
                  :id="`category-${cat.key}`"
                  class="param-section"
                >
                  <h3 class="section-title">{{ cat.label }}</h3>
                  <el-form-item
                    v-for="param in getParamsByCategory(cat.key)"
                    :key="param.code"
                    :label="param.name"
                  >
                    <template v-if="param.type === 'string'">
                      <el-input
                        v-model="paramsFormData[param.code]"
                        :placeholder="param.placeholder"
                        :maxlength="param.maxLength"
                        show-word-limit
                        style="width: 400px"
                      />
                    </template>
                    <template v-else-if="param.type === 'number'">
                      <el-input-number
                        v-model="paramsFormData[param.code]"
                        :min="param.min"
                        :max="param.max"
                        :step="param.step || 1"
                        controls-position="right"
                        style="width: 200px"
                      />
                    </template>
                    <template v-else-if="param.type === 'select'">
                      <el-select v-model="paramsFormData[param.code]" style="width: 200px">
                        <el-option
                          v-for="opt in param.options"
                          :key="opt.value"
                          :label="opt.label"
                          :value="opt.value"
                        />
                      </el-select>
                    </template>
                    <template v-else-if="param.type === 'switch'">
                      <el-switch v-model="paramsFormData[param.code]" />
                    </template>
                    <template v-else-if="param.type === 'textarea'">
                      <el-input
                        v-model="paramsFormData[param.code]"
                        type="textarea"
                        :rows="3"
                        :maxlength="param.maxLength"
                        show-word-limit
                        style="width: 400px"
                      />
                    </template>
                    <template v-else-if="param.type === 'geojson'">
                      <div class="geojson-editor">
                        <div class="geojson-actions">
                          <el-upload
                            accept=".json,.geojson"
                            :auto-upload="false"
                            :show-file-list="false"
                            :on-change="handleGeoJsonUpload"
                            style="display: inline-block; margin-right: 8px;"
                          >
                            <el-button type="primary" size="small">
                              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" style="vertical-align: middle; margin-right: 4px;">
                                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                                <polyline points="17 8 12 3 7 8"/>
                                <line x1="12" y1="3" x2="12" y2="15"/>
                              </svg>
                              导入GeoJSON
                            </el-button>
                          </el-upload>
                          <el-button type="success" size="small" @click="openMapDrawer">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" style="vertical-align: middle; margin-right: 4px;">
                              <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6"/>
                              <line x1="8" y1="2" x2="8" y2="18"/>
                              <line x1="16" y1="6" x2="16" y2="22"/>
                            </svg>
                            地图绘制
                          </el-button>
                          <el-button type="warning" size="small" @click="handleExportGeoJson" v-if="geoJsonData">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" style="vertical-align: middle; margin-right: 4px;">
                              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                              <polyline points="7 10 12 15 17 10"/>
                              <line x1="12" y1="15" x2="12" y2="3"/>
                            </svg>
                            导出GeoJSON
                          </el-button>
                          <el-button type="danger" size="small" @click="handleClearGeoJson" v-if="geoJsonData">
                            清除
                          </el-button>
                        </div>
                        <div class="geojson-preview" v-if="geoJsonData">
                          <div class="geojson-info">
                            <el-tag type="success" size="small">已设置关注区域</el-tag>
                            <span class="geojson-detail">{{ getGeoJsonSummary() }}</span>
                          </div>
                          <el-input
                            v-model="geoJsonText"
                            type="textarea"
                            :rows="6"
                            readonly
                            class="geojson-textarea"
                          />
                        </div>
                        <el-empty v-else description="暂未设置关注区域，请导入GeoJSON文件或在地图上绘制" :image-size="60" />
                      </div>
                    </template>
                    <span class="param-remark">{{ param.remark }}</span>
                  </el-form-item>
                </div>
              </el-form>
              <div class="params-actions">
                <el-button type="primary" size="large" @click="handleSaveParams" :loading="saveLoading">
                  保存配置
                </el-button>
                <el-button size="large" @click="handleResetParams">重置</el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 告警分发 -->
        <el-tab-pane label="告警分发" name="alarm">
          <div class="tab-content">
            <div class="search-bar">
              <el-form :model="alarmSearchForm" inline>
                <el-form-item label="隐患点">
                  <el-input v-model="alarmSearchForm.hazardPoint" placeholder="请输入隐患点名称" clearable />
                </el-form-item>
                <el-form-item label="类型">
                  <el-select v-model="alarmSearchForm.type" placeholder="全部类型" clearable style="width: 150px">
                    <el-option label="监测告警" value="alarm" />
                    <el-option label="设备离线通知" value="offline" />
                  </el-select>
                </el-form-item>
                <el-form-item label="状态">
                  <el-select v-model="alarmSearchForm.status" placeholder="全部状态" clearable style="width: 120px">
                    <el-option label="启用" :value="1" />
                    <el-option label="禁用" :value="0" />
                  </el-select>
                </el-form-item>
                <el-form-item label="渠道">
                  <el-select v-model="alarmSearchForm.channel" placeholder="全部渠道" clearable style="width: 120px">
                    <el-option label="短信" value="sms" />
                    <el-option label="邮件" value="email" />
                    <el-option label="系统消息" value="system" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="handleAlarmSearch">查询</el-button>
                  <el-button @click="handleAlarmReset">重置</el-button>
                </el-form-item>
              </el-form>
              <div class="action-btns">
                <el-button type="primary" @click="handleAddAlarmRule">新增规则</el-button>
                <el-button type="success" @click="handleBatchEnableAlarm" :disabled="selectedAlarmRules.length === 0">批量启用</el-button>
                <el-button type="warning" @click="handleBatchDisableAlarm" :disabled="selectedAlarmRules.length === 0">批量禁用</el-button>
                <el-button type="success" @click="handleImportAlarm">导入</el-button>
                <el-button type="warning" @click="handleExportAlarm">导出</el-button>
              </div>
            </div>

            <el-table :data="alarmRuleList" border stripe v-loading="loading" @selection-change="handleAlarmSelectionChange">
              <el-table-column type="selection" width="55" align="center" />
              <el-table-column label="隐患点" min-width="180">
                <template #default="{ row }">
                  <span v-for="(name, idx) in row.hazardPointNames" :key="idx">
                    <el-tag size="small" style="margin-right: 4px;">{{ name }}</el-tag>
                  </span>
                </template>
              </el-table-column>
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
                    <span v-if="!row.level || row.level.length === 0" class="text-gray">无</span>
                  </template>
                  <template v-else-if="row.type === 'offline' && row.deviceNames && row.deviceNames.length > 0">
                    <el-tag v-for="(name, idx) in row.deviceNames" :key="idx" size="small" style="margin-right: 4px;">{{ name }}</el-tag>
                  </template>
                  <span v-else class="text-gray">无</span>
                </template>
              </el-table-column>
              <el-table-column prop="persons" label="通知人员" min-width="150">
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
              <el-table-column label="执行描述" width="200">
                <template #default="{ row }">
                  {{ getExecDescription(row.execTime) }}
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-switch v-model="row.status" :active-value="1" :inactive-value="0"/>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100" fixed="right">
                <template #default="{ row }">
                  <span class="action-link" @click="handleEditAlarmRule(row)">编辑</span>
                  <span class="action-link action-danger" @click="handleDeleteAlarmRule(row)">删除</span>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination">
              <el-pagination
                v-model:current-page="alarmPagination.page"
                v-model:page-size="alarmPagination.size"
                :page-sizes="[10, 20, 50, 100]"
                :total="alarmPagination.total"
                layout="total, sizes, prev, pager, next, jumper"
                prev-text="上一页"
                next-text="下一页"
                :disabled="alarmPagination.total === 0"
                @size-change="handleAlarmSizeChange"
                @current-change="handleAlarmPageChange"
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 地图绘制弹窗 -->
    <el-dialog
      title="地图绘制关注区域"
      v-model="mapDialogVisible"
      width="900px"
      :close-on-click-modal="false"
      class="map-draw-dialog"
    >
      <div class="map-draw-container">
        <div class="map-toolbar">
          <el-button-group>
            <el-button
              :type="drawMode === 'polygon' ? 'primary' : 'default'"
              size="small"
              @click="setDrawMode('polygon')"
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" style="vertical-align: middle; margin-right: 4px;">
                <path d="M12 2l-11 6v10l11 6 11-6V8z"/>
              </svg>
              绘制多边形
            </el-button>
            <el-button
              :type="drawMode === 'rectangle' ? 'primary' : 'default'"
              size="small"
              @click="setDrawMode('rectangle')"
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" style="vertical-align: middle; margin-right: 4px;">
                <rect x="3" y="3" width="18" height="18" rx="2"/>
              </svg>
              绘制矩形
            </el-button>
            <el-button
              :type="drawMode === 'circle' ? 'primary' : 'default'"
              size="small"
              @click="setDrawMode('circle')"
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" style="vertical-align: middle; margin-right: 4px;">
                <circle cx="12" cy="12" r="10"/>
              </svg>
              绘制圆形
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="clearDrawLayer"
              v-if="drawnFeatures.length > 0"
            >
              清除绘制
            </el-button>
          </el-button-group>
          <span class="draw-hint" v-if="drawMode">
            <el-tag type="warning" size="small">{{ getDrawHint() }}</el-tag>
          </span>
        </div>
        <div ref="mapContainerRef" class="map-container"></div>
        <div class="drawn-info" v-if="drawnFeatures.length > 0">
          <el-tag type="success" size="small">已绘制 {{ drawnFeatures.length }} 个区域</el-tag>
          <span class="feature-types">{{ getFeatureTypesSummary() }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="mapDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmDraw" :disabled="drawnFeatures.length === 0">
          确认使用此区域
        </el-button>
      </template>
    </el-dialog>

    <!-- 告警规则弹窗 -->
    <el-dialog
      :title="alarmDialogTitle"
      v-model="alarmDialogVisible"
      width="650px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="alarmFormRef"
        :model="alarmFormData"
        :rules="alarmFormRules"
        label-width="100px"
      >
        <el-form-item label="隐患点" :prop="isEditAlarm ? 'hazardPointId' : 'hazardPointIds'">
          <el-select
            v-model="currentHazardPoints"
            :multiple="!isEditAlarm"
            placeholder="请选择隐患点"
            style="width: 100%"
          >
            <el-option
              v-for="hp in hazardPointList"
              :key="hp.id"
              :label="hp.name"
              :value="hp.id"
            />
          </el-select>
          <span v-if="!isEditAlarm" class="form-hint">支持多选，确定后按隐患点列表循环保存</span>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="alarmFormData.type">
            <el-radio label="alarm">监测告警</el-radio>
            <el-radio label="offline">设备离线通知</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="告警等级" prop="level" v-if="alarmFormData.type === 'alarm'">
          <el-select v-model="alarmFormData.level" multiple placeholder="请选择告警等级（支持多选）" style="width: 100%">
            <el-option label="四级(注意)" value="四级(注意)" />
            <el-option label="三级(警示)" value="三级(警示)" />
            <el-option label="二级(警戒)" value="二级(警戒)" />
            <el-option label="一级(警报)" value="一级(警报)" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联设备" prop="deviceIds" v-if="alarmFormData.type === 'offline'">
          <el-select v-model="alarmFormData.deviceIds" multiple placeholder="请选择设备" style="width: 100%">
            <el-option
              v-for="device in deviceList"
              :key="device.id"
              :label="`${device.deviceCode} - ${device.name}`"
              :value="device.id"
            />
          </el-select>
          <span class="form-hint">支持多选</span>
        </el-form-item>
        <el-form-item label="通知人员" prop="personIds">
          <el-select v-model="alarmFormData.personIds" multiple placeholder="请选择通知人员" style="width: 100%">
            <el-option
              v-for="user in userList"
              :key="user.id"
              :label="user.realName"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="通知渠道" prop="channels">
          <el-checkbox-group v-model="alarmFormData.channels">
            <el-checkbox label="sms">短信</el-checkbox>
            <el-checkbox label="email">邮件</el-checkbox>
            <el-checkbox label="system" checked>系统消息</el-checkbox>
          </el-checkbox-group>
          <span class="form-hint">系统消息包括PC端和移动端的系统消息，默认勾选</span>
        </el-form-item>
        <el-form-item label="执行时间" v-if="alarmFormData.type === 'offline'">
          <el-radio-group v-model="alarmFormData.execType" class="exec-type-group">
            <el-radio label="realtime">实时执行</el-radio>
            <el-radio label="timed">定时</el-radio>
          </el-radio-group>
          <div v-if="alarmFormData.execType === 'timed'" class="exec-time-config">
            <span class="exec-label">每</span>
            <el-input-number v-model="alarmFormData.execFrequencyNum" :min="1" :max="99" style="width: 80px" />
            <el-select v-model="alarmFormData.execFrequencyUnit" style="width: 100px">
              <el-option label="分钟" value="minute" />
              <el-option label="小时" value="hour" />
              <el-option label="天" value="day" />
              <el-option label="周" value="week" />
              <el-option label="月" value="month" />
              <el-option label="年" value="year" />
            </el-select>
            <span class="exec-label">在</span>
            <el-input v-model="alarmFormData.execTimePoints" placeholder="多个时间点用逗号隔开" style="width: 150px" />
            <span class="exec-label">执行</span>
            <span class="form-hint">时间点示例：分钟填秒数(10,20)，小时填分钟数(10,50)，天填小时数(8,10)，周填星期(1-7)，月填日期(1,16)，年填天数(1,36)</span>
          </div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="alarmFormData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="alarmFormData.remark"
            type="textarea"
            :rows="2"
            placeholder="请输入备注"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="alarmDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAlarmSubmit" :loading="alarmSubmitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, reactive, ref} from 'vue'
import type {FormInstance, FormRules, UploadFile} from 'element-plus'
import {ElMessage, ElMessageBox} from 'element-plus'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import 'leaflet-draw/dist/leaflet.draw.css'
import 'leaflet-draw'

interface ParamItem {
  code: string
  name: string
  type: 'string' | 'number' | 'select' | 'switch' | 'textarea' | 'geojson'
  category: string
  value: any
  placeholder?: string
  maxLength?: number
  min?: number
  max?: number
  step?: number
  options?: Array<{ label: string; value: any }>
  remark: string
}

interface AlarmRule {
  id: number
  hazardPointIds: number[]
  hazardPointNames: string[]
  type: 'alarm' | 'offline'
  level: string[]
  persons: string[]
  personIds: number[]
  deviceIds: number[]
  deviceNames: string[]
  channels: string[]
  execTime: string
  status: number
  remark: string
  createTime: string
}

interface HazardPoint {
  id: number
  name: string
}

interface User {
  id: number
  realName: string
}

interface Device {
  id: number
  name: string
  deviceCode: string
}

const activeTab = ref('params')
const loading = ref(false)
const saveLoading = ref(false)
const currentCategory = ref('basic')

// 系统参数
const paramCategories = [
  { key: 'basic', label: '基础配置' },
  { key: 'data', label: '数据管理' },
  { key: 'alarm', label: '告警配置' },
  { key: 'security', label: '安全设置' }
]

const paramList = ref<ParamItem[]>([
  { code: 'sys_name', name: '系统名称', type: 'string', category: 'basic', value: '地质灾害监测预警系统', placeholder: '请输入系统名称', maxLength: 50, remark: '系统显示名称' },
  { code: 'sys_logo', name: '系统Logo', type: 'string', category: 'basic', value: '', placeholder: '请输入Logo地址', maxLength: 200, remark: '系统Logo图片地址' },
  { code: 'sys_copyright', name: '版权信息', type: 'string', category: 'basic', value: '© 2024 地质灾害监测预警系统', placeholder: '请输入版权信息', maxLength: 100, remark: '页面底部版权信息' },
  { code: 'single_hazard_entry', name: '单一隐患点直接进入', type: 'switch', category: 'basic', value: false, remark: '只有一个隐患点时是否直接进入详情页' },
  { code: 'sys_focus_area', name: '系统关注范围区域', type: 'geojson', category: 'basic', value: null, remark: '系统在地图上关注的地理范围，支持GeoJSON格式' },

  { code: 'log_keep_days', name: '日志保留时长(天)', type: 'number', category: 'data', value: 365, min: 90, max: 3650, step: 30, remark: '系统日志保留天数' },
  { code: 'auto_cleanup', name: '自动清理', type: 'switch', category: 'data', value: true, remark: '是否启用数据自动清理' },
  { code: 'cleanup_time', name: '清理执行时间', type: 'string', category: 'data', value: '02:00', placeholder: '如: 02:00', maxLength: 10, remark: '每日自动清理执行时间' },

  { code: 'alarm_enable', name: '告警总开关', type: 'switch', category: 'alarm', value: true, remark: '是否启用系统告警功能' },
  { code: 'login_fail_lock', name: '登录失败锁定', type: 'switch', category: 'security', value: true, remark: '登录失败多次后是否锁定账号' },
  { code: 'login_fail_times', name: '允许失败次数', type: 'number', category: 'security', value: 5, min: 3, max: 10, remark: '允许的最大登录失败次数' },
  { code: 'lock_duration', name: '锁定时长(分钟)', type: 'number', category: 'security', value: 30, min: 5, max: 1440, step: 5, remark: '账号锁定后自动解锁时间' },
  { code: 'token_expire', name: 'Token过期(小时)', type: 'number', category: 'security', value: 2, min: 1, max: 24, remark: '用户登录Token有效期' },
  { code: 'password_expire', name: '密码有效期(天)', type: 'number', category: 'security', value: 90, min: 30, max: 365, step: 30, remark: '密码过期后需强制修改' }
])

const paramsFormData = reactive<Record<string, any>>({})

// 初始化参数表单数据
paramList.value.forEach(p => {
  paramsFormData[p.code] = p.value
})

const paramsFormRef = ref<FormInstance>()

// GeoJSON 相关
const geoJsonData = ref<any>(null)
const geoJsonText = computed(() => {
  return geoJsonData.value ? JSON.stringify(geoJsonData.value, null, 2) : ''
})

const getGeoJsonSummary = () => {
  if (!geoJsonData.value) return ''
  const features = geoJsonData.value.features || []
  const types = features.map((f: any) => f.geometry?.type).filter(Boolean)
  const uniqueTypes = [...new Set(types)]
  return `共 ${features.length} 个要素，类型: ${uniqueTypes.join('、')}`
}

const handleGeoJsonUpload = (uploadFile: UploadFile) => {
  const file = uploadFile.raw
  if (!file) return

  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const content = e.target?.result as string
      const parsed = JSON.parse(content)

      if (!parsed.type || parsed.type !== 'FeatureCollection') {
        ElMessage.error('无效的GeoJSON文件，必须是FeatureCollection类型')
        return
      }

      geoJsonData.value = parsed
      paramsFormData.sys_focus_area = parsed
      ElMessage.success('GeoJSON文件导入成功')
    } catch (err) {
      ElMessage.error('GeoJSON文件解析失败，请检查文件格式')
    }
  }
  reader.readAsText(file)
}

const handleExportGeoJson = () => {
  if (!geoJsonData.value) return
  const blob = new Blob([JSON.stringify(geoJsonData.value, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'system-focus-area.geojson'
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('GeoJSON导出成功')
}

const handleClearGeoJson = () => {
  geoJsonData.value = null
  paramsFormData.sys_focus_area = null
  ElMessage.success('关注区域已清除')
}

// 地图绘制相关
const mapDialogVisible = ref(false)
const mapContainerRef = ref<HTMLElement | null>(null)
let mapInstance: L.Map | null = null
let drawLayer: L.FeatureGroup | null = null
let currentDrawHandler: any = null
const drawMode = ref<string>('')
const drawnFeatures = ref<any[]>([])

const openMapDrawer = () => {
  mapDialogVisible.value = true
  drawMode.value = ''
  drawnFeatures.value = []
  nextTick(() => {
    initMap()
  })
}

const initMap = () => {
  if (!mapContainerRef.value) return

  if (mapInstance) {
    mapInstance.remove()
  }

  mapInstance = L.map(mapContainerRef.value).setView([39.9042, 116.4074], 10)

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors'
  }).addTo(mapInstance)

  drawLayer = L.featureGroup().addTo(mapInstance)

  // 如果已有GeoJSON数据，显示在地图上
  if (geoJsonData.value) {
    try {
      const layer = L.geoJSON(geoJsonData.value, {
        style: {
          color: '#1890ff',
          weight: 3,
          fillColor: '#1890ff',
          fillOpacity: 0.2
        }
      })
      drawLayer.addLayer(layer)
      const bounds = layer.getBounds()
      if (bounds.isValid()) {
        mapInstance.fitBounds(bounds, { padding: [50, 50] })
      }
      drawnFeatures.value = geoJsonData.value.features || []
    } catch (e) {
      console.error('加载已有GeoJSON失败', e)
    }
  }
}

const setDrawMode = (mode: string) => {
  if (!mapInstance || !drawLayer) return

  // 清除之前的绘制处理器
  if (currentDrawHandler) {
    currentDrawHandler.disable()
    currentDrawHandler = null
  }

  if (drawMode.value === mode) {
    drawMode.value = ''
    return
  }

  drawMode.value = mode

  // 创建新的绘制处理器
  const Draw = (L as any).Draw
  if (mode === 'polygon') {
    currentDrawHandler = new Draw.Polygon(mapInstance, {
      allowIntersection: false,
      showArea: true,
      shapeOptions: {
        color: '#1890ff',
        weight: 3,
        fillColor: '#1890ff',
        fillOpacity: 0.2
      }
    })
  } else if (mode === 'rectangle') {
    currentDrawHandler = new Draw.Rectangle(mapInstance, {
      shapeOptions: {
        color: '#1890ff',
        weight: 3,
        fillColor: '#1890ff',
        fillOpacity: 0.2
      }
    })
  } else if (mode === 'circle') {
    currentDrawHandler = new Draw.Circle(mapInstance, {
      shapeOptions: {
        color: '#1890ff',
        weight: 3,
        fillColor: '#1890ff',
        fillOpacity: 0.2
      }
    })
  }

  if (currentDrawHandler) {
    currentDrawHandler.enable()
  }

  // 监听绘制完成事件
  mapInstance.once(Draw.Event.CREATED, (e: any) => {
    const layer = e.layer
    drawLayer!.addLayer(layer)
    drawMode.value = ''
    currentDrawHandler = null
    updateDrawnFeatures()
  })
}

const clearDrawLayer = () => {
  if (drawLayer) {
    drawLayer.clearLayers()
  }
  drawnFeatures.value = []
}

const updateDrawnFeatures = () => {
  if (!drawLayer) return
  const geojson = drawLayer.toGeoJSON() as any
  drawnFeatures.value = geojson.features || []
}

const getDrawHint = () => {
  const hints: Record<string, string> = {
    polygon: '点击地图开始绘制多边形，双击完成绘制',
    rectangle: '按住鼠标拖动绘制矩形',
    circle: '点击并拖动绘制圆形'
  }
  return hints[drawMode.value] || ''
}

const getFeatureTypesSummary = () => {
  const types = drawnFeatures.value.map(f => f.geometry?.type).filter(Boolean)
  const uniqueTypes = [...new Set(types)]
  return uniqueTypes.join('、')
}

const handleConfirmDraw = () => {
  if (!drawLayer || drawnFeatures.value.length === 0) return

  const geojson = drawLayer.toGeoJSON()
  geoJsonData.value = geojson
  paramsFormData.sys_focus_area = geojson
  mapDialogVisible.value = false
  ElMessage.success('关注区域已保存')
}

const scrollToCategory = (key: string) => {
  currentCategory.value = key
  nextTick(() => {
    const el = document.getElementById(`category-${key}`)
    el?.scrollIntoView({behavior: 'smooth', block: 'start'})
  })
}

const getParamsByCategory = (category: string) => {
  return paramList.value.filter(p => p.category === category)
}

const handleSaveParams = () => {
  saveLoading.value = true
  setTimeout(() => {
    saveLoading.value = false
    ElMessage.success('系统参数保存成功')
  }, 800)
}

const handleResetParams = () => {
  paramList.value.forEach(p => {
    paramsFormData[p.code] = p.value
  })
  geoJsonData.value = null
  ElMessage.info('已重置为默认值')
}

// 告警分发
const alarmSearchForm = reactive({
  hazardPoint: '',
  type: '',
  status: undefined as number | undefined,
  channel: ''
})

const alarmPagination = reactive({ page: 1, size: 10, total: 0 })

const hazardPointList = ref<HazardPoint[]>([
  { id: 1, name: 'XX山区滑坡监测点' },
  { id: 2, name: 'YY矿区沉降监测点' },
  { id: 3, name: 'ZZ水库坝体监测点' },
  { id: 4, name: 'WW公路边坡监测点' },
  { id: 5, name: 'QQ隧道口监测点' }
])

const userList = ref<User[]>([
  { id: 1, realName: '系统管理员' },
  { id: 2, realName: '张三' },
  { id: 3, realName: '李四' },
  { id: 4, realName: '王五' },
  { id: 5, realName: '赵六' }
])

const deviceList = ref<Device[]>([
  { id: 1, name: 'GNSS接收机-A1', deviceCode: 'GNSS-001' },
  { id: 2, name: '裂缝计-B1', deviceCode: 'LF-001' },
  { id: 3, name: '位移计-C1', deviceCode: 'WY-001' },
  { id: 4, name: '雨量计-D1', deviceCode: 'YL-001' },
  { id: 5, name: '水位计-E1', deviceCode: 'SW-001' },
  { id: 6, name: 'GNSS接收机-A2', deviceCode: 'GNSS-002' },
  { id: 7, name: '裂缝计-B2', deviceCode: 'LF-002' },
  { id: 8, name: '视频监控-F1', deviceCode: 'VD-001' }
])

const allAlarmRules = ref<AlarmRule[]>([
  { id: 1, hazardPointIds: [1], hazardPointNames: ['XX山区滑坡监测点'], type: 'alarm', level: ['一级(警报)'], persons: ['张三', '李四'], personIds: [2, 3], deviceIds: [], deviceNames: [], channels: ['sms', 'system'], execTime: '', status: 1, remark: '滑坡位移超限立即通知', createTime: '2024-01-01 10:00:00' },
  { id: 2, hazardPointIds: [1], hazardPointNames: ['XX山区滑坡监测点'], type: 'offline', level: [], persons: ['张三'], personIds: [2], deviceIds: [1, 2], deviceNames: ['GNSS-001 - GNSS接收机-A1', 'LF-001 - 裂缝计-B1'], channels: ['sms', 'email', 'system'], execTime: 'day|8,14,18', status: 1, remark: '设备离线通知', createTime: '2024-01-05 09:00:00' },
  { id: 3, hazardPointIds: [2, 3], hazardPointNames: ['YY矿区沉降监测点', 'ZZ水库坝体监测点'], type: 'alarm', level: ['二级(警戒)', '一级(警报)'], persons: ['王五', '赵六'], personIds: [4, 5], deviceIds: [], deviceNames: [], channels: ['sms', 'system'], execTime: '', status: 1, remark: '矿区沉降监测告警', createTime: '2024-01-10 08:30:00' },
  { id: 4, hazardPointIds: [3], hazardPointNames: ['ZZ水库坝体监测点'], type: 'alarm', level: ['一级(警报)'], persons: ['系统管理员', '张三'], personIds: [1, 2], deviceIds: [], deviceNames: [], channels: ['sms', 'system'], execTime: '', status: 0, remark: '水库坝体压力超限', createTime: '2024-01-12 10:00:00' },
  { id: 5, hazardPointIds: [4], hazardPointNames: ['WW公路边坡监测点'], type: 'offline', level: [], persons: ['李四'], personIds: [3], deviceIds: [6, 7], deviceNames: ['GNSS-002 - GNSS接收机-A2', 'LF-002 - 裂缝计-B2'], channels: ['email', 'system'], execTime: 'hour|30', status: 1, remark: '公路边坡设备状态', createTime: '2024-01-15 11:00:00' },
  { id: 6, hazardPointIds: [5], hazardPointNames: ['QQ隧道口监测点'], type: 'alarm', level: ['三级(警示)', '四级(注意)'], persons: ['王五'], personIds: [4], deviceIds: [], deviceNames: [], channels: ['sms', 'system'], execTime: '', status: 1, remark: '隧道口变形监测', createTime: '2024-01-20 14:00:00' }
])

const alarmRuleList = computed(() => {
  let result = allAlarmRules.value

  if (alarmSearchForm.hazardPoint) {
    result = result.filter(r => r.hazardPointNames.some(name => name.includes(alarmSearchForm.hazardPoint)))
  }
  if (alarmSearchForm.type) {
    result = result.filter(r => r.type === alarmSearchForm.type)
  }
  if (alarmSearchForm.status !== undefined) {
    result = result.filter(r => r.status === alarmSearchForm.status)
  }
  if (alarmSearchForm.channel) {
    result = result.filter(r => r.channels.includes(alarmSearchForm.channel))
  }

  alarmPagination.total = result.length
  const start = (alarmPagination.page - 1) * alarmPagination.size
  return result.slice(start, start + alarmPagination.size)
})

const getAlarmLevelType = (level: string) => {
  const map: Record<string, string> = { '一级(警报)': 'danger', '二级(警戒)': 'warning', '三级(警示)': 'info', '四级(注意)': 'success' }
  return map[level] || 'info'
}

const getChannelLabel = (channel: string) => {
  const map: Record<string, string> = { sms: '短信', email: '邮件', system: '系统消息' }
  return map[channel] || channel
}

const getExecDescription = (execTime: string) => {
  if (!execTime) return '-'
  const parts = execTime.split('|')
  if (parts.length !== 2) {
    return execTime || '-'
  }
  
  const [frequency, timeStr] = parts
  const freqLabels: Record<string, string> = {
    'minute': '分钟',
    'hour': '小时',
    'day': '天',
    'week': '周',
    'month': '月',
    'year': '年'
  }
  
  const freqLabel = freqLabels[frequency] || frequency
  const timeValues = timeStr.split(',').filter(t => t.trim())
  
  if (frequency === 'minute') {
    return `每${freqLabel}第${timeValues.join('、')}秒执行`
  } else if (frequency === 'hour') {
    return `每${freqLabel}第${timeValues.join('、')}分钟执行`
  } else if (frequency === 'day') {
    return `每${freqLabel}第${timeValues.join('、')}小时执行`
  } else if (frequency === 'week') {
    return `每周${timeValues.join('、')}执行`
  } else if (frequency === 'month') {
    return `每月${timeValues.join('、')}日执行`
  } else if (frequency === 'year') {
    return `每年第${timeValues.join('、')}天执行`
  }
  
  return `${freqLabel}: ${timeStr}`
}

const handleAlarmSearch = () => { alarmPagination.page = 1 }
const handleAlarmReset = () => {
  alarmSearchForm.hazardPoint = ''
  alarmSearchForm.type = ''
  alarmSearchForm.status = undefined
  alarmSearchForm.channel = ''
  alarmPagination.page = 1
}
const handleAlarmSizeChange = (val: number) => { alarmPagination.size = val; alarmPagination.page = 1 }
const handleAlarmPageChange = (val: number) => { alarmPagination.page = val }

// 告警规则弹窗
const alarmDialogVisible = ref(false)
const alarmDialogTitle = ref('新增告警规则')
const alarmSubmitLoading = ref(false)
const alarmFormRef = ref<FormInstance>()
const isEditAlarm = ref(false)

// 批量操作
const selectedAlarmRules = ref<AlarmRule[]>([])

const alarmFormData = reactive({
  id: 0,
  hazardPointId: undefined as number | undefined,
  hazardPointIds: [] as number[],
  type: 'alarm' as 'alarm' | 'offline',
  level: ['四级(注意)'] as string[],
  personIds: [] as number[],
  deviceIds: [] as number[],
  channels: ['system'] as string[],
  execTime: '',
  execType: 'realtime' as 'realtime' | 'timed',
  execFrequencyNum: 1,
  execFrequencyUnit: 'hour' as 'minute' | 'hour' | 'day' | 'week' | 'month' | 'year',
  execTimePoints: '',
  status: 1,
  remark: ''
})

const currentHazardPoints = computed({
  get: () => {
    if (isEditAlarm.value) {
      return alarmFormData.hazardPointId
    }
    return alarmFormData.hazardPointIds
  },
  set: (val: number | number[]) => {
    if (isEditAlarm.value) {
      alarmFormData.hazardPointId = val as number
    } else {
      alarmFormData.hazardPointIds = val as number[]
    }
  }
})

const alarmFormRules: FormRules = {
  hazardPointId: [{ required: true, message: '请选择隐患点', trigger: 'change' }],
  hazardPointIds: [{ required: true, message: '请选择隐患点', trigger: 'change', type: 'array' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  level: [{ required: true, message: '请选择告警等级', trigger: 'change', type: 'array' }],
  personIds: [{ required: true, message: '请选择通知人员', trigger: 'change', type: 'array' }],
  deviceIds: [{ required: true, message: '请选择关联设备', trigger: 'change', type: 'array' }],
  channels: [{ required: true, message: '请选择通知渠道', trigger: 'change', type: 'array' }],
  execTime: [{ required: true, message: '请输入执行时间', trigger: 'change' }]
}

const handleAddAlarmRule = () => {
  isEditAlarm.value = false
  alarmDialogTitle.value = '新增告警规则'
  resetAlarmForm()
  alarmDialogVisible.value = true
}

const handleEditAlarmRule = (row: AlarmRule) => {
  isEditAlarm.value = true
  alarmDialogTitle.value = '编辑告警规则'
  
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
  
  Object.assign(alarmFormData, {
    id: row.id,
    hazardPointId: row.hazardPointIds.length > 0 ? row.hazardPointIds[0] : undefined,
    hazardPointIds: [...row.hazardPointIds],
    type: row.type,
    level: row.level || '四级(注意)',
    personIds: [...row.personIds],
    deviceIds: [...row.deviceIds],
    channels: [...row.channels],
    execTime: execTime,
    execType,
    execFrequencyNum,
    execFrequencyUnit,
    execTimePoints,
    status: row.status,
    remark: row.remark
  })
  alarmDialogVisible.value = true
}

const handleAlarmSelectionChange = (val: AlarmRule[]) => {
  selectedAlarmRules.value = val
}

const handleBatchEnableAlarm = () => {
  if (selectedAlarmRules.value.length === 0) {
    ElMessage.warning('请选择要启用的告警规则')
    return
  }
  const count = selectedAlarmRules.value.length
  ElMessageBox.confirm(`确定要批量启用选中的 ${count} 条告警规则吗？`, '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    selectedAlarmRules.value.forEach(rule => {
      rule.status = 1
    })
    selectedAlarmRules.value = []
    ElMessage.success(`成功启用 ${count} 条告警规则`)
  }).catch(() => {})
}

const handleBatchDisableAlarm = () => {
  if (selectedAlarmRules.value.length === 0) {
    ElMessage.warning('请选择要禁用的告警规则')
    return
  }
  const count = selectedAlarmRules.value.length
  ElMessageBox.confirm(`确定要批量禁用选中的 ${count} 条告警规则吗？`, '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    selectedAlarmRules.value.forEach(rule => {
      rule.status = 0
    })
    selectedAlarmRules.value = []
    ElMessage.success(`成功禁用 ${count} 条告警规则`)
  }).catch(() => {})
}

const handleDeleteAlarmRule = (row: AlarmRule) => {
  ElMessageBox.confirm(`确定要删除该告警规则吗？`, '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    const index = allAlarmRules.value.findIndex(r => r.id === row.id)
    if (index !== -1) allAlarmRules.value.splice(index, 1)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const resetAlarmForm = () => {
  alarmFormData.id = 0
  alarmFormData.hazardPointId = undefined
  alarmFormData.hazardPointIds = []
  alarmFormData.type = 'alarm'
  alarmFormData.level = ['四级(注意)']
  alarmFormData.personIds = []
  alarmFormData.deviceIds = []
  alarmFormData.channels = ['system']
  alarmFormData.execTime = ''
  alarmFormData.status = 1
  alarmFormData.remark = ''
}

const handleAlarmSubmit = async () => {
  if (!alarmFormRef.value) return
  await alarmFormRef.value.validate((valid) => {
    if (valid) {
      let execTimeValue = ''
      if (alarmFormData.type === 'offline' && alarmFormData.execType === 'timed' && alarmFormData.execTimePoints) {
        execTimeValue = `${alarmFormData.execFrequencyUnit}|${alarmFormData.execTimePoints}`
      }
      
      alarmSubmitLoading.value = true
      setTimeout(() => {
        const persons = alarmFormData.personIds.map(id => userList.value.find(u => u.id === id)?.realName || '')
        const devices = alarmFormData.deviceIds.map(id => {
          const d = deviceList.value.find(dev => dev.id === id)
          return d ? `${d.deviceCode} - ${d.name}` : ''
        }).filter(Boolean)

        if (isEditAlarm.value) {
          const rule = allAlarmRules.value.find(r => r.id === alarmFormData.id)
          if (rule) {
            const hp = hazardPointList.value.find(h => h.id === alarmFormData.hazardPointId)
            Object.assign(rule, {
              hazardPointIds: alarmFormData.hazardPointId ? [alarmFormData.hazardPointId] : [],
              hazardPointNames: hp ? [hp.name] : [],
              type: alarmFormData.type,
              level: alarmFormData.type === 'alarm' ? [...alarmFormData.level] : [],
              personIds: [...alarmFormData.personIds],
              persons,
              deviceIds: [...alarmFormData.deviceIds],
              deviceNames: [...devices],
              channels: [...alarmFormData.channels],
              execTime: execTimeValue,
              status: alarmFormData.status,
              remark: alarmFormData.remark
            })
          }
          ElMessage.success('修改成功')
        } else {
          const selectedHps = alarmFormData.hazardPointIds.map(id => hazardPointList.value.find(h => h.id === id)).filter((hp): hp is HazardPoint => hp !== undefined)
          selectedHps.forEach(hp => {
            allAlarmRules.value.push({
              id: allAlarmRules.value.length + 1,
              hazardPointIds: [hp.id],
              hazardPointNames: [hp.name],
              type: alarmFormData.type,
              level: alarmFormData.type === 'alarm' ? [...alarmFormData.level] : [],
              personIds: [...alarmFormData.personIds],
              persons,
              deviceIds: [...alarmFormData.deviceIds],
              deviceNames: [...devices],
              channels: [...alarmFormData.channels],
              execTime: execTimeValue,
              status: alarmFormData.status,
              remark: alarmFormData.remark,
              createTime: new Date().toLocaleString('zh-CN', { hour12: false })
            })
          })
          ElMessage.success(`新增成功，共创建 ${selectedHps.length} 条告警规则`)
        }
        alarmDialogVisible.value = false
        alarmSubmitLoading.value = false
      }, 500)
    }
  })
}

const handleImportAlarm = () => {
  ElMessage.info('导入功能开发中')
}

const handleExportAlarm = () => {
  ElMessage.success('告警规则导出成功')
}
</script>

<style scoped>
.page-content {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  min-height: calc(100% - 32px);
}

.page-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e8e8e8;
}

.page-body {
  padding: 0;
}

.tab-content {
  padding: 16px 0;
}

/* 系统参数样式 */
.params-content {
  display: flex;
  gap: 20px;
  min-height: calc(100vh - 280px);
}

.params-sidebar {
  width: 160px;
  flex-shrink: 0;
  border-right: 1px solid #e8e8e8;
  padding-right: 12px;
}

.category-item {
  padding: 12px 16px;
  cursor: pointer;
  border-radius: 6px;
  margin-bottom: 4px;
  font-size: 14px;
  color: #606266;
  transition: all 0.3s;
}

.category-item:hover {
  background: #f5f7fa;
  color: #1890ff;
}

.category-item.active {
  background: #e6f7ff;
  color: #1890ff;
  font-weight: 500;
}

.params-main {
  flex: 1;
  overflow-y: auto;
  max-height: calc(100vh - 280px);
  padding-right: 12px;
}

.param-section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid #e8e8e8;
}

.param-remark {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}

.params-actions {
  position: sticky;
  bottom: 0;
  background: #fff;
  padding: 16px 0;
  border-top: 1px solid #e8e8e8;
  margin-top: 20px;
  display: flex;
  gap: 12px;
}

/* GeoJSON编辑器样式 */
.geojson-editor {
  width: 100%;
}

.geojson-actions {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.geojson-preview {
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 12px;
  background: #fafafa;
}

.geojson-info {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.geojson-detail {
  color: #606266;
  font-size: 13px;
}

.geojson-textarea :deep(.el-textarea__inner) {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  background: #f5f7fa;
}

/* 地图绘制样式 */
.map-draw-dialog :deep(.el-dialog__body) {
  padding: 10px 20px;
}

.map-draw-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.map-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.draw-hint {
  margin-left: auto;
}

.map-container {
  width: 100%;
  height: 500px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
}

.drawn-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.feature-types {
  color: #606266;
  font-size: 13px;
}

/* 告警分发样式 */
.search-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
}

.action-btns {
  display: flex;
  gap: 8px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-form--inline .el-form-item) {
  margin-right: 16px;
  margin-bottom: 10px;
}

.action-link {
  display: inline-block;
  padding: 4px 10px;
  margin: 0 4px;
  color: #303133;
  font-size: 13px;
  cursor: pointer;
  transition: color 0.2s ease;
}

.action-link:hover {
  color: #1890ff;
}

.action-link.action-warning {
  color: #faad14;
}

.action-link.action-warning:hover {
  color: #d48806;
}

.action-link.action-success {
  color: #52c41a;
}

.action-link.action-success:hover {
  color: #389e0d;
}

.action-link.action-danger {
  color: #f5222d;
}

.action-link.action-danger:hover {
  color: #cf1322;
}

.form-hint {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

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
  font-size: 14px;
  color: #606266;
}

.text-gray {
  color: #909399;
}
</style>
