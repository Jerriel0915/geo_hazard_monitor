<template>
  <div class="page-content">
    <div class="page-title">日志管理</div>
    <div class="page-body">
      <el-tabs v-model="activeTab" type="border-card">
        <!-- 操作日志 -->
        <el-tab-pane label="操作日志" name="operation">
          <div class="tab-content">
            <div class="search-bar">
              <el-form :model="opSearchForm" inline>
                <el-form-item label="操作类型">
                  <el-select v-model="opSearchForm.type" placeholder="全部类型" clearable style="width: 140px">
                    <el-option label="登录" value="login" />
                    <el-option label="新增" value="add" />
                    <el-option label="修改" value="update" />
                    <el-option label="删除" value="delete" />
                    <el-option label="导出" value="export" />
                    <el-option label="告警处置" value="alarm_dispose" />
                    <el-option label="参数配置" value="config" />
                  </el-select>
                </el-form-item>
                <el-form-item label="操作用户">
                  <el-input v-model="opSearchForm.username" placeholder="请输入用户名" clearable />
                </el-form-item>
                <el-form-item label="操作时间">
                  <el-date-picker
                    v-model="opSearchForm.timeRange"
                    type="datetimerange"
                    range-separator="至"
                    start-placeholder="开始时间"
                    end-placeholder="结束时间"
                    value-format="YYYY-MM-DD HH:mm:ss"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="handleOpSearch">查询</el-button>
                  <el-button @click="handleOpReset">重置</el-button>
                </el-form-item>
              </el-form>
              <el-button type="success" @click="handleExport">导出Excel</el-button>
            </div>

            <el-table :data="operationLogList" border stripe v-loading="loading">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="type" label="操作类型" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="getLogTypeType(row.type)" size="small">{{ getLogTypeLabel(row.type) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="username" label="操作用户" width="120" />
              <el-table-column prop="realName" label="真实姓名" width="120" />
              <el-table-column prop="orgName" label="所属组织" width="150" />
              <el-table-column prop="ip" label="IP地址" width="140" />
              <el-table-column prop="location" label="操作地点" width="120" />
              <el-table-column prop="content" label="操作内容" min-width="200" show-overflow-tooltip />
              <el-table-column prop="status" label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                    {{ row.status === 1 ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="duration" label="耗时(ms)" width="90" align="center" />
              <el-table-column prop="createTime" label="操作时间" width="160" />
            </el-table>

            <div class="pagination">
              <el-pagination
                v-model:current-page="opPagination.page"
                v-model:page-size="opPagination.size"
                :page-sizes="[10, 20, 50, 100]"
                :total="opPagination.total"
                layout="total, sizes, prev, pager, next, jumper"
                prev-text="上一页"
                next-text="下一页"
                :disabled="opPagination.total === 0"
                @size-change="handleOpSizeChange"
                @current-change="handleOpPageChange"
              />
            </div>
          </div>
        </el-tab-pane>

        <!-- 登录日志 -->
        <el-tab-pane label="登录日志" name="login">
          <div class="tab-content">
            <div class="search-bar">
              <el-form :model="loginSearchForm" inline>
                <el-form-item label="用户名">
                  <el-input v-model="loginSearchForm.username" placeholder="请输入用户名" clearable />
                </el-form-item>
                <el-form-item label="登录状态">
                  <el-select v-model="loginSearchForm.status" placeholder="全部状态" clearable style="width: 120px">
                    <el-option label="成功" :value="1" />
                    <el-option label="失败" :value="0" />
                  </el-select>
                </el-form-item>
                <el-form-item label="登录时间">
                  <el-date-picker
                    v-model="loginSearchForm.timeRange"
                    type="datetimerange"
                    range-separator="至"
                    start-placeholder="开始时间"
                    end-placeholder="结束时间"
                    value-format="YYYY-MM-DD HH:mm:ss"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="handleLoginSearch">查询</el-button>
                  <el-button @click="handleLoginReset">重置</el-button>
                </el-form-item>
              </el-form>
              <el-button type="success" @click="handleExportLogin">导出Excel</el-button>
            </div>

            <el-table :data="loginLogList" border stripe v-loading="loading">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="username" label="用户名" width="120" />
              <el-table-column prop="realName" label="真实姓名" width="120" />
              <el-table-column prop="ip" label="IP地址" width="140" />
              <el-table-column prop="location" label="登录地点" width="120" />
              <el-table-column prop="browser" label="浏览器" width="140" />
              <el-table-column prop="os" label="操作系统" width="140" />
              <el-table-column prop="status" label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                    {{ row.status === 1 ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="msg" label="消息" min-width="150" show-overflow-tooltip />
              <el-table-column prop="createTime" label="登录时间" width="160" />
            </el-table>

            <div class="pagination">
              <el-pagination
                v-model:current-page="loginPagination.page"
                v-model:page-size="loginPagination.size"
                :page-sizes="[10, 20, 50, 100]"
                :total="loginPagination.total"
                layout="total, sizes, prev, pager, next, jumper"
                prev-text="上一页"
                next-text="下一页"
                :disabled="loginPagination.total === 0"
                @size-change="handleLoginSizeChange"
                @current-change="handleLoginPageChange"
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'

interface OperationLog {
  id: number
  type: string
  username: string
  realName: string
  orgName: string
  ip: string
  location: string
  content: string
  status: number
  duration: number
  createTime: string
}

interface LoginLog {
  id: number
  username: string
  realName: string
  ip: string
  location: string
  browser: string
  os: string
  status: number
  msg: string
  createTime: string
}

const activeTab = ref('operation')
const loading = ref(false)

// 操作日志
const opSearchForm = reactive({
  type: '',
  username: '',
  timeRange: [] as string[]
})

const opPagination = reactive({ page: 1, size: 10, total: 0 })

const allOperationLogs = ref<OperationLog[]>([
  { id: 1, type: 'login', username: 'admin', realName: '系统管理员', orgName: '地质灾害监测中心', ip: '192.168.1.100', location: '北京', content: '用户登录系统', status: 1, duration: 120, createTime: '2024-03-20 09:30:15' },
  { id: 2, type: 'add', username: 'admin', realName: '系统管理员', orgName: '地质灾害监测中心', ip: '192.168.1.100', location: '北京', content: '新增用户：zhangsan', status: 1, duration: 230, createTime: '2024-03-20 09:35:22' },
  { id: 3, type: 'update', username: 'zhangsan', realName: '张三', orgName: '监测一部', ip: '192.168.1.101', location: '北京', content: '修改隐患点：XX山区滑坡监测点', status: 1, duration: 180, createTime: '2024-03-20 10:15:08' },
  { id: 4, type: 'delete', username: 'admin', realName: '系统管理员', orgName: '地质灾害监测中心', ip: '192.168.1.100', location: '北京', content: '删除设备：DEV-2024-001', status: 1, duration: 150, createTime: '2024-03-20 11:00:45' },
  { id: 5, type: 'export', username: 'lisi', realName: '李四', orgName: '北京监测组', ip: '192.168.1.102', location: '北京', content: '导出监测数据报表', status: 1, duration: 3500, createTime: '2024-03-20 14:20:33' },
  { id: 6, type: 'alarm_dispose', username: 'wangwu', realName: '王五', orgName: '天津监测组', ip: '192.168.1.103', location: '天津', content: '处置告警：位移超限告警-ALM-2024-003', status: 1, duration: 420, createTime: '2024-03-20 15:45:18' },
  { id: 7, type: 'config', username: 'admin', realName: '系统管理员', orgName: '地质灾害监测中心', ip: '192.168.1.100', location: '北京', content: '修改系统参数：数据保留时长设置为365天', status: 1, duration: 200, createTime: '2024-03-20 16:10:05' },
  { id: 8, type: 'login', username: 'zhaoliu', realName: '赵六', orgName: '河北监测组', ip: '192.168.1.104', location: '石家庄', content: '用户登录系统', status: 0, duration: 80, createTime: '2024-03-20 17:30:00' },
  { id: 9, type: 'update', username: 'zhangsan', realName: '张三', orgName: '监测一部', ip: '192.168.1.101', location: '北京', content: '修改告警阈值配置', status: 1, duration: 260, createTime: '2024-03-21 08:45:12' },
  { id: 10, type: 'add', username: 'admin', realName: '系统管理员', orgName: '地质灾害监测中心', ip: '192.168.1.100', location: '北京', content: '新增组织：监测三部', status: 1, duration: 190, createTime: '2024-03-21 09:20:30' },
  { id: 11, type: 'export', username: 'lisi', realName: '李四', orgName: '北京监测组', ip: '192.168.1.102', location: '北京', content: '导出设备清单', status: 1, duration: 1200, createTime: '2024-03-21 10:00:15' },
  { id: 12, type: 'alarm_dispose', username: 'wangwu', realName: '王五', orgName: '天津监测组', ip: '192.168.1.103', location: '天津', content: '处置告警：设备离线告警-DEV-2024-005', status: 1, duration: 310, createTime: '2024-03-21 11:30:45' }
])

const operationLogList = computed(() => {
  let result = allOperationLogs.value

  if (opSearchForm.type) {
    result = result.filter(log => log.type === opSearchForm.type)
  }
  if (opSearchForm.username) {
    result = result.filter(log => log.username.includes(opSearchForm.username))
  }
  if (opSearchForm.timeRange && opSearchForm.timeRange.length === 2) {
    const start = new Date(opSearchForm.timeRange[0]).getTime()
    const end = new Date(opSearchForm.timeRange[1]).getTime()
    result = result.filter(log => {
      const t = new Date(log.createTime).getTime()
      return t >= start && t <= end
    })
  }

  opPagination.total = result.length
  const start = (opPagination.page - 1) * opPagination.size
  return result.slice(start, start + opPagination.size)
})

const getLogTypeType = (type: string) => {
  const map: Record<string, string> = {
    login: 'primary',
    add: 'success',
    update: 'warning',
    delete: 'danger',
    export: 'info',
    alarm_dispose: 'danger',
    config: 'warning'
  }
  return map[type] || 'info'
}

const getLogTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    login: '登录',
    add: '新增',
    update: '修改',
    delete: '删除',
    export: '导出',
    alarm_dispose: '告警处置',
    config: '参数配置'
  }
  return map[type] || type
}

const handleOpSearch = () => { opPagination.page = 1 }
const handleOpReset = () => {
  opSearchForm.type = ''
  opSearchForm.username = ''
  opSearchForm.timeRange = []
  opPagination.page = 1
}
const handleOpSizeChange = (val: number) => { opPagination.size = val; opPagination.page = 1 }
const handleOpPageChange = (val: number) => { opPagination.page = val }

// 登录日志
const loginSearchForm = reactive({
  username: '',
  status: undefined as number | undefined,
  timeRange: [] as string[]
})

const loginPagination = reactive({ page: 1, size: 10, total: 0 })

const allLoginLogs = ref<LoginLog[]>([
  { id: 1, username: 'admin', realName: '系统管理员', ip: '192.168.1.100', location: '北京', browser: 'Chrome 122.0', os: 'Windows 11', status: 1, msg: '登录成功', createTime: '2024-03-20 09:30:15' },
  { id: 2, username: 'zhangsan', realName: '张三', ip: '192.168.1.101', location: '北京', browser: 'Chrome 121.0', os: 'Windows 10', status: 1, msg: '登录成功', createTime: '2024-03-20 08:15:30' },
  { id: 3, username: 'lisi', realName: '李四', ip: '192.168.1.102', location: '北京', browser: 'Firefox 123.0', os: 'macOS 14.0', status: 1, msg: '登录成功', createTime: '2024-03-20 10:00:00' },
  { id: 4, username: 'wangwu', realName: '王五', ip: '192.168.1.103', location: '天津', browser: 'Edge 122.0', os: 'Windows 11', status: 0, msg: '密码错误', createTime: '2024-03-20 14:20:10' },
  { id: 5, username: 'wangwu', realName: '王五', ip: '192.168.1.103', location: '天津', browser: 'Edge 122.0', os: 'Windows 11', status: 1, msg: '登录成功', createTime: '2024-03-20 14:25:35' },
  { id: 6, username: 'zhaoliu', realName: '赵六', ip: '192.168.1.104', location: '石家庄', browser: 'Chrome 122.0', os: 'Windows 10', status: 0, msg: '账号已锁定', createTime: '2024-03-20 17:30:00' },
  { id: 7, username: 'admin', realName: '系统管理员', ip: '192.168.1.100', location: '北京', browser: 'Chrome 122.0', os: 'Windows 11', status: 1, msg: '登录成功', createTime: '2024-03-21 08:00:00' },
  { id: 8, username: 'zhangsan', realName: '张三', ip: '192.168.1.101', location: '北京', browser: 'Chrome 121.0', os: 'Windows 10', status: 1, msg: '登录成功', createTime: '2024-03-21 08:45:00' }
])

const loginLogList = computed(() => {
  let result = allLoginLogs.value

  if (loginSearchForm.username) {
    result = result.filter(log => log.username.includes(loginSearchForm.username))
  }
  if (loginSearchForm.status !== undefined) {
    result = result.filter(log => log.status === loginSearchForm.status)
  }
  if (loginSearchForm.timeRange && loginSearchForm.timeRange.length === 2) {
    const start = new Date(loginSearchForm.timeRange[0]).getTime()
    const end = new Date(loginSearchForm.timeRange[1]).getTime()
    result = result.filter(log => {
      const t = new Date(log.createTime).getTime()
      return t >= start && t <= end
    })
  }

  loginPagination.total = result.length
  const start = (loginPagination.page - 1) * loginPagination.size
  return result.slice(start, start + loginPagination.size)
})

const handleLoginSearch = () => { loginPagination.page = 1 }
const handleLoginReset = () => {
  loginSearchForm.username = ''
  loginSearchForm.status = undefined
  loginSearchForm.timeRange = []
  loginPagination.page = 1
}
const handleLoginSizeChange = (val: number) => { loginPagination.size = val; loginPagination.page = 1 }
const handleLoginPageChange = (val: number) => { loginPagination.page = val }

const handleExport = () => {
  ElMessage.success('操作日志导出成功')
}

const handleExportLogin = () => {
  ElMessage.success('登录日志导出成功')
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
  border-bottom: 1px solid #e4e7ed;
}

.page-body {
  padding: 0;
}

.tab-content {
  padding: 16px 0;
}

.search-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
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
</style>
