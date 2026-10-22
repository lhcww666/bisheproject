import request from '@/utils/request'

// 查询设备档案列表
export function listDevice(query) {
  return request({
    url: '/module/device/list',
    method: 'get',
    params: query
  })
}

// 查询设备档案详细
export function getDevice(id) {
  return request({
    url: '/module/device/' + id,
    method: 'get'
  })
}

// 查询设备表单可选的实验室
export function listDeviceRoomOptions() {
  return request({
    url: '/module/device/roomOptions',
    method: 'get'
  })
}

// 新增设备档案
export function addDevice(data) {
  return request({
    url: '/module/device',
    method: 'post',
    data: data
  })
}

// 修改设备档案
export function updateDevice(data) {
  return request({
    url: '/module/device',
    method: 'put',
    data: data
  })
}

// 删除设备档案
export function delDevice(id) {
  return request({
    url: '/module/device/' + id,
    method: 'delete'
  })
}
