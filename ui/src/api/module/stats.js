import request from '@/utils/request'

// 角色感知的数据看板
export function getStatsDashboard(params) {
  return request({
    url: '/module/stats/dashboard',
    method: 'get',
    params: params
  })
}
