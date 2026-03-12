import request from '@/utils/request'

export function listRule(query) {
    return request({
        url: '/iot/rule/list',
        method: 'get',
        params: query
    })
}

export function getRule(ruleId) {
    return request({
        url: '/iot/rule/' + ruleId,
        method: 'get'
    })
}

export function addRule(data) {
    return request({
        url: '/iot/rule',
        method: 'post',
        data
    })
}

export function updateRule(data) {
    return request({
        url: '/iot/rule',
        method: 'put',
        data
    })
}

export function deleteRule(ids) {
    return request({
        url: '/iot/rule/' + ids,
        method: 'delete'
    })
}

// 以下为扩展接口，需后端支持
export function validateRuleExpression(body) {
    return request({
        url: '/iot/rule/validate',
        method: 'post',
        data: body
    })
}

export function testRule(body) {
    return request({
        url: '/iot/rule/test',
        method: 'post',
        data: body
    })
}

