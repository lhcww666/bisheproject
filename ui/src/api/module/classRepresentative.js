import request from '@/utils/request'

// 查询班级负责人列表
export function listClassRepresentative(query) {
  return request({
    url: '/module/classRepresentative/list',
    method: 'get',
    params: query
  })
}

// 查询班级负责人详细
export function getClassRepresentative(id) {
  return request({
    url: '/module/classRepresentative/' + id,
    method: 'get'
  })
}

// 新增班级负责人
export function addClassRepresentative(data) {
  return request({
    url: '/module/classRepresentative',
    method: 'post',
    data: data
  })
}

// 修改班级负责人
export function updateClassRepresentative(data) {
  return request({
    url: '/module/classRepresentative',
    method: 'put',
    data: data
  })
}

// 删除班级负责人
export function delClassRepresentative(id) {
  return request({
    url: '/module/classRepresentative/' + id,
    method: 'delete'
  })
}
