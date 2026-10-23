import request from '@/utils/request'

/**
 * Agent 评测接口。
 * 支持确定性离线回归，以及调用真实模型和工具链的 LLM 评测。
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

// 使用当前角色可用的真实模型和工具执行评测
export function runAgentLlmEval() {
  return request({
    url: '/module/agent/eval/run-llm',
    method: 'post',
    timeout: 600000
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
