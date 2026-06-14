import {computed, ref, type Ref} from 'vue'

/**
 * 表格排序 composable
 *
 * 用法:
 *   const sort = useTableSort()
 *   // 在列头放 TableSortHeader，点击触发 sort.toggle(field)
 *   // 用 sort.sorted(list) 获取排序后数据
 *
 * 可选传入初始排序字段（如 'code'）。
 */
export function useTableSort(initialField?: string) {
    const sortField = ref(initialField || '')
    const sortOrder = ref<'asc' | 'desc' | ''>('')

    /** 切换: 同一列在 asc → desc → none 循环 */
    function toggle(field: string) {
        if (sortField.value !== field) {
            sortField.value = field
            sortOrder.value = 'asc'
        } else if (sortOrder.value === 'asc') {
            sortOrder.value = 'desc'
        } else {
            sortField.value = ''
            sortOrder.value = ''
        }
    }

    /** 对数组做本地排序（不改变原数组） */
    function sorted<T extends Record<string, any>>(list: T[]): T[] {
        if (!sortField.value || !sortOrder.value) return list
        const dir = sortOrder.value === 'asc' ? 1 : -1
        return [...list].sort((a, b) => {
            const va = a[sortField.value]
            const vb = b[sortField.value]
            // null/undefined 排末尾
            if (va == null && vb == null) return 0
            if (va == null) return 1
            if (vb == null) return -1
            // 数字按数值比
            if (typeof va === 'number' && typeof vb === 'number') return (va - vb) * dir
            // 其他按字符串比
            return String(va).localeCompare(String(vb)) * dir
        })
    }

    /** 当前排序信息，用于组件 props */
    const sortInfo = computed(() => ({field: sortField.value, order: sortOrder.value}))

    return {sortField, sortOrder, sortInfo, toggle, sorted}
}
