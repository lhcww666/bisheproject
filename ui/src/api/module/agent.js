import request from '@/utils/request'

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
