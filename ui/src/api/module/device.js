import request from '@/utils/request'

// 查询设备信息列表
export function listDevice(query) {
  return request({
    url: '/module/device/list',
    method: 'get',
    params: query
  })
}

// 查询设备信息详细
export function getDevice(id) {
  return request({
    url: '/module/device/' + id,
    method: 'get'
  })
}

// 新增设备信息
export function addDevice(data) {
  return request({
    url: '/module/device',
    method: 'post',
    data: data
  })
}

// 修改设备信息
export function updateDevice(data) {
  return request({
    url: '/module/device',
    method: 'put',
    data: data
  })
}

// 删除设备信息
export function delDevice(id) {
  return request({
    url: '/module/device/' + id,
    method: 'delete'
  })
}
