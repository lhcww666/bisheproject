package com.ruoyi.module.agent.eval.domain;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评测批次及明细。
 *
 * @author itjialu
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentEvalRunView extends AgentEvalRun
{
    private List<AgentEvalResult> results;
}
