<template>
  <div class="recipient-picker">
    <el-tabs v-model="activeTab">
      <!-- 按角色 -->
      <el-tab-pane label="按角色" name="role">
        <el-checkbox
          v-model="roleAll"
          @change="onRoleAllChange"
          style="margin-bottom: 8px;"
        >所有角色</el-checkbox>
        <el-checkbox-group v-model="localRoleIds" :disabled="roleAll" class="checkbox-grid">
          <el-checkbox
            v-for="r in options.roles"
            :key="r.id"
            :label="r.id"
          >{{ r.name }}</el-checkbox>
        </el-checkbox-group>
      </el-tab-pane>

      <!-- 按部门 -->
      <el-tab-pane label="按部门" name="dept">
        <el-checkbox
          v-model="deptAll"
          @change="onDeptAllChange"
          style="margin-bottom: 8px;"
        >所有部门</el-checkbox>
        <el-tree
          ref="deptTreeRef"
          :data="deptTreeData"
          show-checkbox
          check-strictly
          node-key="id"
          :props="{ label: 'name', children: 'children' }"
          :disabled="deptAll"
          @check="onDeptTreeCheck"
        />
        <div class="form-hint">提示：勾选父部门仅取父部门本身，不会自动包含子部门</div>
      </el-tab-pane>

      <!-- 指定人员 -->
      <el-tab-pane label="指定人员" name="user">
        <el-select
          v-model="localUserIds"
          multiple
          filterable
          placeholder="搜索用户名"
          style="width: 100%;"
        >
          <el-option label="所有用户" value="*" />
          <el-option
            v-for="u in options.users"
            :key="u.id"
            :label="u.name + (u.deptName ? '(' + u.deptName + ')' : '')"
            :value="u.id"
          />
        </el-select>
      </el-tab-pane>
    </el-tabs>

    <!-- 已选汇总 -->
    <div class="selection-summary" v-if="hasAnySelection">
      <span class="summary-label">已选：</span>
      <el-tag v-if="roleAll" type="warning" closable @close="roleAll = false">全部角色</el-tag>
      <el-tag v-for="rid in localRoleIds" v-else :key="'r'+rid" type="info"
              closable @close="removeId('role', rid)">
        {{ roleName(rid) }}
      </el-tag>
      <el-tag v-if="deptAll" type="warning" closable @close="deptAll = false">全部部门</el-tag>
      <el-tag v-for="did in localDeptIds" v-else :key="'d'+did" type="info"
              closable @close="removeId('dept', did)">
        {{ deptName(did) }}
      </el-tag>
      <el-tag v-if="localUserIds.includes('*')" type="warning" closable
              @close="removeWildcard('user')">全部用户</el-tag>
      <el-tag v-for="uid in localUserIds.filter(i => i !== '*')" v-else :key="'u'+uid"
              type="info" closable @close="removeId('user', uid)">
        {{ userName(uid) }}
      </el-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getRecipientOptions,
  type RecipientOptions,
} from '@/api/alarmDispatch'

interface SelectionModel {
  roleIds?: string[]
  deptIds?: string[]
  userIds?: string[]
}

const props = defineProps<{ modelValue: SelectionModel }>()
const emit = defineEmits<{
  (e: 'update:modelValue', v: SelectionModel): void
}>()

const activeTab = ref<'role' | 'dept' | 'user'>('role')
const options = ref<RecipientOptions>({ roles: [], depts: [], users: [] })

const roleAll = ref(false)
const deptAll = ref(false)
const localRoleIds = ref<string[]>([])
const localDeptIds = ref<string[]>([])
const localUserIds = ref<string[]>([])

const deptTreeRef = ref()

/** 防止 modelValue ↔ local 双向监听死循环 */
let internalUpdate = false

// 加载选项
onMounted(async () => {
  try {
    const res = await getRecipientOptions()
    options.value = (res as any).data || res
  } catch {
    ElMessage.error('接收人选项加载失败')
  }
})

// 初始化 modelValue（编辑回填）
watch(() => props.modelValue, (v) => {
  if (!v || internalUpdate) return
  if (v.roleIds?.includes('*')) { roleAll.value = true; localRoleIds.value = [] }
  else { roleAll.value = false; localRoleIds.value = v.roleIds ? [...v.roleIds] : [] }
  if (v.deptIds?.includes('*')) { deptAll.value = true; localDeptIds.value = [] }
  else { deptAll.value = false; localDeptIds.value = v.deptIds ? [...v.deptIds] : [] }
  localUserIds.value = v.userIds ? [...v.userIds] : []
  // 同步部门树勾选状态（el-tree 内部状态不随 localDeptIds 自动更新）
  nextTick(() => {
    const keys = v.deptIds?.includes('*') ? [] : (v.deptIds || [])
    deptTreeRef.value?.setCheckedKeys(keys)
  })
}, { immediate: true })

// 部门树构造：根据 parentId 拼装层级树；无 parentId 视为根节点
const deptTreeData = computed(() => {
  const list = options.value.depts
  const map = new Map<string, any>()
  const roots: any[] = []
  // 第一遍：所有节点入 map，预留 children 数组
  list.forEach(d => {
    map.set(String(d.id), { id: String(d.id), name: d.name, parentId: d.parentId, children: [] })
  })
  // 第二遍：按 parentId 串联；parentId 为空、"0" 或不在 map 中视为根
  list.forEach(d => {
    const node = map.get(String(d.id))!
    const pid = d.parentId != null ? String(d.parentId) : null
    const parent = pid != null && pid !== '0' ? map.get(pid) : null
    if (parent) {
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
})

// 通配符互斥
function onRoleAllChange(v: any) {
  if (v) localRoleIds.value = []
  syncEmit()
}
function onDeptAllChange(v: any) {
  if (v) {
    localDeptIds.value = []
    deptTreeRef.value?.setCheckedKeys([])
  }
  syncEmit()
}
function onDeptTreeCheck() {
  const checked = deptTreeRef.value?.getCheckedKeys() || []
  localDeptIds.value = checked.filter((k: any) => k !== '*')
  syncEmit()
}

function removeId(type: 'role' | 'dept' | 'user', id: string) {
  if (type === 'role') localRoleIds.value = localRoleIds.value.filter(i => i !== id)
  if (type === 'dept') {
    localDeptIds.value = localDeptIds.value.filter(i => i !== id)
    deptTreeRef.value?.setChecked(id, false, false)
  }
  if (type === 'user') localUserIds.value = localUserIds.value.filter(i => i !== id)
  syncEmit()
}

function removeWildcard(type: 'user') {
  if (type === 'user') localUserIds.value = localUserIds.value.filter(i => i !== '*')
  syncEmit()
}

const hasAnySelection = computed(() => {
  return roleAll.value || deptAll.value
      || localUserIds.value.includes('*')
      || localRoleIds.value.length > 0
      || localDeptIds.value.length > 0
      || localUserIds.value.filter(i => i !== '*').length > 0
})

function syncEmit() {
  const result: SelectionModel = {}
  if (roleAll.value) result.roleIds = ['*']
  else if (localRoleIds.value.length) result.roleIds = [...localRoleIds.value]

  if (deptAll.value) result.deptIds = ['*']
  else if (localDeptIds.value.length) result.deptIds = [...localDeptIds.value]

  if (localUserIds.value.length) result.userIds = [...localUserIds.value]

  internalUpdate = true
  emit('update:modelValue', result)
  nextTick(() => { internalUpdate = false })
}

// 监听本地变更同步出去
watch([roleAll, localRoleIds, deptAll, localDeptIds, localUserIds], syncEmit, { deep: true })

function roleName(id: string) {
  return options.value.roles.find(r => r.id === id)?.name || id
}
function deptName(id: string) {
  return options.value.depts.find(d => d.id === id)?.name || id
}
function userName(id: string) {
  return options.value.users.find(u => u.id === id)?.name || id
}
</script>

<style scoped>
.recipient-picker {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
}
.checkbox-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 4px 12px;
}
.selection-summary {
  margin-top: 12px;
  padding-top: 8px;
  border-top: 1px dashed #e4e7ed;
}
.summary-label {
  font-size: 13px;
  color: #606266;
  margin-right: 8px;
}
.form-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}
.el-tag {
  margin: 2px 4px 2px 0;
}
</style>
