import request from '@/utils/request'

// 查询设备基本信息列表
export function listDevice(query) {
  return request({
    url: '/device/device/list',
    method: 'get',
    params: query
  })
}

// 查询设备基本信息详细
export function getDevice(id) {
  return request({
    url: '/device/device/' + id,
    method: 'get'
  })
}

// 新增设备基本信息
export function addDevice(data) {
  return request({
    url: '/device/device',
    method: 'post',
    data: data
  })
}

// 修改设备基本信息
export function updateDevice(data) {
  return request({
    url: '/device/device',
    method: 'put',
    data: data
  })
}

// 删除设备基本信息
export function delDevice(id) {
  return request({
    url: '/device/device/' + id,
    method: 'delete'
  })
}
