import request from '@/utils/request'

// 查询预约审核列表
export function listReservationReview(query) {
  return request({
    url: '/module/reservationReview/list',
    method: 'get',
    params: query
  })
}

// 查询预约审核详细
export function getReservationReview(id) {
  return request({
    url: '/module/reservationReview/' + id,
    method: 'get'
  })
}

// 新增预约审核
export function addReservationReview(data) {
  return request({
    url: '/module/reservationReview',
    method: 'post',
    data: data
  })
}

// 修改预约审核
export function updateReservationReview(data) {
  return request({
    url: '/module/reservationReview',
    method: 'put',
    data: data
  })
}

// 删除预约审核
export function delReservationReview(id) {
  return request({
    url: '/module/reservationReview/' + id,
    method: 'delete'
  })
}
