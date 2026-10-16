import request from '@/utils/request'

// 查询实验室信息列表
export function listRoom(query) {
  return request({
    url: '/module/room/list',
    method: 'get',
    params: query
  })
}

// 查询实验室信息详细
export function getRoom(id) {
  return request({
    url: '/module/room/' + id,
    method: 'get'
  })
}

// 新增实验室信息
export function addRoom(data) {
  return request({
    url: '/module/room',
    method: 'post',
    data: data
  })
}

// 修改实验室信息
export function updateRoom(data) {
  return request({
    url: '/module/room',
    method: 'put',
    data: data
  })
}

// 删除实验室信息
export function delRoom(id) {
  return request({
    url: '/module/room/' + id,
    method: 'delete'
  })
}
