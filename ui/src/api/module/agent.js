import request from '@/utils/request'

/**
 * 智能助手前端接口。
 * 页面只调用这些接口，不直接拼接后端 URL，便于后续切换流式接口。
 */

// 查询模型是否已配置，用于页面顶部的状态标签
export function getAgentStatus() {
  return request({
    url: '/module/agent/status',
    method: 'get'
  })
}

// 当前用户的会话列表
export function listAgentSessions() {
  return request({
    url: '/module/agent/sessions',
    method: 'get'
  })
}

// 新建会话
export function createAgentSession(title) {
  return request({
    url: '/module/agent/sessions',
    method: 'post',
    params: title ? { title } : {}
  })
}

// 查询会话消息
export function listAgentMessages(sessionId) {
  return request({
    url: '/module/agent/sessions/' + sessionId + '/messages',
    method: 'get'
  })
}

// 发送消息
export function chatWithAgent(data) {
  return request({
    url: '/module/agent/chat',
    method: 'post',
    data: data
  })
}

// 删除会话
export function deleteAgentSession(sessionId) {
  return request({
    url: '/module/agent/sessions/' + sessionId,
    method: 'delete'
  })
}

// 直接检索安全知识库，便于在没有模型 Key 时验证 RAG 召回结果
export function searchAgentKnowledge(q, topK = 3) {
  return request({
    url: '/module/agent/knowledge/search',
    method: 'get',
    params: { q, topK }
  })
}
