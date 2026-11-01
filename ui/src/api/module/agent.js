import request from '@/utils/request'
import { getToken } from '@/utils/auth'

export function getAgentStatus() {
  return request({
    url: '/module/agent/status',
    method: 'get'
  })
}

export function listAgentSessions() {
  return request({
    url: '/module/agent/sessions',
    method: 'get'
  })
}

export function createAgentSession(data = {}) {
  return request({
    url: '/module/agent/sessions',
    method: 'post',
    data
  })
}

export function getAgentMessages(sessionId) {
  return request({
    url: `/module/agent/sessions/${sessionId}/messages`,
    method: 'get'
  })
}

export function deleteAgentSession(sessionId) {
  return request({
    url: `/module/agent/sessions/${sessionId}`,
    method: 'delete'
  })
}

export function sendAgentChat(data) {
  return request({
    url: '/module/agent/chat',
    method: 'post',
    data
  })
}

export async function sendAgentChatStream(data, handlers = {}) {
  const response = await fetch(
    `${import.meta.env.VITE_APP_BASE_API}/module/agent/chat/stream`,
    {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${getToken()}`,
        'Content-Type': 'application/json',
        Accept: 'text/event-stream'
      },
      body: JSON.stringify(data)
    }
  )

  const contentType = response.headers.get('content-type') || ''
  if (!response.ok || contentType.includes('application/json')) {
    const payload = await response.json().catch(() => ({}))
    throw new Error(payload.msg || payload.message || '流式对话请求失败')
  }
  if (!response.body) {
    throw new Error('浏览器不支持流式响应')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let completed = false

  while (true) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
    const blocks = buffer.split(/\r?\n\r?\n/)
    buffer = blocks.pop() || ''

    for (const block of blocks) {
      const event = parseSseBlock(block)
      if (!event.data) continue
      let payload
      try {
        payload = JSON.parse(event.data)
      } catch {
        continue
      }
      if (payload.type === 'tool') handlers.onTool?.(payload)
      else if (payload.type === 'citation') handlers.onCitation?.(payload)
      else if (payload.type === 'message.delta') handlers.onDelta?.(payload)
      else if (payload.type === 'done') {
        completed = true
        handlers.onDone?.(payload)
      } else if (payload.type === 'error') {
        completed = true
        handlers.onError?.(payload)
      }
    }

    if (done) break
  }

  if (!completed) {
    throw new Error('流式响应意外结束')
  }
}

function parseSseBlock(block) {
  let event = 'message'
  const data = []
  for (const line of block.split(/\r?\n/)) {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      data.push(line.slice(5).trim())
    }
  }
  return { event, data: data.join('\n') }
}

export function getReservationDraft(draftId) {
  return request({
    url: `/module/agent/drafts/${draftId}`,
    method: 'get'
  })
}

export function cancelReservationDraft(draftId) {
  return request({
    url: `/module/agent/drafts/${draftId}/cancel`,
    method: 'post'
  })
}

export function confirmReservationDraft(draftId) {
  return request({
    url: `/module/agent/drafts/${draftId}/confirm`,
    method: 'post'
  })
}
