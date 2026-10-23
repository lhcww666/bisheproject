package com.ruoyi.module.agent.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.module.agent.domain.ChatRequest;

/**
 * 智能助手高成本接口限流约束测试。
 *
 * @author itjialu
 */
class AgentControllerRateLimitTest
{
    @Test
    void chatAndModelCheckHaveIndependentIpLimits()
            throws NoSuchMethodException
    {
        RateLimiter chat = AgentController.class.getMethod("chat", ChatRequest.class)
                .getAnnotation(RateLimiter.class);
        RateLimiter modelCheck = AgentController.class.getMethod("modelCheck")
                .getAnnotation(RateLimiter.class);

        assertNotNull(chat);
        assertEquals(12, chat.count());
        assertEquals(60, chat.time());
        assertEquals(LimitType.IP, chat.limitType());

        assertNotNull(modelCheck);
        assertEquals(5, modelCheck.count());
        assertEquals(60, modelCheck.time());
        assertEquals(LimitType.IP, modelCheck.limitType());
    }
}
