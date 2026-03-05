<template>
  <div class="app-container">
    <div v-if="!showDetail">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="产品密钥" prop="productKey">
        <el-input
          v-model="queryParams.productKey"
          placeholder="请输入产品密钥"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="产品名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入产品名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="设备类型" prop="nodeType">
        <el-select
          v-model="queryParams.nodeType"
          placeholder="请选择设备类型"
          clearable
          @keyup.enter="handleQuery"
        >
          <el-option label="直连设备" value="0"></el-option>
          <el-option label="网关" value="1"></el-option>
          <el-option label="传感器" value="2"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['product:product:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['product:product:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['product:product:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['product:product:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

      <el-row :gutter="20" v-loading="loading">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" v-for="item in productList" :key="item.id" class="mb-4">
          <el-card shadow="hover" class="product-card">
            <template #header>
              <div class="card-header">
                <span class="product-name" @click="handleDetail(item)">{{ item.name }}</span>
                <el-tag v-if="item.nodeType === 0" size="small">直连设备</el-tag>
                <el-tag v-else-if="item.nodeType === 1" type="success" size="small">网关</el-tag>
                <el-tag v-else-if="item.nodeType === 2" type="warning" size="small">传感器</el-tag>
              </div>
            </template>
            <div class="card-content">
              <div class="info-item">
                <span class="label">产品密钥：</span>
                <span class="value">{{ item.productKey }}</span>
              </div>
              <div class="info-item">
                <span class="label">描述：</span>
                <span class="value">{{ item.remarks || '暂无描述' }}</span>
              </div>
            </div>
            <div class="card-footer">
              <el-tooltip content="修改" placement="top">
                <el-button link type="primary" icon="Edit" @click="handleUpdate(item)"
                           v-hasPermi="['product:product:edit']"></el-button>
              </el-tooltip>
              <el-tooltip content="物模型设置" placement="top">
                <el-button link type="primary" icon="Setting" @click="handleThingModel(item)"
                           v-hasPermi="['product:product:edit']"></el-button>
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <el-button link type="danger" icon="Delete" @click="handleDelete(item)"
                           v-hasPermi="['product:product:remove']"></el-button>
              </el-tooltip>
            </div>
          </el-card>
        </el-col>
      </el-row>
    
    <pagination
        v-show="total>0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
    />
    </div>

    <!-- 产品详情视图 -->
    <div v-else class="product-detail">
      <!-- 详情头部 -->
      <div class="detail-header">
        <el-page-header @back="handleBack" :title="'返回'">
          <template #content>
            <span class="text-large font-600 mr-3">{{ currentProduct.name }}</span>
            <el-tag type="success" size="small" class="ml-2">正常</el-tag>
          </template>
        </el-page-header>
        <div class="detail-info mt-2 ml-10">
          <span class="info-label">设备数量：</span>
          <span class="info-value">{{ deviceCount }}</span>
        </div>
      </div>

      <!-- 详情页签 -->
      <el-tabs v-model="activeTab" class="detail-tabs mt-4">
        <el-tab-pane label="配置信息" name="config">
          <el-descriptions :column="2" border class="mt-4">
            <el-descriptions-item label="产品名称">{{ currentProduct.name }}</el-descriptions-item>
            <el-descriptions-item label="产品密钥">{{ currentProduct.productKey }}</el-descriptions-item>
            <el-descriptions-item label="设备类型">
              <el-tag v-if="currentProduct.nodeType === 0">直连设备</el-tag>
              <el-tag v-else-if="currentProduct.nodeType === 1" type="success">网关</el-tag>
              <el-tag v-else-if="currentProduct.nodeType === 2" type="warning">传感器</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="产品描述">{{ currentProduct.remarks }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="物模型" name="tsl">
          <div class="tsl-container mt-4">
            <div class="mb-2">
              <el-button type="primary" plain icon="Plus" @click="openTslDialog('property')">添加属性</el-button>
              <el-button type="primary" plain icon="Plus" @click="openTslDialog('service')">添加功能</el-button>
              <el-button type="primary" plain icon="Plus" @click="openTslDialog('event')">添加事件</el-button>
              <el-button type="success" icon="Check" @click="saveTslToBackend">保存物模型</el-button>
              <el-button type="info" plain icon="Edit" @click="handleThingModel(currentProduct)">JSON编辑</el-button>
            </div>
            <el-tabs type="border-card">
              <el-tab-pane label="属性定义">
                <el-table :data="thingModelData.properties" style="width: 100%">
                  <el-table-column prop="identifier" label="标识符" width="120"/>
                  <el-table-column prop="name" label="名称" width="120"/>
                  <el-table-column prop="dataType.type" label="数据类型" width="100">
                    <template #default="scope">
                      <el-tag size="small">{{ scope.row.dataType?.type || '-' }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="数据规格" min-width="200">
                    <template #default="scope">
                      <div class="specs-text text-xs text-gray-600">
                        {{ formatDataSpecs(scope.row.dataType) }}
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="accessMode" label="读写类型" width="90">
                    <template #default="scope">
                      <el-tag v-if="scope.row.accessMode === 'r'" type="info">只读</el-tag>
                      <el-tag v-else type="success">读写</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="description" label="描述" show-overflow-tooltip/>
                  <el-table-column label="操作" align="center" width="180" fixed="right">
                    <template #default="scope">
                      <el-button link type="primary" icon="View" @click="handleViewTslDetail('property', scope.row)">
                        详情
                      </el-button>
                      <el-button link type="primary" icon="Edit"
                                 @click="handleEditTslItem('property', scope.row, scope.$index)">修改
                      </el-button>
                      <el-button link type="danger" icon="Delete"
                                 @click="handleDeleteTslItem('property', scope.$index)">删除
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>
              <el-tab-pane label="功能定义">
                <el-table :data="thingModelData.services" style="width: 100%">
                  <el-table-column prop="identifier" label="标识符" width="120"/>
                  <el-table-column prop="name" label="名称" width="120"/>
                  <el-table-column prop="callType" label="调用方式" width="90">
                    <template #default="scope">
                      <el-tag v-if="scope.row.callType === 'sync'" type="success">同步</el-tag>
                      <el-tag v-else type="warning">异步</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="输入参数" min-width="150">
                    <template #default="scope">
                      <div v-if="scope.row.inputData && scope.row.inputData.length > 0">
                        <el-tag v-for="(param, idx) in scope.row.inputData" :key="idx" size="small" class="mr-1 mb-1">
                          {{ param.name }}({{ param.dataType?.type }})
                        </el-tag>
                      </div>
                      <span v-else class="text-gray-400">无</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="输出参数" min-width="150">
                    <template #default="scope">
                      <div v-if="scope.row.outputData && scope.row.outputData.length > 0">
                        <el-tag v-for="(param, idx) in scope.row.outputData" :key="idx" size="small" type="success"
                                class="mr-1 mb-1">
                          {{ param.name }}({{ param.dataType?.type }})
                        </el-tag>
                      </div>
                      <span v-else class="text-gray-400">无</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="description" label="描述" show-overflow-tooltip/>
                  <el-table-column label="操作" align="center" width="220" fixed="right">
                    <template #default="scope">
                      <el-button link type="primary" icon="Setting"
                                 @click="manageServiceParams(scope.row, scope.$index)">配置参数
                      </el-button>
                      <el-button link type="primary" icon="Edit"
                                 @click="handleEditTslItem('service', scope.row, scope.$index)">修改
                      </el-button>
                      <el-button link type="danger" icon="Delete"
                                 @click="handleDeleteTslItem('service', scope.$index)">删除
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>
              <el-tab-pane label="事件定义">
                <el-table :data="thingModelData.events" style="width: 100%">
                  <el-table-column prop="identifier" label="标识符" width="120"/>
                  <el-table-column prop="name" label="名称" width="120"/>
                  <el-table-column prop="type" label="事件类型" width="100">
                    <template #default="scope">
                      <el-tag v-if="scope.row.type === 'info'" type="info">信息</el-tag>
                      <el-tag v-else-if="scope.row.type === 'alert'" type="warning">告警</el-tag>
                      <el-tag v-else-if="scope.row.type === 'error'" type="danger">故障</el-tag>
                      <span v-else>{{ scope.row.type }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="输出参数" min-width="200">
                    <template #default="scope">
                      <div v-if="scope.row.outputData && scope.row.outputData.length > 0">
                        <el-tag v-for="(param, idx) in scope.row.outputData" :key="idx" size="small" class="mr-1 mb-1">
                          {{ param.name }}({{ param.dataType?.type }})
                        </el-tag>
                      </div>
                      <span v-else class="text-gray-400">无</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="description" label="描述" show-overflow-tooltip/>
                  <el-table-column label="操作" align="center" width="220" fixed="right">
                    <template #default="scope">
                      <el-button link type="primary" icon="Setting" @click="manageEventParams(scope.row, scope.$index)">
                        配置参数
                      </el-button>
                      <el-button link type="primary" icon="Edit"
                                 @click="handleEditTslItem('event', scope.row, scope.$index)">修改
                      </el-button>
                      <el-button link type="danger" icon="Delete" @click="handleDeleteTslItem('event', scope.$index)">
                        删除
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>
            </el-tabs>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 添加或修改产品管理对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="productRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="产品密钥" prop="productKey">
        <el-input v-model="form.productKey" placeholder="请输入产品密钥" />
      </el-form-item>
      <el-form-item label="产品名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入产品名称" />
      </el-form-item>
      <el-form-item label="设备类型" prop="nodeType">
        <el-select v-model="form.nodeType" placeholder="请选择设备类型">
          <el-option label="直连设备" value="0"></el-option>
          <el-option label="网关" value="1"></el-option>
          <el-option label="传感器" value="2"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="描述" prop="remarks">
        <el-input v-model="form.remarks" type="textarea" placeholder="请输入描述" />
      </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 物模型编辑对话框 -->
    <el-dialog :title="thingModelTitle" v-model="thingModelOpen" width="800px" append-to-body>
      <el-form ref="thingModelRef" :model="thingModelForm" label-width="80px">
        <el-form-item label="产品编号">
          <el-input v-model="thingModelForm.productKey" disabled placeholder="产品编号" />
        </el-form-item>
        <el-form-item label="物模型内容">
          <el-input 
            v-model="thingModelForm.thingModelContent" 
            type="textarea" 
            :rows="10" 
            placeholder="请输入物模型JSON内容" 
          />
        </el-form-item>
        <el-form-item>
          <el-alert
            title="提示信息"
            type="info"
            description="物模型格式应为JSON格式，包含properties（属性）和events（事件）字段。保存后将自动创建对应的产品数据表。"
            show-icon
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitThingModel">保存物模型</el-button>
          <el-button @click="cancelThingModel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- TSL Item Editing Dialog (属性/服务/事件基础编辑) -->
    <el-dialog :title="tslDialog.title" v-model="tslDialog.visible" width="700px" append-to-body>
      <el-form ref="tslItemRef" :model="currentTslItem" label-width="100px" :rules="tslItemRules">
        <!-- Common Fields -->
        <el-form-item label="标识符" prop="identifier">
          <el-input v-model="currentTslItem.identifier" placeholder="例如: temperature" :disabled="tslDialog.isEdit"/>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="currentTslItem.name" placeholder="例如: 温度"/>
        </el-form-item>

        <!-- Property Specific -->
        <template v-if="tslDialog.type === 'property'">
          <el-form-item label="数据类型" prop="dataType.type">
            <el-select v-model="currentTslItem.dataType.type" placeholder="请选择类型" @change="handleDataTypeChange">
              <el-option label="int (整数)" value="int"/>
              <el-option label="float (单精度浮点)" value="float"/>
              <el-option label="double (双精度浮点)" value="double"/>
              <el-option label="text (字符串)" value="text"/>
              <el-option label="bool (布尔型)" value="bool"/>
              <el-option label="date (时间)" value="date"/>
              <el-option label="enum (枚举)" value="enum"/>
              <el-option label="struct (结构体)" value="struct"/>
              <el-option label="array (数组)" value="array"/>
            </el-select>
          </el-form-item>

          <!-- 数值类型配置 (int/float/double) -->
          <template v-if="['int', 'float', 'double'].includes(currentTslItem.dataType.type)">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="最小值">
                  <el-input-number v-model="currentTslItem.dataType.specs.min" style="width: 100%"/>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="最大值">
                  <el-input-number v-model="currentTslItem.dataType.specs.max" style="width: 100%"/>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="步长">
                  <el-input-number v-model="currentTslItem.dataType.specs.step" :min="0" style="width: 100%"/>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="单位">
                  <el-input v-model="currentTslItem.dataType.specs.unit" placeholder="如：℃"/>
                </el-form-item>
              </el-col>
            </el-row>
          </template>

          <!-- 文本类型配置 -->
          <template v-if="currentTslItem.dataType.type === 'text'">
            <el-form-item label="数据长度">
              <el-input-number v-model="currentTslItem.dataType.specs.length" :min="1" :max="10240"
                               placeholder="最大10240" style="width: 100%"/>
            </el-form-item>
          </template>

          <!-- 布尔类型配置 -->
          <template v-if="currentTslItem.dataType.type === 'bool'">
            <el-form-item label="布尔值定义">
              <el-row :gutter="10">
                <el-col :span="12">
                  <el-input v-model="currentTslItem.dataType.specs['0']" placeholder="0 代表的含义，如：关">
                    <template #prepend>0</template>
                  </el-input>
                </el-col>
                <el-col :span="12">
                  <el-input v-model="currentTslItem.dataType.specs['1']" placeholder="1 代表的含义，如：开">
                    <template #prepend>1</template>
                  </el-input>
                </el-col>
              </el-row>
            </el-form-item>
          </template>

          <!-- 枚举类型配置 -->
          <template v-if="currentTslItem.dataType.type === 'enum'">
            <el-form-item label="枚举项" prop="enumJson">
              <el-input type="textarea" v-model="currentTslItem._enumJson"
                        placeholder='{"0": "关机", "1": "开机", "2": "待机"}' rows="4"/>
              <div class="form-tip">请输入JSON格式的键值对，键为数字，值为描述</div>
            </el-form-item>
          </template>

          <!-- 数组类型配置 -->
          <template v-if="currentTslItem.dataType.type === 'array'">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="数组长度">
                  <el-input-number v-model="currentTslItem.dataType.specs.size" :min="1" :max="512"
                                   style="width: 100%"/>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="元素类型">
                  <el-select v-model="currentTslItem.dataType.specs.item.type" placeholder="选择元素类型">
                    <el-option label="int" value="int"/>
                    <el-option label="float" value="float"/>
                    <el-option label="double" value="double"/>
                    <el-option label="text" value="text"/>
                    <el-option label="struct" value="struct"/>
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </template>

          <!-- 结构体类型配置 -->
          <template v-if="currentTslItem.dataType.type === 'struct'">
            <el-form-item label="结构体字段">
              <el-button type="primary" link @click="addStructField" class="mb-2">
                <el-icon>
                  <Plus/>
                </el-icon>
                添加字段
              </el-button>
              <div v-for="(field, idx) in currentTslItem.dataType.specs" :key="idx" class="struct-field-item">
                <el-row :gutter="5">
                  <el-col :span="6">
                    <el-input v-model="field.identifier" placeholder="标识符" size="small"/>
                  </el-col>
                  <el-col :span="6">
                    <el-input v-model="field.name" placeholder="名称" size="small"/>
                  </el-col>
                  <el-col :span="8">
                    <el-select v-model="field.dataType.type" placeholder="类型" size="small">
                      <el-option label="int" value="int"/>
                      <el-option label="float" value="float"/>
                      <el-option label="double" value="double"/>
                      <el-option label="text" value="text"/>
                      <el-option label="bool" value="bool"/>
                      <el-option label="date" value="date"/>
                      <el-option label="enum" value="enum"/>
                    </el-select>
                  </el-col>
                  <el-col :span="4">
                    <el-button type="danger" link @click="removeStructField(idx)">
                      <el-icon>
                        <Delete/>
                      </el-icon>
                    </el-button>
                  </el-col>
                </el-row>
              </div>
            </el-form-item>
          </template>

          <el-form-item label="读写类型" prop="accessMode">
            <el-radio-group v-model="currentTslItem.accessMode">
              <el-radio label="r">只读</el-radio>
              <el-radio label="rw">读写</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>

        <!-- Service Specific -->
        <template v-if="tslDialog.type === 'service'">
          <el-form-item label="调用方式" prop="callType">
            <el-radio-group v-model="currentTslItem.callType">
              <el-radio label="sync">同步</el-radio>
              <el-radio label="async">异步</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-alert title="提示" type="info" show-icon :closable="false" class="mb-4">
            <template #default>
              输入/输出参数请使用"配置参数"功能进行详细设置
            </template>
          </el-alert>
        </template>

        <!-- Event Specific -->
        <template v-if="tslDialog.type === 'event'">
          <el-form-item label="事件类型" prop="type">
            <el-radio-group v-model="currentTslItem.type">
              <el-radio label="info">信息</el-radio>
              <el-radio label="alert">告警</el-radio>
              <el-radio label="error">故障</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-alert title="提示" type="info" show-icon :closable="false" class="mb-4">
            <template #default>
              输出参数请使用"配置参数"功能进行详细设置
            </template>
          </el-alert>
        </template>

        <el-form-item label="描述" prop="description">
          <el-input type="textarea" v-model="currentTslItem.description" rows="3"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitTslItem">确 定</el-button>
          <el-button @click="tslDialog.visible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 服务/事件参数管理对话框 -->
    <el-dialog :title="paramDialog.title" v-model="paramDialog.visible" width="800px" append-to-body>
      <div class="param-management">
        <!-- 输入参数部分（仅服务有） -->
        <template v-if="paramDialog.type === 'service'">
          <div class="param-section">
            <div class="section-header">
              <span class="section-title">输入参数</span>
              <el-button type="primary" size="small" @click="openParamEditDialog('input')">
                <el-icon>
                  <Plus/>
                </el-icon>
                添加参数
              </el-button>
            </div>
            <el-table :data="currentServiceParams.inputData" size="small" border class="param-table">
              <el-table-column prop="identifier" label="标识符" width="120"/>
              <el-table-column prop="name" label="名称" width="120"/>
              <el-table-column label="数据类型" width="100">
                <template #default="scope">
                  <el-tag size="small">{{ scope.row.dataType?.type }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="数据规格">
                <template #default="scope">
                  <span class="text-xs text-gray-600">{{ formatDataSpecs(scope.row.dataType) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120" align="center">
                <template #default="scope">
                  <el-button link type="primary" size="small" @click="editParam('input', scope.$index)">修改</el-button>
                  <el-button link type="danger" size="small" @click="deleteParam('input', scope.$index)">删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </template>

        <!-- 输出参数部分 -->
        <div class="param-section mt-4">
          <div class="section-header">
            <span class="section-title">{{ paramDialog.type === 'service' ? '输出参数' : '输出参数' }}</span>
            <el-button type="primary" size="small" @click="openParamEditDialog('output')">
              <el-icon>
                <Plus/>
              </el-icon>
              添加参数
            </el-button>
          </div>
          <el-table :data="currentServiceParams.outputData" size="small" border class="param-table">
            <el-table-column prop="identifier" label="标识符" width="120"/>
            <el-table-column prop="name" label="名称" width="120"/>
            <el-table-column label="数据类型" width="100">
              <template #default="scope">
                <el-tag size="small">{{ scope.row.dataType?.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="数据规格">
              <template #default="scope">
                <span class="text-xs text-gray-600">{{ formatDataSpecs(scope.row.dataType) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="scope">
                <el-button link type="primary" size="small" @click="editParam('output', scope.$index)">修改</el-button>
                <el-button link type="danger" size="small" @click="deleteParam('output', scope.$index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="saveServiceParams">确 定</el-button>
          <el-button @click="paramDialog.visible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 参数编辑对话框（用于服务/事件的参数） -->
    <el-dialog :title="paramEditDialog.title" v-model="paramEditDialog.visible" width="600px" append-to-body>
      <el-form ref="paramEditRef" :model="currentParam" label-width="100px" :rules="paramEditRules">
        <el-form-item label="标识符" prop="identifier">
          <el-input v-model="currentParam.identifier" placeholder="参数标识符" :disabled="paramEditDialog.isEdit"/>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="currentParam.name" placeholder="参数名称"/>
        </el-form-item>
        <el-form-item label="数据类型" prop="dataType.type">
          <el-select v-model="currentParam.dataType.type" placeholder="选择类型" @change="handleParamDataTypeChange">
            <el-option label="int (整数)" value="int"/>
            <el-option label="float (单精度浮点)" value="float"/>
            <el-option label="double (双精度浮点)" value="double"/>
            <el-option label="text (字符串)" value="text"/>
            <el-option label="bool (布尔型)" value="bool"/>
            <el-option label="date (时间)" value="date"/>
            <el-option label="enum (枚举)" value="enum"/>
            <el-option label="struct (结构体)" value="struct"/>
            <el-option label="array (数组)" value="array"/>
          </el-select>
        </el-form-item>

        <!-- 数据类型详细配置（复用属性的逻辑） -->
        <template v-if="['int', 'float', 'double'].includes(currentParam.dataType.type)">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="最小值">
                <el-input-number v-model="currentParam.dataType.specs.min" style="width: 100%"/>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="最大值">
                <el-input-number v-model="currentParam.dataType.specs.max" style="width: 100%"/>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="步长">
                <el-input-number v-model="currentParam.dataType.specs.step" :min="0" style="width: 100%"/>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="单位">
                <el-input v-model="currentParam.dataType.specs.unit" placeholder="如：℃"/>
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <template v-if="currentParam.dataType.type === 'text'">
          <el-form-item label="数据长度">
            <el-input-number v-model="currentParam.dataType.specs.length" :min="1" :max="10240" style="width: 100%"/>
          </el-form-item>
        </template>

        <template v-if="currentParam.dataType.type === 'bool'">
          <el-form-item label="布尔值定义">
            <el-row :gutter="10">
              <el-col :span="12">
                <el-input v-model="currentParam.dataType.specs['0']" placeholder="0 代表的含义">
                  <template #prepend>0</template>
                </el-input>
              </el-col>
              <el-col :span="12">
                <el-input v-model="currentParam.dataType.specs['1']" placeholder="1 代表的含义">
                  <template #prepend>1</template>
                </el-input>
              </el-col>
            </el-row>
          </el-form-item>
        </template>

        <template v-if="currentParam.dataType.type === 'enum'">
          <el-form-item label="枚举项">
            <el-input type="textarea" v-model="currentParam._enumJson"
                      placeholder='{"0": "关机", "1": "开机"}' rows="3"/>
            <div class="form-tip">JSON格式，键为数字，值为描述</div>
          </el-form-item>
        </template>

        <template v-if="currentParam.dataType.type === 'array'">
          <el-form-item label="数组长度">
            <el-input-number v-model="currentParam.dataType.specs.size" :min="1" :max="512" style="width: 100%"/>
          </el-form-item>
          <el-form-item label="元素类型">
            <el-select v-model="currentParam.dataType.specs.item.type" placeholder="选择元素类型">
              <el-option label="int" value="int"/>
              <el-option label="float" value="float"/>
              <el-option label="double" value="double"/>
              <el-option label="text" value="text"/>
            </el-select>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitParamEdit">确 定</el-button>
          <el-button @click="paramEditDialog.visible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 详情查看对话框 -->
    <el-dialog title="物模型详情" v-model="detailDialog.visible" width="600px" append-to-body>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="标识符">{{ detailDialog.data.identifier }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ detailDialog.data.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          {{ tslDialog.type === 'property' ? '属性' : tslDialog.type === 'service' ? '服务' : '事件' }}
        </el-descriptions-item>
        <el-descriptions-item label="描述">{{ detailDialog.data.description || '-' }}</el-descriptions-item>
        <template v-if="tslDialog.type === 'property'">
          <el-descriptions-item label="数据类型">{{ detailDialog.data.dataType?.type }}</el-descriptions-item>
          <el-descriptions-item label="读写类型">{{
              detailDialog.data.accessMode === 'r' ? '只读' : '读写'
            }}
          </el-descriptions-item>
          <el-descriptions-item label="数据规格">
            <pre>{{ formatSpecsJson(detailDialog.data.dataType) }}</pre>
          </el-descriptions-item>
        </template>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup name="Product">
import {addProduct, delProduct, getProduct, listProduct, updateProduct} from "@/api/product/product"
import {addProductTsl, getProductTsl, updateProductTsl} from "@/api/product/productTsl"
import {listDevice} from "@/api/device/device"

const { proxy } = getCurrentInstance()

const productList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
// 物模型相关变量
const thingModelOpen = ref(false)
const thingModelTitle = ref("")
const thingModelForm = reactive({
  productKey: '',
  productId: '',
  thingModelContent: '',
  isNewModel: false 
})

// 详情页相关变量
const showDetail = ref(false)
const currentProduct = ref({})
const deviceCount = ref(0)
const activeTab = ref('config')
const thingModelData = reactive({
  properties: [],
  services: [],
  events: []
})

// 参数管理相关
const paramDialog = reactive({
  visible: false,
  title: '',
  type: 'service', // 'service' 或 'event'
  serviceIndex: -1
})

const currentServiceParams = reactive({
  inputData: [],
  outputData: []
})

// 参数编辑对话框
const paramEditDialog = reactive({
  visible: false,
  title: '',
  isEdit: false,
  ioType: 'input', // 'input' 或 'output'
  editIndex: -1
})

const currentParam = ref({
  identifier: '',
  name: '',
  dataType: {
    type: 'int',
    specs: {}
  },
  _enumJson: ''
})

const paramEditRules = {
  identifier: [
    {required: true, message: "标识符不能为空", trigger: "blur"},
    {pattern: /^[a-zA-Z0-9_]+$/, message: "只能包含字母、数字和下划线", trigger: "blur"}
  ],
  name: [{required: true, message: "名称不能为空", trigger: "blur"}],
  'dataType.type': [{required: true, message: "数据类型不能为空", trigger: "change"}]
}

// 详情查看对话框
const detailDialog = reactive({
  visible: false,
  data: {}
})

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    productKey: null,
    name: null,
    nodeType: null,
  },
  rules: {
    productKey: [
      { required: true, message: "产品密钥不能为空", trigger: "blur" }
    ],
    name: [
      { required: true, message: "产品名称不能为空", trigger: "blur" }
    ],
    nodeType: [
      { required: true, message: "设备类型不能为空", trigger: "change" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

// 格式化数据规格显示（表格中简短显示）
function formatDataSpecs(dataType) {
  if (!dataType || !dataType.type) return '-'

  const specs = dataType.specs || {}
  const type = dataType.type

  switch (type) {
    case 'int':
    case 'float':
    case 'double':
      let range = ''
      if (specs.min !== undefined && specs.max !== undefined) {
        range = `${specs.min}~${specs.max}`
      }
      if (specs.unit) range += ` ${specs.unit}`
      if (specs.step) range += ` 步长${specs.step}`
      return range || '无限制'
    case 'text':
      return `长度限制: ${specs.length || 10240}`
    case 'bool':
      return `0:${specs['0'] || '假'} 1:${specs['1'] || '真'}`
    case 'enum':
      try {
        return JSON.stringify(specs)
      } catch (e) {
        return '枚举类型'
      }
    case 'date':
      return 'UTC毫秒时间戳'
    case 'array':
      return `数组[${specs.size || '-'}] of ${specs.item?.type || '?'}`
    case 'struct':
      const fields = specs || []
      return `结构体(${fields.length}个字段)`
    default:
      return '-'
  }
}

// 格式化规格为JSON字符串（详情查看）
function formatSpecsJson(dataType) {
  if (!dataType) return ''
  return JSON.stringify(dataType.specs || {}, null, 2)
}

/** 查看产品详情 */
function handleDetail(row) {
  currentProduct.value = row
  showDetail.value = true
  activeTab.value = 'config'
  deviceCount.value = 0 

  listDevice({productId: row.id}).then(res => {
    deviceCount.value = res.total
  })

  getProductTsl(row.id).then(res => {
    if (res.data && res.data.tsl) {
      let tsl = res.data.tsl
      if (typeof tsl === 'string') {
        try {
          tsl = JSON.parse(tsl)
        } catch (e) {
          console.error("Failed to parse TSL", e)
          tsl = {}
        }
      }

      thingModelData.properties = tsl.properties || []
      thingModelData.services = tsl.services || []
      thingModelData.events = tsl.events || []
    } else {
      thingModelData.properties = []
      thingModelData.services = []
      thingModelData.events = []
    }
  })
}

/** 返回列表 */
function handleBack() {
  showDetail.value = false
  currentProduct.value = {}
  deviceCount.value = 0
}

/** 查询产品管理列表 */
function getList() {
  loading.value = true
  listProduct(queryParams.value).then(response => {
    productList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    productKey: null,
    name: null,
    nodeType: null,
    remarks: null
  }
  proxy.resetForm("productRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加产品管理"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getProduct(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改产品管理"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["productRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateProduct(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addProduct(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除产品管理编号为"' + _ids + '"的数据项？').then(function() {
    return delProduct(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('product/product/export', {
    ...queryParams.value
  }, `product_${new Date().getTime()}.xlsx`)
}

/** 物模型设置按钮操作 */
function handleThingModel(row) {
  resetThingModel()
  thingModelForm.productId = row.id
  thingModelForm.productKey = row.productKey
  thingModelTitle.value = "设置物模型 - " + row.name
  
  getProductThingModel(row.id).then(response => {
    thingModelForm.isNewModel = response.data?.isNewModel || false
    
    if (response.data && response.data.thingModel) {
      thingModelForm.thingModelContent = JSON.stringify(response.data.thingModel, null, 2)
    } else {
      thingModelForm.thingModelContent = JSON.stringify({
        "schema": "",
        "profile": {
          "productKey": row.productKey
        },
        "properties": [],
        "services": [],
        "events": []
      }, null, 2)
    }
    thingModelOpen.value = true
  }).catch(() => {
    thingModelForm.thingModelContent = JSON.stringify({
      "schema": "",
      "profile": {
        "productKey": row.productKey
      },
      "properties": [],
      "services": [],
      "events": []
    }, null, 2)
    thingModelForm.isNewModel = true
    thingModelOpen.value = true
  })
}

/** 保存物模型 */
function submitThingModel() {
  try {
    const thingModel = JSON.parse(thingModelForm.thingModelContent)

    if (!thingModel.schema) {
      thingModel.schema = ""
    }
    if (!thingModel.profile) {
      thingModel.profile = {productKey: thingModelForm.productKey}
    } else {
      thingModel.profile.productKey = thingModelForm.productKey
    }

    const submitData = {
      productId: thingModelForm.productId,
      productKey: thingModelForm.productKey,
      thingModel: thingModel,
      isNewModel: thingModelForm.isNewModel 
    }
    
    saveProductThingModel(submitData).then(response => {
      proxy.$modal.msgSuccess("物模型保存成功")
      thingModelOpen.value = false
      // 刷新详情数据
      if (showDetail.value) {
        handleDetail(currentProduct.value)
      }
    }).catch(error => {
      proxy.$modal.msgError("物模型保存失败: " + (error.message || '未知错误'))
    })
  } catch (e) {
    proxy.$modal.msgError("物模型格式不正确，请检查JSON格式")
  }
}

/** 取消物模型编辑 */
function cancelThingModel() {
  thingModelOpen.value = false
  resetThingModel()
}

/** 重置物模型表单 */
function resetThingModel() {
  thingModelForm.productKey = ''
  thingModelForm.productId = ''
  thingModelForm.thingModelContent = ''
  thingModelForm.isNewModel = false 
}

// 将本地函数替换为从API导入的函数
const getProductThingModel = (productId) => {
  return getProductTsl(productId).then(response => {
    const hasExistingModel = response.data && response.data.tsl !== null && response.data.tsl !== undefined
    let tsl = response.data?.tsl || {}
    if (typeof tsl === 'string') {
      try {
        tsl = JSON.parse(tsl)
      } catch (e) {
        tsl = {}
      }
    }
    return {
      data: {
        thingModel: tsl,
        isNewModel: !hasExistingModel 
      }
    }
  })
}

const saveProductThingModel = (data) => {
  const productTsl = {
    productId: data.productId,
    tsl: data.thingModel
  }
  if (data.isNewModel) {
    return addProductTsl(productTsl)
  } else {
    return updateProductTsl(productTsl)
  }
}

// TSL Editing Logic
const tslDialog = reactive({
  visible: false,
  title: '',
  type: '',
  isEdit: false,
  editIndex: -1
})

const currentTslItem = ref({
  identifier: '',
  name: '',
  dataType: {
    type: 'int',
    specs: {}
  }
})

const tslItemRules = {
  identifier: [
    {required: true, message: "标识符不能为空", trigger: "blur"},
    {pattern: /^[a-zA-Z0-9_]+$/, message: "标识符只能包含字母、数字和下划线", trigger: "blur"}
  ],
  name: [{required: true, message: "名称不能为空", trigger: "blur"}],
  'dataType.type': [{required: true, message: "数据类型不能为空", trigger: "change"}]
}

function handleDataTypeChange(type) {
  // 重置specs
  if (!currentTslItem.value.dataType.specs) {
    currentTslItem.value.dataType.specs = {}
  }

  // 根据类型初始化默认specs
  switch (type) {
    case 'int':
    case 'float':
    case 'double':
      currentTslItem.value.dataType.specs = {min: 0, max: 100, step: 1}
      break
    case 'text':
      currentTslItem.value.dataType.specs = {length: 1024}
      break
    case 'bool':
      currentTslItem.value.dataType.specs = {'0': '假', '1': '真'}
      break
    case 'enum':
      currentTslItem.value.dataType.specs = {}
      break
    case 'date':
      currentTslItem.value.dataType.specs = {}
      break
    case 'array':
      currentTslItem.value.dataType.specs = {size: 10, item: {type: 'int'}}
      break
    case 'struct':
      currentTslItem.value.dataType.specs = []
      break
  }
}

function addStructField() {
  if (!Array.isArray(currentTslItem.value.dataType.specs)) {
    currentTslItem.value.dataType.specs = []
  }
  currentTslItem.value.dataType.specs.push({
    identifier: '',
    name: '',
    dataType: {
      type: 'int',
      specs: {}
    }
  })
}

function removeStructField(index) {
  currentTslItem.value.dataType.specs.splice(index, 1)
}

function openTslDialog(type) {
  tslDialog.type = type
  tslDialog.isEdit = false
  tslDialog.editIndex = -1
  tslDialog.visible = true

  if (type === 'property') {
    tslDialog.title = '添加属性'
    currentTslItem.value = {
      identifier: '',
      name: '',
      accessMode: 'rw',
      dataType: {
        type: 'int',
        specs: {min: 0, max: 100, step: 1}
      },
      description: '',
      _enumJson: ''
    }
  } else if (type === 'service') {
    tslDialog.title = '添加功能'
    currentTslItem.value = {
      identifier: '',
      name: '',
      callType: 'sync',
      inputData: [],
      outputData: [],
      description: ''
    }
  } else if (type === 'event') {
    tslDialog.title = '添加事件'
    currentTslItem.value = {
      identifier: '',
      name: '',
      type: 'info',
      outputData: [],
      description: ''
    }
  }
}

function handleEditTslItem(type, row, index) {
  tslDialog.type = type
  tslDialog.isEdit = true
  tslDialog.editIndex = index
  tslDialog.visible = true

  // Deep copy
  currentTslItem.value = JSON.parse(JSON.stringify(row))

  // Handle enum JSON for display
  if ((type === 'property' || type === 'service' || type === 'event') && currentTslItem.value.dataType?.type === 'enum') {
    currentTslItem.value._enumJson = JSON.stringify(currentTslItem.value.dataType.specs || {}, null, 2)
  } else {
    currentTslItem.value._enumJson = ''
  }

  if (type === 'property') {
    tslDialog.title = '修改属性'
    if (!currentTslItem.value.dataType) {
      currentTslItem.value.dataType = {type: 'int', specs: {}}
    }
    if (!currentTslItem.value.dataType.specs) {
      currentTslItem.value.dataType.specs = {}
    }
    // 确保struct类型的specs是数组
    if (currentTslItem.value.dataType.type === 'struct' && !Array.isArray(currentTslItem.value.dataType.specs)) {
      currentTslItem.value.dataType.specs = []
    }
  } else if (type === 'service') {
    tslDialog.title = '修改功能'
  } else if (type === 'event') {
    tslDialog.title = '修改事件'
  }
}

function handleDeleteTslItem(type, index) {
  proxy.$modal.confirm('确认删除该项吗？').then(() => {
    if (type === 'property') {
      thingModelData.properties.splice(index, 1)
    } else if (type === 'service') {
      thingModelData.services.splice(index, 1)
    } else if (type === 'event') {
      thingModelData.events.splice(index, 1)
    }
    proxy.$modal.msgSuccess("删除成功，请记得保存物模型")
  })
}

function handleViewTslDetail(type, row) {
  detailDialog.data = row
  tslDialog.type = type
  detailDialog.visible = true
}

function submitTslItem() {
  proxy.$refs["tslItemRef"].validate(valid => {
    if (valid) {
      // Handle Enum JSON parsing
      if (currentTslItem.value.dataType?.type === 'enum' && currentTslItem.value._enumJson) {
        try {
          const specs = JSON.parse(currentTslItem.value._enumJson || '{}')
          currentTslItem.value.dataType.specs = specs
        } catch (e) {
          proxy.$modal.msgError("枚举项JSON格式错误")
          return
        }
      }

      // Clean up temporary fields
      delete currentTslItem.value._enumJson

      const item = JSON.parse(JSON.stringify(currentTslItem.value))

      if (tslDialog.type === 'property') {
        if (tslDialog.isEdit) {
          thingModelData.properties.splice(tslDialog.editIndex, 1, item)
        } else {
          if (thingModelData.properties.some(p => p.identifier === item.identifier)) {
            proxy.$modal.msgError("标识符已存在")
            return
          }
          thingModelData.properties.push(item)
        }
      } else if (tslDialog.type === 'service') {
        if (tslDialog.isEdit) {
          thingModelData.services.splice(tslDialog.editIndex, 1, item)
        } else {
          if (thingModelData.services.some(s => s.identifier === item.identifier)) {
            proxy.$modal.msgError("标识符已存在")
            return
          }
          thingModelData.services.push(item)
        }
      } else if (tslDialog.type === 'event') {
        if (tslDialog.isEdit) {
          thingModelData.events.splice(tslDialog.editIndex, 1, item)
        } else {
          if (thingModelData.events.some(e => e.identifier === item.identifier)) {
            proxy.$modal.msgError("标识符已存在")
            return
          }
          thingModelData.events.push(item)
        }
      }

      tslDialog.visible = false
      proxy.$modal.msgSuccess(tslDialog.isEdit ? "修改成功" : "添加成功")
    }
  })
}

function saveTslToBackend() {
  const tslContent = {
    schema: "",
    profile: {
      productKey: currentProduct.value.productKey
    },
    properties: thingModelData.properties,
    services: thingModelData.services,
    events: thingModelData.events
  }

  const submitData = {
    productId: currentProduct.value.id,
    productKey: currentProduct.value.productKey,
    thingModel: tslContent,
    isNewModel: false
  }

  saveProductThingModel(submitData).then(response => {
    proxy.$modal.msgSuccess("物模型保存成功")
  }).catch(error => {
    proxy.$modal.msgError("保存失败: " + error.message)
  })
}

// 服务/事件参数管理函数
function manageServiceParams(row, index) {
  paramDialog.type = 'service'
  paramDialog.serviceIndex = index
  paramDialog.title = `配置参数 - ${row.name}`

  currentServiceParams.inputData = row.inputData ? JSON.parse(JSON.stringify(row.inputData)) : []
  currentServiceParams.outputData = row.outputData ? JSON.parse(JSON.stringify(row.outputData)) : []

  paramDialog.visible = true
}

function manageEventParams(row, index) {
  paramDialog.type = 'event'
  paramDialog.serviceIndex = index
  paramDialog.title = `配置参数 - ${row.name}`

  currentServiceParams.inputData = []
  currentServiceParams.outputData = row.outputData ? JSON.parse(JSON.stringify(row.outputData)) : []

  paramDialog.visible = true
}

function openParamEditDialog(ioType) {
  paramEditDialog.ioType = ioType
  paramEditDialog.isEdit = false
  paramEditDialog.editIndex = -1
  paramEditDialog.visible = true
  paramEditDialog.title = ioType === 'input' ? '添加输入参数' : '添加输出参数'

  currentParam.value = {
    identifier: '',
    name: '',
    dataType: {
      type: 'int',
      specs: {min: 0, max: 100, step: 1}
    },
    _enumJson: ''
  }
}

function editParam(ioType, index) {
  paramEditDialog.ioType = ioType
  paramEditDialog.isEdit = true
  paramEditDialog.editIndex = index
  paramEditDialog.visible = true
  paramEditDialog.title = '编辑参数'

  const source = ioType === 'input' ? currentServiceParams.inputData : currentServiceParams.outputData
  currentParam.value = JSON.parse(JSON.stringify(source[index]))

  if (currentParam.value.dataType?.type === 'enum') {
    currentParam.value._enumJson = JSON.stringify(currentParam.value.dataType.specs || {}, null, 2)
  } else {
    currentParam.value._enumJson = ''
  }
}

function deleteParam(ioType, index) {
  if (ioType === 'input') {
    currentServiceParams.inputData.splice(index, 1)
  } else {
    currentServiceParams.outputData.splice(index, 1)
  }
}

function handleParamDataTypeChange(type) {
  if (!currentParam.value.dataType) {
    currentParam.value.dataType = {type: type, specs: {}}
  }

  switch (type) {
    case 'int':
    case 'float':
    case 'double':
      currentParam.value.dataType.specs = {min: 0, max: 100, step: 1}
      break
    case 'text':
      currentParam.value.dataType.specs = {length: 1024}
      break
    case 'bool':
      currentParam.value.dataType.specs = {'0': '假', '1': '真'}
      break
    case 'enum':
      currentParam.value.dataType.specs = {}
      break
    case 'date':
      currentParam.value.dataType.specs = {}
      break
    case 'array':
      currentParam.value.dataType.specs = {size: 10, item: {type: 'int'}}
      break
    case 'struct':
      // struct在参数中通常为空对象或预定义结构，暂不支持嵌套struct定义
      currentParam.value.dataType.specs = {}
      break
  }
}

function submitParamEdit() {
  proxy.$refs["paramEditRef"].validate(valid => {
    if (valid) {
      // Handle Enum JSON parsing
      if (currentParam.value.dataType?.type === 'enum' && currentParam.value._enumJson) {
        try {
          const specs = JSON.parse(currentParam.value._enumJson || '{}')
          currentParam.value.dataType.specs = specs
        } catch (e) {
          proxy.$modal.msgError("枚举项JSON格式错误")
          return
        }
      }

      delete currentParam.value._enumJson
      const item = JSON.parse(JSON.stringify(currentParam.value))

      if (paramEditDialog.isEdit) {
        if (paramEditDialog.ioType === 'input') {
          currentServiceParams.inputData.splice(paramEditDialog.editIndex, 1, item)
        } else {
          currentServiceParams.outputData.splice(paramEditDialog.editIndex, 1, item)
        }
      } else {
        // Check duplicate identifier
        const allParams = [...currentServiceParams.inputData, ...currentServiceParams.outputData]
        if (allParams.some(p => p.identifier === item.identifier)) {
          proxy.$modal.msgError("参数标识符已存在")
          return
        }

        if (paramEditDialog.ioType === 'input') {
          currentServiceParams.inputData.push(item)
        } else {
          currentServiceParams.outputData.push(item)
        }
      }

      paramEditDialog.visible = false
    }
  })
}

function saveServiceParams() {
  if (paramDialog.type === 'service') {
    thingModelData.services[paramDialog.serviceIndex].inputData = currentServiceParams.inputData
    thingModelData.services[paramDialog.serviceIndex].outputData = currentServiceParams.outputData
  } else {
    thingModelData.events[paramDialog.serviceIndex].outputData = currentServiceParams.outputData
  }

  paramDialog.visible = false
  proxy.$modal.msgSuccess("参数配置已更新，请记得保存物模型")
}

getList()
</script>

<style scoped>
.product-detail {
  padding: 20px;
  background-color: #fff;
}

.detail-header {
  border-bottom: 1px solid #eee;
  padding-bottom: 20px;
}

.detail-info {
  font-size: 14px;
  color: #606266;
}

.info-label {
  font-weight: bold;
}

.info-value {
  margin-left: 8px;
}

.detail-tabs {
  min-height: 400px;
}

.specs-text {
  line-height: 1.4;
}

.param-management .param-section {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 16px;
  background-color: #f5f7fa;
}

.param-management .section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.param-management .section-title {
  font-weight: bold;
  font-size: 14px;
  color: #303133;
}

.param-management .param-table {
  background-color: #fff;
}

.struct-field-item {
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
  margin-bottom: 8px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}

pre {
  margin: 0;
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
  max-height: 200px;
  overflow: auto;
}

.product-card {
  transition: all 0.3s;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.product-name {
  font-weight: bold;
  font-size: 16px;
  cursor: pointer;
  color: #409EFF;
}

.card-content {
  flex: 1;
  margin: 15px 0;
}

.info-item {
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
  display: flex;
  align-items: flex-start;
}

.info-item .label {
  width: 80px;
  flex-shrink: 0;
  color: #909399;
}

.info-item .value {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-footer {
  border-top: 1px solid #EBEEF5;
  padding-top: 10px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>