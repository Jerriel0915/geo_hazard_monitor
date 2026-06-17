<template>
  <div class="page">
    <!-- 页头 -->
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">算法库</h2>
        <span class="header__subtitle">Python 算法包管理与版本化</span>
      </div>
      <div class="header__right">
        <el-button v-if="hasPermission('iot:algo-library:add')" type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增算法
        </el-button>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search">
      <el-input
        v-model="searchName"
        placeholder="搜索算法名称"
        class="search__input"
        clearable
        @clear="loadData"
        @keyup.enter="loadData"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="searchStatus" placeholder="状态" clearable class="search__select" @change="loadData">
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" @click="loadData">搜索</el-button>
      <el-button @click="handleResetSearch">重置</el-button>
    </div>

    <!-- 卡片列表（复用全局 .grid/.card 样式） -->
    <div v-loading="loading" class="grid">
      <el-empty v-if="!loading && algoList.length === 0" description="暂无算法" />

      <div
        v-for="item in algoList"
        :key="item.id"
        class="card"
        :class="{ 'card--disabled': item.status !== 1 }"
      >
        <div class="card__header">
          <div class="card__title-row">
            <h3 class="card__title">{{ item.name }}</h3>
            <el-switch
              v-if="hasPermission('iot:algo-library:edit')"
              :model-value="item.status === 1"
              size="small"
              active-text="启用"
              inactive-text="停用"
              @change="(val: boolean) => handleToggleStatus(item, val)"
            />
            <el-tag v-else :type="item.status === 1 ? 'success' : 'info'" size="small">
              {{ item.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </div>
          <p class="card__desc">{{ item.description || '—' }}</p>
        </div>

        <div class="card__meta">
          <div class="card__meta-row">
            <span class="card__meta-label">编码:</span>
            <code class="card__meta-value">{{ item.code }}</code>
          </div>
          <div class="card__meta-row">
            <span class="card__meta-label">版本数:</span>
            <span class="card__meta-value">{{ item.versionCount || 0 }}</span>
          </div>
          <div v-if="item.latestVersionNo" class="card__meta-row">
            <span class="card__meta-label">最近上传:</span>
            <span class="card__meta-value">
              {{ item.latestVersionNo }} · {{ item.latestUploadTime }}
            </span>
          </div>
        </div>

        <div class="card__footer">
          <el-button type="primary" text size="small" @click="handleDetail(item)">
            <el-icon><View /></el-icon> 详情
          </el-button>
          <el-button v-if="hasPermission('iot:algo-library:edit')" type="primary" text size="small" @click="handleEdit(item)">
            <el-icon><Setting /></el-icon> 编辑
          </el-button>
          <el-button v-if="hasPermission('iot:algo-library:remove')" type="danger" text size="small" @click="handleDelete(item)">
            <el-icon><Delete /></el-icon> 删除
          </el-button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > 0" class="pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <AlgoFormDialog v-model="formVisible" :algo="editingItem" @saved="loadData" />

    <!-- 详情抽屉 -->
    <AlgoDetailDrawer v-model="detailVisible" :algo-id="currentId" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, View, Setting, Delete } from '@element-plus/icons-vue'
import {
  getAlgoLibraryPage,
  updateAlgoLibraryStatus,
  deleteAlgoLibrary,
  type AlgoInfo,
  type AlgoInfoPageParams
} from '@/api/algoLibrary'
import { hasPermission } from '@/utils/permission'
import AlgoFormDialog from './components/AlgoFormDialog.vue'
import AlgoDetailDrawer from './components/AlgoDetailDrawer.vue'

// ==================== 列表状态 ====================
const loading = ref(false)
const algoList = ref<AlgoInfo[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)
const searchName = ref('')
const searchStatus = ref<number | ''>('')

// ==================== 弹窗状态 ====================
const formVisible = ref(false)
const editingItem = ref<AlgoInfo | null>(null)
const detailVisible = ref(false)
const currentId = ref<number | null>(null)

// ==================== 数据加载 ====================
async function loadData() {
  loading.value = true
  try {
    const params: AlgoInfoPageParams = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      name: searchName.value || undefined,
      status: searchStatus.value === '' ? undefined : (searchStatus.value as 0 | 1)
    }
    const res: any = await getAlgoLibraryPage(params)
    algoList.value = res.rows || []
    total.value = res.total || 0
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function handleResetSearch() {
  searchName.value = ''
  searchStatus.value = ''
  pageNum.value = 1
  loadData()
}

// ==================== 操作 ====================
function handleAdd() {
  editingItem.value = null
  formVisible.value = true
}

function handleEdit(item: AlgoInfo) {
  editingItem.value = item
  formVisible.value = true
}

function handleDetail(item: AlgoInfo) {
  currentId.value = item.id
  detailVisible.value = true
}

async function handleToggleStatus(item: AlgoInfo, enabled: boolean) {
  const newStatus = enabled ? 1 : 0
  const action = enabled ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定${action}算法「${item.name}」？`, `${action}确认`, { type: 'warning' })
    await updateAlgoLibraryStatus(item.id, newStatus as 0 | 1)
    ElMessage.success(`${action}成功`)
    loadData()
  } catch { /* cancelled */ }
}

async function handleDelete(item: AlgoInfo) {
  try {
    await ElMessageBox.confirm(
      `确定删除算法「${item.name}」？将同时删除该算法下所有版本记录（物理文件保留）。`,
      '删除确认',
      { type: 'warning' }
    )
    await deleteAlgoLibrary(item.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancelled */ }
}

onMounted(() => loadData())
</script>

<style scoped>
.page {
  background: #f0f2f5;
}
</style>
