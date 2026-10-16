import request from '@/utils/request'

// 查询预约设备关联列表
export function listReservationDevice(query) {
  return request({
    url: '/module/reservationDevice/list',
    method: 'get',
    params: query
  })
}

// 查询预约设备关联详细
export function getReservationDevice(id) {
  return request({
    url: '/module/reservationDevice/' + id,
    method: 'get'
  })
}

// 新增预约设备关联
export function addReservationDevice(data) {
  return request({
    url: '/module/reservationDevice',
    method: 'post',
    data: data
  })
}

// 修改预约设备关联
export function updateReservationDevice(data) {
  return request({
    url: '/module/reservationDevice',
    method: 'put',
    data: data
  })
}

// 删除预约设备关联
export function delReservationDevice(id) {
  return request({
    url: '/module/reservationDevice/' + id,
    method: 'delete'
  })
}
