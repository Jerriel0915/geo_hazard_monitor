import {computed, reactive, ref, type Ref} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {
    createHazardPointGroup,
    deleteHazardPointGroup,
    getHazardPointGroups,
    updateHazardPointGroup,
} from '@/api/hazardPoint'
import {showRequestErrorMessage} from '@/utils/errorHandler'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export interface GroupItem {
    id: string
    name: string
    code: string
    description: string
    sortOrder: number
    count: number
}

// ---------------------------------------------------------------------------
// Composable
// ---------------------------------------------------------------------------

export interface UseHazardPointGroupsOptions {
    /** 隐患点总数（来自 useHazardPointCrud），用于"全部"分组计数 */
    total: Ref<number>
}

export function useHazardPointGroups(opts: UseHazardPointGroupsOptions) {
    // ── State ──
    const groupList = ref<GroupItem[]>([])
    const displayGroupList = ref<GroupItem[]>([])
    const loadingGroups = ref(false)
    const groupPageSize = ref(10)
    const groupCurrentPage = ref(1)
    const groupPanelWidth = ref(200)
    const groupFilterName = ref('')

    // ── Group form dialog ──
    const groupDialogVisible = ref(false)
    const groupDialogTitle = ref('')
    const isEditGroup = ref(false)
    const groupFormRef = ref()
    const groupFormData = reactive({
        id: '',
        name: '',
        code: '',
        description: '',
        sortOrder: 0,
    })

    const validateGroupName = (_rule: any, value: string, callback: any) => {
        if (!value) {
            callback();
            return
        }
        const exists = groupList.value.some((g) => g.name === value && g.id !== groupFormData.id)
        if (exists) {
            callback(new Error('分组名称已存在'))
        } else {
            callback()
        }
    }

    const groupFormRules = {
        name: [
            {required: true, message: '请输入分组名称', trigger: 'blur'},
            {validator: validateGroupName, trigger: 'blur'},
        ],
    }

    // ── Computed ──
    const groupOptions = computed(() => groupList.value.filter((g) => g.id !== 'all'))

    // ── Data loading ──
    const loadGroupPage = (page: number) => {
        groupCurrentPage.value = page
        const keyword = groupFilterName.value.trim().toLowerCase()
        const filtered = keyword
            ? groupList.value.filter(g => g.name.toLowerCase().includes(keyword))
            : [...groupList.value]
        const start = (page - 1) * groupPageSize.value
        const end = start + groupPageSize.value
        displayGroupList.value = filtered
            .sort((a, b) => a.sortOrder - b.sortOrder)
            .slice(start, end)
    }

    const loadGroupList = async () => {
        loadingGroups.value = true
        try {
            const response: any = await getHazardPointGroups()
            if (response.code === 200) {
                const groups = response.data.map((item: any) => ({
                    id: String(item.id),
                    name: item.name,
                    code: item.code,
                    description: item.description,
                    sortOrder: item.sortOrder,
                    count: item.count,
                }))
                groupList.value = [
                    {
                        id: 'all',
                        name: '全部',
                        code: 'ALL',
                        description: '所有隐患点',
                        sortOrder: -1,
                        count: opts.total.value
                    },
                    ...groups,
                ]
                loadGroupPage(1)
            } else {
                ElMessage.error(response.msg || '获取分组失败')
            }
        } catch (error) {
            console.error('获取分组失败:', error)
            showRequestErrorMessage(error, '获取分组失败')
        } finally {
            loadingGroups.value = false
        }
    }

    // ── Infinite scroll ──
    const handleGroupListScroll = (e: Event) => {
        const target = e.target as HTMLElement
        if (target.scrollTop + target.clientHeight >= target.scrollHeight - 10 && !loadingGroups.value) {
            loadingGroups.value = true
            setTimeout(() => {
                const nextPage = groupCurrentPage.value + 1
                const totalPages = Math.ceil(groupList.value.length / groupPageSize.value)
                if (nextPage <= totalPages) {
                    const start = (nextPage - 1) * groupPageSize.value
                    const end = start + groupPageSize.value
                    const newGroups = [...groupList.value].sort((a, b) => a.sortOrder - b.sortOrder).slice(start, end)
                    displayGroupList.value = [...displayGroupList.value, ...newGroups]
                    groupCurrentPage.value = nextPage
                }
                loadingGroups.value = false
            }, 500)
        }
    }

    // ── Panel resize ──
    const startResize = (e: MouseEvent) => {
        e.preventDefault()
        const startX = e.clientX
        const startWidth = groupPanelWidth.value
        let rafId: number | null = null
        document.body.style.userSelect = 'none'
        document.body.style.cursor = 'col-resize'
        const onMouseMove = (ev: MouseEvent) => {
            if (rafId !== null) return
            rafId = requestAnimationFrame(() => {
                groupPanelWidth.value = Math.max(150, Math.min(400, startWidth + ev.clientX - startX))
                rafId = null
            })
        }
        const onMouseUp = () => {
            document.removeEventListener('mousemove', onMouseMove)
            document.removeEventListener('mouseup', onMouseUp)
            document.body.style.userSelect = ''
            document.body.style.cursor = ''
        }
        document.addEventListener('mousemove', onMouseMove)
        document.addEventListener('mouseup', onMouseUp)
    }

    // ── Add ──
    const handleAddGroup = () => {
        groupDialogTitle.value = '新增分组'
        isEditGroup.value = false
        Object.assign(groupFormData, {
            id: '',
            name: '',
            description: '',
            sortOrder: groupList.value.length,
        })
        groupDialogVisible.value = true
    }

    // ── Select-dropdown wrappers ──
    const handleAddGroupFromSelect = () => handleAddGroup()

    const handleEditGroupFromSelect = (option: any) => {
        const group = groupList.value.find((g) => g.id === option.id)
        if (!group || group.id === 'all' || group.id === '1') {
            ElMessage.warning('该分组不允许修改')
            return
        }
        groupDialogTitle.value = '修改分组'
        isEditGroup.value = true
        Object.assign(groupFormData, {
            id: group.id,
            name: group.name,
            description: group.description,
            sortOrder: group.sortOrder,
        })
        groupDialogVisible.value = true
    }

    const handleDeleteGroupFromSelect = (option: any) => {
        const group = groupList.value.find((g) => g.id === option.id)
        if (group) handleDeleteGroup(group)
    }

    // ── Edit ──
    const handleEditGroup = (group: GroupItem) => {
        if (group.id === 'all') {
            ElMessage.warning('"全部"分组不允许修改')
            return
        }
        groupDialogTitle.value = '编辑分组'
        isEditGroup.value = true
        Object.assign(groupFormData, {
            id: group.id,
            code: group.code,
            name: group.name,
            description: group.description,
            sortOrder: group.sortOrder,
        })
        groupDialogVisible.value = true
    }

    // ── Delete ──
    const handleDeleteGroup = (group: GroupItem) => {
        if (group.id === 'all') {
            ElMessage.warning('"全部"分组不允许删除')
            return
        }
        if (group.count > 0) {
            ElMessage.warning(`分组"${group.name}"下仍绑定 ${group.count} 个隐患点，禁止删除`)
            return
        }
        ElMessageBox.confirm(`确定要删除分组"${group.name}"吗?`, '删除确认', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
        })
            .then(async () => {
                try {
                    const res: any = await deleteHazardPointGroup(group.id)
                    if (res.code === 200) {
                        ElMessage.success('删除成功')
                        loadGroupList()
                    } else {
                        ElMessage.error(res.msg || '删除失败')
                    }
                } catch (error: any) {
                    console.error('删除失败:', error)
                    const status = error?.response?.status
                    showRequestErrorMessage(error, '删除失败')
                    if (status === 404) loadGroupList()
                }
            })
            .catch(() => {
            })
    }

    // ── Submit form ──
    const handleGroupSubmit = async () => {
        groupFormRef.value.validate(async (valid: boolean) => {
            if (!valid) return
            try {
                let res: any
                if (isEditGroup.value) {
                    res = await updateHazardPointGroup(groupFormData.id, {
                        name: groupFormData.name,
                        description: groupFormData.description,
                        sortOrder: groupFormData.sortOrder,
                        status: 1,
                    })
                } else {
                    res = await createHazardPointGroup({
                        code: `G${Date.now()}`,
                        name: groupFormData.name,
                        description: groupFormData.description,
                        sortOrder: groupFormData.sortOrder,
                        status: 1,
                    })
                }
                if (res.code === 200) {
                    ElMessage.success(isEditGroup.value ? '修改成功' : '新增成功')
                    groupDialogVisible.value = false
                    loadGroupList()
                } else {
                    ElMessage.error(res.msg || '操作失败')
                }
            } catch (error) {
                console.error('提交失败:', error)
                showRequestErrorMessage(error, '操作失败')
            }
        })
    }

    // ── Stats computed (depends on groupList) ──
    const statsGroupCount = computed(() => Math.max(0, groupList.value.length - 1))

    return {
        // state
        groupList,
        displayGroupList,
        loadingGroups,
        groupPageSize,
        groupCurrentPage,
        groupPanelWidth,
        groupDialogVisible,
        groupDialogTitle,
        isEditGroup,
        groupFormRef,
        groupFormData,
        groupFormRules,
        // computed
        groupOptions,
        statsGroupCount,
        // filter
        groupFilterName,
        // actions
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
    }
}
