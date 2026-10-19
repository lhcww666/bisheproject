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

// 学生提交预约申请
export function applyReservation(data) {
  return request({
    url: '/module/reservation/apply',
    method: 'post',
    data: data
  })
}

// 取消本人预约
export function cancelReservation(id) {
  return request({
    url: '/module/reservation/' + id + '/cancel',
    method: 'post'
  })
}

// 查询指定时段可用实验室
export function listAvailableRooms(params) {
  return request({
    url: '/module/reservation/availableRooms',
    method: 'get',
    params: params
  })
}

// 查询实验室中可预约的设备
export function listAvailableDevices(params) {
  return request({
    url: '/module/reservation/availableDevices',
    method: 'get',
    params: params
  })
}

// 查询当前学生是否为本班班级负责人
export function getMyClassRepresentative() {
  return request({
    url: '/module/reservation/myClassRepresentative',
    method: 'get'
  })
}

// 统一审核/弃审：action=APPROVE / REJECT / UNAUDIT
export function updateReservationStatus(data) {
  return request({
    url: '/module/reservation/updateStatus',
    method: 'post',
    data: data
  })
}
