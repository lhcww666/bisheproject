import request from '@/utils/request'

// 查询 Agent 会话审计列表
export function listAgentAuditSessions(query) {
  return request({
    url: '/module/agent/audit/sessions',
    method: 'get',
    params: query
  })
}

// 查询某个会话的消息
export function getAgentAuditMessages(sessionId) {
  return request({
    url: '/module/agent/audit/sessions/' + sessionId + '/messages',
    method: 'get'
  })
}

// 查询工具调用审计
export function listAgentAuditToolCalls(query) {
  return request({
    url: '/module/agent/audit/tool-calls',
    method: 'get',
    params: query
  })
}
