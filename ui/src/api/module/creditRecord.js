import request from '@/utils/request'

// 查询信用分流水列表
export function listCreditRecord(query) {
  return request({
    url: '/module/creditRecord/list',
    method: 'get',
    params: query
  })
}

// 查询信用分流水详细
export function getCreditRecord(id) {
  return request({
    url: '/module/creditRecord/' + id,
    method: 'get'
  })
}

// 新增信用分流水
export function addCreditRecord(data) {
  return request({
    url: '/module/creditRecord',
    method: 'post',
    data: data
  })
}

// 修改信用分流水
export function updateCreditRecord(data) {
  return request({
    url: '/module/creditRecord',
    method: 'put',
    data: data
  })
}

// 删除信用分流水
export function delCreditRecord(id) {
  return request({
    url: '/module/creditRecord/' + id,
    method: 'delete'
  })
}
