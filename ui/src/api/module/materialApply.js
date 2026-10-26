import request from '@/utils/request'

export function listMaterialApply(query) {
  return request({
    url: '/module/material/apply/list',
    method: 'get',
    params: query
  })
}

export function getMaterialApply(id) {
  return request({
    url: '/module/material/apply/' + id,
    method: 'get'
  })
}

export function applyMaterial(data) {
  return request({
    url: '/module/material/apply',
    method: 'post',
    data
  })
}

export function reviewMaterialApply(id, data) {
  return request({
    url: '/module/material/apply/' + id + '/review',
    method: 'post',
    data
  })
}

export function listMaterialReservationOptions() {
  return request({
    url: '/module/material/apply/reservationOptions',
    method: 'get'
  })
}

export function listAvailableMaterials(reservationId) {
  return request({
    url: '/module/material/apply/materialOptions',
    method: 'get',
    params: { reservationId }
  })
}
