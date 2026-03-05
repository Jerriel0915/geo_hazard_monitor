import {mount} from '@vue/test-utils'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import ProductDeviceSelector from './ProductDeviceSelector.vue'
import {listProduct} from '@/api/product/product'
import {listDevice} from '@/api/device/device'

// Mock APIs
vi.mock('@/api/product/product', () => ({
    listProduct: vi.fn()
}))
vi.mock('@/api/device/device', () => ({
    listDevice: vi.fn()
}))

describe('ProductDeviceSelector', () => {
    let wrapper

    beforeEach(() => {
        vi.clearAllMocks()
        listProduct.mockResolvedValue({
            rows: [
                {id: 1, name: 'Product A'},
                {id: 2, name: 'Product B'}
            ]
        })
        listDevice.mockResolvedValue({
            rows: [
                {id: 101, name: 'Device X', productId: 1},
                {id: 102, name: 'Device Y', productId: 1},
                {id: 201, name: 'Device Z', productId: 2}
            ]
        })
    })

    it('renders correctly', () => {
        wrapper = mount(ProductDeviceSelector)
        expect(wrapper.find('.el-tree-select').exists()).toBe(true)
    })

    it('loads data on mount', async () => {
        wrapper = mount(ProductDeviceSelector)
        await new Promise(resolve => setTimeout(resolve, 0))

        expect(listProduct).toHaveBeenCalled()
        expect(listDevice).toHaveBeenCalled()

        // Check tree data structure
        expect(wrapper.vm.treeData).toHaveLength(2)
        expect(wrapper.vm.treeData[0].children).toHaveLength(2)
        expect(wrapper.vm.treeData[1].children).toHaveLength(1)
    })

    it('filters nodes', async () => {
        wrapper = mount(ProductDeviceSelector)
        await new Promise(resolve => setTimeout(resolve, 0))

        wrapper.vm.filterText = 'Device X'
        await wrapper.vm.$nextTick()

        // Check filter logic
        expect(wrapper.vm.filterNode('Device X', {name: 'Device X'})).toBe(true)
        expect(wrapper.vm.filterNode('Device X', {name: 'Device Y'})).toBe(false)
    })
})
