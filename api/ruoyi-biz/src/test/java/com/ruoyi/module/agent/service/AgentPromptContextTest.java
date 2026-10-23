package com.ruoyi.module.agent.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AgentPromptContextTest
{
    @Test
    void currentTimeInstructionContainsServerDateAndWeekday()
    {
        String instruction = AgentPromptContext.currentTimeInstruction();
        assertTrue(instruction.contains("当前服务器时间："));
        assertTrue(instruction.contains("Asia/Shanghai"));
        assertTrue(instruction.contains("星期"));
    }
}
