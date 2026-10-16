import request from '@/utils/request'

// 查询安全考试记录列表
export function listSafetyExamRecord(query) {
  return request({
    url: '/module/safetyExamRecord/list',
    method: 'get',
    params: query
  })
}

// 查询安全考试记录详细
export function getSafetyExamRecord(id) {
  return request({
    url: '/module/safetyExamRecord/' + id,
    method: 'get'
  })
}

// 新增安全考试记录
export function addSafetyExamRecord(data) {
  return request({
    url: '/module/safetyExamRecord',
    method: 'post',
    data: data
  })
}

// 修改安全考试记录
export function updateSafetyExamRecord(data) {
  return request({
    url: '/module/safetyExamRecord',
    method: 'put',
    data: data
  })
}

// 删除安全考试记录
export function delSafetyExamRecord(id) {
  return request({
    url: '/module/safetyExamRecord/' + id,
    method: 'delete'
  })
}
