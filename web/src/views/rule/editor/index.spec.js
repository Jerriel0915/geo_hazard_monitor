import {mount} from '@vue/test-utils'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import RuleEditor from './index.vue'
import ProductDeviceSelector from './components/ProductDeviceSelector.vue'
import {getProductTsl} from '@/api/product/productTsl'
import {addRule, getRule, testRule, updateRule, validateRuleExpression} from '@/api/rule/rule'

// Mock APIs
vi.mock('@/api/product/productTsl')
vi.mock('@/api/rule/rule')
vi.mock('@/api/product/product', () => ({
    listProduct: vi.fn().mockResolvedValue({rows: [{id: 1, name: 'Product A'}]})
}))
vi.mock('@/api/device/device', () => ({
    listDevice: vi.fn().mockResolvedValue({rows: [{id: 101, name: 'Device X', productId: 1}]})
}))

describe('RuleEditor', () => {
    let wrapper

    beforeEach(() => {
        vi.clearAllMocks()
        getProductTsl.mockResolvedValue({
            data: {
                tsl: {
                    properties: [
                        {identifier: 'temp', name: 'Temperature', dataType: {type: 'float', specs: {min: 0, max: 100}}},
                        {
                            identifier: 'status',
                            name: 'Status',
                            dataType: {type: 'bool', specs: {'0': 'Off', '1': 'On'}}
                        },
                        {identifier: 'msg', name: 'Message', dataType: {type: 'text', specs: {length: 200}}}
                    ]
                }
            }
        })
        addRule.mockResolvedValue({})
        updateRule.mockResolvedValue({})
        getRule.mockResolvedValue({data: {}})
        validateRuleExpression.mockResolvedValue({})
        testRule.mockResolvedValue({data: {match: true}})
    })

    it('renders correctly', () => {
        wrapper = mount(RuleEditor)
        expect(wrapper.find('.el-page-header__content').text()).toBe('规则编辑器')
        expect(wrapper.findComponent(ProductDeviceSelector).exists()).toBe(true)
    })

    it('loads TSL and system properties when product selected', async () => {
        wrapper = mount(RuleEditor)
        const selector = wrapper.findComponent(ProductDeviceSelector)

        // Simulate selection
        await selector.vm.$emit('change', [{type: 'product', id: 1, name: 'Product A'}])

        expect(getProductTsl).toHaveBeenCalledWith(1)
        // Wait for async TSL load
        await new Promise(resolve => setTimeout(resolve, 0))

        // 5 system properties + 3 TSL properties = 8
        expect(wrapper.vm.properties).toHaveLength(8)
        const sysProp = wrapper.vm.properties.find(p => p.identifier === 'reportTime')
        expect(sysProp).toBeDefined()
        expect(sysProp.isSystem).toBe(true)
    })

    it('filters operators based on data type', async () => {
        wrapper = mount(RuleEditor)

        // Float type should have numeric operators
        const floatOps = wrapper.vm.getOperators('float')
        expect(floatOps.map(o => o.value)).toContain('>')
        expect(floatOps.map(o => o.value)).toContain('<')
        expect(floatOps.map(o => o.value)).not.toContain('contains')

        // Text type should have string operators
        const textOps = wrapper.vm.getOperators('text')
        expect(textOps.map(o => o.value)).toContain('==')
        expect(textOps.map(o => o.value)).toContain('contains')
        expect(textOps.map(o => o.value)).toContain('regex')
        expect(textOps.map(o => o.value)).not.toContain('>')

        // Date type
        const dateOps = wrapper.vm.getOperators('date')
        expect(dateOps.map(o => o.value)).toContain('>')
        expect(dateOps.map(o => o.value)).toContain('empty')
    })

    it('builds expression correctly for various predicates', async () => {
        wrapper = mount(RuleEditor)
        wrapper.vm.form.conditions = [
            {field: 'temp', op: '>', value: 50, dataType: 'float'},
            {field: 'status', op: '==', value: 1, dataType: 'bool'},
            {field: 'msg', op: 'contains', value: 'error', dataType: 'text'},
            {field: 'msg', op: 'regex', value: '^err.*', dataType: 'text'},
            {field: 'temp', op: 'in', value: '10,20,30', dataType: 'float'},
            {field: 'msg', op: 'empty', value: '', dataType: 'text'}
        ]
        wrapper.vm.form.logical = 'AND'

        const expr = wrapper.vm.buildExpression()

        expect(expr).toContain('temp > 50')
        expect(expr).toContain('status == 1')
        expect(expr).toContain("string.contains(str(msg), 'error')")
        expect(expr).toContain("str(msg) =~ '^err.*'")
        expect(expr).toContain("include(seq.set(10,20,30), temp)")
        expect(expr).toContain("string.length(str(msg)) == 0")
    })

    it('handles field change and resets operator', async () => {
        wrapper = mount(RuleEditor)
        // Pre-load properties
        wrapper.vm.properties = [
            {identifier: 'temp', dataType: 'float'},
            {identifier: 'msg', dataType: 'text'}
        ]

        const condition = {field: 'temp', op: '>', value: 50, dataType: 'float'}

        // Change field to 'msg' (text)
        wrapper.vm.handleFieldChange('msg', condition)

        expect(condition.dataType).toBe('text')
        expect(condition.value).toBeUndefined()
        // Should auto-select first valid op for text (usually ==)
        expect(condition.op).toBe('==')
    })

    it('validates and saves rule', async () => {
        wrapper = mount(RuleEditor)
        wrapper.vm.form.name = 'Test Rule'
        wrapper.vm.form.productId = 1
        wrapper.vm.form.ruleExpression = 'temp > 50'

        await wrapper.vm.onSave()

        expect(validateRuleExpression).toHaveBeenCalled()
        expect(addRule).toHaveBeenCalled()
    })

    it('correctly handles product selection and updates form validation', async () => {
        wrapper = mount(RuleEditor)

        // Mock validateField on the form ref
        const validateFieldMock = vi.fn()
        // Need to set step1Form ref after mount
        wrapper.vm.step1Form = {validateField: validateFieldMock, validate: vi.fn()}

        // Simulate selection from ProductDeviceSelector
        const selectedProduct = {type: 'product', id: 123, name: 'Product 123'}
        await wrapper.vm.handleSelectorChange([selectedProduct])

        // Check form model update
        expect(wrapper.vm.form.productId).toBe('123')
        expect(wrapper.vm.form.productName).toBe('Product 123')

        // Check if validation was triggered (The fix)
        expect(validateFieldMock).toHaveBeenCalledWith('productId')

        // Check if TSL was loaded
        expect(getProductTsl).toHaveBeenCalledWith('123')
    })
})
