import request from '@/utils/request'

// 查询题库列表
export function listSafetyQuestion(query) {
  return request({
    url: '/module/safetyQuestion/list',
    method: 'get',
    params: query
  })
}

// 查询题目详情（含选项和答案）
export function getSafetyQuestion(id) {
  return request({
    url: '/module/safetyQuestion/' + id,
    method: 'get'
  })
}

// 新增题目
export function addSafetyQuestion(data) {
  return request({
    url: '/module/safetyQuestion',
    method: 'post',
    data: data
  })
}

// 修改题目
export function updateSafetyQuestion(data) {
  return request({
    url: '/module/safetyQuestion',
    method: 'put',
    data: data
  })
}

// 删除题目
export function delSafetyQuestion(id) {
  return request({
    url: '/module/safetyQuestion/' + id,
    method: 'delete'
  })
}
