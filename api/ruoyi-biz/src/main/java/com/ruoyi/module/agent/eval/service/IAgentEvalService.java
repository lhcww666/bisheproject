package com.ruoyi.module.agent.eval.service;

import java.util.List;

import com.ruoyi.module.agent.eval.domain.AgentEvalCase;
import com.ruoyi.module.agent.eval.domain.AgentEvalRun;
import com.ruoyi.module.agent.eval.domain.AgentEvalRunView;

/**
 * Agent 评测服务。
 *
 * @author itjialu
 */
public interface IAgentEvalService
{
    List<AgentEvalCase> listCases();

    AgentEvalRunView runOffline();

    List<AgentEvalRun> listRuns();

    AgentEvalRunView getRun(Long runId);
}
