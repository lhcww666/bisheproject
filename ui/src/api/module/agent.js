import request from '@/utils/request'
import { getToken } from '@/utils/auth'

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

// 发起一次最小模型请求，检查 Key、网络和 DeepSeek 接口是否可用
export function checkAgentModel() {
  return request({
    url: '/module/agent/model-check',
    method: 'post'
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

/**
 * 以 SSE 读取 Agent 回复。
 * 使用 fetch 是因为原生 EventSource 不支持 POST 请求体和 Authorization 请求头。
 */
export async function streamAgentChat(data, handlers = {}) {
  const baseUrl = import.meta.env.VITE_APP_BASE_API || ''
  const response = await fetch(baseUrl + '/module/agent/chat/stream', {
    method: 'POST',
    headers: {
      Accept: 'text/event-stream',
      Authorization: 'Bearer ' + getToken(),
      'Content-Type': 'application/json;charset=UTF-8'
    },
    body: JSON.stringify(data)
  })
  const contentType = response.headers.get('content-type') || ''
  if (!response.ok || !contentType.includes('text/event-stream')) {
    const body = await response.text()
    let message = body
    try {
      const json = JSON.parse(body)
      message = json.message || json.msg || body
    } catch {
      // 非 JSON 响应直接使用原始文本。
    }
    throw new Error(message || '流式接口请求失败')
  }
  if (!response.body) {
    throw new Error('当前浏览器不支持流式响应')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let doneData = null
  const dispatch = block => {
    let eventName = 'message'
    const dataLines = []
    block.split(/\r?\n/).forEach(line => {
      if (line.startsWith('event:')) eventName = line.slice(6).trim()
      if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
    })
    if (!dataLines.length) return
    const raw = dataLines.join('\n')
    let payload = raw
    try {
      payload = JSON.parse(raw)
    } catch {
      // 兼容纯文本 SSE 数据。
    }
    if (eventName === 'thinking') handlers.onThinking?.(payload)
    if (eventName === 'chunk') handlers.onChunk?.(payload.text || '')
    if (eventName === 'done') {
      doneData = payload
      handlers.onDone?.(payload)
    }
    if (eventName === 'error') {
      throw new Error(payload.message || '智能助手暂时不可用')
    }
  }

  while (true) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
    const blocks = buffer.split(/\r?\n\r?\n/)
    buffer = blocks.pop() || ''
    blocks.forEach(dispatch)
    if (done) break
  }
  buffer += decoder.decode()
  if (buffer.trim()) dispatch(buffer)
  return doneData
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

// 用户点击确认后提交预约草稿，后端会重新执行完整业务校验
export function confirmAgentDraft(draftId) {
  return request({
    url: '/module/agent/drafts/' + draftId + '/confirm',
    method: 'post'
  })
}

// 取消尚未确认的草稿
export function cancelAgentDraft(draftId) {
  return request({
    url: '/module/agent/drafts/' + draftId + '/cancel',
    method: 'post'
  })
}

// 查询会话中待确认的预约草稿，页面刷新后用于恢复草稿卡片
export function listPendingAgentDrafts(sessionId) {
  return request({
    url: '/module/agent/drafts/pending',
    method: 'get',
    params: sessionId ? { sessionId } : {}
  })
}

// 管理员知识库管理：文档
export function listKnowledgeDocuments(params) {
  return request({
    url: '/module/agent/knowledge/admin/documents',
    method: 'get',
    params
  })
}

export function getKnowledgeDocument(id) {
  return request({
    url: '/module/agent/knowledge/admin/documents/' + id,
    method: 'get'
  })
}

export function addKnowledgeDocument(data) {
  return request({
    url: '/module/agent/knowledge/admin/documents',
    method: 'post',
    data
  })
}

export function updateKnowledgeDocument(data) {
  return request({
    url: '/module/agent/knowledge/admin/documents',
    method: 'put',
    data
  })
}

export function deleteKnowledgeDocument(id) {
  return request({
    url: '/module/agent/knowledge/admin/documents/' + id,
    method: 'delete'
  })
}

// 管理员知识库管理：知识片段
export function listKnowledgeChunks(params) {
  return request({
    url: '/module/agent/knowledge/admin/chunks',
    method: 'get',
    params
  })
}

export function addKnowledgeChunk(data) {
  return request({
    url: '/module/agent/knowledge/admin/chunks',
    method: 'post',
    data
  })
}

export function updateKnowledgeChunk(data) {
  return request({
    url: '/module/agent/knowledge/admin/chunks',
    method: 'put',
    data
  })
}

export function deleteKnowledgeChunk(id) {
  return request({
    url: '/module/agent/knowledge/admin/chunks/' + id,
    method: 'delete'
  })
}
