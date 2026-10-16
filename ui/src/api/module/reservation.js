import request from '@/utils/request'

// 查询实验室预约列表
export function listReservation(query) {
  return request({
    url: '/module/reservation/list',
    method: 'get',
    params: query
  })
}

// 查询实验室预约详细
export function getReservation(id) {
  return request({
    url: '/module/reservation/' + id,
    method: 'get'
  })
}

// 新增实验室预约
export function addReservation(data) {
  return request({
    url: '/module/reservation',
    method: 'post',
    data: data
  })
}

// 修改实验室预约
export function updateReservation(data) {
  return request({
    url: '/module/reservation',
    method: 'put',
    data: data
  })
}

// 删除实验室预约
export function delReservation(id) {
  return request({
    url: '/module/reservation/' + id,
    method: 'delete'
  })
}
