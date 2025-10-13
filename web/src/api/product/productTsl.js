import request from '@/utils/request'

// 获取产品物模型
export function getProductTsl(id) {
  return request({
    url: '/iot/product/productTsl/' + id,
    method: 'get'
  })
}

// 新增产品物模型
export function addProductTsl(productTsl) {
  return request({
    url: '/iot/product/productTsl',
    method: 'post',
    data: productTsl
  })
}

// 修改产品物模型
export function updateProductTsl(productTsl) {
  return request({
    url: '/iot/product/productTsl',
    method: 'put',
    data: productTsl
  })
}

// 删除产品物模型
export function deleteProductTsl(ids) {
  return request({
    url: '/iot/product/productTsl/' + ids,
    method: 'delete'
  })
}