import request from '@/utils/request'

// 查询违规记录列表
export function listViolationRecord(query) {
  return request({
    url: '/module/violationRecord/list',
    method: 'get',
    params: query
  })
}

// 查询违规记录详情
export function getViolationRecord(id) {
  return request({
    url: '/module/violationRecord/' + id,
    method: 'get'
  })
}

// 管理员登记违规
export function addViolationRecord(data) {
  return request({
    url: '/module/violationRecord',
    method: 'post',
    data: data
  })
}

// 撤销违规并回补信用分
export function revokeViolationRecord(id, reason) {
  return request({
    url: '/module/violationRecord/' + id + '/revoke',
    method: 'post',
    data: { reason }
  })
}
