import request from '@/utils/request'

/**
 * Agent 离线评测接口。
 * 当前为确定性离线路由与 RAG 检索评测，不依赖模型 API Key。
 */

// 查询启用的评测用例
export function listAgentEvalCases() {
  return request({
    url: '/module/agent/eval/cases',
    method: 'get'
  })
}

// 执行一次离线评测
export function runAgentEval() {
  return request({
    url: '/module/agent/eval/run',
    method: 'post'
  })
}

// 查询最近评测批次
export function listAgentEvalRuns() {
  return request({
    url: '/module/agent/eval/runs',
    method: 'get'
  })
}

// 查询批次指标和逐条结果
export function getAgentEvalRun(runId) {
  return request({
    url: '/module/agent/eval/runs/' + runId,
    method: 'get'
  })
}
