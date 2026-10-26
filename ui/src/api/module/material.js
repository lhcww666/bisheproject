import request from '@/utils/request'

export function listMaterial(query) {
  return request({
    url: '/module/material/list',
    method: 'get',
    params: query
  })
}

export function getMaterial(id) {
  return request({
    url: '/module/material/' + id,
    method: 'get'
  })
}

export function addMaterial(data) {
  return request({
    url: '/module/material',
    method: 'post',
    data
  })
}

export function updateMaterial(data) {
  return request({
    url: '/module/material',
    method: 'put',
    data
  })
}

export function delMaterial(ids) {
  return request({
    url: '/module/material/' + ids,
    method: 'delete'
  })
}

export function adjustMaterialStock(id, data) {
  return request({
    url: '/module/material/' + id + '/stock-adjust',
    method: 'post',
    data
  })
}

export function listMaterialRoomOptions() {
  return request({
    url: '/module/material/roomOptions',
    method: 'get'
  })
}
