import request from '@/utils/request'

// 助手配置状态
export function getAssistantStatus() {
  return request({
    url: '/module/assistant/status',
    method: 'get'
  })
}

// 当前用户的会话列表
export function listAssistantSessions() {
  return request({
    url: '/module/assistant/sessions',
    method: 'get'
  })
}

// 新建会话
export function createAssistantSession(title) {
  return request({
    url: '/module/assistant/sessions',
    method: 'post',
    params: title ? { title } : {}
  })
}

// 查询会话消息
export function listAssistantMessages(sessionId) {
  return request({
    url: '/module/assistant/sessions/' + sessionId + '/messages',
    method: 'get'
  })
}

// 发送消息
export function chatWithAssistant(data) {
  return request({
    url: '/module/assistant/chat',
    method: 'post',
    data: data
  })
}

// 删除会话
export function deleteAssistantSession(sessionId) {
  return request({
    url: '/module/assistant/sessions/' + sessionId,
    method: 'delete'
  })
}
