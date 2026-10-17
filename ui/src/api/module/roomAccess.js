import request from '@/utils/request'

// 查询实验室准入规则列表
export function listRoomAccess(query) {
  return request({
    url: '/module/roomAccess/list',
    method: 'get',
    params: query
  })
}

// 查询实验室准入规则详细
export function getRoomAccess(id) {
  return request({
    url: '/module/roomAccess/' + id,
    method: 'get'
  })
}

// 查询准入配置下拉数据
export function getRoomAccessOptions() {
  return request({
    url: '/module/roomAccess/options',
    method: 'get'
  })
}

// 新增实验室准入规则
export function addRoomAccess(data) {
  return request({
    url: '/module/roomAccess',
    method: 'post',
    data: data
  })
}

// 修改实验室准入规则
export function updateRoomAccess(data) {
  return request({
    url: '/module/roomAccess',
    method: 'put',
    data: data
  })
}

// 删除实验室准入规则
export function delRoomAccess(id) {
  return request({
    url: '/module/roomAccess/' + id,
    method: 'delete'
  })
}
