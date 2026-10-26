import request from '@/utils/request'

export function listMaterialStockRecord(query) {
  return request({
    url: '/module/material/stock-record/list',
    method: 'get',
    params: query
  })
}

export function getMaterialStockRecord(id) {
  return request({
    url: '/module/material/stock-record/' + id,
    method: 'get'
  })
}
