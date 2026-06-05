<template>
  <el-drawer :model-value="visible" size="90%" @close="handleClose">
    <template #header>
      <div style="display: flex; align-items: center; justify-content: space-between; width: 100%;">
        <div>
          <h3 style="margin: 0; font-size: 16px;">脚本编辑器</h3>
          <p style="margin: 4px 0 0; font-size: 13px; color: #86909c;">
            {{ triggerMode === 'REALTIME' ? '实时触发 — run(TriggerMessage msg)' : '周期触发 — run()' }}
          </p>
        </div>
        <div style="display: flex; gap: 8px;">
          <el-button @click="handleTest" :loading="testing">
            <el-icon><VideoPlay /></el-icon> 测试运行
          </el-button>
          <el-button type="primary" @click="handleSave" :loading="saving">
            <el-icon><Check /></el-icon> 保存
          </el-button>
        </div>
      </div>
    </template>

    <div class="script-editor-layout">
      <!-- 左侧：工具面板 -->
      <div class="tool-panel">
        <h4 class="panel-title">预置工具</h4>
        <el-collapse v-model="expandedTools">
          <el-collapse-item v-for="group in toolGroups" :key="group.name" :title="group.name" :name="group.name">
            <div v-for="tool in group.tools" :key="tool.sign" class="tool-item" @click="insertSnippet(tool.snippet)">
              <div class="tool-name">{{ tool.name }}</div>
              <code class="tool-sign">{{ tool.sign }}</code>
              <p class="tool-desc">{{ tool.desc }}</p>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>

      <!-- 中间：编辑区 -->
      <div class="blockly-area">
        <div class="area-header">
          <span class="area-label">可视化编程</span>
          <el-radio-group v-model="editMode" size="small" @change="handleModeChange">
            <el-radio-button value="blockly">拼图模式</el-radio-button>
            <el-radio-button value="code">代码模式</el-radio-button>
          </el-radio-group>
        </div>

        <!-- Blockly 工作区 -->
        <div v-show="editMode === 'blockly'" class="blockly-workspace" ref="blocklyContainer"></div>

        <!-- 代码编辑区 -->
        <div v-show="editMode === 'code'" class="code-area">
          <div class="code-template" v-if="!codeContent">
            <el-button @click="applyTemplate">加载脚本模板</el-button>
          </div>
          <textarea
            ref="codeTextarea"
            v-model="codeContent"
            class="code-editor"
            spellcheck="false"
            placeholder="在此编辑 Groovy 脚本..."
          />
        </div>
      </div>

      <!-- 右侧：代码预览（仅拼图模式） -->
      <div class="preview-panel" v-show="editMode === 'blockly'">
        <div class="area-header">
          <span class="area-label">生成代码预览</span>
          <el-button size="small" text @click="syncCodeFromBlockly">同步到编辑器</el-button>
        </div>
        <pre class="code-preview"><code>{{ blocklyCode || '// 从 Blockly 工作区生成的代码将在此显示' }}</code></pre>
      </div>
    </div>

    <!-- 测试结果 -->
    <el-dialog v-model="testResultVisible" title="测试运行结果" width="520px" append-to-body>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="状态">
          <el-tag v-if="testResult" :type="testResult.status === 'SUCCESS' ? 'success' : 'danger'" effect="dark">
            {{ testResult.status === 'SUCCESS' ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="testResult" label="耗时">{{ testResult.durationMs }}ms</el-descriptions-item>
        <el-descriptions-item v-if="testResult" label="输出">
          <pre style="margin: 0; font-size: 12px; white-space: pre-wrap;">{{ testResult.output }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="testResult?.errorMsg" label="错误">
          <pre style="margin: 0; font-size: 12px; color: #f53f3f; white-space: pre-wrap;">{{ testResult.errorMsg }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoPlay, Check } from '@element-plus/icons-vue'
import { getCompositeAlarmDetail, updateScriptCode, testCompositeAlarm, type CompositeAlarmLog } from '@/api/compositeAlarm'
import * as Blockly from 'blockly'
import { JavascriptGenerator, Order } from 'blockly/javascript'
import 'blockly/blocks'

// ==================== Blockly 自定义块定义 ====================

const ALARM_COLOUR = 20       // 红橙色 - 告警查询
const DEVICE_COLOUR = 160     // 绿色 - 设备数据
const TOOL_COLOUR = 260       // 紫色 - 工具方法
const LOG_COLOUR = 40         // 黄色 - 日志输出
const CONTROL_COLOUR = 210    // 蓝色 - 控制流
const RETURN_COLOUR = 0       // 红色 - 告警返回

function defineCustomBlocks() {
  // 告警返回块
  Blockly.Blocks['alarm_return'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('触发告警')
        .appendField(new Blockly.FieldDropdown([
          ['一级(提示)', '1'],
          ['二级(警告)', '2'],
          ['三级(危险)', '3']
        ]), 'LEVEL')
      this.appendValueInput('MESSAGE')
        .setCheck('String')
        .appendField('消息')
      this.appendValueInput('DETAIL')
        .setCheck('String')
        .appendField('详情')
      this.setPreviousStatement(true, null)
      this.setNextStatement(true, null)
      this.setColour(RETURN_COLOUR)
      this.setTooltip('返回告警结果，触发告警通知')
    }
  }

  // 无告警返回
  Blockly.Blocks['alarm_noop'] = {
    init: function () {
      this.appendDummyInput().appendField('无告警返回')
      this.setPreviousStatement(true, null)
      this.setNextStatement(true, null)
      this.setColour(RETURN_COLOUR)
      this.setTooltip('返回 null，不触发告警')
    }
  }

  // 查询告警记录
  Blockly.Blocks['alarm_query'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('查询告警')
        .appendField(new Blockly.FieldDropdown([
          ['全部', '0'],
          ['一级', '1'],
          ['二级', '2'],
          ['三级', '3']
        ]), 'LEVEL')
      this.appendValueInput('POINT_ID')
        .setCheck('String')
        .appendField('点位ID')
      this.appendValueInput('SINCE')
        .setCheck('String')
        .appendField('起始时间')
      this.setOutput(true, 'Array')
      this.setColour(ALARM_COLOUR)
      this.setTooltip('查询阈值告警或综合告警记录')
    }
  }

  // 查询设备列表
  Blockly.Blocks['query_devices'] = {
    init: function () {
      this.appendValueInput('POINT_ID')
        .setCheck('String')
        .appendField('查询设备列表 点位')
      this.setOutput(true, 'Array')
      this.setColour(DEVICE_COLOUR)
      this.setTooltip('查询隐患点下所有设备')
    }
  }

  // 查询历史数据
  Blockly.Blocks['query_history'] = {
    init: function () {
      this.appendValueInput('DEVICE_ID')
        .setCheck('String')
        .appendField('查询历史 设备')
      this.appendValueInput('SENSOR_CODE')
        .setCheck('String')
        .appendField('传感器')
      this.appendValueInput('START')
        .setCheck('String')
        .appendField('起始')
      this.appendValueInput('END')
        .setCheck('String')
        .appendField('结束')
      this.setOutput(true, 'Array')
      this.setColour(DEVICE_COLOUR)
      this.setTooltip('从 IoTDB 查询历史时序数据')
    }
  }

  // 查询最新数据
  Blockly.Blocks['query_latest'] = {
    init: function () {
      this.appendValueInput('DEVICE_ID')
        .setCheck('String')
        .appendField('查询最新数据 设备')
      this.appendValueInput('SENSOR_CODE')
        .setCheck('String')
        .appendField('传感器')
      this.setOutput(true, null)
      this.setColour(DEVICE_COLOUR)
      this.setTooltip('查询设备最新一条传感器数据')
    }
  }

  // 查询天气数据
  Blockly.Blocks['query_weather'] = {
    init: function () {
      this.appendValueInput('POINT_ID')
        .setCheck('String')
        .appendField('查询天气 点位')
      this.setOutput(true, null)
      this.setColour(DEVICE_COLOUR)
      this.setTooltip('查询隐患点位置天气信息')
    }
  }

  // 调用算法
  Blockly.Blocks['invoke_algorithm'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('调用算法')
        .appendField(new Blockly.FieldTextInput('slope_stability'), 'NAME')
      this.appendValueInput('PARAMS')
        .appendField('参数(Map)')
      this.setOutput(true, null)
      this.setColour(TOOL_COLOUR)
      this.setTooltip('调用预置 Python 算法')
    }
  }

  // 存储数据
  Blockly.Blocks['store_data'] = {
    init: function () {
      this.appendValueInput('KEY')
        .setCheck('String')
        .appendField('存储 键')
      this.appendValueInput('VALUE')
        .appendField('值')
      this.appendValueInput('TTL')
        .setCheck('Number')
        .appendField('TTL(秒)')
      this.setPreviousStatement(true, null)
      this.setNextStatement(true, null)
      this.setColour(TOOL_COLOUR)
      this.setTooltip('策略级暂存数据，跨次执行持久化')
    }
  }

  // 读取数据
  Blockly.Blocks['get_data'] = {
    init: function () {
      this.appendValueInput('KEY')
        .setCheck('String')
        .appendField('读取暂存')
      this.setOutput(true, null)
      this.setColour(TOOL_COLOUR)
      this.setTooltip('读取策略暂存数据')
    }
  }

  // 获取应用范围
  Blockly.Blocks['get_scopes'] = {
    init: function () {
      this.appendDummyInput().appendField('获取应用范围(隐患点列表)')
      this.setOutput(true, 'Array')
      this.setColour(TOOL_COLOUR)
      this.setTooltip('获取策略绑定的所有隐患点')
    }
  }

  // 日志块
  const logLevels = [
    { type: 'log_info', label: 'INFO', colour: LOG_COLOUR },
    { type: 'log_warn', label: 'WARN', colour: LOG_COLOUR },
    { type: 'log_error', label: 'ERROR', colour: LOG_COLOUR }
  ]
  for (const lv of logLevels) {
    Blockly.Blocks[lv.type] = {
      init: function () {
        this.appendValueInput('MSG')
          .setCheck('String')
          .appendField(`${lv.label} 日志`)
        this.setPreviousStatement(true, null)
        this.setNextStatement(true, null)
        this.setColour(lv.colour)
        this.setTooltip(`记录${lv.label}级别日志`)
      }
    }
  }

  // 获取触发消息字段
  Blockly.Blocks['trigger_field'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('触发消息')
        .appendField(new Blockly.FieldDropdown([
          ['来源类型', 'sourceType'],
          ['来源ID', 'sourceId'],
          ['设备ID', 'payload.deviceId'],
          ['数值', 'payload.value']
        ]), 'FIELD')
      this.setOutput(true, 'String')
      this.setColour(CONTROL_COLOUR)
      this.setTooltip('获取实时触发消息中的字段值')
    }
  }

  // 传感器代码常量
  Blockly.Blocks['sensor_code'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('传感器')
        .appendField(new Blockly.FieldDropdown([
          ['位移(displacement)', 'displacement'],
          ['降雨量(rainfall)', 'rainfall'],
          ['水位(water_level)', 'water_level'],
          ['土壤湿度(soil_moisture)', 'soil_moisture'],
          ['温度(temperature)', 'temperature']
        ]), 'CODE')
      this.setOutput(true, 'String')
      this.setColour(DEVICE_COLOUR)
      this.setTooltip('选择传感器代码')
    }
  }

  // 数值比较判断（简化版）
  Blockly.Blocks['threshold_check'] = {
    init: function () {
      this.appendValueInput('VALUE')
        .appendField('判断')
      this.appendDummyInput()
        .appendField(new Blockly.FieldDropdown([
          ['>', '>'],
          ['>=', '>='],
          ['<', '<'],
          ['<=', '<='],
          ['==', '==']
        ]), 'OP')
      this.appendValueInput('THRESHOLD')
        .appendField('阈值')
      this.setOutput(true, 'Boolean')
      this.setColour(CONTROL_COLOUR)
      this.setTooltip('数值阈值判断')
    }
  }

  // 数值累加
  Blockly.Blocks['score_add'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('评分累加')
        .appendField(new Blockly.FieldTextInput('score'), 'VAR')
        .appendField('+')
        .appendField(new Blockly.FieldNumber(1, 0), 'DELTA')
      this.setPreviousStatement(true, null)
      this.setNextStatement(true, null)
      this.setColour(CONTROL_COLOUR)
      this.setTooltip('向评分变量累加分数')
    }
  }

  // 字符串拼接
  Blockly.Blocks['string_concat'] = {
    init: function () {
      this.appendValueInput('A').appendField('拼接')
      this.appendValueInput('B').appendField('+')
      this.setOutput(true, 'String')
      this.setColour(160)
      this.setTooltip('字符串拼接')
    }
  }

  // 定义变量 def
  Blockly.Blocks['def_variable'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('def')
        .appendField(new Blockly.FieldTextInput('result'), 'VAR')
        .appendField('=')
      this.appendValueInput('VALUE')
      this.setPreviousStatement(true, null)
      this.setNextStatement(true, null)
      this.setColour(330)
      this.setTooltip('定义 Groovy 变量')
    }
  }

  // 读取数据属性 .value
  Blockly.Blocks['data_value'] = {
    init: function () {
      this.appendValueInput('OBJ')
        .appendField('数据')
      this.appendDummyInput()
        .appendField('.')
        .appendField(new Blockly.FieldDropdown([
          ['value', 'value'],
          ['name', 'name'],
          ['id', 'id'],
          ['time', 'time']
        ]), 'PROP')
      this.setOutput(true, null)
      this.setColour(DEVICE_COLOUR)
      this.setTooltip('读取数据的属性字段')
    }
  }
}

// ==================== Groovy 代码生成器 ====================

// 使用 javascriptGenerator 作为基础，因为它和 Groovy 语法足够接近
function createGroovyGenerator(): JavascriptGenerator {
  const gen = new JavascriptGenerator('Groovy')

  // 重写 init 以使用 def 替代 var
  const origInit = gen.init.bind(gen)
  gen.init = function (workspace) {
    origInit(workspace)
    gen.nameDB_!.setVariableMap(workspace.getVariableMap())
  }

  // 重写 finish 以使用 def 声明变量
  const origFinish = gen.finish.bind(gen)
  gen.finish = function (code: string): string {
    // 将 var xxx = 替换为 def xxx =
    const defCode = (gen as any).definitions_ || {}
    const varDefs: string[] = []
    for (const key of Object.keys(defCode)) {
      if (key.startsWith('var_')) {
        varDefs.push(defCode[key].replace(/^var /, 'def '))
      }
    }
    return varDefs.filter(d => d.trim()).join('\n') + (varDefs.length ? '\n' : '') + code
  }

  // 注册自定义块的代码生成器

  // alarm_return
  gen.forBlock['alarm_return'] = function (block, generator) {
    const level = block.getFieldValue('LEVEL')
    const message = generator.valueToCode(block, 'MESSAGE', Order.ATOMIC) || "''"
    const detail = generator.valueToCode(block, 'DETAIL', Order.ATOMIC) || "''"
    return `return [level: ${level}, message: ${message}, detail: ${detail}]\n`
  }

  // alarm_noop
  gen.forBlock['alarm_noop'] = function () {
    return 'return null\n'
  }

  // alarm_query
  gen.forBlock['alarm_query'] = function (block, generator) {
    const level = block.getFieldValue('LEVEL')
    const pointId = generator.valueToCode(block, 'POINT_ID', Order.ATOMIC) || 'null'
    const since = generator.valueToCode(block, 'SINCE', Order.ATOMIC) || 'null'
    const levelArg = level === '0' ? 'null' : level
    return [`queryAlarms(${levelArg}, ${pointId}, ${since})`, Order.FUNCTION_CALL]
  }

  // query_devices
  gen.forBlock['query_devices'] = function (block, generator) {
    const pointId = generator.valueToCode(block, 'POINT_ID', Order.ATOMIC) || 'null'
    return [`queryDevices(${pointId})`, Order.FUNCTION_CALL]
  }

  // query_history
  gen.forBlock['query_history'] = function (block, generator) {
    const deviceId = generator.valueToCode(block, 'DEVICE_ID', Order.ATOMIC) || 'null'
    const sensor = generator.valueToCode(block, 'SENSOR_CODE', Order.ATOMIC) || "'displacement'"
    const start = generator.valueToCode(block, 'START', Order.ATOMIC) || 'null'
    const end = generator.valueToCode(block, 'END', Order.ATOMIC) || 'null'
    return [`queryHistory(${deviceId}, ${sensor}, ${start}, ${end})`, Order.FUNCTION_CALL]
  }

  // query_latest
  gen.forBlock['query_latest'] = function (block, generator) {
    const deviceId = generator.valueToCode(block, 'DEVICE_ID', Order.ATOMIC) || 'null'
    const sensor = generator.valueToCode(block, 'SENSOR_CODE', Order.ATOMIC) || "'displacement'"
    return [`queryLatest(${deviceId}, ${sensor})`, Order.FUNCTION_CALL]
  }

  // query_weather
  gen.forBlock['query_weather'] = function (block, generator) {
    const pointId = generator.valueToCode(block, 'POINT_ID', Order.ATOMIC) || 'null'
    return [`queryWeather(${pointId})`, Order.FUNCTION_CALL]
  }

  // invoke_algorithm
  gen.forBlock['invoke_algorithm'] = function (block, generator) {
    const name = block.getFieldValue('NAME')
    const params = generator.valueToCode(block, 'PARAMS', Order.ATOMIC) || '[:]'
    return [`invokeAlgorithm('${name}', ${params})`, Order.FUNCTION_CALL]
  }

  // store_data
  gen.forBlock['store_data'] = function (block, generator) {
    const key = generator.valueToCode(block, 'KEY', Order.ATOMIC) || "''"
    const value = generator.valueToCode(block, 'VALUE', Order.ATOMIC) || 'null'
    const ttl = generator.valueToCode(block, 'TTL', Order.ATOMIC) || '0'
    return `storeData(${key}, ${value}, ${ttl})\n`
  }

  // get_data
  gen.forBlock['get_data'] = function (block, generator) {
    const key = generator.valueToCode(block, 'KEY', Order.ATOMIC) || "''"
    return [`getData(${key})`, Order.FUNCTION_CALL]
  }

  // get_scopes
  gen.forBlock['get_scopes'] = function () {
    return ['getScopes()', Order.FUNCTION_CALL]
  }

  // log blocks
  gen.forBlock['log_info'] = function (block, generator) {
    const msg = generator.valueToCode(block, 'MSG', Order.ATOMIC) || "''"
    return `logInfo(${msg})\n`
  }
  gen.forBlock['log_warn'] = function (block, generator) {
    const msg = generator.valueToCode(block, 'MSG', Order.ATOMIC) || "''"
    return `logWarn(${msg})\n`
  }
  gen.forBlock['log_error'] = function (block, generator) {
    const msg = generator.valueToCode(block, 'MSG', Order.ATOMIC) || "''"
    return `logError(${msg})\n`
  }

  // trigger_field
  gen.forBlock['trigger_field'] = function (block) {
    const field = block.getFieldValue('FIELD')
    return [`msg.${field}`, Order.MEMBER]
  }

  // sensor_code
  gen.forBlock['sensor_code'] = function (block) {
    const code = block.getFieldValue('CODE')
    return [`'${code}'`, Order.ATOMIC]
  }

  // threshold_check
  gen.forBlock['threshold_check'] = function (block, generator) {
    const value = generator.valueToCode(block, 'VALUE', Order.ATOMIC) || '0'
    const op = block.getFieldValue('OP')
    const threshold = generator.valueToCode(block, 'THRESHOLD', Order.ATOMIC) || '0'
    return [`${value} ${op} ${threshold}`, Order.RELATIONAL]
  }

  // score_add
  gen.forBlock['score_add'] = function (block) {
    const v = block.getFieldValue('VAR')
    const d = block.getFieldValue('DELTA')
    return `${v} += ${d}\n`
  }

  // string_concat
  gen.forBlock['string_concat'] = function (block, generator) {
    const a = generator.valueToCode(block, 'A', Order.ATOMIC) || "''"
    const b = generator.valueToCode(block, 'B', Order.ATOMIC) || "''"
    return [`${a} + ${b}`, Order.ADDITION]
  }

  // def_variable
  gen.forBlock['def_variable'] = function (block, generator) {
    const v = block.getFieldValue('VAR')
    const val = generator.valueToCode(block, 'VALUE', Order.ATOMIC) || 'null'
    return `def ${v} = ${val}\n`
  }

  // data_value
  gen.forBlock['data_value'] = function (block, generator) {
    const obj = generator.valueToCode(block, 'OBJ', Order.ATOMIC) || 'data'
    const prop = block.getFieldValue('PROP')
    return [`${obj}.${prop}`, Order.MEMBER]
  }

  // 覆盖内置块的变量声明方式: 用 def 替代 var
  gen.forBlock['variables_set'] = function (block: any, generator: any) {
    const varName = generator.getVariableName(block.getFieldValue('VAR'))
    const value = generator.valueToCode(block, 'VALUE', Order.ASSIGNMENT) || '0'
    return `${varName} = ${value}\n`
  }

  gen.forBlock['variables_get'] = function (block: any, generator: any) {
    const varName = generator.getVariableName(block.getFieldValue('VAR'))
    return [varName, Order.ATOMIC]
  }

  // 覆盖 for 循环: 用 Groovy for-in 语法
  gen.forBlock['controls_forEach'] = function (block: any, generator: any) {
    const varName = generator.getVariableName(block.getFieldValue('VAR'))
    const list = generator.valueToCode(block, 'LIST', Order.ASSIGNMENT) || '[]'
    const body = generator.statementToCode(block, 'DO')
    return `for (${varName} in ${list}) {\n${body}}\n`
  }

  // 覆盖 for 计数循环: 用 Groovy 范围语法
  gen.forBlock['controls_for'] = function (block: any, generator: any) {
    const varName = generator.getVariableName(block.getFieldValue('VAR'))
    const from = generator.valueToCode(block, 'FROM', Order.ASSIGNMENT) || '0'
    const to = generator.valueToCode(block, 'TO', Order.ASSIGNMENT) || '10'
    const by = generator.valueToCode(block, 'BY', Order.ASSIGNMENT) || '1'
    const body = generator.statementToCode(block, 'DO')
    if (by === '1') {
      return `for (${varName} in ${from}..${to}) {\n${body}}\n`
    }
    return `for (${varName} in (${from}..${to}).step(${by})) {\n${body}}\n`
  }

  // 覆盖 text_join: 用 Groovy 字符串拼接
  gen.forBlock['text_join'] = function (block: any, generator: any) {
    const count = block.inputList.filter((input: any) => input.name.startsWith('ADD')).length
    if (count === 0) return ["''", Order.ATOMIC]
    const parts: string[] = []
    for (let i = 0; i < count; i++) {
      const part = generator.valueToCode(block, `ADD${i}`, Order.NONE) || "''"
      parts.push(part)
    }
    return [parts.join(' + '), Order.ADDITION]
  }

  return gen
}

// ==================== Blockly 工具箱定义 ====================

function getToolboxDefinition(triggerMode: 'PERIODIC' | 'REALTIME') {
  const realtimeBlocks = triggerMode === 'REALTIME' ? [
    { kind: 'block', type: 'trigger_field' }
  ] : []

  return {
    kind: 'categoryToolbox',
    contents: [
      {
        kind: 'category',
        name: '控制流',
        colour: CONTROL_COLOUR,
        contents: [
          { kind: 'block', type: 'controls_if' },
          { kind: 'block', type: 'controls_ifelse' },
          { kind: 'block', type: 'controls_forEach' },
          { kind: 'block', type: 'def_variable' },
          { kind: 'block', type: 'threshold_check' },
          { kind: 'block', type: 'score_add' },
          ...realtimeBlocks
        ]
      },
      {
        kind: 'category',
        name: '告警查询',
        colour: ALARM_COLOUR,
        contents: [
          { kind: 'block', type: 'alarm_query' }
        ]
      },
      {
        kind: 'category',
        name: '设备与数据',
        colour: DEVICE_COLOUR,
        contents: [
          { kind: 'block', type: 'query_devices' },
          { kind: 'block', type: 'query_history' },
          { kind: 'block', type: 'query_latest' },
          { kind: 'block', type: 'query_weather' },
          { kind: 'block', type: 'sensor_code' },
          { kind: 'block', type: 'data_value' }
        ]
      },
      {
        kind: 'category',
        name: '工具方法',
        colour: TOOL_COLOUR,
        contents: [
          { kind: 'block', type: 'invoke_algorithm' },
          { kind: 'block', type: 'store_data' },
          { kind: 'block', type: 'get_data' },
          { kind: 'block', type: 'get_scopes' }
        ]
      },
      {
        kind: 'category',
        name: '日志输出',
        colour: LOG_COLOUR,
        contents: [
          { kind: 'block', type: 'log_info' },
          { kind: 'block', type: 'log_warn' },
          { kind: 'block', type: 'log_error' }
        ]
      },
      {
        kind: 'category',
        name: '告警返回',
        colour: RETURN_COLOUR,
        contents: [
          { kind: 'block', type: 'alarm_return' },
          { kind: 'block', type: 'alarm_noop' }
        ]
      },
      {
        kind: 'category',
        name: '逻辑',
        colour: '210',
        contents: [
          { kind: 'block', type: 'logic_compare' },
          { kind: 'block', type: 'logic_operation' },
          { kind: 'block', type: 'logic_negate' },
          { kind: 'block', type: 'logic_boolean' },
          { kind: 'block', type: 'logic_null' }
        ]
      },
      {
        kind: 'category',
        name: '数学',
        colour: '230',
        contents: [
          { kind: 'block', type: 'math_number' },
          { kind: 'block', type: 'math_arithmetic' },
          { kind: 'block', type: 'math_modulo' }
        ]
      },
      {
        kind: 'category',
        name: '文本',
        colour: '160',
        contents: [
          { kind: 'block', type: 'text' },
          { kind: 'block', type: 'text_join' },
          { kind: 'block', type: 'string_concat' }
        ]
      },
      {
        kind: 'category',
        name: '变量',
        colour: '330',
        custom: 'VARIABLE'
      }
    ]
  }
}

// ==================== 组件逻辑 ====================

defineCustomBlocks()

const props = defineProps<{
  visible: boolean
  alarmId: number
  triggerMode: 'PERIODIC' | 'REALTIME'
}>()

const emit = defineEmits<{
  'update:visible': [val: boolean]
  saved: []
}>()

const editMode = ref<'blockly' | 'code'>('code')
const codeContent = ref('')
const blocklyCode = ref('')
const saving = ref(false)
const testing = ref(false)
const testResultVisible = ref(false)
const testResult = ref<CompositeAlarmLog | null>(null)
const expandedTools = ref(['告警查询', '设备与数据', '工具方法'])
const blocklyContainer = ref<HTMLDivElement>()
const codeTextarea = ref<HTMLTextAreaElement>()

let workspace: Blockly.WorkspaceSvg | null = null
let groovyGenerator: JavascriptGenerator | null = null
let blocklyStateJson: string | null = null

const toolGroups = [
  {
    name: '告警查询',
    tools: [
      { name: '查询告警记录', sign: 'queryAlarms(level, pointId, since)', desc: '查询阈值告警或综合告警记录', snippet: 'def alarms = queryAlarms(2, pointId, since)' }
    ]
  },
  {
    name: '设备与数据',
    tools: [
      { name: '查询设备列表', sign: 'queryDevices(pointId)', desc: '查询隐患点下所有设备', snippet: 'def devices = queryDevices(pointId)' },
      { name: '查询历史数据', sign: 'queryHistory(deviceId, sensorCode, start, end)', desc: '从 IoTDB 查询历史时序数据', snippet: 'def history = queryHistory(deviceId, \'displacement\', start, end)' },
      { name: '查询最新数据', sign: 'queryLatest(deviceId, sensorCode)', desc: '查询设备最新一条数据', snippet: 'def latest = queryLatest(deviceId, \'displacement\')' },
      { name: '查询天气数据', sign: 'queryWeather(pointId)', desc: '查询隐患点位置天气信息', snippet: 'def weather = queryWeather(pointId)' }
    ]
  },
  {
    name: '工具方法',
    tools: [
      { name: '调用算法', sign: 'invokeAlgorithm(name, params)', desc: '调用预置 Python 算法', snippet: 'def result = invokeAlgorithm(\'slope_stability\', [angle: 45, cohesion: 20])' },
      { name: '存储数据', sign: 'storeData(key, value, ttl)', desc: '策略级暂存数据，跨次执行持久化', snippet: 'storeData(\'last_check\', now(), 3600)' },
      { name: '读取数据', sign: 'getData(key)', desc: '读取暂存数据', snippet: 'def lastCheck = getData(\'last_check\')' },
      { name: '获取应用范围', sign: 'getScopes()', desc: '获取策略绑定的所有隐患点', snippet: 'def points = getScopes()' }
    ]
  },
  {
    name: '日志输出',
    tools: [
      { name: 'INFO 日志', sign: 'logInfo(msg)', desc: '记录信息级别日志', snippet: 'logInfo(\'检查完成\')' },
      { name: 'WARN 日志', sign: 'logWarn(msg)', desc: '记录警告级别日志', snippet: 'logWarn(\'数据异常\')' },
      { name: 'ERROR 日志', sign: 'logError(msg)', desc: '记录错误级别日志', snippet: 'logError(\'处理失败\')' }
    ]
  }
]

// ==================== Blockly 生命周期 ====================

function initBlockly() {
  if (!blocklyContainer.value) return
  // Ensure container has dimensions before injection
  const rect = blocklyContainer.value.getBoundingClientRect()
  if (rect.width === 0 || rect.height === 0) return

  disposeBlockly()

  groovyGenerator = createGroovyGenerator()

  workspace = Blockly.inject(blocklyContainer.value, {
    toolbox: getToolboxDefinition(props.triggerMode),
    grid: { spacing: 25, length: 3, colour: '#e5e6eb', snap: true },
    trashcan: true,
    scrollbars: true,
    sounds: false,
    renderer: 'zelos',
    move: { scrollbars: true, drag: true, wheel: true }
  })

  // 恢复之前保存的状态
  if (blocklyStateJson) {
    try {
      const state = JSON.parse(blocklyStateJson)
      Blockly.serialization.workspaces.load(state, workspace)
    } catch { /* ignore */ }
  }

  // 监听变化，实时生成代码
  workspace.addChangeListener(() => {
    generateGroovyCode()
  })

  Blockly.svgResize(workspace)
}

function disposeBlockly() {
  if (workspace) {
    workspace.dispose()
    workspace = null
  }
  groovyGenerator = null
}

function generateGroovyCode() {
  if (!workspace || !groovyGenerator) return
  try {
    const code = groovyGenerator.workspaceToCode(workspace)
    blocklyCode.value = code
  } catch {
    blocklyCode.value = '// 代码生成出错'
  }
}

function syncCodeFromBlockly() {
  if (blocklyCode.value) {
    codeContent.value = blocklyCode.value
    editMode.value = 'code'
    ElMessage.success('代码已同步到编辑器')
  }
}

function saveBlocklyState() {
  if (workspace) {
    try {
      const state = Blockly.serialization.workspaces.save(workspace)
      blocklyStateJson = JSON.stringify(state)
    } catch { /* ignore */ }
  }
}

function handleModeChange(mode: 'blockly' | 'code') {
  if (mode === 'blockly') {
    // Double nextTick to ensure v-show toggles the container to visible first
    nextTick(() => {
      nextTick(() => {
        initBlockly()
      })
    })
  } else {
    // 切换到代码模式时同步 Blockly 生成的代码
    if (blocklyCode.value && !codeContent.value) {
      codeContent.value = blocklyCode.value
    }
    saveBlocklyState()
    disposeBlockly()
  }
}

// ==================== 数据加载 ====================

watch(() => props.visible, async (val) => {
  if (val) {
    editMode.value = 'code'
    blocklyStateJson = null
    try {
      const detail = await getCompositeAlarmDetail(props.alarmId)
      codeContent.value = detail.scriptCode || ''
      if (detail.scriptXml) {
        blocklyStateJson = detail.scriptXml
      }
    } catch {
      codeContent.value = ''
    }
  } else {
    saveBlocklyState()
    disposeBlockly()
  }
}, { immediate: true })

function handleClose() {
  saveBlocklyState()
  disposeBlockly()
  emit('update:visible', false)
}

onBeforeUnmount(() => {
  disposeBlockly()
})

// ==================== 工具面板操作 ====================

function applyTemplate() {
  if (props.triggerMode === 'REALTIME') {
    codeContent.value = `// 实时触发模式 — 订阅数据到达时执行
// msg: TriggerMessage { sourceType, sourceId, payload }
def run(TriggerMessage msg) {
    def deviceId = msg.payload.deviceId
    def value = msg.payload.value

    logInfo("收到触发: sourceType=" + msg.sourceType + " deviceId=" + deviceId)

    // 在此编写判断逻辑
    def data = queryLatest(deviceId, 'displacement')
    if (data.value > 10) {
        return [level: 2, message: '监测值超限', detail: "设备\${deviceId} 当前值: \${data.value}"]
    }
    return null
}`
  } else {
    codeContent.value = `// 周期触发模式 — 按 Cron 表达式定时执行
def run() {
    def points = getScopes()

    for (point in points) {
        def devices = queryDevices(point.id)
        for (device in devices) {
            def data = queryLatest(device.id, 'displacement')
            logInfo("点位: \${point.name} 设备: \${device.name} 值: \${data.value}")

            if (data.value > 10) {
                return [level: 2, message: '位移超限', detail: "点位:\${point.name} 值:\${data.value}"]
            }
        }
    }
    return null
}`
  }
}

function insertSnippet(snippet: string) {
  if (editMode.value === 'code') {
    codeContent.value = codeContent.value ? codeContent.value + '\n' + snippet : snippet
  }
}

// ==================== 保存与测试 ====================

async function handleSave() {
  const code = editMode.value === 'blockly' ? blocklyCode.value : codeContent.value
  if (!code?.trim()) {
    ElMessage.warning('脚本内容不能为空')
    return
  }
  saving.value = true
  try {
    saveBlocklyState()
    await updateScriptCode(props.alarmId, code, blocklyStateJson || undefined)
    ElMessage.success('脚本已保存')
    emit('saved')
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleTest() {
  const code = editMode.value === 'blockly' ? blocklyCode.value : codeContent.value
  if (!code?.trim()) {
    ElMessage.warning('请先编写脚本')
    return
  }
  testing.value = true
  try {
    testResult.value = await testCompositeAlarm(props.alarmId)
    testResultVisible.value = true
  } catch (e: any) {
    ElMessage.error(e.message || '测试失败')
  } finally {
    testing.value = false
  }
}
</script>

<style scoped>
.script-editor-layout {
  display: flex;
  gap: 0;
  height: calc(100vh - 80px);
  background: #fff;
}

.tool-panel {
  width: 280px;
  border-right: 1px solid #e5e6eb;
  overflow-y: auto;
  flex-shrink: 0;
  background: #fafbfc;
}

.panel-title {
  margin: 0;
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  border-bottom: 1px solid #e5e6eb;
}

.tool-item {
  padding: 8px 16px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid #f2f3f5;
}

.tool-item:hover {
  background: #e8f3ff;
}

.tool-name {
  font-size: 13px;
  font-weight: 500;
  color: #1d2129;
  margin-bottom: 2px;
}

.tool-sign {
  display: block;
  font-size: 11px;
  color: #5b8def;
  font-family: 'Courier New', monospace;
  margin-bottom: 4px;
}

.tool-desc {
  margin: 0;
  font-size: 12px;
  color: #86909c;
}

.blockly-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.area-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  border-bottom: 1px solid #e5e6eb;
  background: #fafbfc;
}

.area-label {
  font-size: 13px;
  font-weight: 600;
  color: #4e5969;
}

.blockly-workspace {
  flex: 1;
  position: relative;
  min-height: 400px;
}

.code-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
}

.code-template {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(247, 248, 250, 0.9);
  z-index: 1;
}

.code-editor {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  padding: 16px;
  font-family: 'JetBrains Mono', 'Fira Code', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.7;
  color: #1d2129;
  background: #fff;
  tab-size: 4;
}

.code-editor:focus {
  background: #fefefe;
}

.preview-panel {
  width: 380px;
  border-left: 1px solid #e5e6eb;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.code-preview {
  flex: 1;
  margin: 0;
  padding: 16px;
  font-family: 'JetBrains Mono', 'Fira Code', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.7;
  color: #4e5969;
  background: #fafbfc;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

:deep(.el-collapse-item__header) {
  font-size: 13px;
  font-weight: 500;
}

:deep(.el-collapse-item__content) {
  padding: 0;
}
</style>
