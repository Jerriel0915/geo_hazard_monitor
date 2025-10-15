import request from '@/utils/request'

// 查询监测对象基本信息表列表
export function listMonitoringObject(query) {
  return request({
    url: '/monitor/object/list',
    method: 'get',
    params: query
  })
}

// 查询监测对象基本信息表详细信息
export function getMonitoringObject(id) {
  return request({
    url: '/monitor/object/' + id,
    method: 'get'
  })
}

// 新增监测对象基本信息表
export function addMonitoringObject(data) {
  return request({
    url: '/monitor/object',
    method: 'post',
    data: data
  })
}

// 修改监测对象基本信息表
export function updateMonitoringObject(data) {
  return request({
    url: '/monitor/object',
    method: 'put',
    data: data
  })
}

// 删除监测对象基本信息表
export function delMonitoringObject(id) {
  return request({
    url: '/monitor/object/' + id,
    method: 'delete'
  })
}

// 导出监测对象基本信息表
export function exportMonitoringObject(query) {
  return request({
    url: '/monitor/object/export',
    method: 'post',
    params: query
  })
}